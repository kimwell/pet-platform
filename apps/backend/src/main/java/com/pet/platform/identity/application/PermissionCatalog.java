package com.pet.platform.identity.application;

import com.pet.platform.platform.application.PlatformPermissions;
import com.pet.platform.shared.tenancy.DataScopeType;
import com.pet.platform.shared.security.PrincipalType;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 仅基础管理权限；角色关系不能将数据库任意字符串变成有效权限。 */
public final class PermissionCatalog {
    private PermissionCatalog() { }
    public static final Set<String> ADMIN_PERMISSIONS;
    public record Permission(String code,PrincipalType principalType,String chineseName,String action,Set<DataScopeType> scopes) {
        public Permission { scopes=Set.copyOf(scopes); }
    }
    public static final Map<String,Permission> DECLARED;
    static {
        var codes = new HashSet<>(Set.of("identity:user:list","identity:user:create","identity:user:update","identity:user:disable",
                "identity:user:reset-password","identity:role:list","identity:role:update"));
        codes.addAll(PlatformPermissions.STORE_MANAGEMENT);
        ADMIN_PERMISSIONS = Set.copyOf(codes);
        var names=Map.of("identity:user:list","查看员工","identity:user:create","创建员工","identity:user:update","修改员工",
                "identity:user:disable","停用员工","identity:user:reset-password","重置员工密码","identity:role:list","查看角色",
                "identity:role:update","修改角色授权","platform:store:list","查看门店","platform:store:update","修改门店");
        codes.add("identity:user:revoke-sessions");
        DECLARED = codes.stream().collect(Collectors.toUnmodifiableMap(c -> c, c -> {
            Set<DataScopeType> scopes;
            if (c.equals("identity:user:create") || c.startsWith("identity:role:")) scopes=Set.of(DataScopeType.TENANT);
            else if (c.equals("identity:user:revoke-sessions")) scopes=Set.of(DataScopeType.TENANT,DataScopeType.STORES);
            else if (c.startsWith("platform:store:")) scopes=Set.of(DataScopeType.TENANT,DataScopeType.STORES);
            else scopes=Set.of(DataScopeType.TENANT,DataScopeType.STORES,DataScopeType.SELF);
            return new Permission(c,PrincipalType.STAFF,c.equals("identity:user:revoke-sessions")?"撤销员工全部会话":names.get(c),c.substring(c.lastIndexOf(':')+1),scopes);
        }));
    }
    public static boolean supports(String code, DataScopeType type) { return DECLARED.containsKey(code) && DECLARED.get(code).scopes().contains(type); }
}
