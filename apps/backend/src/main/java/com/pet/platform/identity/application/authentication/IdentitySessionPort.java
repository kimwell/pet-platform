package com.pet.platform.identity.application.authentication;
import java.time.Instant;
import java.util.UUID;
/** 两域共有的设备生命周期；域选择由服务端装配固定，不作为请求参数。 */
public interface IdentitySessionPort<T> {
    record State(UUID principalId,UUID tenantId,long securityVersion,long tenantSecurityVersion) { }
    record Fact(UUID sessionId,UUID tenantId,UUID principalId,long securityVersion,long tenantSecurityVersion,
        StaffSessionPort.Channel channel,Instant expiresAt,int idleTimeoutSeconds,String csrfToken) {
        @Override public String toString(){return "Fact[受限设备事实]";}
    }
    record Issued<T>(String token,Fact session,T identity) { @Override public String toString(){return "Issued[受限会话]";} }
    Issued<T> create(T identity,StaffSessionPort.Channel channel);
    Fact verify(String token,StaffSessionPort.Channel channel);
    void touch(String token); void logout(String token);
    void limit(String ip,String account); void limitSensitive(String ip,UUID tenant,UUID actor,UUID target);
    void revokeBefore(UUID tenant,UUID principal,long cutoff);
    boolean isActive(UUID tenant,UUID principal,UUID sessionId);
    String createPre(String csrf);String readPre(String pre);long preTtl(String pre);void deletePre(String pre);
}
