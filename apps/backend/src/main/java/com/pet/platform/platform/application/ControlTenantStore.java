package com.pet.platform.platform.application;
import com.pet.platform.shared.api.PageResponse;
import com.pet.platform.platform.application.ControlModels.*;
import java.util.*;
/** 仅租户与门店元数据的固定入口，不能访问租户业务或建立租户身份。 */
public interface ControlTenantStore {
 PageResponse<TenantView> tenants(ControlQuery query);TenantView tenant(UUID id);
 ControlMutation tenantWrite(String action,UUID id,long version,String code,String name,String status);
 List<ControlStoreView> stores(UUID tenant);
 ControlMutation storeWrite(String action,UUID tenant,UUID id,long version,String code,String name,String status);
}
