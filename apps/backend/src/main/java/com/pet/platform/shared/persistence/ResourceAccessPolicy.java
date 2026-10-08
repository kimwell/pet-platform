package com.pet.platform.shared.persistence;

import com.pet.platform.shared.exception.TenantAccessDeniedException;
import com.pet.platform.shared.tenancy.*;
import jakarta.persistence.criteria.*;
import java.util.*;

/** 模块静态声明支持的范围与owner属性；任何未声明组合均拒绝。 */
public final class ResourceAccessPolicy {
    private final boolean stores;
    private final String ownerType;
    private final String ownerId;
    private ResourceAccessPolicy(boolean stores, String ownerType, String ownerId) {
        this.stores = stores; this.ownerType = ownerType; this.ownerId = ownerId;
    }
    public static ResourceAccessPolicy tenantOnly() { return new ResourceAccessPolicy(false, null, null); }
    public static ResourceAccessPolicy stores() { return new ResourceAccessPolicy(true, null, null); }
    public static ResourceAccessPolicy owned(String typeAttribute, String idAttribute) {
        return new ResourceAccessPolicy(false, attribute(typeAttribute), attribute(idAttribute));
    }
    public static ResourceAccessPolicy storesAndOwned(String typeAttribute, String idAttribute) {
        return new ResourceAccessPolicy(true, attribute(typeAttribute), attribute(idAttribute));
    }
    private static String attribute(String value) {
        if (value == null || !value.matches("[a-zA-Z][a-zA-Z0-9]*")) throw new IllegalArgumentException("范围属性必须是静态单层名称");
        return value;
    }
    void validate(TenantContext context) {
        for (var type : context.dataScope().types()) {
            if (type == DataScopeType.STORES && !stores || type == DataScopeType.SELF && ownerId == null) throw new TenantAccessDeniedException();
        }
    }
    Predicate predicate(Root<?> root, CriteriaBuilder cb, TenantContext context) {
        validate(context);
        var scope = context.dataScope();
        var branches = new ArrayList<Predicate>();
        for (var type : scope.types()) {
            branches.add(switch (type) {
                case TENANT -> !stores ? cb.conjunction() : context.authorizedStoreIds().isEmpty()
                        ? cb.disjunction() : root.get("storeId").in(context.authorizedStoreIds());
                case STORES -> scope.storeIds().isEmpty() ? cb.disjunction() : root.get("storeId").in(scope.storeIds());
                case SELF -> cb.and(cb.equal(root.get(ownerType), context.principalType()), cb.equal(root.get(ownerId), context.principalId()));
            });
        }
        return cb.and(cb.equal(root.get("tenantId"), context.tenantId()), cb.or(branches.toArray(Predicate[]::new)));
    }
    boolean isSecurityAttribute(String name) {
        return Set.of("id", "tenantId", "storeId", "createdAt", "updatedAt", "version").contains(name)
                || name.equals(ownerType) || name.equals(ownerId);
    }
}
