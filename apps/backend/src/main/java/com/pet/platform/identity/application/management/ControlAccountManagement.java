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
/** 本模块独立用例，共享受限身份与事务执行机制。 */
@Service
public final class ControlAccountManagement extends PlatformManagementExecutor {
 public ControlAccountManagement(ControlAccountStore accounts,ControlTenantStore tenants,PlatformAuthentication auth,PlatformSessionPort sessions,PlatformSecurityOperations security,PlatformScopeGuard guard,PasswordService passwords){ super(accounts,tenants,auth,sessions,security,guard,passwords); }

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
}
