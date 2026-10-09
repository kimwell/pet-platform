package com.pet.platform.identity.infrastructure;
import com.pet.platform.identity.application.authentication.PlatformHttpAuthentication;
import com.pet.platform.identity.application.management.*;
import com.pet.platform.identity.application.management.ControlAccountModels.*;
import com.pet.platform.platform.application.*;
import com.pet.platform.platform.application.ControlModels.ControlMutation;
import com.pet.platform.shared.api.*;
import com.pet.platform.shared.exception.BusinessException;
import com.pet.platform.shared.tenancy.TenantContextHolder;
import com.pet.platform.shared.observability.TraceContext;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.dao.*;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;
import tools.jackson.databind.json.JsonMapper;
@Repository public class ControlAccountJdbc implements ControlAccountStore {
 private final JdbcTemplate jdbc;private final JsonMapper json;private final TransactionTemplate tx;
 public ControlAccountJdbc(JdbcTemplate jdbc,JsonMapper json,PlatformTransactionManager manager){this.jdbc=jdbc;this.json=json;tx=new TransactionTemplate(manager);}
 public <T>T transaction(PlatformHttpAuthentication.Authenticated c,Supplier<T> work){
  if(TenantContextHolder.current().isPresent()||TransactionSynchronizationManager.isActualTransactionActive())throw new BusinessException(ErrorCode.PERMISSION_DENIED);
  try{return tx.execute(s->{jdbc.queryForObject("select set_config('pet.platform_id',?,true)",String.class,c.identity().id().toString());jdbc.queryForObject("select set_config('pet.platform_security',?,true)",String.class,Long.toString(c.session().securityVersion()));jdbc.queryForObject("select set_config('pet.platform_authorization',?,true)",String.class,Long.toString(c.identity().authorizationVersion()));return work.get();});}
  catch(DataAccessException e){String state="";for(Throwable t=e;t!=null;t=t.getCause())if(t instanceof java.sql.SQLException x)state=x.getSQLState();
   switch(state){case "P0201":throw new BusinessException(ErrorCode.SESSION_REVOKED);case "P0203":throw new BusinessException(ErrorCode.PERMISSION_DENIED);case "P0204":throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);case "P0209":throw new BusinessException(ErrorCode.VERSION_CONFLICT);case "P0210":throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);case "P0222":throw new BusinessException(ErrorCode.VALIDATION_FAILED,List.of(new FieldErrorDetail("permissions","INVALID","账号必须保留本人会话和改密权限")));default:if(e instanceof DuplicateKeyException)throw e;throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE);}
  }catch(TransactionException e){throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE);}
 }
 public PageResponse<ControlAccountView> accounts(ControlQuery q){return json.readValue(jdbc.queryForObject("select pet_control.account_read(null,?,?,?,?,?,?)::text",String.class,q.keyword(),q.status(),q.page().page(),q.page().pageSize(),q.sortBy(),q.sortOrder()),json.getTypeFactory().constructParametricType(PageResponse.class,ControlAccountView.class));}
 public ControlAccountView account(UUID id){return json.readValue(jdbc.queryForObject("select pet_control.account_read(?,null,null,1,20,'createdAt','desc')::text",String.class,id),ControlAccountView.class);}
 public ControlMutation write(String action,UUID id,long version,String login,String name,String hash,List<String> permissions,String status){
  return json.readValue(jdbc.execute((ConnectionCallback<String>)c->{try(var q=c.prepareStatement("select pet_control.account_write(?,?,?,?,?,?,?,?,?)::text")){q.setString(1,action);q.setObject(2,id);q.setLong(3,version);q.setString(4,login);q.setString(5,name);q.setString(6,hash);var a=c.createArrayOf("text",permissions==null?new String[0]:permissions.toArray(String[]::new));try{q.setArray(7,a);q.setString(8,status);q.setString(9,TraceContext.currentId());try(var v=q.executeQuery()){v.next();return v.getString(1);}}finally{a.free();}}}),ControlMutation.class);
 }
}
