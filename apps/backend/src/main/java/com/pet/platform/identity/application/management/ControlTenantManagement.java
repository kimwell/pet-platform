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
public final class ControlTenantManagement extends PlatformManagementExecutor {
 public ControlTenantManagement(ControlAccountStore accounts,ControlTenantStore tenants,PlatformAuthentication auth,PlatformSessionPort sessions,PlatformSecurityOperations security,PlatformScopeGuard guard,PasswordService passwords){ super(accounts,tenants,auth,sessions,security,guard,passwords); }

 public PageResponse<TenantView> tenants(PlatformHttpAuthentication.Authenticated c,ControlQuery q){return execute(c,"platform:tenant:list",()->tenants.tenants(q));}

 public TenantView tenant(PlatformHttpAuthentication.Authenticated c,UUID id){return execute(c,"platform:tenant:detail",()->tenants.tenant(id));}

 public ControlMutation createTenant(PlatformHttpAuthentication.Authenticated c,CreateTenant input){String code=code(input.code()),name=name(input.name(),"name");try{return execute(c,"platform:tenant:create",()->tenants.tenantWrite("CREATE",null,0,code,name,null));}catch(DuplicateKeyException e){throw field("code","租户编码已存在");}}

 public ControlMutation editTenant(PlatformHttpAuthentication.Authenticated c,UUID id,EditControlName input){return execute(c,"platform:tenant:update",()->tenants.tenantWrite("EDIT",id,version(input.version()),null,name(input.name(),"name"),null));}

 public ControlMutation tenantStatus(PlatformHttpAuthentication.Authenticated c,UUID id,ControlStatus input){return execute(c,"platform:tenant:"+(input.status().equals("ACTIVE")?"enable":"disable"),()->tenants.tenantWrite("STATUS",id,version(input.version()),null,null,input.status()));}

 public List<ControlStoreView> stores(PlatformHttpAuthentication.Authenticated c,UUID tenant){return execute(c,"platform:store:control-list",()->tenants.stores(tenant));}

 public ControlMutation createStore(PlatformHttpAuthentication.Authenticated c,UUID tenant,CreateControlStore input){try{return execute(c,"platform:store:control-create",()->tenants.storeWrite("CREATE",tenant,null,0,code(input.code()),name(input.name(),"name"),null));}catch(DuplicateKeyException e){throw field("code","此租户的门店编码已存在");}}

 public ControlMutation editStore(PlatformHttpAuthentication.Authenticated c,UUID tenant,UUID id,EditControlName input){return execute(c,"platform:store:control-update",()->tenants.storeWrite("EDIT",tenant,id,version(input.version()),null,name(input.name(),"name"),null));}

 public ControlMutation storeStatus(PlatformHttpAuthentication.Authenticated c,UUID tenant,UUID id,ControlStatus input){return execute(c,"platform:store:control-status",()->tenants.storeWrite("STATUS",tenant,id,version(input.version()),null,null,input.status()));}
}
