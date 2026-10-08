package com.pet.platform.customeridentity.infrastructure;
import com.pet.platform.customeridentity.application.*;
import com.pet.platform.identity.application.authentication.*;
import com.pet.platform.identity.infrastructure.session.*;
import com.pet.platform.shared.api.ErrorCode;
import com.pet.platform.shared.exception.BusinessException;
/** 固定CUSTOMER空间，复用已验会话引擎；不签发WEB Cookie。 */
public final class SaCustomerSessions implements CustomerSessionPort {
 private final SaIdentitySessions<CustomerIdentity> engine;private final AuthenticationRedis redis;
 public SaCustomerSessions(AuthenticationRedis redis,CustomerIdentityStore store,String env,int absolute,int idle){this.redis=redis;
  engine=new SaIdentitySessions<>(redis,env,"customer",s->store.load(s.tenantId(),s.principalId()).orElseThrow(()->new BusinessException(ErrorCode.LOGIN_FAILED)),i->new IdentitySessionPort.State(i.customerId(),i.tenantId(),i.securityVersion(),i.tenantSecurityVersion()),absolute,idle,absolute,idle);
 }
 private Fact fact(IdentitySessionPort.Fact f){return new Fact(f.sessionId(),f.tenantId(),f.principalId(),f.securityVersion(),f.tenantSecurityVersion(),f.expiresAt(),f.idleTimeoutSeconds());}
 public Issued create(CustomerIdentity identity){var r=engine.create(identity,StaffSessionPort.Channel.MINIPROGRAM);return new Issued(r.token(),fact(r.session()),r.identity());}
 public Fact verify(String token){return fact(engine.verify(token,StaffSessionPort.Channel.MINIPROGRAM));}
 public void logout(String token){engine.logout(token);}public void touch(String token){engine.touch(token);}
 public void revokeBefore(java.util.UUID tenant,java.util.UUID customer,long cutoff){engine.revokeBefore(tenant,customer,cutoff);}
 public void limitEntry(String source){redis.reserveCustomerAttempt("entry",SaIdentitySessions.digest(source),60);}
 public void limitExchange(String source,String config,int max){redis.reserveCustomerAttempt("exchange",SaIdentitySessions.digest(source+"/"+config),max);redis.reserveCustomerAttempt("source",SaIdentitySessions.digest(source),20);}
}
