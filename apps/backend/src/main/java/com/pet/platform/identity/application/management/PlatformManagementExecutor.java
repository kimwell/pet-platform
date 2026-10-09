package com.pet.platform.identity.application.management;
import com.pet.platform.identity.application.*;
import com.pet.platform.identity.application.authentication.*;
import com.pet.platform.identity.application.management.ControlAccountModels.*;
import com.pet.platform.platform.application.ControlQuery;
import com.pet.platform.platform.application.ControlTenantStore;
import com.pet.platform.platform.application.ControlModels.*;
import com.pet.platform.shared.api.*;
import com.pet.platform.shared.exception.BusinessException;
import com.pet.platform.shared.security.PlatformScopeGuard;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
/** PLATFORM事务逐操作重验正式身份；平台不能建立TenantContext。 */
abstract class PlatformManagementExecutor {
 protected final ControlAccountStore accounts;protected final ControlTenantStore tenants;protected final PlatformAuthentication auth;protected final PlatformSessionPort sessions;protected final PlatformSecurityOperations security;protected final PlatformScopeGuard guard;protected final PasswordService passwords;
 protected PlatformManagementExecutor(ControlAccountStore accounts,ControlTenantStore tenants,PlatformAuthentication auth,PlatformSessionPort sessions,PlatformSecurityOperations security,PlatformScopeGuard guard,PasswordService passwords){this.accounts=accounts;this.tenants=tenants;this.auth=auth;this.sessions=sessions;this.security=security;this.guard=guard;this.passwords=passwords;}
 protected static BusinessException field(String key,String message){return new BusinessException(ErrorCode.VALIDATION_FAILED,List.of(new FieldErrorDetail(key,"INVALID",message)));}
 static long version(String v){try{if(!v.matches("0|[1-9][0-9]{0,18}"))throw new IllegalArgumentException();return Long.parseLong(v);}catch(RuntimeException e){throw field("version","资源版本格式无效");}}
 protected static String name(String value,String key){try{return IdentityNames.name(value);}catch(IllegalArgumentException e){throw field(key,e.getMessage());}}
 protected static String code(String value){try{return IdentityNames.tenantCode(value);}catch(IllegalArgumentException e){throw field("code",e.getMessage());}}
 protected static String login(String value){try{return IdentityNames.loginName(value);}catch(IllegalArgumentException e){throw field("loginName",e.getMessage());}}
 protected final List<String> grants(List<String> value){if(!PlatformPermissions.DECLARED.containsAll(value)||new HashSet<>(value).size()!=value.size())throw field("permissions","权限未登记或重复");return List.copyOf(value);}
 protected final String hash(String raw,String field){char[] chars=raw.toCharArray();try{return passwords.hash(chars);}catch(IllegalArgumentException e){throw field(field,e.getMessage());}finally{Arrays.fill(chars,'\0');}}
 protected final void verify(PlatformHttpAuthentication.Authenticated c,String permission){var a=auth.load(c.identity().id()).orElseThrow(()->new BusinessException(ErrorCode.SESSION_REVOKED));if(a.securityVersion()!=c.session().securityVersion()||a.authorizationVersion()!=c.identity().authorizationVersion()||!sessions.isActive(null,a.id(),c.session().sessionId()))throw new BusinessException(ErrorCode.SESSION_REVOKED);if(!a.permissions().contains(permission))throw new BusinessException(ErrorCode.PERMISSION_DENIED);}
 protected final <T>T execute(PlatformHttpAuthentication.Authenticated c,String permission,Supplier<T> action){guard.requirePermission(permission);return accounts.transaction(c,()->{verify(c,permission);T result=action.get();verify(c,permission);return result;});}

}
