package com.pet.platform.shared.tenancy;

import com.pet.platform.shared.security.PrincipalType;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** 单个权限的不可变行范围；SELF仍须由业务登记归属，不能自动解释为createdBy。 */
public record DataScope(UUID tenantId, PrincipalType principalType, UUID principalId,
                        Set<DataScopeType> types, Set<UUID> storeIds) {
    public DataScope {
        Objects.requireNonNull(tenantId); Objects.requireNonNull(principalType); Objects.requireNonNull(principalId);
        if (principalType == PrincipalType.PLATFORM) throw new IllegalArgumentException("平台身份不是租户数据范围");
        types = Set.copyOf(types); storeIds = Set.copyOf(storeIds);
        if (types.isEmpty() || (types.contains(DataScopeType.TENANT) && types.size() != 1)
                || (!types.contains(DataScopeType.STORES) && !storeIds.isEmpty())) {
            throw new IllegalArgumentException("数据范围组合不正确");
        }
    }

    public boolean isSubsetOf(DataScope outer) {
        if (!sameSubject(outer)) return false;
        return outer.types.contains(DataScopeType.TENANT)
                || (!types.contains(DataScopeType.TENANT) && outer.types.containsAll(types)
                    && outer.storeIds.containsAll(storeIds));
    }

    private boolean sameSubject(DataScope other) {
        return tenantId.equals(other.tenantId) && principalType == other.principalType && principalId.equals(other.principalId);
    }

    /** 调用者必须先按同一permissionCode分组；只合并同租户同主体，再与身份门店上限相交。 */
    static DataScope mergeScopes(Collection<DataScope> roleScopes, Set<UUID> authorizedStores) {
        var scopes = java.util.List.copyOf(roleScopes);
        if (scopes.isEmpty()) throw new IllegalArgumentException("没有待合并的权限范围");
        var first = scopes.getFirst();
        var types = new HashSet<DataScopeType>(); var stores = new HashSet<UUID>();
        for (var scope : scopes) {
            if (!first.sameSubject(scope)) throw new IllegalArgumentException("不能合并不同身份的范围");
            types.addAll(scope.types); stores.addAll(scope.storeIds);
        }
        if (types.contains(DataScopeType.TENANT)) { types = new HashSet<>(Set.of(DataScopeType.TENANT)); stores.clear(); }
        else stores.retainAll(Set.copyOf(authorizedStores));
        return new DataScope(first.tenantId, first.principalType, first.principalId, types, stores);
    }
    @Override public String toString() { return "DataScope[受限数据范围]"; }
}
