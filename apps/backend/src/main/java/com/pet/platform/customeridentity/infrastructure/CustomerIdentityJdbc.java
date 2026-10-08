package com.pet.platform.customeridentity.infrastructure;
import com.pet.platform.customeridentity.application.*;
import com.pet.platform.shared.api.ErrorCode;
import com.pet.platform.shared.exception.BusinessException;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;
/** 精确登记五个客户认证函数；不设置GUC，不接收业务实体或动态SQL。 */
@Repository
public class CustomerIdentityJdbc implements CustomerIdentityStore {
 private final JdbcTemplate jdbc;
 public CustomerIdentityJdbc(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public Optional<Tenant> tenant(String code){try{return jdbc.query("select * from pet_customer.resolve_tenant(?)",(r,n)->new Tenant(r.getObject(1,UUID.class),r.getLong(2)),code).stream().findFirst();}catch(DataAccessException e){throw unavailable();}}
 public Optional<CustomerIdentity> register(UUID tenant,String app,String open){return query("select * from pet_customer.register_wechat(?,?,?)",tenant,app,open);}
 public Optional<CustomerIdentity> load(UUID tenant,UUID customer){return query("select * from pet_customer.current_identity(?,?)",tenant,customer);}
 private Optional<CustomerIdentity> query(String sql,Object...args){try{return jdbc.query(sql,(r,n)->new CustomerIdentity(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getLong(3),r.getLong(4)),args).stream().findFirst();}catch(DataAccessException e){throw unavailable();}}
 public long revoke(CustomerSessionPort.Fact f,String trace){try{Long n=jdbc.queryForObject("select pet_customer.revoke_sessions(?,?,?,?,?)",Long.class,f.tenantId(),f.customerId(),f.securityVersion(),f.tenantSecurityVersion(),trace);if(n==null)throw new BusinessException(ErrorCode.SESSION_REVOKED);return n;}catch(DataAccessException e){throw unavailable();}}
 public void event(UUID tenant,UUID customer,String result,String trace){try{jdbc.queryForObject("select pet_customer.record_login(?,?,?,?)",Object.class,tenant,customer,result,trace);}catch(DataAccessException e){throw unavailable();}}
 private static BusinessException unavailable(){return new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE);}
}
