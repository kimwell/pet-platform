package com.pet.platform.shared.security;

import com.pet.platform.shared.tenancy.DataScope;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** 内部最小认证授权事实，非HTTP DTO；不含Token、个人资料或可执行权限表达式。 */
public record CurrentPrincipal(PrincipalType principalType, UUID principalId, UUID tenantId,
        UUID sessionId, long authorizationVersion, Set<String> permissionCodes,
        Set<UUID> authorizedStoreIds, Map<String, DataScope> grants) {
    public CurrentPrincipal {
        Objects.requireNonNull(principalType); Objects.requireNonNull(principalId);
        Objects.requireNonNull(sessionId);
        if (authorizationVersion < 0) throw new IllegalArgumentException("授权版本必须非负");
        permissionCodes = Set.copyOf(permissionCodes);
        authorizedStoreIds = Set.copyOf(authorizedStoreIds);
        grants = Map.copyOf(grants);
        if (permissionCodes.stream().anyMatch(code -> !code.matches("[a-z][a-z0-9-]*:[a-z][a-z0-9-]*:[a-z][a-z0-9-]*"))) {
            throw new IllegalArgumentException("权限代码格式不正确");
        }
        if (principalType == PrincipalType.PLATFORM) {
            if (tenantId != null || !authorizedStoreIds.isEmpty() || !grants.isEmpty()) {
                throw new IllegalArgumentException("平台身份不得携带租户范围");
            }
        } else {
            Objects.requireNonNull(tenantId, "租户身份必须有可信租户");
            for (var entry : grants.entrySet()) {
                var scope = entry.getValue();
                if (!permissionCodes.contains(entry.getKey()) || !tenantId.equals(scope.tenantId())
                        || principalType != scope.principalType() || !principalId.equals(scope.principalId())
                        || !authorizedStoreIds.containsAll(scope.storeIds())) {
                    throw new IllegalArgumentException("授权范围与可信身份不一致");
                }
                if (principalType == PrincipalType.CUSTOMER && !scope.types().equals(Set.of(com.pet.platform.shared.tenancy.DataScopeType.SELF))) {
                    throw new IllegalArgumentException("客户仅允许明确的本人能力");
                }
            }
        }
    }

    @Override public String toString() { return "CurrentPrincipal[受限身份事实]"; }
}
