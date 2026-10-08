package com.pet.platform.identity.application.authentication;
import com.pet.platform.identity.application.*;
import com.pet.platform.shared.api.ErrorCode;
import com.pet.platform.shared.exception.BusinessException;
import com.pet.platform.shared.security.*;
import com.pet.platform.shared.observability.TraceContext;
import jakarta.servlet.http.*;
import java.time.Instant;
import java.util.*;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
/** PLATFORM仅WEB；从端点选择域，忽略其他域Cookie，不接受客户端域选择。 */
@Service public final class PlatformHttpAuthentication {
    private static final String ATTRIBUTE=PlatformHttpAuthentication.class.getName()+".identity";
    public record Authenticated(String token,IdentitySessionPort.Fact session,PlatformIdentity identity,CurrentPrincipal principal){@Override public String toString(){return "Authenticated[受限平台身份]";}}
    private final PlatformAuthentication identities;private final PlatformSessionPort sessions;private final PasswordService passwords;private final PlatformIdentityStore store;
    private final WebCookieSecurity web;
    public PlatformHttpAuthentication(PlatformAuthentication identities,PlatformSessionPort sessions,PasswordService passwords,PlatformIdentityStore store,Environment env){this.identities=identities;this.sessions=sessions;this.passwords=passwords;this.store=store;web=new WebCookieSecurity("platform","/api/platform/",env);}
    public static Authenticated current(HttpServletRequest r){return (Authenticated)r.getAttribute(ATTRIBUTE);}
    public void clear(HttpServletRequest r){r.removeAttribute(ATTRIBUTE);}
    public void clearCookies(HttpServletResponse r){web.setCookie(r,web.cookieName(),"",true);web.setCookie(r,web.preCookieName(),"",true);}
    public void authenticateRequest(HttpServletRequest r,HttpServletResponse response){
        String path=WebCookieSecurity.requestPath(r);if(!path.startsWith("/api/platform/"))return;
        response.setHeader("Cache-Control","no-store");response.setHeader("Pragma","no-cache");
        for(String n:List.of("token","access_token","satoken","password","currentPassword","newPassword","X-Platform-Token","loginType"))if(r.getParameterMap().containsKey(n))throw error(ErrorCode.BAD_REQUEST);
        String sid=WebCookieSecurity.readCookie(r,web.cookieName());
        int supplied=0;for(String n:List.of("X-Staff-Token","X-Customer-Token","X-Platform-Token","Authorization","X-Login-Type"))if(WebCookieSecurity.oneHeader(r,n)!=null)supplied++;
        if(supplied>1 || (supplied>0 && sid!=null))throw error(ErrorCode.AUTH_CREDENTIAL_AMBIGUOUS);
        if(supplied>0)throw error(ErrorCode.AUTH_DOMAIN_MISMATCH);
        if(sid!=null)try{
            var session=sessions.verify(sid,StaffSessionPort.Channel.WEB);
            if(session.tenantId()!=null)throw error(ErrorCode.AUTH_DOMAIN_MISMATCH);
            var identity=identities.load(session.principalId()).orElseThrow(() -> error(ErrorCode.SESSION_REVOKED));
            if(identity.securityVersion()!=session.securityVersion())throw error(ErrorCode.SESSION_REVOKED);
            var principal=new CurrentPrincipal(PrincipalType.PLATFORM,identity.id(),null,session.sessionId(),identity.authorizationVersion(),identity.permissions(),Set.of(),Map.of());
            r.setAttribute(ATTRIBUTE,new Authenticated(sid,session,identity,principal));
        }catch(BusinessException failure){if(Set.of(ErrorCode.SESSION_EXPIRED,ErrorCode.SESSION_REVOKED).contains(failure.error().code()))clearCookies(response);throw failure;}
        boolean login=path.equals("/api/platform/auth/login"),csrf=path.equals("/api/platform/auth/csrf");
        if(!login && !csrf && current(r)==null)throw error(ErrorCode.AUTH_REQUIRED);
        if(!Set.of("GET","HEAD","OPTIONS").contains(r.getMethod())){
            var current=current(r);String expected;
            if(current!=null)expected=current.session().csrfToken();else{String pre=WebCookieSecurity.readCookie(r,web.preCookieName());expected=pre==null?null:sessions.readPre(pre);}
            web.validate(r,expected);
        }
        if(current(r)!=null && !path.startsWith("/api/platform/auth/"))sessions.touch(sid);
    }
    public StaffHttpAuthentication.Csrf csrf(HttpServletRequest r,HttpServletResponse response){
        var c=current(r);if(c!=null)return new StaffHttpAuthentication.Csrf(c.session().csrfToken(),c.session().expiresAt());
        String pre=WebCookieSecurity.readCookie(r,web.preCookieName()),value=pre==null?null:sessions.readPre(pre);
        if(value==null){value=WebCookieSecurity.random();pre=sessions.createPre(value);web.setCookie(response,web.preCookieName(),pre,false);}
        return new StaffHttpAuthentication.Csrf(value,Instant.ofEpochMilli(System.currentTimeMillis()).plusSeconds(Math.max(0,sessions.preTtl(pre))));
    }
    public IdentitySessionPort.Issued<PlatformIdentity> login(String loginName,String raw,HttpServletRequest r,HttpServletResponse response){
        char[] password=raw==null?null:raw.toCharArray();UUID actor=null;
        try {
            String name;try{name=IdentityNames.loginName(loginName);passwords.validate(password);}catch(IllegalArgumentException invalid){throw error(ErrorCode.VALIDATION_FAILED);}
            sessions.limit(r.getRemoteAddr(),name);
            var identity=identities.verify(name,password).orElseThrow(() -> error(ErrorCode.LOGIN_FAILED));actor=identity.id();
            if(current(r)!=null)sessions.logout(current(r).token());
            var issued=sessions.create(identity,StaffSessionPort.Channel.WEB);
            try{store.event(actor,"LOGIN","SUCCESS",TraceContext.currentId());}catch(RuntimeException failed){sessions.logout(issued.token());throw failed;}
            String pre=WebCookieSecurity.readCookie(r,web.preCookieName());if(pre!=null)sessions.deletePre(pre);
            web.setCookie(response,web.preCookieName(),"",true);web.setCookie(response,web.cookieName(),issued.token(),false);
            return issued;
        }catch(BusinessException failure){if(failure.error().code()==ErrorCode.LOGIN_FAILED)store.event(null,"LOGIN","LOGIN_FAILED",TraceContext.currentId());throw failure;}
        finally{if(password!=null)Arrays.fill(password,'\0');}
    }
    public void logout(HttpServletRequest r,HttpServletResponse response){var c=current(r);sessions.logout(c.token());store.event(c.identity().id(),"LOGOUT","SUCCESS",TraceContext.currentId());clearCookies(response);}
    private static BusinessException error(ErrorCode code){return new BusinessException(code);}
}
