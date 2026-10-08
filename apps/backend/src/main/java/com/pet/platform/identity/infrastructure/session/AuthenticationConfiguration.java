package com.pet.platform.identity.infrastructure.session;

import com.pet.platform.identity.application.authentication.*;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration(proxyBeanMethods=false)
public class AuthenticationConfiguration {
    @Bean static org.springframework.beans.factory.config.BeanPostProcessor saContextBeforeAuthentication() {
        return new org.springframework.beans.factory.config.BeanPostProcessor() {
            @Override public Object postProcessAfterInitialization(Object bean,String name) {
                if(name.equals("saTokenContextFilterForServlet") && bean instanceof FilterRegistrationBean<?> registration) {
                    registration.setOrder(Ordered.HIGHEST_PRECEDENCE+3);
                    registration.setDispatcherTypes(DispatcherType.REQUEST);
                    registration.setAsyncSupported(true);
                }
                return bean;
            }
        };
    }
    @Bean AuthenticationRedis authenticationRedis(RedisConnectionFactory factory,Environment env){return new AuthenticationRedis(factory,env.getRequiredProperty("pet.environment"));}
    @Bean StaffSessionPort staffSessionPort(AuthenticationRedis redis,StaffAuthentication auth,Environment env){
        int wa=number(env,"web-absolute-seconds",28800),wi=number(env,"web-idle-seconds",1800),ma=number(env,"mini-absolute-seconds",604800),mi=number(env,"mini-idle-seconds",86400);
        if(!"test".equals(env.getRequiredProperty("pet.environment")) && (wa!=28800 || wi!=1800 || ma!=604800 || mi!=86400))throw new IllegalStateException("会话期限不得覆盖冻结生产策略");
        if(wi>wa || mi>ma)throw new IllegalStateException("闲置期限不得超过绝对期限");
        if(!"none".equals(env.getProperty("server.forward-headers-strategy","none")))throw new IllegalStateException("当前认证仅支持直接对端地址，server.forward-headers-strategy必须为none");
        return new SaStaffSessions(redis,auth,env.getRequiredProperty("pet.environment"),wa,wi,ma,mi);
    }
    @Bean PlatformSessionPort platformSessionPort(AuthenticationRedis redis,PlatformAuthentication auth,Environment env){return new SaPlatformSessions(redis,auth,env.getRequiredProperty("pet.environment"),number(env,"web-absolute-seconds",28800),number(env,"web-idle-seconds",1800));}
    private int number(Environment env,String name,int fallback){int n=env.getProperty("pet.auth."+name,Integer.class,fallback);if(n<1 || n>604800)throw new IllegalStateException("会话期限配置超出允许范围");return n;}
    @Bean StaffAuthenticationFilter staffPrincipalProvider(StaffHttpAuthentication auth,PlatformHttpAuthentication platform,com.pet.platform.customeridentity.application.CustomerHttpAuthentication customer,@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver){return new StaffAuthenticationFilter(auth,platform,customer,resolver);}
    @Bean FilterRegistrationBean<StaffAuthenticationFilter> staffAuthenticationRegistration(StaffAuthenticationFilter filter){var r=new FilterRegistrationBean<>(filter);r.setOrder(Ordered.HIGHEST_PRECEDENCE+5);r.setDispatcherTypes(DispatcherType.REQUEST);return r;}
}
