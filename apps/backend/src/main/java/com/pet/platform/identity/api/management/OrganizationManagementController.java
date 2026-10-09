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
public class OrganizationManagementController {
    private final OrganizationManagementService management;
    public OrganizationManagementController(OrganizationManagementService management){this.management=management;}
    private static void noQuery(HttpServletRequest r){if(!r.getParameterMap().isEmpty())throw new BusinessException(ErrorCode.BAD_REQUEST);}
    private static PageQuery page(HttpServletRequest r){if(!Set.of("page","pageSize").containsAll(r.getParameterMap().keySet()))throw new BusinessException(ErrorCode.BAD_REQUEST);return PageQuery.from(r.getParameterMap());}
    @io.swagger.v3.oas.annotations.Parameters({@io.swagger.v3.oas.annotations.Parameter(name="page",in=io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY,schema=@io.swagger.v3.oas.annotations.media.Schema(type="integer",minimum="1",maximum="2147483647")),@io.swagger.v3.oas.annotations.Parameter(name="pageSize",in=io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY,schema=@io.swagger.v3.oas.annotations.media.Schema(type="integer",minimum="1",maximum="100"))})
    @GetMapping("/identity/organizations") @Operation(operationId="listOrganizations",summary="本租户内部组织列表",description="identity:organization:list，TENANT范围；组织不决定业务数据范围。")
    public ApiResponse.Success<PageResponse<OrganizationView>> organizations(HttpServletRequest r){var q=page(r);return ApiResponse.success(management.organizations(StaffHttpAuthentication.current(r),q.page(),q.pageSize()));}
    @GetMapping("/identity/organizations/{id}") @Operation(operationId="getOrganization",summary="本租户组织详情")
    public ApiResponse.Success<OrganizationView> organization(@PathVariable String id,HttpServletRequest r){noQuery(r);return ApiResponse.success(management.organization(StaffHttpAuthentication.current(r),EmployeeQuery.id(id)));}
    @PostMapping("/identity/organizations") @Operation(operationId="createOrganization",summary="创建本租户内部组织")
    public ApiResponse.Success<MutationResult> createOrganization(@Valid @RequestBody CreateOrganization input,HttpServletRequest r){noQuery(r);return ApiResponse.success(management.createOrganization(StaffHttpAuthentication.current(r),input));}
    @PutMapping("/identity/organizations/{id}") @Operation(operationId="editOrganization",summary="修改本租户组织名称")
    public ApiResponse.Success<MutationResult> editOrganization(@PathVariable String id,@Valid @RequestBody EditOrganization input,HttpServletRequest r){noQuery(r);return ApiResponse.success(management.editOrganization(StaffHttpAuthentication.current(r),EmployeeQuery.id(id),input));}
    @PutMapping("/identity/organizations/{id}/status") @Operation(operationId="setOrganizationStatus",summary="启停本租户内部组织")
    public ApiResponse.Success<MutationResult> organizationStatus(@PathVariable String id,@Valid @RequestBody ChangeStatus input,HttpServletRequest r){noQuery(r);return ApiResponse.success(management.organizationStatus(StaffHttpAuthentication.current(r),EmployeeQuery.id(id),input));}
}
