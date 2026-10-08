package com.pet.platform.identity.infrastructure;

import com.pet.platform.identity.application.authentication.StaffSecurityStore;
import com.pet.platform.shared.api.ErrorCode;
import com.pet.platform.shared.exception.BusinessException;
import com.pet.platform.shared.security.PrincipalType;
import com.pet.platform.shared.tenancy.TenantContextHolder;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 登记的固定安全SQL；同一事务连接GUC、行锁、凭据、撤销意图和追加记录。 */
@Repository
public class StaffSecurityJdbc implements StaffSecurityStore {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    public StaffSecurityJdbc(JdbcTemplate jdbc,PlatformTransactionManager manager){this.jdbc=jdbc;tx=new TransactionTemplate(manager);}
    @Override public <T> T transaction(UUID tenant,Supplier<T> work) {
        var context=TenantContextHolder.current().orElseThrow(() -> new BusinessException(ErrorCode.PERMISSION_DENIED));
        if(context.principalType()!=PrincipalType.STAFF || !context.tenantId().equals(tenant))throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        if(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("安全事务必须在独立应用边界执行");
        try{return tx.execute(status -> {jdbc.queryForObject("select set_config('pet.tenant_id',?,true)",String.class,tenant.toString());T result=work.get();if(!context.equals(TenantContextHolder.current().orElseThrow()))throw new BusinessException(ErrorCode.PERMISSION_DENIED);return result;});}
        catch(DataAccessException e){throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE);}
    }
    @Override public void lockTenant(){jdbc.execute("select pet_identity.lock_staff_tenant()");}
    @Override public Optional<Credential> lockEmployee(UUID id){return jdbc.query("select * from pet_identity.lock_staff_credential(?)",(r,n) -> new Credential(r.getString(1),r.getLong(2),r.getLong(3),r.getBoolean(4),r.getBoolean(5),r.getBoolean(6),"ACTIVE".equals(r.getString(7))),id).stream().findFirst();}
    @Override public Set<UUID> employeeStores(UUID id){return Set.copyOf(jdbc.query("select store_id from identity_employee_store where employee_id=?",(r,n) -> r.getObject(1,UUID.class),id));}
    @Override public void change(UUID id,String hash,boolean required){if(jdbc.update("update identity_employee set password_hash=?,password_change_required=?,updated_at=clock_timestamp() where id=?",hash,required,id)!=1)throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);}
    @Override public long revoke(UUID id) {
        Long version=jdbc.queryForObject("update identity_employee set security_version=security_version+1,version=version+1,updated_at=clock_timestamp() where id=? returning security_version",Long.class,id);
        jdbc.update("insert into identity_session_cleanup(id,tenant_id,employee_id,revoke_before) values(?,?,?,?)",UUID.randomUUID(),tenant(),id,version);
        return version;
    }
    @Override public void event(UUID actor,UUID target,String op,String result,String trace){jdbc.update("insert into identity_security_event(id,tenant_id,operator_id,target_employee_id,operation,result,trace_id) values(?,?,?,?,?,?,?)",UUID.randomUUID(),tenant(),actor,target,op,result,trace);}
    @Override public long pending(UUID employee){return jdbc.queryForObject("select coalesce(max(revoke_before),0) from identity_session_cleanup where employee_id=? and not completed",Long.class,employee);}
    @Override public void completed(UUID employee,long cutoff){jdbc.update("update identity_session_cleanup set completed=true,completed_at=clock_timestamp() where employee_id=? and revoke_before<=? and not completed",employee,cutoff);}
    private UUID tenant(){return TenantContextHolder.current().orElseThrow().tenantId();}
}
