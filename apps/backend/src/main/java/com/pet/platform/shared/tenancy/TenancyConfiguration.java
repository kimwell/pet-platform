package com.pet.platform.shared.tenancy;

import com.pet.platform.shared.api.ErrorCode;
import com.pet.platform.shared.exception.BusinessException;
import com.pet.platform.shared.security.CurrentPrincipalProvider;
import com.pet.platform.shared.security.PlatformScopeGuard;
import jakarta.servlet.DispatcherType;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration(proxyBeanMethods = false)
public class TenancyConfiguration {
    @Bean @ConditionalOnMissingBean(CurrentPrincipalProvider.class)
    CurrentPrincipalProvider currentPrincipalProvider() { return Optional::empty; }
    @Bean @ConditionalOnMissingBean(StoreOwnershipReader.class)
    StoreOwnershipReader storeOwnershipReader() {
        return storeId -> { throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE); };
    }
    @Bean StoreScopeGuard storeScopeGuard(StoreOwnershipReader reader) { return new StoreScopeGuard(reader); }
    @Bean TrustedTenantExecutor trustedTenantExecutor(CurrentPrincipalProvider provider) { return new TrustedTenantExecutor(provider); }
    @Bean PlatformScopeGuard platformScopeGuard(CurrentPrincipalProvider provider) { return new PlatformScopeGuard(provider); }
    @Bean FilterRegistrationBean<TenantContextFilter> tenantContextFilterRegistration(CurrentPrincipalProvider provider,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        var registration = new FilterRegistrationBean<>(new TenantContextFilter(provider, resolver));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ERROR);
        return registration;
    }
}
