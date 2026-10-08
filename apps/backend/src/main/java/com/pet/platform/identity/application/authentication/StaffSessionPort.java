package com.pet.platform.identity.application.authentication;

import com.pet.platform.shared.security.CurrentPrincipal;
import java.time.Instant;
import java.util.UUID;

/** 身份模块的会话端口，业务模块不接触Sa-Token。 */
public interface StaffSessionPort {
    enum Channel { WEB, MINIPROGRAM }
    record SessionFact(UUID sessionId,UUID tenantId,UUID employeeId,long securityVersion,long tenantSecurityVersion,
            Channel channel,Instant expiresAt,int idleTimeoutSeconds,String csrfToken) {
        @Override public String toString(){return "SessionFact[受限会话]";}
    }
    record Issued(String token,SessionFact session,StaffIdentity identity) { @Override public String toString(){return "Issued[受限凭据]";} }
    Issued create(StaffIdentity identity,Channel channel);
    SessionFact verify(String token,Channel channel);
    void touch(String token);
    void logout(String token);
    void limit(String ip,String normalizedAccount);
    String createPre(String csrf);
    String readPre(String pre);
    long preTtl(String pre);
    void deletePre(String pre);
}
