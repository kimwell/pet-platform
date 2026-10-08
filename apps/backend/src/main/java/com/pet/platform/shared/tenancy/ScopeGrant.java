package com.pet.platform.shared.tenancy;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** 合并前保留权限标识，防止list的范围被update借用。 */
public record ScopeGrant(String permissionCode, DataScope dataScope) {
    public ScopeGrant {
        Objects.requireNonNull(permissionCode); Objects.requireNonNull(dataScope);
        if (!permissionCode.matches("[a-z][a-z0-9-]*:[a-z][a-z0-9-]*:[a-z][a-z0-9-]*")) throw new IllegalArgumentException("权限代码格式不正确");
    }
    public static ScopeGrant mergeForPermission(String permissionCode, Collection<ScopeGrant> roleGrants, Set<UUID> authorizedStores) {
        var grants = List.copyOf(roleGrants);
        if (grants.isEmpty() || grants.stream().anyMatch(g -> !permissionCode.equals(g.permissionCode))) {
            throw new IllegalArgumentException("只能合并同一权限的角色范围");
        }
        return new ScopeGrant(permissionCode, DataScope.mergeScopes(grants.stream().map(ScopeGrant::dataScope).toList(), authorizedStores));
    }
    @Override public String toString() { return "ScopeGrant[受限权限范围]"; }
}
