package com.pet.platform.customeridentity.infrastructure;
import com.pet.platform.customeridentity.application.*;
import com.pet.platform.shared.api.ErrorCode;
import com.pet.platform.shared.exception.BusinessException;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
/** 固定入口白名单；tenantCode只用来查有效租户，不是认证凭据。 */
@Component
public final class ServerWechatConfigResolver implements WechatConfigResolver {
 private final WechatProperties settings;private final CustomerIdentityStore store;private final Environment env;
 public ServerWechatConfigResolver(WechatProperties settings,CustomerIdentityStore store,Environment env){this.settings=settings;this.store=store;this.env=env;
  if(settings.applications().size()>16 || settings.entries().size()>64)throw new IllegalStateException("微信配置数量超出范围");
  settings.applications().forEach((id,a)->{if(!id.matches("[a-z][a-z0-9-]{0,31}") || a.version()<1 || a.appId()==null || !a.appId().matches("wx[a-f0-9]{16}") || a.secretProperty()==null || !a.secretProperty().matches("PET_WECHAT_[A-Z0-9_]+_SECRET") || a.timeoutMillis()<100 || a.timeoutMillis()>10000 || a.exchangesPerMinute()<1 || a.exchangesPerMinute()>20)throw new IllegalStateException("微信服务端配置不合法");});
  settings.entries().forEach((id,e)->{if(!id.matches("[a-z][a-z0-9-]{0,31}") || !settings.applications().containsKey(e.application()) || e.tenantCodes()==null || e.tenantCodes().isEmpty() || e.tenantCodes().stream().anyMatch(c->!c.matches("[a-z0-9][a-z0-9-]{0,31}")))throw new IllegalStateException("微信入口配置不合法");});
 }
 public Resolved resolve(String code,String entry){
  if(!settings.enabled())throw error(ErrorCode.CAPABILITY_DISABLED);
  var e=settings.entries().get(entry);
  if(e==null || !e.enabled() || !e.tenantCodes().contains(code))throw error(ErrorCode.LOGIN_FAILED);
  var a=settings.applications().get(e.application());
  var tenant=store.tenant(code).orElseThrow(()->error(ErrorCode.LOGIN_FAILED));
  String secret=env.getProperty(a.secretProperty());
  if(secret==null || !secret.matches("[A-Za-z0-9]{16,128}"))throw error(ErrorCode.WECHAT_CONFIGURATION_MISSING);
  return new Resolved(tenant.id(),e.application(),a.version(),a.appId(),secret,a.timeoutMillis(),a.exchangesPerMinute());
 }
 private static BusinessException error(ErrorCode code){return new BusinessException(code);}
}
