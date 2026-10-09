package com.pet.platform.identity.api.management;
import com.pet.platform.identity.application.authentication.PlatformHttpAuthentication;
import com.pet.platform.identity.application.employee.EmployeeQuery;
import com.pet.platform.identity.application.management.*;
import com.pet.platform.identity.application.management.ControlAccountModels.*;
import com.pet.platform.platform.application.*;
import com.pet.platform.platform.application.ControlModels.*;
import com.pet.platform.shared.api.*;
import com.pet.platform.shared.exception.BusinessException;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@io.swagger.v3.oas.annotations.tags.Tag(name="control-management-controller")
@RestController @RequestMapping(value="/api/platform",produces="application/json") @SecurityRequirement(name="PlatformCookie")
@io.swagger.v3.oas.annotations.responses.ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200",description="成功",useReturnTypeSchema=true),@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="400",description="参数格式或未知字段",content=@Content(schema=@Schema(implementation=ApiResponse.Failure.class))),@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="401",description="未登录或平台身份失效",content=@Content(schema=@Schema(implementation=ApiResponse.Failure.class))),@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="403",description="权限、授予上限或CSRF拒绝",content=@Content(schema=@Schema(implementation=ApiResponse.Failure.class))),@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="404",description="资源不存在",content=@Content(schema=@Schema(implementation=ApiResponse.Failure.class))),@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="409",description="版本、状态或最后管理入口冲突",content=@Content(schema=@Schema(implementation=ApiResponse.Failure.class))),@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="422",description="字段校验失败",content=@Content(schema=@Schema(implementation=ApiResponse.Failure.class))),@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="429",description="敏感操作频控",content=@Content(schema=@Schema(implementation=ApiResponse.Failure.class))),@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="503",description="依赖故障，拒绝执行",content=@Content(schema=@Schema(implementation=ApiResponse.Failure.class)))})
public class ControlTenantController {
 private final ControlTenantManagement control;
 public ControlTenantController(ControlTenantManagement control){this.control=control;}
 private static PlatformHttpAuthentication.Authenticated current(HttpServletRequest r){return PlatformHttpAuthentication.current(r);}
 private static void noQuery(HttpServletRequest r){if(!r.getParameterMap().isEmpty())throw new BusinessException(ErrorCode.BAD_REQUEST);}
 @Parameters({@Parameter(name="keyword",in=io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY),@Parameter(name="status",in=io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY,schema=@Schema(allowableValues={"ACTIVE","DISABLED"})),@Parameter(name="page",in=io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY,schema=@Schema(type="integer",minimum="1",maximum="2147483647",defaultValue="1")),@Parameter(name="pageSize",in=io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY,schema=@Schema(type="integer",minimum="1",maximum="100",defaultValue="20")),@Parameter(name="sortBy",in=io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY),@Parameter(name="sortOrder",in=io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY,schema=@Schema(allowableValues={"asc","desc"}))})
 @GetMapping("/tenants") @Operation(operationId="listControlTenants",summary="平台租户列表",description="platform:tenant:list；筛选分页排序，id稳定次序，total字符串。")
 public ApiResponse.Success<PageResponse<TenantView>> tenants(HttpServletRequest r){return ApiResponse.success(control.tenants(current(r),ControlQuery.from(r.getParameterMap(),false)));}
 @GetMapping("/tenants/{id}") @Operation(operationId="getControlTenant",summary="租户详情",description="platform:tenant:detail；正式PLATFORM身份，写请求CSRF，版本和治理约束在事务中复核。")
 public ApiResponse.Success<TenantView> getControlTenant(@PathVariable String id,HttpServletRequest r){noQuery(r);return ApiResponse.success(control.tenant(current(r),EmployeeQuery.id(id)));}
 @PostMapping("/tenants") @Operation(operationId="createControlTenant",summary="创建待初始化租户",description="platform:tenant:create；正式PLATFORM身份，写请求CSRF，版本和治理约束在事务中复核。")
 public ApiResponse.Success<ControlMutation> createControlTenant(@Valid @RequestBody CreateTenant input,HttpServletRequest r){noQuery(r);return ApiResponse.success(control.createTenant(current(r),input));}
 @PutMapping("/tenants/{id}") @Operation(operationId="editControlTenant",summary="修改租户基本资料",description="platform:tenant:update；正式PLATFORM身份，写请求CSRF，版本和治理约束在事务中复核。")
 public ApiResponse.Success<ControlMutation> editControlTenant(@PathVariable String id,@Valid @RequestBody EditControlName input,HttpServletRequest r){noQuery(r);return ApiResponse.success(control.editTenant(current(r),EmployeeQuery.id(id),input));}
 @PutMapping("/tenants/{id}/status") @Operation(operationId="setControlTenantStatus",summary="启用或停用租户",description="platform:tenant:enable / platform:tenant:disable；正式PLATFORM身份，写请求CSRF，版本和治理约束在事务中复核。")
 public ApiResponse.Success<ControlMutation> setControlTenantStatus(@PathVariable String id,@Valid @RequestBody ControlStatus input,HttpServletRequest r){noQuery(r);return ApiResponse.success(control.tenantStatus(current(r),EmployeeQuery.id(id),input));}
 @GetMapping("/tenants/{tenantId}/stores") @Operation(operationId="listControlStores",summary="租户最小门店目录",description="platform:store:control-list；正式PLATFORM身份，写请求CSRF，版本和治理约束在事务中复核。")
 public ApiResponse.Success<List<ControlStoreView>> listControlStores(@PathVariable String tenantId,HttpServletRequest r){noQuery(r);return ApiResponse.success(control.stores(current(r),EmployeeQuery.id(tenantId)));}
 @PostMapping("/tenants/{tenantId}/stores") @Operation(operationId="createControlStore",summary="创建正式门店元数据",description="platform:store:control-create；正式PLATFORM身份，写请求CSRF，版本和治理约束在事务中复核。")
 public ApiResponse.Success<ControlMutation> createControlStore(@PathVariable String tenantId,@Valid @RequestBody CreateControlStore input,HttpServletRequest r){noQuery(r);return ApiResponse.success(control.createStore(current(r),EmployeeQuery.id(tenantId),input));}
 @PutMapping("/tenants/{tenantId}/stores/{id}") @Operation(operationId="editControlStore",summary="修改门店名称",description="platform:store:control-update；正式PLATFORM身份，写请求CSRF，版本和治理约束在事务中复核。")
 public ApiResponse.Success<ControlMutation> editControlStore(@PathVariable String tenantId,@PathVariable String id,@Valid @RequestBody EditControlName input,HttpServletRequest r){noQuery(r);return ApiResponse.success(control.editStore(current(r),EmployeeQuery.id(tenantId),EmployeeQuery.id(id),input));}
 @PutMapping("/tenants/{tenantId}/stores/{id}/status") @Operation(operationId="setControlStoreStatus",summary="修改门店状态",description="platform:store:control-status；正式PLATFORM身份，写请求CSRF，版本和治理约束在事务中复核。")
 public ApiResponse.Success<ControlMutation> setControlStoreStatus(@PathVariable String tenantId,@PathVariable String id,@Valid @RequestBody ControlStatus input,HttpServletRequest r){noQuery(r);return ApiResponse.success(control.storeStatus(current(r),EmployeeQuery.id(tenantId),EmployeeQuery.id(id),input));}
}
