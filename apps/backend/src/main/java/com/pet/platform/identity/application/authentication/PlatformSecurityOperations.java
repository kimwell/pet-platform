package com.pet.platform.identity.application.authentication;
import com.pet.platform.identity.application.PasswordService;
import com.pet.platform.shared.api.ErrorCode;
import com.pet.platform.shared.exception.BusinessException;
import com.pet.platform.shared.security.PlatformScopeGuard;
import com.pet.platform.shared.observability.TraceContext;
import java.util.*;
import org.springframework.stereotype.Service;
import org.slf4j.*;
/** 仅本人改密/全撤销；DB代际、记录和意图原子，Redis提交后精确清理。 */
@Service public final class PlatformSecurityOperations {
    private static final Logger LOG=LoggerFactory.getLogger(PlatformSecurityOperations.class);
    private final PlatformIdentityStore store;private final PlatformSessionPort sessions;private final PasswordService passwords;private final PlatformScopeGuard guard;
    public PlatformSecurityOperations(PlatformIdentityStore store,PlatformSessionPort sessions,PasswordService passwords,PlatformScopeGuard guard){this.store=store;this.sessions=sessions;this.passwords=passwords;this.guard=guard;}
    public boolean execute(PlatformHttpAuthentication.Authenticated current,String confirm,String replacement,String ip){
        boolean change=replacement!=null;String permission=change?PlatformPermissions.PASSWORD:PlatformPermissions.SESSION,op=change?"CHANGE_PASSWORD":"LOGOUT_ALL";
        UUID id=guard.requirePermission(permission).principalId();
        if(!id.equals(current.identity().id()))throw error(ErrorCode.AUTH_DOMAIN_MISMATCH);
        sessions.limitSensitive(ip,null,id,id);
        char[] old=confirm==null?null:confirm.toCharArray(),next=replacement==null?null:replacement.toCharArray();
        try {
            store.transaction(id,() -> {
                var credential=store.lockCredential();var authority=store.load(id).orElseThrow(() -> error(ErrorCode.SESSION_REVOKED));
                if(credential.securityVersion()!=current.session().securityVersion() || !sessions.isActive(null,id,current.session().sessionId()))throw error(ErrorCode.SESSION_REVOKED);
                if(!authority.permissions().contains(permission))throw error(ErrorCode.PERMISSION_DENIED);
                if(!credential.matches(passwords,old))throw error(ErrorCode.SECURITY_CONFIRMATION_FAILED);
                String hash=null;if(change){try{passwords.validate(next);}catch(IllegalArgumentException invalid){throw error(ErrorCode.VALIDATION_FAILED);}if(credential.matches(passwords,next))throw error(ErrorCode.VALIDATION_FAILED);hash=passwords.hash(next);}
                long version=store.revoke(hash);store.event(id,op,"SUCCESS",TraceContext.currentId());
                var after=store.load(id).orElseThrow(() -> error(ErrorCode.SESSION_REVOKED));
                if(after.securityVersion()!=version || after.authorizationVersion()!=authority.authorizationVersion() || !after.permissions().equals(authority.permissions()))throw error(ErrorCode.PERMISSION_DENIED);
                if(!sessions.isActive(null,id,current.session().sessionId()))throw error(ErrorCode.SESSION_REVOKED);
                return version;
            });
        }catch(BusinessException failure){try{store.event(id,op,failure.error().code().name(),TraceContext.currentId());}catch(RuntimeException unavailable){LOG.warn("平台失败记录未完成，操作={}，traceId={}",op,TraceContext.currentId());}throw failure;}
        finally{if(old!=null)Arrays.fill(old,'\0');if(next!=null)Arrays.fill(next,'\0');}
        return cleanup(id);
    }
    public boolean cleanup(UUID id){try{long cutoff=store.pending(id);if(cutoff==0)return true;sessions.revokeBefore(null,id,cutoff);store.completed(id,cutoff);return true;}catch(RuntimeException e){LOG.warn("平台会话物理清理待重试，主体={}，traceId={}",id,TraceContext.currentId());return false;}}
    private static BusinessException error(ErrorCode code){return new BusinessException(code);}
}
