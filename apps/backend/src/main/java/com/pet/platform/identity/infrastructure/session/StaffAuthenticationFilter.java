package com.pet.platform.identity.infrastructure.session;

import com.pet.platform.identity.application.authentication.StaffHttpAuthentication;
import com.pet.platform.identity.application.authentication.PlatformHttpAuthentication;
import com.pet.platform.identity.application.authentication.WebCookieSecurity;
import com.pet.platform.shared.security.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Optional;
import org.springframework.web.context.request.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/** Trace之后、Tenant之前；请求属性与Sa-Token servlet storage均在本次请求结束时清理。 */
public final class StaffAuthenticationFilter extends OncePerRequestFilter implements SessionPrincipalProvider {
    private final StaffHttpAuthentication auth;
    private final PlatformHttpAuthentication platform;
    private final HandlerExceptionResolver resolver;
    public StaffAuthenticationFilter(StaffHttpAuthentication auth,PlatformHttpAuthentication platform,HandlerExceptionResolver resolver){this.auth=auth;this.platform=platform;this.resolver=resolver;}
    @Override public Optional<CurrentPrincipal> currentPrincipal(){
        if(!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes a))return Optional.empty();
        var p=PlatformHttpAuthentication.current(a.getRequest());if(p!=null)return Optional.of(p.principal());
        var current=StaffHttpAuthentication.current(a.getRequest());return current==null?Optional.empty():Optional.of(current.principal());
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException {
        var previous=RequestContextHolder.getRequestAttributes();var attributes=new ServletRequestAttributes(request,response);
        RequestContextHolder.setRequestAttributes(attributes);
        try {if(WebCookieSecurity.requestPath(request).startsWith("/api/platform/"))platform.authenticateRequest(request,response);else auth.authenticateRequest(request,response);chain.doFilter(request,response);}
        catch(Exception e){if(response.isCommitted() || resolver.resolveException(request,response,null,e)==null){if(e instanceof IOException io)throw io;if(e instanceof ServletException se)throw se;throw new ServletException("认证请求执行失败",e);}}
        finally {auth.clear(request);platform.clear(request);attributes.requestCompleted();if(previous==null)RequestContextHolder.resetRequestAttributes();else RequestContextHolder.setRequestAttributes(previous);}
    }
}
