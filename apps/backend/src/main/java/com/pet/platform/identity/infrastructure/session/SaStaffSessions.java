package com.pet.platform.identity.infrastructure.session;
import com.pet.platform.identity.application.authentication.*;
import java.util.UUID;
/** STAFF映射保持原端口，设备生命周期由两域共用引擎实现。 */
public final class SaStaffSessions implements StaffSessionPort {
    private final IdentitySessionPort<StaffIdentity> engine;
    public SaStaffSessions(AuthenticationRedis redis,StaffAuthentication auth,String env,int wa,int wi,int ma,int mi){
        engine=new SaIdentitySessions<>(redis,env,"staff",s -> auth.loadForSession(s.tenantId(),s.principalId()).orElseThrow(() -> new com.pet.platform.shared.exception.BusinessException(com.pet.platform.shared.api.ErrorCode.LOGIN_FAILED)),
            i -> new IdentitySessionPort.State(i.employeeId(),i.tenantId(),i.securityVersion(),i.tenantSecurityVersion()),wa,wi,ma,mi);
    }
    public static String digest(String s){return SaIdentitySessions.digest(s);}
    public static String random(){return SaIdentitySessions.random();}
    private SessionFact fact(IdentitySessionPort.Fact f){return new SessionFact(f.sessionId(),f.tenantId(),f.principalId(),f.securityVersion(),f.tenantSecurityVersion(),f.channel(),f.expiresAt(),f.idleTimeoutSeconds(),f.csrfToken());}
    public Issued create(StaffIdentity i,Channel c){var r=engine.create(i,c);return new Issued(r.token(),fact(r.session()),r.identity());}
    public SessionFact verify(String t,Channel c){return fact(engine.verify(t,c));}
    public void touch(String t){engine.touch(t);}public void logout(String t){engine.logout(t);}
    public void limit(String ip,String a){engine.limit(ip,a);}public void limitSensitive(String ip,UUID tenant,UUID actor,UUID target){engine.limitSensitive(ip,tenant,actor,target);}
    public void revokeBefore(UUID tenant,UUID id,long v){engine.revokeBefore(tenant,id,v);}
    public boolean isActive(UUID tenant,UUID id,UUID s){return engine.isActive(tenant,id,s);}
    public String createPre(String s){return engine.createPre(s);}public String readPre(String s){return engine.readPre(s);}
    public long preTtl(String s){return engine.preTtl(s);}public void deletePre(String s){engine.deletePre(s);}
}
