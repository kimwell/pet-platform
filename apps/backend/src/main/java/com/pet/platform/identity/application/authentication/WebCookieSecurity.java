package com.pet.platform.identity.application.authentication;
import com.pet.platform.shared.api.ErrorCode;
import com.pet.platform.shared.exception.BusinessException;
import jakarta.servlet.http.*;
import java.net.URI;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.http.ResponseCookie;
import org.springframework.core.env.Environment;
/** 两域共有的来源、Cookie和同步CSRF规则；路径/域由服务端固定。 */
public final class WebCookieSecurity {
    private final String path,origin,cookie,pre;private final boolean secure;
    public WebCookieSecurity(String domain,String path,Environment env){
        this.path=path;secure=env.getProperty("pet.auth.cookie-secure",Boolean.class,true);
        if("prod".equals(env.getRequiredProperty("pet.environment")) && !secure)throw new IllegalStateException("生产pet.auth.cookie-secure必须为true");
        String prefix=secure?"__Secure-pet_":"pet_dev_";cookie=prefix+domain+"_sid";pre=prefix+domain+"_pre";
        origin=canonicalOrigin(env.getRequiredProperty("pet.public-origin"));
    }
    public String cookieName(){return cookie;}public String preCookieName(){return pre;}
    public void validate(HttpServletRequest request,String expected){
        String source=oneHeader(request,"Origin");
        if(source!=null) {if(!source.equals(origin))throw error(ErrorCode.CSRF_INVALID);}
        else {String referer=oneHeader(request,"Referer");if(referer==null || !origin.equals(canonicalOrigin(referer)))throw error(ErrorCode.CSRF_INVALID);}
        String content=request.getContentType();
        if(request.getContentLengthLong()>0 && (content==null || !content.split(";",2)[0].equalsIgnoreCase("application/json")))throw error(ErrorCode.CSRF_INVALID);
        String supplied=oneHeader(request,"X-CSRF-Token");
        if(expected==null || supplied==null || !supplied.matches("[A-Za-z0-9_-]{43}")
            || !MessageDigest.isEqual(hash(expected),hash(supplied)))throw error(ErrorCode.CSRF_INVALID);
    }
    /** 认证和MVC使用同一服务器路径；API不接受编码/矩阵参数别名绕过域及CSRF。 */
    public static String requestPath(HttpServletRequest request) {
        String raw=request.getRequestURI(),route=request.getServletPath(),context=request.getContextPath();
        if(route==null || route.isEmpty())route=raw.substring(context.length());
        if(raw.startsWith(context+"/api/") || route.startsWith("/api/")) {
            if(!raw.equals(context+route) || raw.indexOf('%')>=0 || raw.indexOf(';')>=0)throw error(ErrorCode.BAD_REQUEST);
        }
        return route;
    }
    public static byte[] hash(String s) {try{return MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));}catch(NoSuchAlgorithmException e){throw new IllegalStateException("安全摘要算法不可用");}}
    public static String random(){byte[] b=new byte[32];new SecureRandom().nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
    public static String canonicalOrigin(String value) {try{URI u=URI.create(value);if(u.getHost()==null || u.getUserInfo()!=null || !Set.of("http","https").contains(u.getScheme()))return "INVALID";int p=u.getPort();return u.getScheme()+"://"+u.getHost()+(p==-1 || (p==80 && u.getScheme().equals("http")) || (p==443 && u.getScheme().equals("https"))?"":":"+p);}catch(IllegalArgumentException e){return "INVALID";}}
    public static String oneHeader(HttpServletRequest r,String name){var values=Collections.list(r.getHeaders(name));if(values.size()>1)throw error(ErrorCode.AUTH_CREDENTIAL_AMBIGUOUS);return values.isEmpty()?null:values.getFirst();}
    public static String parseBearer(String raw){if(!raw.matches("Bearer [A-Za-z0-9_-]{16,256}"))throw error(ErrorCode.BAD_REQUEST);return raw.substring(7);}
    public static String readCookie(HttpServletRequest r,String name){String result=null;if(r.getCookies()!=null)for(Cookie c:r.getCookies())if(c.getName().equals(name)){if(result!=null)throw error(ErrorCode.AUTH_CREDENTIAL_AMBIGUOUS);result=c.getValue();}if(result!=null && !result.matches("[A-Za-z0-9_-]{16,256}"))throw error(ErrorCode.BAD_REQUEST);return result;}
    public void setCookie(HttpServletResponse r,String name,String value,boolean delete){var b=ResponseCookie.from(name,value).path(path).httpOnly(true).secure(secure).sameSite("Lax");if(delete)b.maxAge(0);r.addHeader("Set-Cookie",b.build().toString());}
    private static BusinessException error(ErrorCode code){return new BusinessException(code);}
}
