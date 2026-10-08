package com.pet.platform.identity.infrastructure;

import com.pet.platform.identity.application.PermissionCatalog;
import com.pet.platform.identity.application.authentication.*;
import com.pet.platform.shared.api.ErrorCode;
import com.pet.platform.shared.exception.BusinessException;
import com.pet.platform.shared.security.PrincipalType;
import com.pet.platform.shared.tenancy.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

/** 唯一认证前SQL登记入口；仅调用两个固定函数，不设置GUC或加载实体。 */
@Repository
public class AuthenticationJdbc implements IdentityLookup {
    private final JdbcTemplate jdbc;
    public AuthenticationJdbc(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Override public Optional<AuthenticationCandidate> candidate(String code,String login) {
        try {
            return jdbc.query("select * from pet_identity.authentication_candidate(?,?)", (r,n) ->
                new AuthenticationCandidate(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getString(3),r.getString(4),r.getLong(5),r.getLong(6),r.getLong(7)),code,login).stream().findFirst();
        } catch (DataAccessException failure) { throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE); }
    }
    @Override public Optional<StaffIdentity> load(UUID tenant,UUID employee) {
        if (tenant==null || employee==null) return Optional.empty();
        try {
            return jdbc.query("select * from pet_identity.staff_authorization(?,?)", rs -> {
                if (!rs.next()) return Optional.<StaffIdentity>empty();
                String displayName=rs.getString(1);long security=rs.getLong(2),authorization=rs.getLong(3),tenantSecurity=rs.getLong(4);
                var array=rs.getArray(5);Set<UUID> stores;
                try { stores=Set.copyOf(Arrays.asList((UUID[])array.getArray())); } finally { array.free(); }
                boolean mustChange=rs.getBoolean(8);
                var roles=new HashMap<String,List<ScopeGrant>>();
                do {
                    String code=rs.getString(6);
                    if (code==null) continue;
                    var type=DataScopeType.valueOf(rs.getString(7));
                    if (!PermissionCatalog.supports(code,type)) throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE);
                    var scope=new DataScope(tenant,PrincipalType.STAFF,employee,Set.of(type),type==DataScopeType.STORES ? stores : Set.of());
                    roles.computeIfAbsent(code,k -> new ArrayList<>()).add(new ScopeGrant(code,scope));
                } while (rs.next());
                var grants=new HashMap<String,DataScope>();
                roles.forEach((code,list) -> grants.put(code,ScopeGrant.mergeForPermission(code,list,stores).dataScope()));
                return Optional.of(new StaffIdentity(tenant,employee,displayName,security,authorization,tenantSecurity,stores,grants,mustChange));
            },tenant,employee);
        } catch (DataAccessException | IllegalArgumentException failure) { throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE); }
    }
}
