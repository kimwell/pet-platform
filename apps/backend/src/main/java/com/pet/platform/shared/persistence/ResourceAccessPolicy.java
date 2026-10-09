package com.pet.platform.shared.persistence;

import com.pet.platform.shared.exception.TenantAccessDeniedException;
import com.pet.platform.shared.tenancy.*;
import jakarta.persistence.criteria.*;
import com.pet.platform.shared.security.PrincipalType;
import java.util.*;

/** 模块静态声明支持的范围与owner属性；任何未声明组合均拒绝。 */
public final class ResourceAccessPolicy {
    private final boolean stores;
    private final String ownerType;
    private final String ownerId;
    private final Class<? extends TenantScopedEntity> storeRelation;
    private final String relationTarget;
    private final PrincipalType subjectType;
    private ResourceAccessPolicy(boolean stores, String ownerType, String ownerId) {
        this(stores,ownerType,ownerId,null,null,null);
    }
    private ResourceAccessPolicy(boolean stores, String ownerType, String ownerId,
            Class<? extends TenantScopedEntity> relation, String target, PrincipalType subjectType) {
        this.stores=stores;this.ownerType=ownerType;this.ownerId=ownerId;
        this.storeRelation=relation;this.relationTarget=target;this.subjectType=subjectType;
    }
    public static ResourceAccessPolicy tenantOnly() { return new ResourceAccessPolicy(false, null, null); }
    public static ResourceAccessPolicy stores() { return new ResourceAccessPolicy(true, null, null); }
    public static ResourceAccessPolicy owned(String typeAttribute, String idAttribute) {
        return new ResourceAccessPolicy(false, attribute(typeAttribute), attribute(idAttribute));
    }
    public static ResourceAccessPolicy storesAndOwned(String typeAttribute, String idAttribute) {
        return new ResourceAccessPolicy(true, attribute(typeAttribute), attribute(idAttribute));
    }
    /** 租户级主体的多对多门店范围；实体ID为该明确身份域的本人ID。 */
    public static ResourceAccessPolicy relatedStoresAndSelf(Class<? extends TenantScopedEntity> relation,
            String targetAttribute, PrincipalType subjectType) {
        return new ResourceAccessPolicy(true,null,"id",Objects.requireNonNull(relation),
                attribute(targetAttribute),Objects.requireNonNull(subjectType));
    }
    /** EXISTS不增加根行数量；关系tenant与根tenant同时校验。 */
    public static Predicate relatedStore(Root<?> root, CriteriaBuilder cb, CommonAbstractCriteria query,
            Class<? extends TenantScopedEntity> relationType, String targetAttribute, Set<UUID> storeIds) {
        if (storeIds.isEmpty()) return cb.disjunction();
        var subquery = query.subquery(Integer.class);
        var relation = subquery.from(relationType);
        subquery.select(cb.literal(1)).where(cb.equal(relation.get("tenantId"), root.get("tenantId")),
                cb.equal(relation.get(attribute(targetAttribute)), root.get("id")), relation.get("storeId").in(storeIds));
        return cb.exists(subquery);
    }
    private static String attribute(String value) {
        if (value == null || !value.matches("[a-zA-Z][a-zA-Z0-9]*")) throw new IllegalArgumentException("范围属性必须是静态单层名称");
        return value;
    }
    void validate(TenantContext context) {
        if(subjectType!=null && context.principalType()!=subjectType) throw new TenantAccessDeniedException();
        for (var type : context.dataScope().types()) {
            if (type == DataScopeType.STORES && !stores || type == DataScopeType.SELF && ownerId == null) throw new TenantAccessDeniedException();
        }
    }
    Predicate predicate(Root<?> root, CriteriaBuilder cb, CommonAbstractCriteria query, TenantContext context) {
        validate(context);
        var scope = context.dataScope();
        var branches = new ArrayList<Predicate>();
        for (var type : scope.types()) {
            branches.add(switch (type) {
                case TENANT -> !stores || storeRelation != null ? cb.conjunction() : context.authorizedStoreIds().isEmpty()
                        ? cb.disjunction() : root.get("storeId").in(context.authorizedStoreIds());
                case STORES -> storeRelation != null ? relatedStore(root, cb, query, storeRelation, relationTarget, scope.storeIds())
                        : scope.storeIds().isEmpty() ? cb.disjunction() : root.get("storeId").in(scope.storeIds());
                case SELF -> subjectType != null ? context.principalType() == subjectType
                        ? cb.equal(root.get("id"), context.principalId()) : cb.disjunction()
                        : cb.and(cb.equal(root.get(ownerType), context.principalType()), cb.equal(root.get(ownerId), context.principalId()));
            });
        }
        return cb.and(cb.equal(root.get("tenantId"), context.tenantId()), cb.or(branches.toArray(Predicate[]::new)));
    }
    boolean isSecurityAttribute(String name) {
        return Set.of("id", "tenantId", "storeId", "createdAt", "updatedAt", "version").contains(name)
                || name.equals(ownerType) || name.equals(ownerId);
    }
}
