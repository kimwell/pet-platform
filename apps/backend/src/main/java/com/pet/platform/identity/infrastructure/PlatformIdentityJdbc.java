package com.pet.platform.identity.infrastructure;
import com.pet.platform.identity.application.authentication.*;
import com.pet.platform.shared.api.ErrorCode;
import com.pet.platform.shared.exception.BusinessException;
import com.pet.platform.shared.tenancy.TenantContextHolder;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.*;
/** 固定控制面函数登记；禁止租户事务或任意SQL/账号列表。 */
@Repository public class PlatformIdentityJdbc implements PlatformIdentityStore {
    private final JdbcTemplate jdbc;private final TransactionTemplate tx;
    public PlatformIdentityJdbc(JdbcTemplate jdbc,PlatformTransactionManager manager){this.jdbc=jdbc;tx=new TransactionTemplate(manager);}
    private <T> T safe(Supplier<T> work){try{if(TenantContextHolder.current().isPresent())throw new BusinessException(ErrorCode.PERMISSION_DENIED);return work.get();}catch(DataAccessException | IllegalArgumentException e){throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE);}}
    public Optional<PlatformCredential> candidate(String login){return safe(() -> jdbc.query("select * from pet_control.authentication_candidate(?)",(r,n) -> new PlatformCredential(r.getObject(1,UUID.class),r.getString(2),r.getLong(3)),login).stream().findFirst());}
    public Optional<PlatformIdentity> load(UUID id){return safe(() -> jdbc.query("select * from pet_control.authorization(?)",(r,n) -> {var a=r.getArray(4);try{return new PlatformIdentity(id,r.getString(1),r.getLong(2),r.getLong(3),Set.copyOf(Arrays.asList((String[])a.getArray())));}finally{a.free();}},id).stream().findFirst());}
    public <T> T transaction(UUID id,Supplier<T> work){return safe(() -> {
        if(TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("平台安全事务必须独立执行");
        return tx.execute(s -> {jdbc.queryForObject("select set_config('pet.platform_id',?,true)",String.class,id.toString());T result=work.get();if(TenantContextHolder.current().isPresent())throw new BusinessException(ErrorCode.PERMISSION_DENIED);return result;});
    });}
    public PlatformCredential lockCredential(){return safe(() -> jdbc.query("select * from pet_control.lock_credential()",(r,n) -> new PlatformCredential(null,r.getString(1),r.getLong(2))).stream().findFirst().orElseThrow(() -> new BusinessException(ErrorCode.SESSION_REVOKED)));}
    public long revoke(String hash){return safe(() -> jdbc.queryForObject("select pet_control.revoke_self(?)",Long.class,hash));}
    public void event(UUID actor,String op,String result,String trace){safe(() -> {jdbc.queryForObject("select pet_control.security_event(?,?,?,?)",Object.class,actor,op,result,trace);return null;});}
    public long pending(UUID id){return safe(() -> jdbc.queryForObject("select pet_control.pending_cleanup(?)",Long.class,id));}
    public void completed(UUID id,long cutoff){safe(() -> {jdbc.queryForObject("select pet_control.complete_cleanup(?,?)",Object.class,id,cutoff);return null;});}
}
