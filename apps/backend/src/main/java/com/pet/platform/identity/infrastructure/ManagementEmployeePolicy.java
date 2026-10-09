package com.pet.platform.identity.infrastructure;

import com.pet.platform.identity.domain.*;
import com.pet.platform.shared.persistence.*;
import com.pet.platform.shared.security.PrincipalType;
import com.pet.platform.identity.application.management.EmployeePolicy;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Repository;

/** 使用原受控JPA投影策略做目标行检查；不加载含凭据的Entity。 */
@Repository
public class ManagementEmployeePolicy implements EmployeePolicy {
    private final Map<String,Reader> readers=new HashMap<>();
    public ManagementEmployeePolicy(EntityManager em,Clock clock){for(String p:List.of("identity:user:detail","identity:user:update","identity:user:disable","identity:user:enable","identity:user:roles","identity:user:stores"))readers.put(p,new Reader(em,clock,p));}
    public record IdView(UUID id) { }
    @Override public void require(String permission,UUID id){readers.get(permission).check(id);}
    private static final class Reader extends ScopedPersistence<Employee>{
        Reader(EntityManager em,Clock clock,String permission){super(em,permission,Employee.class,ResourceAccessPolicy.relatedStoresAndSelf(EmployeeStore.class,"employeeId",PrincipalType.STAFF),Set.of(),clock,null);}
        void check(UUID id){projectedRequired(id,IdView.class,List.of("id"));}
    }
}
