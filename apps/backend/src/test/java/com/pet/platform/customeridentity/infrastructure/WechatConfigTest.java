package com.pet.platform.customeridentity.infrastructure;
import com.pet.platform.customeridentity.application.*;
import com.pet.platform.shared.api.ErrorCode;
import com.pet.platform.shared.exception.BusinessException;
import org.junit.jupiter.api.*;import org.springframework.mock.env.MockEnvironment;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;
/** 配置/故障单元夹具；真实租户解析另由PG集成覆盖。 */
class WechatConfigTest {
 final CustomerIdentityStore store=mock(CustomerIdentityStore.class);final UUID tenant=UUID.randomUUID();
 WechatProperties props(boolean enabled){return new WechatProperties(enabled,Map.of("main",new WechatProperties.Application(1,"wx0000000000000001","PET_WECHAT_LOCAL_SECRET",1000,20)),Map.of("entry",new WechatProperties.Entry(true,"main",Set.of("tenant"))));}
 @Test void defaultDisabledDoesNotRequireSecretsOrPerformDatabaseQuery(){var r=new ServerWechatConfigResolver(new WechatProperties(false,null,null),store,new MockEnvironment());assertEquals(ErrorCode.CAPABILITY_DISABLED,assertThrows(BusinessException.class,()->r.resolve("tenant","entry")).error().code());verifyNoInteractions(store);}
 @Test void missingSecretFailsOnlyConfiguredEntryWithExplicitError(){when(store.tenant("tenant")).thenReturn(Optional.of(new CustomerIdentityStore.Tenant(tenant,0)));var r=new ServerWechatConfigResolver(props(true),store,new MockEnvironment());assertEquals(ErrorCode.WECHAT_CONFIGURATION_MISSING,assertThrows(BusinessException.class,()->r.resolve("tenant","entry")).error().code());}
 @Test void serverEntryRejectsOtherTenantBeforeDatabaseLookup(){var r=new ServerWechatConfigResolver(props(true),store,new MockEnvironment());assertEquals(ErrorCode.LOGIN_FAILED,assertThrows(BusinessException.class,()->r.resolve("other","entry")).error().code());verifyNoInteractions(store);}
 @Test void secretIsReadFromNamedServerPropertyAndNeverToString(){when(store.tenant("tenant")).thenReturn(Optional.of(new CustomerIdentityStore.Tenant(tenant,0)));var r=new ServerWechatConfigResolver(props(true),store,new MockEnvironment().withProperty("PET_WECHAT_LOCAL_SECRET","OnlyTestSecretInput1234567890"));var c=r.resolve("tenant","entry");assertEquals(tenant,c.tenantId());assertEquals("wx0000000000000001",c.appId());assertFalse(c.toString().contains(c.secret()));}
 @Test void arbitrarySecretPropertyAndUnboundedTimeoutAreRejected(){var invalidProperty=new WechatProperties(true,Map.of("main",new WechatProperties.Application(1,"wx0000000000000001","ANY_SECRET",1000,20)),Map.of());assertThrows(IllegalStateException.class,()->new ServerWechatConfigResolver(invalidProperty,store,new MockEnvironment()));var p=new WechatProperties(true,Map.of("main",new WechatProperties.Application(1,"wx0000000000000001","PET_WECHAT_LOCAL_SECRET",10001,20)),Map.of());var invalid=p;assertThrows(IllegalStateException.class,()->new ServerWechatConfigResolver(invalid,store,new MockEnvironment()));}
}
