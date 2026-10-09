package com.pet.platform.identity.infrastructure;

import com.pet.platform.identity.application.employee.*;
import com.pet.platform.identity.domain.*;
import com.pet.platform.shared.persistence.*;
import com.pet.platform.shared.security.PrincipalType;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

/** 两个固定权限适配器共用字段策略，关系EXISTS不join根结果。 */
@Repository
public class EmployeeQueries implements EmployeeQueryStore {
    private final Reader list, detail;
    public EmployeeQueries(EntityManager em, Clock clock) {
        list=new Reader(em,clock,EmployeeDirectory.LIST); detail=new Reader(em,clock,EmployeeDirectory.DETAIL);
    }
    @Override public Page<EmployeeView> list(EmployeeQuery query, Pageable page) { return list.list(query,page); }
    @Override public EmployeeView detail(UUID id) { return detail.detail(id); }
    private static final class Reader extends ScopedPersistence<Employee> {
        private static final List<String> FIELDS=List.of("id","loginName","displayName","status","createdAt","updatedAt");
        Reader(EntityManager em, Clock clock, String permission) {
            super(em,permission,Employee.class,ResourceAccessPolicy.relatedStoresAndSelf(EmployeeStore.class,"employeeId",PrincipalType.STAFF),Set.of(),clock,null);
        }
        Page<EmployeeView> list(EmployeeQuery input, Pageable page) {
            return projectedPage(EmployeeView.class,FIELDS,(root,cb,query) -> {
                var predicates=new ArrayList<jakarta.persistence.criteria.Predicate>();
                if (input.keyword()!=null) {
                    String literal=input.keyword().toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_");
                    predicates.add(cb.or(cb.like(cb.lower(root.get("loginName")),"%"+literal+"%",'\\'),
                            cb.like(cb.lower(root.get("displayName")),"%"+literal+"%",'\\')));
                }
                if (input.status()!=null) predicates.add(cb.equal(root.get("status"),input.status()));
                if (input.storeId()!=null) predicates.add(ResourceAccessPolicy.relatedStore(root,cb,query,EmployeeStore.class,"employeeId",Set.of(input.storeId())));
                return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
            },page);
        }
        EmployeeView detail(UUID id) { return projectedRequired(id,EmployeeView.class,FIELDS); }
    }
}
