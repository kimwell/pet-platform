package com.pet.platform.customeridentity.application;
import com.pet.platform.shared.api.ErrorCode;
import com.pet.platform.shared.exception.BusinessException;
import com.pet.platform.shared.observability.TraceContext;
import com.pet.platform.shared.security.*;
import com.pet.platform.shared.tenancy.*;
import jakarta.servlet.http.*;
import java.util.*;
import org.springframework.stereotype.Service;
/** 客户HTTP入口：外部交换在DB事务前；内部结果只沿认证服务调用链流转。 */
@Service
public final class CustomerHttpAuthentication {
 private static final String ATTRIBUTE=CustomerHttpAuthentication.class.getName()+".identity";
 public static final String SESSION_PERMISSION="customer:session:manage";
 public record Authenticated(String token,CustomerSessionPort.Fact session,CustomerIdentity identity,CurrentPrincipal principal){@Override public String toString(){return "Authenticated[受限客户身份]";}}
 private final CustomerIdentityStore store;private final CustomerSessionPort sessions;private final WechatMiniProgramGateway gateway;private final WechatConfigResolver configs;
 public CustomerHttpAuthentication(CustomerIdentityStore store,CustomerSessionPort sessions,WechatMiniProgramGateway gateway,WechatConfigResolver configs){this.store=store;this.sessions=sessions;this.gateway=gateway;this.configs=configs;}
 public static Authenticated current(HttpServletRequest request){return (Authenticated)request.getAttribute(ATTRIBUTE);}public void clear(HttpServletRequest request){request.removeAttribute(ATTRIBUTE);}
 public void authenticateRequest(HttpServletRequest request,HttpServletResponse response,String path){
  response.setHeader("Cache-Control","no-store");response.setHeader("Pragma","no-cache");
  boolean login=path.equals("/api/customer/auth/wechat/login");
  if(login)sessions.limitEntry(request.getRemoteAddr()); // MVC校验之前，异常输入也计数。
  for(String name:List.of("token","access_token","X-Customer-Token","satoken","code","openId","loginType"))if(request.getParameterMap().containsKey(name))throw error(ErrorCode.BAD_REQUEST);
  String own=one(request,"X-Customer-Token");int count=own==null?0:1;boolean other=false;
  for(String name:List.of("X-Staff-Token","X-Platform-Token","Authorization","X-Login-Type")){if(one(request,name)!=null){count++;other=true;}}
  boolean customerCookie=false;if(request.getCookies()!=null)for(var c:request.getCookies())if(Set.of("__Secure-pet_customer_sid","pet_dev_customer_sid").contains(c.getName()))customerCookie=true;
  if(count>1 || (customerCookie && own!=null))throw error(ErrorCode.AUTH_CREDENTIAL_AMBIGUOUS);
  if(other || customerCookie)throw error(ErrorCode.AUTH_DOMAIN_MISMATCH);
  if(login){if(own!=null)throw error(ErrorCode.AUTH_CREDENTIAL_AMBIGUOUS);return;}
  if(own==null)throw error(ErrorCode.AUTH_REQUIRED);
  if(!own.matches("Bearer [A-Za-z0-9_-]{16,256}"))throw error(ErrorCode.BAD_REQUEST);
  String token=own.substring(7);var fact=sessions.verify(token);
  var identity=store.load(fact.tenantId(),fact.customerId()).orElseThrow(()->error(ErrorCode.SESSION_REVOKED));
  if(identity.securityVersion()!=fact.securityVersion() || identity.tenantSecurityVersion()!=fact.tenantSecurityVersion())throw error(ErrorCode.SESSION_REVOKED);
  var scope=new DataScope(identity.tenantId(),PrincipalType.CUSTOMER,identity.customerId(),Set.of(DataScopeType.SELF),Set.of());
  var principal=new CurrentPrincipal(PrincipalType.CUSTOMER,identity.customerId(),identity.tenantId(),fact.sessionId(),0,Set.of(SESSION_PERMISSION),Set.of(),Map.of(SESSION_PERMISSION,scope));
  request.setAttribute(ATTRIBUTE,new Authenticated(token,fact,identity,principal));
  if(!path.startsWith("/api/customer/auth/"))sessions.touch(token);
 }
 public CustomerSessionPort.Issued login(String tenant,String entry,String code,HttpServletRequest request){
  UUID trustedTenant=null,customer=null;CustomerSessionPort.Issued issued=null;
  try {
   if(tenant==null || !tenant.matches("[a-z0-9][a-z0-9-]{0,31}") || entry==null || !entry.matches("[a-z][a-z0-9-]{0,31}") || code==null || !code.matches("[A-Za-z0-9_-]{1,256}"))throw error(ErrorCode.VALIDATION_FAILED);
   var cfg=configs.resolve(tenant,entry);trustedTenant=cfg.tenantId();sessions.limitExchange(request.getRemoteAddr(),cfg.configId(),cfg.exchangesPerMinute());
   var verified=gateway.exchange(cfg,code);
   if(verified==null || !cfg.appId().equals(verified.appId()) || verified.openId()==null || !verified.openId().matches("[A-Za-z0-9_-]{1,128}"))throw error(ErrorCode.WECHAT_RESPONSE_INVALID);
   var identity=store.register(trustedTenant,verified.appId(),verified.openId()).orElseThrow(()->error(ErrorCode.LOGIN_FAILED));customer=identity.customerId();
   issued=sessions.create(identity);
   store.event(trustedTenant,customer,"SUCCESS",TraceContext.currentId());
   return issued;
  }catch(BusinessException failure){
   if(issued!=null){try{sessions.logout(issued.token());}catch(BusinessException ignored){/* 不返回未完成的Token；期限仍约束悬挂设备。 */}}
   try{store.event(trustedTenant,customer,failure.error().code().name(),TraceContext.currentId());}catch(BusinessException ignored){org.slf4j.LoggerFactory.getLogger(getClass()).warn("客户登录安全记录未完成，结果={}，traceId={}",failure.error().code(),TraceContext.currentId());}
   throw failure;
  }
 }
 public void logout(HttpServletRequest request){var c=current(request);if(c==null)throw error(ErrorCode.AUTH_REQUIRED);sessions.logout(c.token());}
 public boolean logoutAll(HttpServletRequest request){var c=current(request);if(c==null)throw error(ErrorCode.AUTH_REQUIRED);
  sessions.limitEntry(request.getRemoteAddr());sessions.verify(c.token());
  long cutoff=store.revoke(c.session(),TraceContext.currentId());
  try{sessions.revokeBefore(c.identity().tenantId(),c.identity().customerId(),cutoff);return true;}catch(BusinessException failure){return false;}
 }
 private static String one(HttpServletRequest r,String name){var values=Collections.list(r.getHeaders(name));if(values.size()>1 || (!values.isEmpty() && values.getFirst().contains(",")))throw error(ErrorCode.AUTH_CREDENTIAL_AMBIGUOUS);return values.isEmpty()?null:values.getFirst();}
 private static BusinessException error(ErrorCode code){return new BusinessException(code);}
}
