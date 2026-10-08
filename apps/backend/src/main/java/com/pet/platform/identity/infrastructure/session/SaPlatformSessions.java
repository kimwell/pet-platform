package com.pet.platform.identity.infrastructure.session;
import com.pet.platform.identity.application.authentication.*;
import com.pet.platform.shared.api.ErrorCode;
import com.pet.platform.shared.exception.BusinessException;
/** 固定平台身份域，不制造tenantId或平台小程序渠道。 */
public final class SaPlatformSessions extends SaIdentitySessions<PlatformIdentity> implements PlatformSessionPort {
    public SaPlatformSessions(AuthenticationRedis redis,PlatformAuthentication auth,String env,int absolute,int idle){
        super(redis,env,"platform",s -> auth.load(s.principalId()).orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED)),i -> new State(i.id(),null,i.securityVersion(),0),absolute,idle,absolute,idle);
    }
}
