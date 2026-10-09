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
public class RoleManagementController {
    private final RoleManagementService management;
    public RoleManagementController(RoleManagementService management){this.management=management;}
    private static void noQuery(HttpServletRequest r){if(!r.getParameterMap().isEmpty())throw new BusinessException(ErrorCode.BAD_REQUEST);}
    private static PageQuery page(HttpServletRequest r){if(!Set.of("page","pageSize").containsAll(r.getParameterMap().keySet()))throw new BusinessException(ErrorCode.BAD_REQUEST);return PageQuery.from(r.getParameterMap());}
    @io.swagger.v3.oas.annotations.Parameters({@io.swagger.v3.oas.annotations.Parameter(name="page",in=io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY,schema=@io.swagger.v3.oas.annotations.media.Schema(type="integer",minimum="1",maximum="2147483647",defaultValue="1")),@io.swagger.v3.oas.annotations.Parameter(name="pageSize",in=io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY,schema=@io.swagger.v3.oas.annotations.media.Schema(type="integer",minimum="1",maximum="100",defaultValue="20"))})
    @GetMapping("/identity/roles") @Operation(operationId="listRoles",summary="角色列表",description="identity:role:list；独立TENANT读取，分页total字符串。")
    public ApiResponse.Success<PageResponse<RoleSummary>> listRoles(HttpServletRequest r){var p=page(r);return ApiResponse.success(management.roles(StaffHttpAuthentication.current(r),p.page(),p.pageSize()));}
    @GetMapping("/identity/roles/{roleId}") @Operation(operationId="getRole",summary="角色详情",description="identity:role:detail；独立TENANT读取。")
    public ApiResponse.Success<RoleDetail> role(@PathVariable String roleId,HttpServletRequest r){noQuery(r);return ApiResponse.success(management.role(StaffHttpAuthentication.current(r),EmployeeQuery.id(roleId)));}
    @PostMapping("/identity/roles") @Operation(operationId="createRole",summary="创建角色",description="identity:role:create；ACTIVE空权限，不创建保留角色。")
    public ApiResponse.Success<MutationResult> createRole(@Valid @RequestBody CreateRole input,HttpServletRequest r){noQuery(r);return ApiResponse.success(management.createRole(StaffHttpAuthentication.current(r),input));}
    @PutMapping("/identity/roles/{roleId}") @Operation(operationId="editRole",summary="修改角色名称与启停",description="identity:role:update；保护角色不可修改，关联身份全部失效。")
    public ApiResponse.Success<MutationResult> editRole(@PathVariable String roleId,@Valid @RequestBody EditRole input,HttpServletRequest r){noQuery(r);return ApiResponse.success(management.roleChange(StaffHttpAuthentication.current(r),EmployeeQuery.id(roleId),input.version(),input.name(),input.status(),null));}
    @PutMapping("/identity/roles/{roleId}/grants") @Operation(operationId="setRoleGrants",summary="配置角色逐权限数据范围",description="identity:role:grant；全量替换；同权限并集，不同权限不互借，受操作者逐权限可授予范围约束。")
    public ApiResponse.Success<MutationResult> grants(@PathVariable String roleId,@Valid @RequestBody RoleGrants input,HttpServletRequest r){noQuery(r);return ApiResponse.success(management.roleChange(StaffHttpAuthentication.current(r),EmployeeQuery.id(roleId),input.version(),null,null,input.grants()));}
    @GetMapping("/identity/permissions") @Operation(operationId="listPermissionOptions",summary="正式权限及可授予范围",description="identity:role:detail；服务端计算可授予范围，写入仍独立检查。")
    public ApiResponse.Success<List<PermissionOption>> permissions(HttpServletRequest r){noQuery(r);return ApiResponse.success(management.permissions(StaffHttpAuthentication.current(r)));}
}
