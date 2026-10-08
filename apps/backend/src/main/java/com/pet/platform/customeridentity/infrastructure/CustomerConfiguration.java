package com.pet.platform.customeridentity.infrastructure;
import com.pet.platform.customeridentity.application.*;
import com.pet.platform.identity.infrastructure.session.AuthenticationRedis;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
@Configuration(proxyBeanMethods=false) @EnableConfigurationProperties(WechatProperties.class)
public class CustomerConfiguration {
 @Bean CustomerSessionPort customerSessionPort(AuthenticationRedis redis,CustomerIdentityStore store,Environment env){
  int absolute=env.getProperty("pet.auth.customer-absolute-seconds",Integer.class,2592000),idle=env.getProperty("pet.auth.customer-idle-seconds",Integer.class,604800);
  if(absolute<1 || absolute>2592000 || idle<1 || idle>absolute || (!"test".equals(env.getRequiredProperty("pet.environment")) && (absolute!=2592000 || idle!=604800)))throw new IllegalStateException("客户会话期限不得覆盖冻结策略");
  for(String name:java.util.List.of("cn.binarywang.wx","me.chanjar.weixin","org.apache.http"))if(!"OFF".equalsIgnoreCase(env.getProperty("logging.level."+name,"OFF")))throw new IllegalStateException("微信登录SDK与HTTP原始日志必须关闭");
  return new SaCustomerSessions(redis,store,env.getRequiredProperty("pet.environment"),absolute,idle);
 }
}
