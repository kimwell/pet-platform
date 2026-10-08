package com.pet.platform.identity.application.authentication;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.pet.platform.identity.application.PasswordService;
import java.util.UUID;

/** 受限认证内部对象，禁止响应序列化；没有凭据getter。 */
@JsonIgnoreType @JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.NONE,getterVisibility=JsonAutoDetect.Visibility.NONE,isGetterVisibility=JsonAutoDetect.Visibility.NONE)
public final class AuthenticationCandidate {
    private final UUID tenantId,employeeId;
    private final String passwordHash,status;
    private final long securityVersion,authorizationVersion,tenantSecurityVersion;
    public AuthenticationCandidate(UUID tenantId,UUID employeeId,String passwordHash,String status,long securityVersion,long authorizationVersion,long tenantSecurityVersion) {
        this.tenantId=tenantId;this.employeeId=employeeId;this.passwordHash=passwordHash;this.status=status;
        this.securityVersion=securityVersion;this.authorizationVersion=authorizationVersion;this.tenantSecurityVersion=tenantSecurityVersion;
    }
    public UUID tenantId() { return tenantId; }
    public UUID employeeId() { return employeeId; }
    public boolean accepts(PasswordService passwords,char[] password) { return passwords.matches(password,passwordHash) && "ACTIVE".equals(status); }
    public boolean sameSecurityState(StaffIdentity identity) { return identity.securityVersion()==securityVersion && identity.authorizationVersion()==authorizationVersion && identity.tenantSecurityVersion()==tenantSecurityVersion; }
    @Override public String toString() { return "AuthenticationCandidate[受限凭据]"; }
}
