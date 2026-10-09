package com.pet.platform.identity.api.management;

import com.pet.platform.identity.application.management.*;
import com.pet.platform.identity.application.management.ManagementModels.*;
import com.pet.platform.identity.application.authentication.StaffHttpAuthentication;
import com.pet.platform.identity.application.employee.EmployeeQuery;
import com.pet.platform.shared.api.*;
import com.pet.platform.shared.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@io.swagger.v3.oas.annotations.tags.Tag(name="identity-management-controller")
@RestController @RequestMapping("/api/admin")
@SecurityRequirement(name="StaffCookie") @SecurityRequirement(name="StaffToken")
@io.swagger.v3.oas.annotations.responses.ApiResponses({
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200",description="成功，统一响应信封",useReturnTypeSchema=true),
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="400",description="未知或格式错误输入",content=@io.swagger.v3.oas.annotations.media.Content(schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=ApiResponse.Failure.class))),
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="401",description="未登录或旧身份失效",content=@io.swagger.v3.oas.annotations.media.Content(schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=ApiResponse.Failure.class))),
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="403",description="无权限、保护账号、授予上限或CSRF拒绝",content=@io.swagger.v3.oas.annotations.media.Content(schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=ApiResponse.Failure.class))),
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="404",description="目标或关联不存在或不可访问",content=@io.swagger.v3.oas.annotations.media.Content(schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=ApiResponse.Failure.class))),
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="409",description="版本或生命周期冲突",content=@io.swagger.v3.oas.annotations.media.Content(schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=ApiResponse.Failure.class))),
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="422",description="字段验证失败",content=@io.swagger.v3.oas.annotations.media.Content(schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=ApiResponse.Failure.class))),
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="503",description="依赖故障或写入结果未确认，禁止自动重试",content=@io.swagger.v3.oas.annotations.media.Content(schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=ApiResponse.Failure.class)))})
public class EmployeeManagementController {
    private final EmployeeManagementService management;
    public EmployeeManagementController(EmployeeManagementService management){this.management=management;}
    private static void noQuery(HttpServletRequest r){if(!r.getParameterMap().isEmpty())throw new BusinessException(ErrorCode.BAD_REQUEST);}
    private static PageQuery page(HttpServletRequest r){if(!Set.of("page","pageSize").containsAll(r.getParameterMap().keySet()))throw new BusinessException(ErrorCode.BAD_REQUEST);return PageQuery.from(r.getParameterMap());}
    @GetMapping("/identity/users/{employeeId}/management") @Operation(operationId="getEmployeeManagement",summary="员工管理资料",description="独立detail权限及全门店覆盖；关联ID、版本、保护及改密状态，不含凭据。")
    public ApiResponse.Success<EmployeeManagement> employee(@PathVariable String employeeId,HttpServletRequest r){noQuery(r);return ApiResponse.success(management.employee(StaffHttpAuthentication.current(r),EmployeeQuery.id(employeeId)));}
    @PostMapping("/identity/users") @Operation(operationId="createEmployee",summary="创建员工",description="identity:user:create；初始ACTIVE、无关联、强制首次改密。")
    public ApiResponse.Success<MutationResult> create(@Valid @RequestBody CreateEmployee input,HttpServletRequest r){noQuery(r);return ApiResponse.success(management.create(StaffHttpAuthentication.current(r),input));}
    @PutMapping("/identity/users/{employeeId}") @Operation(operationId="editEmployee",summary="修改员工姓名",description="identity:user:update；版本冲突409；账号固定。")
    public ApiResponse.Success<MutationResult> edit(@PathVariable String employeeId,@Valid @RequestBody EditEmployee input,HttpServletRequest r){noQuery(r);return ApiResponse.success(management.edit(StaffHttpAuthentication.current(r),EmployeeQuery.id(employeeId),input));}
    @PutMapping("/identity/users/{employeeId}/status") @Operation(operationId="setEmployeeStatus",summary="启用或停用员工",description="分别identity:user:enable/disable；安全代际失效；禁止本人及保留管理员。")
    public ApiResponse.Success<MutationResult> status(@PathVariable String employeeId,@Valid @RequestBody ChangeStatus input,HttpServletRequest r){noQuery(r);return ApiResponse.success(management.status(StaffHttpAuthentication.current(r),EmployeeQuery.id(employeeId),input));}
    @PutMapping("/identity/users/{employeeId}/roles") @Operation(operationId="setEmployeeRoles",summary="配置员工角色",description="identity:user:roles；全量替换，租户/授予上限检查，同事务撤销旧身份。")
    public ApiResponse.Success<MutationResult> roles(@PathVariable String employeeId,@Valid @RequestBody EmployeeAssignments input,HttpServletRequest r){noQuery(r);return ApiResponse.success(management.assign(StaffHttpAuthentication.current(r),EmployeeQuery.id(employeeId),input,true));}
    @PutMapping("/identity/users/{employeeId}/stores") @Operation(operationId="setEmployeeStores",summary="配置员工门店授权",description="identity:user:stores；全量替换，ACTIVE归属和逐权限授予上限检查。")
    public ApiResponse.Success<MutationResult> stores(@PathVariable String employeeId,@Valid @RequestBody EmployeeAssignments input,HttpServletRequest r){noQuery(r);return ApiResponse.success(management.assign(StaffHttpAuthentication.current(r),EmployeeQuery.id(employeeId),input,false));}
    @io.swagger.v3.oas.annotations.Parameters({@io.swagger.v3.oas.annotations.Parameter(name="page",in=io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY,schema=@io.swagger.v3.oas.annotations.media.Schema(type="integer",minimum="1",maximum="2147483647",defaultValue="1")),@io.swagger.v3.oas.annotations.Parameter(name="pageSize",in=io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY,schema=@io.swagger.v3.oas.annotations.media.Schema(type="integer",minimum="1",maximum="100",defaultValue="20"))})
    @GetMapping("/platform/stores/options") @Operation(operationId="listStoreOptions",summary="门店授权选择读取",description="platform:store:list；仅本租户ACTIVE、当前操作范围与身份门店上限交集，分页total字符串。")
    public ApiResponse.Success<PageResponse<StoreOption>> storeOptions(HttpServletRequest r){var p=page(r);return ApiResponse.success(management.stores(StaffHttpAuthentication.current(r),p.page(),p.pageSize()));}
}
