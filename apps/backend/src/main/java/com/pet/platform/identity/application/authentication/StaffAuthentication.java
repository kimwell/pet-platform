package com.pet.platform.identity.application.authentication;

import com.pet.platform.identity.application.IdentityNames;
import com.pet.platform.identity.application.PasswordService;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** 仅认证接入契约，密码比较在数据库查询之外；本轮不签发会话或信任请求tenantId。 */
@Service
public final class StaffAuthentication {
    private final IdentityLookup lookup;
    private final PasswordService passwords;
    private final String dummyHash;
    public StaffAuthentication(IdentityLookup lookup,PasswordService passwords) {
        this.lookup=lookup;this.passwords=passwords;
        // 随机运行时不存在账号的等成本比较值，不是固定凭据或默认账号。
        char[] dummy=UUID.randomUUID().toString().toCharArray();
        try { dummyHash=passwords.hash(dummy); } finally { java.util.Arrays.fill(dummy,'\0'); }
    }
    public Optional<AuthenticationCandidate> findCandidate(String tenantCode,String login) {
        try { return lookup.candidate(IdentityNames.tenantCode(tenantCode),IdentityNames.loginName(login)); }
        catch (IllegalArgumentException invalid) { return Optional.empty(); }
    }
    public Optional<StaffIdentity> verifyCredentials(String tenantCode,String login,char[] password) {
        var candidate=findCandidate(tenantCode,login);
        if (candidate.isEmpty()) { passwords.matches(password,dummyHash); return Optional.empty(); }
        var fact=candidate.orElseThrow();
        if (!fact.accepts(passwords,password)) return Optional.empty();
        // 哈希期间状态发生变化时失败；后续签发仍需session安全版本链路。
        return lookup.load(fact.tenantId(),fact.employeeId()).filter(fact::sameSecurityState);
    }
    public Optional<StaffIdentity> loadForSession(UUID trustedTenantId,UUID employeeId) { return lookup.load(trustedTenantId,employeeId); }
}
