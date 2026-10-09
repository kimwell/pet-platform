package com.pet.platform.identity.application.management;

import com.pet.platform.identity.application.management.ManagementModels.*;
import java.util.*;

/** 固定管理用例持久化端口，事务由正式安全事务边界承载。 */
public interface ManagementStore {
    List<ManagementModels.OrganizationView> organizations(int page,int size);long organizationCount();
    ManagementModels.OrganizationView organization(UUID id);UUID createOrganization(String code,String name);
    void editOrganization(UUID id,String name);void organizationStatus(UUID id,String status);

    void serializeTenant();
    EmployeeManagement employee(UUID id);
    List<RoleSummary> roles(int page,int pageSize);
    long roleCount();
    RoleDetail role(UUID id);
    List<StoreOption> stores(int page,int pageSize);
    long storeCount();
    UUID createEmployee(String login,String name,String hash);
    void editEmployee(UUID id,String name);
    void status(UUID id,String status);
    void assignments(UUID id,List<UUID> ids,boolean roles);
    void authorizationChanged(UUID id);
    UUID createRole(String code,String name);
    void editRole(UUID id,String name,String status);
    void grants(UUID id,List<PermissionGrant> grants);
    List<UUID> roleEmployees(UUID role);
    void lockEmployees(Collection<UUID> ids);
    void event(UUID target,String operation);
}
