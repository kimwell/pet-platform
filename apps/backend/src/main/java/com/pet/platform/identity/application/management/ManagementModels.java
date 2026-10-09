package com.pet.platform.identity.application.management;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.*;
import com.pet.platform.shared.tenancy.DataScopeType;

/** 管理白名单；版本为十进制字符串，凭据只进入写入输入。 */
public final class ManagementModels {
    private ManagementModels() { }
    @Schema(requiredProperties={"id","loginName","displayName","status","version","roleIds","storeIds","protectedAccount","passwordChangeRequired"})
    public record EmployeeManagement(UUID id,String loginName,String displayName,
        @Schema(allowableValues={"ACTIVE","DISABLED"}) String status,String version,List<UUID> roleIds,List<UUID> storeIds,boolean protectedAccount,boolean passwordChangeRequired) { }
    @Schema(requiredProperties={"permissionCode","scopeType"})
    public record PermissionGrant(@NotNull String permissionCode,@NotNull DataScopeType scopeType) { }
    @Schema(requiredProperties={"id","code","name","status","version","protectedRole"})
    public record RoleSummary(UUID id,String code,String name,@Schema(allowableValues={"ACTIVE","DISABLED"}) String status,String version,boolean protectedRole) { }
    @Schema(requiredProperties={"role","grants"})
    public record RoleDetail(RoleSummary role,List<PermissionGrant> grants) { }
    @Schema(requiredProperties={"id","code","name"})
    public record StoreOption(UUID id,String code,String name) { }
    @Schema(requiredProperties={"code","name","scopes","grantableScopes"})
    public record PermissionOption(String code,String name,Set<DataScopeType> scopes,Set<DataScopeType> grantableScopes) { }
    @Schema(requiredProperties={"id","code","name","status","version"})
    public record OrganizationView(UUID id,String code,String name,@Schema(allowableValues={"ACTIVE","DISABLED"}) String status,String version) { }
    public record CreateOrganization(@NotNull String code,@NotNull String name) { }
    public record EditOrganization(@NotNull String version,@NotNull String name) { }
    public record CreateEmployee(@NotNull String loginName,@NotNull String displayName,
        @NotNull @Size(min=12,max=256) @Schema(format="password",writeOnly=true) String initialPassword) {
        @Override public String toString(){return "CreateEmployee[受限输入]";}
    }
    public record EditEmployee(@NotNull String version,@NotNull String displayName) { }
    public record ChangeStatus(@NotNull String version,@NotNull @Pattern(regexp="ACTIVE|DISABLED") String status) { }
    public record EmployeeAssignments(@NotNull String version,@NotNull @Size(max=100) List<@NotNull UUID> ids) { }
    public record CreateRole(@NotNull String code,@NotNull String name) { }
    public record EditRole(@NotNull String version,@NotNull String name,@NotNull @Pattern(regexp="ACTIVE|DISABLED") String status) { }
    public record RoleGrants(@NotNull String version,@NotNull @Size(max=100) List<@NotNull @jakarta.validation.Valid PermissionGrant> grants) { }
    @Schema(requiredProperties={"id","version","sessionCleanupComplete"})
    public record MutationResult(UUID id,String version,boolean sessionCleanupComplete) { }
}
