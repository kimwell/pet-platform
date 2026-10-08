package com.pet.platform.customeridentity.application;
import java.time.Instant;
import java.util.UUID;
/** 复用现有会话引擎，仅客户MINIPROGRAM渠道。 */
public interface CustomerSessionPort {
 record Fact(UUID sessionId,UUID tenantId,UUID customerId,long securityVersion,long tenantSecurityVersion,Instant expiresAt,int idleTimeoutSeconds) {}
 record Issued(String token,Fact session,CustomerIdentity identity) { @Override public String toString(){return "Issued[受限客户会话]";} }
 Issued create(CustomerIdentity identity);
 Fact verify(String token);
 void logout(String token);
 void touch(String token);
 void revokeBefore(java.util.UUID tenant,java.util.UUID customer,long cutoff);
 void limitEntry(String source);
 void limitExchange(String source,String configId,int maximum);
}
