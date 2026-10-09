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
@Service public class ControlManagement {
 private final ControlAccountStore accounts;private final ControlTenantStore tenants;private final PlatformAuthentication auth;private final PlatformSessionPort sessions;private final PlatformSecurityOperations security;private final PlatformScopeGuard guard;private final PasswordService passwords;
 public ControlManagement(ControlAccountStore accounts,ControlTenantStore tenants,PlatformAuthentication auth,PlatformSessionPort sessions,PlatformSecurityOperations security,PlatformScopeGuard guard,PasswordService passwords){this.accounts=accounts;this.tenants=tenants;this.auth=auth;this.sessions=sessions;this.security=security;this.guard=guard;this.passwords=passwords;}
 private static BusinessException field(String key,String message){return new BusinessException(ErrorCode.VALIDATION_FAILED,List.of(new FieldErrorDetail(key,"INVALID",message)));}
 static long version(String v){try{if(!v.matches("0|[1-9][0-9]{0,18}"))throw new IllegalArgumentException();return Long.parseLong(v);}catch(RuntimeException e){throw field("version","资源版本格式无效");}}
 private static String name(String value,String key){try{return IdentityNames.name(value);}catch(IllegalArgumentException e){throw field(key,e.getMessage());}}
 private static String code(String value){try{return IdentityNames.tenantCode(value);}catch(IllegalArgumentException e){throw field("code",e.getMessage());}}
 private static String login(String value){try{return IdentityNames.loginName(value);}catch(IllegalArgumentException e){throw field("loginName",e.getMessage());}}
 private List<String> grants(List<String> value){if(!PlatformPermissions.DECLARED.containsAll(value)||new HashSet<>(value).size()!=value.size())throw field("permissions","权限未登记或重复");return List.copyOf(value);}
 private String hash(String raw,String field){char[] chars=raw.toCharArray();try{return passwords.hash(chars);}catch(IllegalArgumentException e){throw field(field,e.getMessage());}finally{Arrays.fill(chars,'\0');}}
 private void verify(PlatformHttpAuthentication.Authenticated c,String permission){var a=auth.load(c.identity().id()).orElseThrow(()->new BusinessException(ErrorCode.SESSION_REVOKED));if(a.securityVersion()!=c.session().securityVersion()||a.authorizationVersion()!=c.identity().authorizationVersion()||!sessions.isActive(null,a.id(),c.session().sessionId()))throw new BusinessException(ErrorCode.SESSION_REVOKED);if(!a.permissions().contains(permission))throw new BusinessException(ErrorCode.PERMISSION_DENIED);}
 private <T>T execute(PlatformHttpAuthentication.Authenticated c,String permission,Supplier<T> action){guard.requirePermission(permission);return accounts.transaction(c,()->{verify(c,permission);T result=action.get();verify(c,permission);return result;});}
 public PageResponse<TenantView> tenants(PlatformHttpAuthentication.Authenticated c,ControlQuery q){return execute(c,"platform:tenant:list",()->tenants.tenants(q));}
 public TenantView tenant(PlatformHttpAuthentication.Authenticated c,UUID id){return execute(c,"platform:tenant:detail",()->tenants.tenant(id));}
 public ControlMutation createTenant(PlatformHttpAuthentication.Authenticated c,CreateTenant input){String code=code(input.code()),name=name(input.name(),"name");try{return execute(c,"platform:tenant:create",()->tenants.tenantWrite("CREATE",null,0,code,name,null));}catch(DuplicateKeyException e){throw field("code","租户编码已存在");}}
 public ControlMutation editTenant(PlatformHttpAuthentication.Authenticated c,UUID id,EditControlName input){return execute(c,"platform:tenant:update",()->tenants.tenantWrite("EDIT",id,version(input.version()),null,name(input.name(),"name"),null));}
 public ControlMutation tenantStatus(PlatformHttpAuthentication.Authenticated c,UUID id,ControlStatus input){return execute(c,"platform:tenant:"+(input.status().equals("ACTIVE")?"enable":"disable"),()->tenants.tenantWrite("STATUS",id,version(input.version()),null,null,input.status()));}
 public PageResponse<ControlAccountView> accounts(PlatformHttpAuthentication.Authenticated c,ControlQuery q){return execute(c,"platform:account:list",()->accounts.accounts(q));}
 public ControlAccountView account(PlatformHttpAuthentication.Authenticated c,UUID id){return execute(c,"platform:account:detail",()->accounts.account(id));}
 public List<ControlPermissionOption> permissions(PlatformHttpAuthentication.Authenticated c){return execute(c,"platform:account:grant",()->PlatformPermissions.DECLARED.stream().sorted().map(p->new ControlPermissionOption(p,c.identity().permissions().contains(p))).toList());}
 public ControlMutation createAccount(PlatformHttpAuthentication.Authenticated c,CreateControlAccount input){String login=login(input.loginName()),name=name(input.displayName(),"displayName"),hash=hash(input.initialPassword(),"initialPassword");var grants=grants(input.permissions());guard.requirePermission("platform:account:grant");try{return execute(c,"platform:account:create",()->accounts.write("CREATE",null,0,login,name,hash,grants,null));}catch(DuplicateKeyException e){throw field("loginName","平台账号已存在");}}
 public ControlMutation editAccount(PlatformHttpAuthentication.Authenticated c,UUID id,EditControlAccount input){return execute(c,"platform:account:update",()->accounts.write("EDIT",id,version(input.version()),null,name(input.displayName(),"displayName"),null,null,null));}
 private ControlMutation changeAccount(PlatformHttpAuthentication.Authenticated c,String permission,String action,UUID id,long version,String hash,List<String> grants,String status,String ip){sessions.limitSensitive(ip,null,c.identity().id(),id);var result=execute(c,permission,()->accounts.write(action,id,version,null,null,hash,grants,status));return new ControlMutation(result.id(),result.version(),security.cleanup(id));}
 public ControlMutation accountStatus(PlatformHttpAuthentication.Authenticated c,UUID id,ControlStatus input,String ip){return changeAccount(c,"platform:account:"+(input.status().equals("ACTIVE")?"enable":"disable"),"STATUS",id,version(input.version()),null,null,input.status(),ip);}
 public ControlMutation accountGrants(PlatformHttpAuthentication.Authenticated c,UUID id,ControlAccountGrants input,String ip){return changeAccount(c,"platform:account:grant","GRANTS",id,version(input.version()),null,grants(input.permissions()),null,ip);}
 public ControlMutation reset(PlatformHttpAuthentication.Authenticated c,UUID id,ControlAccountReset input,String ip){return changeAccount(c,"platform:account:reset-password","RESET",id,version(input.version()),hash(input.newPassword(),"newPassword"),null,null,ip);}
 public ControlMutation revoke(PlatformHttpAuthentication.Authenticated c,UUID id,ControlAccountRevoke input,String ip){return changeAccount(c,"platform:account:revoke-sessions","REVOKE",id,version(input.version()),null,null,null,ip);}
 public List<ControlStoreView> stores(PlatformHttpAuthentication.Authenticated c,UUID tenant){return execute(c,"platform:store:control-list",()->tenants.stores(tenant));}
 public ControlMutation createStore(PlatformHttpAuthentication.Authenticated c,UUID tenant,CreateControlStore input){try{return execute(c,"platform:store:control-create",()->tenants.storeWrite("CREATE",tenant,null,0,code(input.code()),name(input.name(),"name"),null));}catch(DuplicateKeyException e){throw field("code","此租户的门店编码已存在");}}
 public ControlMutation editStore(PlatformHttpAuthentication.Authenticated c,UUID tenant,UUID id,EditControlName input){return execute(c,"platform:store:control-update",()->tenants.storeWrite("EDIT",tenant,id,version(input.version()),null,name(input.name(),"name"),null));}
 public ControlMutation storeStatus(PlatformHttpAuthentication.Authenticated c,UUID tenant,UUID id,ControlStatus input){return execute(c,"platform:store:control-status",()->tenants.storeWrite("STATUS",tenant,id,version(input.version()),null,null,input.status()));}
}
