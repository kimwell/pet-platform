package com.pet.platform.identity.application.authentication;

import com.pet.platform.identity.application.IdentityNames;
import com.pet.platform.identity.application.PasswordService;
import com.pet.platform.shared.api.ErrorCode;
import com.pet.platform.shared.exception.BusinessException;
import com.pet.platform.shared.security.*;
import jakarta.servlet.http.*;
import java.net.URI;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.core.env.Environment;

/** STAFF HTTP安全用例：来源、载体与CSRF先于会话建立；内部事实仅放服务端请求属性。 */
@Service
public final class StaffHttpAuthentication {
    private static final String ATTRIBUTE=StaffHttpAuthentication.class.getName()+".identity";
    public record Authenticated(String token,StaffSessionPort.SessionFact session,StaffIdentity identity,CurrentPrincipal principal) {
        @Override public String toString(){return "Authenticated[受限请求身份]";}
    }
    public record Csrf(String csrfToken,Instant expiresAt) { }
    public record Login(String tenantCode,String loginName,String password) { @Override public String toString(){return "Login[受限输入]";} }
    private final StaffAuthentication identities;
    private final StaffSessionPort sessions;
    private final PasswordService passwords;
    private final String cookie,preCookie;
    private final WebCookieSecurity web;
    public StaffHttpAuthentication(StaffAuthentication identities,StaffSessionPort sessions,PasswordService passwords,Environment env) {
        this.identities=identities;this.sessions=sessions;this.passwords=passwords;
        web=new WebCookieSecurity("staff","/api/admin/",env);cookie=web.cookieName();preCookie=web.preCookieName();
    }
    public String cookieName(){return cookie;}
    public String preCookieName(){return preCookie;}
    public static Authenticated current(HttpServletRequest request) {return (Authenticated)request.getAttribute(ATTRIBUTE);}
    public void clear(HttpServletRequest request){request.removeAttribute(ATTRIBUTE);}
    public void authenticateRequest(HttpServletRequest request,HttpServletResponse response) {
        String path=WebCookieSecurity.requestPath(request);
        if(!path.startsWith("/api/admin/"))return;
        response.setHeader("Cache-Control","no-store");response.setHeader("Pragma","no-cache");
        // 禁止URL凭据；其他客户端tenantId参数不会参与可信身份构造。
        for(String name:List.of("token","access_token","X-Staff-Token","satoken","password","currentPassword","newPassword","loginType"))if(request.getParameterMap().containsKey(name))throw error(ErrorCode.BAD_REQUEST);
        String staff=oneHeader(request,"X-Staff-Token"),customer=oneHeader(request,"X-Customer-Token"),platform=oneHeader(request,"X-Platform-Token"),authorization=oneHeader(request,"Authorization");
        if(oneHeader(request,"X-Login-Type")!=null)throw error(ErrorCode.AUTH_DOMAIN_MISMATCH);
        String sid=readCookie(request,cookie);
        int headers=(staff==null?0:1)+(customer==null?0:1)+(platform==null?0:1)+(authorization==null?0:1);
        if(headers>1 || (sid!=null && headers>0))throw error(ErrorCode.AUTH_CREDENTIAL_AMBIGUOUS);
        if(customer!=null || platform!=null || authorization!=null)throw error(ErrorCode.AUTH_DOMAIN_MISMATCH);
        boolean miniLogin=path.equals("/api/admin/auth/token/login"),webLogin=path.equals("/api/admin/auth/login"),csrf=path.equals("/api/admin/auth/csrf");
        if(miniLogin) {
            if(sid!=null || readCookie(request,preCookie)!=null || staff!=null)throw error(ErrorCode.AUTH_CREDENTIAL_AMBIGUOUS);
            return;
        }
        if((webLogin || csrf) && staff!=null)throw error(ErrorCode.AUTH_DOMAIN_MISMATCH);
        StaffSessionPort.Channel channel=sid==null?StaffSessionPort.Channel.MINIPROGRAM:StaffSessionPort.Channel.WEB;
        String token=sid!=null?sid:staff==null?null:parseBearer(staff);
        if(token!=null) {
            try {
                if(!token.matches("[A-Za-z0-9_-]{16,256}"))throw error(ErrorCode.BAD_REQUEST);
                var session=sessions.verify(token,channel);
                var identity=identities.loadForSession(session.tenantId(),session.employeeId()).orElseThrow(() -> error(ErrorCode.SESSION_REVOKED));
                if(identity.securityVersion()!=session.securityVersion() || identity.tenantSecurityVersion()!=session.tenantSecurityVersion())throw error(ErrorCode.SESSION_REVOKED);
                var principal=new CurrentPrincipal(PrincipalType.STAFF,identity.employeeId(),identity.tenantId(),session.sessionId(),identity.authorizationVersion(),identity.grants().keySet(),identity.authorizedStoreIds(),identity.grants());
                request.setAttribute(ATTRIBUTE,new Authenticated(token,session,identity,principal));
            } catch(BusinessException failure) {
                // 仅确认失效后清浏览器定位Cookie；503与来源冲突不能触发凭据清理。
                if(sid!=null && Set.of(ErrorCode.SESSION_EXPIRED,ErrorCode.SESSION_REVOKED).contains(failure.error().code())) {
                    setCookie(response,cookie,"",true);setCookie(response,preCookie,"",true);
                }
                throw failure;
            }
        }
        var current=current(request);
        if(!webLogin && !csrf && current==null)throw error(ErrorCode.AUTH_REQUIRED);
        boolean unsafe=!Set.of("GET","HEAD","OPTIONS").contains(request.getMethod());
        if(unsafe && (webLogin || channel==StaffSessionPort.Channel.WEB))validateCsrf(request);
        if(current!=null && current.identity().passwordChangeRequired()
            && !Set.of("/api/admin/auth/me","/api/admin/auth/csrf","/api/admin/auth/password","/api/admin/auth/logout","/api/admin/auth/logout-all").contains(path))throw error(ErrorCode.PASSWORD_CHANGE_REQUIRED);
        // me、csrf及auth维护端点不续闲置期限；后续业务请求才更新活跃时间。
        if(current!=null && !path.startsWith("/api/admin/auth/"))sessions.touch(token);
    }
    public StaffSessionPort.Issued login(Login input,StaffSessionPort.Channel channel,HttpServletRequest request,HttpServletResponse response) {
        String code,name;
        char[] password=input.password()==null?null:input.password().toCharArray();
        try {
            try {code=IdentityNames.tenantCode(input.tenantCode());name=IdentityNames.loginName(input.loginName());passwords.validate(password);}
            catch(IllegalArgumentException e){throw error(ErrorCode.VALIDATION_FAILED);}
            // 只使用直接对端地址；全局forwarded处理关闭，转发头不影响频控来源。
            sessions.limit(request.getRemoteAddr(),code+"\0"+name);
            var identity=identities.verifyCredentials(code,name,password).orElseThrow(() -> error(ErrorCode.LOGIN_FAILED));
            if(current(request)!=null)sessions.logout(current(request).token());
            var issued=sessions.create(identity,channel);
            if(channel==StaffSessionPort.Channel.WEB) {
                String pre=readCookie(request,preCookie);if(pre!=null)sessions.deletePre(pre);
                setCookie(response,preCookie,"",true);setCookie(response,cookie,issued.token(),false);
            }
            return issued;
        } finally {if(password!=null)Arrays.fill(password,'\0');}
    }
    public Csrf csrf(HttpServletRequest request,HttpServletResponse response) {
        var current=current(request);
        if(current!=null)return new Csrf(current.session().csrfToken(),current.session().expiresAt());
        String pre=readCookie(request,preCookie),value=pre==null?null:sessions.readPre(pre);
        if(value==null) {value=random();pre=sessions.createPre(value);setCookie(response,preCookie,pre,false);}
        return new Csrf(value,Instant.ofEpochMilli(System.currentTimeMillis()).plusSeconds(Math.max(0,sessions.preTtl(pre))));
    }
    public void logout(HttpServletRequest request,HttpServletResponse response) {
        var current=current(request);if(current==null)throw error(ErrorCode.AUTH_REQUIRED);
        sessions.logout(current.token());
        if(current.session().channel()==StaffSessionPort.Channel.WEB){setCookie(response,cookie,"",true);setCookie(response,preCookie,"",true);}
    }
    public void clearCurrentCookies(HttpServletRequest request,HttpServletResponse response) {
        if(current(request).session().channel()==StaffSessionPort.Channel.WEB){setCookie(response,cookie,"",true);setCookie(response,preCookie,"",true);}
    }
    private void validateCsrf(HttpServletRequest request){
        var current=current(request);String expected;
        if(current!=null)expected=current.session().csrfToken();else{String pre=readCookie(request,preCookie);expected=pre==null?null:sessions.readPre(pre);}
        web.validate(request,expected);
    }
    private static String random(){return WebCookieSecurity.random();}
    private static String oneHeader(HttpServletRequest r,String name){return WebCookieSecurity.oneHeader(r,name);}
    private static String parseBearer(String raw){if(!raw.matches("Bearer [A-Za-z0-9_-]{16,256}"))throw error(ErrorCode.BAD_REQUEST);return raw.substring(7);}
    private static String readCookie(HttpServletRequest r,String name){return WebCookieSecurity.readCookie(r,name);}
    private void setCookie(HttpServletResponse r,String name,String value,boolean delete){web.setCookie(r,name,value,delete);}
    private static BusinessException error(ErrorCode code){return new BusinessException(code);}
}
