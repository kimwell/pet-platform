package com.pet.platform.platform.infrastructure;
import com.pet.platform.platform.application.*;
import com.pet.platform.platform.application.ControlModels.*;
import com.pet.platform.shared.api.PageResponse;
import com.pet.platform.shared.observability.TraceContext;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;
/** 固定SECURITY DEFINER调用；不读取控制面表、不接受SQL或租户身份参数。 */
@Repository public class ControlTenantJdbc implements ControlTenantStore {
 private final JdbcTemplate jdbc;private final JsonMapper json;
 public ControlTenantJdbc(JdbcTemplate jdbc,JsonMapper json){this.jdbc=jdbc;this.json=json;}
 public PageResponse<TenantView> tenants(ControlQuery q){return json.readValue(jdbc.queryForObject("select pet_control.tenant_read(null,?,?,?,?,?,?)::text",String.class,q.keyword(),q.status(),q.page().page(),q.page().pageSize(),q.sortBy(),q.sortOrder()),json.getTypeFactory().constructParametricType(PageResponse.class,TenantView.class));}
 public TenantView tenant(UUID id){return json.readValue(jdbc.queryForObject("select pet_control.tenant_read(?,null,null,1,20,'createdAt','desc')::text",String.class,id),TenantView.class);}
 public ControlMutation tenantWrite(String action,UUID id,long version,String code,String name,String status){return json.readValue(jdbc.queryForObject("select pet_control.tenant_write(?,?,?,?,?,?,?)::text",String.class,action,id,version,code,name,status,TraceContext.currentId()),ControlMutation.class);}
 public List<ControlStoreView> stores(UUID tenant){return json.readValue(jdbc.queryForObject("select pet_control.store_read(?)::text",String.class,tenant),json.getTypeFactory().constructCollectionType(List.class,ControlStoreView.class));}
 public ControlMutation storeWrite(String action,UUID tenant,UUID id,long version,String code,String name,String status){return json.readValue(jdbc.queryForObject("select pet_control.store_write(?,?,?,?,?,?,?,?)::text",String.class,action,tenant,id,version,code,name,status,TraceContext.currentId()),ControlMutation.class);}
}
