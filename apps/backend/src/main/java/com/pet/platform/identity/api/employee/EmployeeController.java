package com.pet.platform.identity.api.employee;

import com.pet.platform.identity.application.employee.*;
import com.pet.platform.shared.api.PageResponse;
import com.pet.platform.shared.api.ErrorCode;
import com.pet.platform.shared.exception.BusinessException;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/identity/users")
@SecurityRequirement(name="StaffCookie") @SecurityRequirement(name="StaffToken")
@ApiResponses({@ApiResponse(responseCode="200",description="成功，统一响应信封",useReturnTypeSchema=true),
        @ApiResponse(responseCode="400",description="未知/重复查询字段、非法状态、ID、分页或排序",content=@Content(schema=@Schema(implementation=com.pet.platform.shared.api.ApiResponse.Failure.class))),
        @ApiResponse(responseCode="401",description="未登录、会话失效或身份域错误",content=@Content(schema=@Schema(implementation=com.pet.platform.shared.api.ApiResponse.Failure.class))),
        @ApiResponse(responseCode="403",description="缺少当前读取权限或强制改密限制",content=@Content(schema=@Schema(implementation=com.pet.platform.shared.api.ApiResponse.Failure.class))),
        @ApiResponse(responseCode="404",description="不存在或当前操作范围不可访问",content=@Content(schema=@Schema(implementation=com.pet.platform.shared.api.ApiResponse.Failure.class))),
        @ApiResponse(responseCode="422",description="keyword约束失败或分页offset过深",content=@Content(schema=@Schema(implementation=com.pet.platform.shared.api.ApiResponse.Failure.class))),
        @ApiResponse(responseCode="503",description="数据库或认证依赖不可用",content=@Content(schema=@Schema(implementation=com.pet.platform.shared.api.ApiResponse.Failure.class)))})
public class EmployeeController {
    private final EmployeeDirectory directory;
    public EmployeeController(EmployeeDirectory directory) { this.directory=directory; }
    @GetMapping
    @Operation(operationId="listEmployees",summary="员工分页查询",description="identity:user:list；TENANT本租户、STORES有效授权门店与目标关系任一交集、SELF本人；同权限并集。账号和姓名大小写不敏感的字面量包含匹配。列表及count同一快照，total十进制字符串；不输出角色、门店关系或写能力。")
    @Parameters({@Parameter(name="keyword",in=ParameterIn.QUERY,description="1～100个Unicode码点，不能全空白；不trim；%、_、反斜线按字面量",schema=@Schema(type="string",minLength=1,maxLength=100)),
            @Parameter(name="status",in=ParameterIn.QUERY,schema=@Schema(type="string",allowableValues={"ACTIVE","DISABLED"})),
            @Parameter(name="storeId",in=ParameterIn.QUERY,description="有效本租户门店且当前操作范围和身份门店上限均允许；仅SELF拒绝404",schema=@Schema(type="string",format="uuid",pattern="^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")),
            @Parameter(name="page",in=ParameterIn.QUERY,schema=@Schema(type="integer",minimum="1",maximum="2147483647",defaultValue="1")),
            @Parameter(name="pageSize",in=ParameterIn.QUERY,schema=@Schema(type="integer",minimum="1",maximum="100",defaultValue="20")),
            @Parameter(name="sortBy",in=ParameterIn.QUERY,schema=@Schema(type="string",allowableValues={"id","loginName","displayName","status","createdAt","updatedAt"},defaultValue="createdAt")),
            @Parameter(name="sortOrder",in=ParameterIn.QUERY,description="有方向须有sortBy；同向追加id，NULLS LAST",schema=@Schema(type="string",allowableValues={"asc","desc"},defaultValue="desc"))})
    public com.pet.platform.shared.api.ApiResponse.Success<PageResponse<EmployeeView>> list(HttpServletRequest request) {
        return com.pet.platform.shared.api.ApiResponse.success(directory.list(EmployeeQuery.from(request.getParameterMap())));
    }
    @GetMapping("/{employeeId}")
    @Parameter(name="employeeId",in=ParameterIn.PATH,required=true,schema=@Schema(type="string",format="uuid",pattern="^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"))
    @Operation(operationId="getEmployee",summary="员工详情查询",description="独立identity:user:detail及其自身范围；字段与列表一致。不存在、跨租户、范围外统一404；不授权任何修改或凭据操作。")
    public com.pet.platform.shared.api.ApiResponse.Success<EmployeeView> detail(@PathVariable String employeeId, HttpServletRequest request) {
        if (!request.getParameterMap().isEmpty()) throw new BusinessException(ErrorCode.BAD_REQUEST);
        return com.pet.platform.shared.api.ApiResponse.success(directory.detail(EmployeeQuery.id(employeeId)));
    }
}
