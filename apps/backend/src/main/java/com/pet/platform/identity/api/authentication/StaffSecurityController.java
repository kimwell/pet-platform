package com.pet.platform.identity.api.authentication;

import com.pet.platform.identity.application.authentication.*;
import com.pet.platform.shared.api.ApiResponse;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping(produces="application/json")
@SecurityRequirement(name="StaffCookie") @SecurityRequirement(name="StaffToken")
@ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200",description="数据库变更与记录成功；X-Session-Cleanup为COMPLETE或PENDING，data=null",useReturnTypeSchema=true,
        headers=@io.swagger.v3.oas.annotations.headers.Header(name="X-Session-Cleanup",schema=@Schema(type="string",allowableValues={"COMPLETE","PENDING"}))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="400",description="输入格式或载体错误",content=@Content(schema=@Schema(implementation=ApiResponse.Failure.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="401",description="未登录或旧会话已失效",content=@Content(schema=@Schema(implementation=ApiResponse.Failure.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="403",description="权限、目标保护、重新确认或CSRF失败",content=@Content(schema=@Schema(implementation=ApiResponse.Failure.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="404",description="目标不存在或超出管理范围",content=@Content(schema=@Schema(implementation=ApiResponse.Failure.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="409",description="目标版本竞争或状态冲突",content=@Content(schema=@Schema(implementation=ApiResponse.Failure.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="422",description="输入校验失败或新旧密码相同",content=@Content(schema=@Schema(implementation=ApiResponse.Failure.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="429",description="频控，响应Retry-After",content=@Content(schema=@Schema(implementation=ApiResponse.Failure.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="503",description="变更未确认成功；依赖不可用，禁止自动重试",content=@Content(schema=@Schema(implementation=ApiResponse.Failure.class)))})
public class StaffSecurityController {
    public record ChangePasswordInput(@NotNull @Size(min=12,max=256) @Schema(format="password",writeOnly=true) String currentPassword,
        @NotNull @Size(min=12,max=256) @Schema(format="password",writeOnly=true) String newPassword) {
        @Override public String toString(){return "ChangePasswordInput[受限输入]";}
    }
    public record ConfirmationInput(@NotNull @Size(min=12,max=256) @Schema(format="password",writeOnly=true) String currentPassword) {
        @Override public String toString(){return "ConfirmationInput[受限输入]";}
    }
    public record ResetPasswordInput(@NotNull @Pattern(regexp="^(0|[1-9][0-9]{0,18})$") String version,
        @NotNull @Size(min=12,max=256) @Schema(format="password",writeOnly=true) String currentPassword,
        @NotNull @Size(min=12,max=256) @Schema(format="password",writeOnly=true,description="操作者通过安全人工渠道交付的临时密码；响应不回显") String newPassword) {
        @Override public String toString(){return "ResetPasswordInput[受限输入]";}
    }
    public record RevokeSessionsInput(@NotNull @Pattern(regexp="^(0|[1-9][0-9]{0,18})$") String version,
        @NotNull @Size(min=12,max=256) @Schema(format="password",writeOnly=true) String currentPassword) {
        @Override public String toString(){return "RevokeSessionsInput[受限输入]";}
    }
    private final StaffSecurityOperations security;
    private final StaffHttpAuthentication auth;
    public StaffSecurityController(StaffSecurityOperations security,StaffHttpAuthentication auth){this.security=security;this.auth=auth;}
    @Parameters({@Parameter(name="X-CSRF-Token",in=ParameterIn.HEADER,description="WEB Cookie写入必须；MINIPROGRAM Token无需浏览器CSRF"),
    @Parameter(name="Origin",in=ParameterIn.HEADER,description="WEB必须匹配固定来源，缺失时提供同源Referer")})
    @PutMapping(value="/api/admin/auth/password",consumes="application/json")
    @Operation(summary="本人修改员工密码",description="当前密码重新确认；不接受employeeId；成功含当前设备的全部STAFF会话失效，清WEB Cookie与CSRF，必须重新登录")
    public ApiResponse.Success<Void> change(@Valid @RequestBody ChangePasswordInput input,HttpServletRequest request,HttpServletResponse response){return execute(request,response,StaffSecurityOperations.Operation.CHANGE_PASSWORD,null,null,input.currentPassword(),input.newPassword());}
    @Parameters({@Parameter(name="X-CSRF-Token",in=ParameterIn.HEADER,description="WEB Cookie写入必须；MINIPROGRAM Token无需浏览器CSRF"),
    @Parameter(name="Origin",in=ParameterIn.HEADER,description="WEB必须匹配固定来源，缺失时提供同源Referer")})
    @PostMapping(value="/api/admin/auth/logout-all",consumes="application/json")
    @Operation(summary="本人退出全部员工会话",description="当前密码重新确认；数据库安全版本递增；WEB和小程序设备全部失效，重复旧会话401")
    public ApiResponse.Success<Void> logoutAll(@Valid @RequestBody ConfirmationInput input,HttpServletRequest request,HttpServletResponse response){return execute(request,response,StaffSecurityOperations.Operation.LOGOUT_ALL,null,null,input.currentPassword(),null);}
    @Parameters({@Parameter(name="X-CSRF-Token",in=ParameterIn.HEADER,description="WEB Cookie写入必须；MINIPROGRAM Token无需浏览器CSRF"),
    @Parameter(name="Origin",in=ParameterIn.HEADER,description="WEB必须匹配固定来源，缺失时提供同源Referer")})
    @PutMapping(value="/api/admin/identity/users/{employeeId}/password",consumes="application/json")
    @Operation(summary="授权管理员重置员工临时密码",description="identity:user:reset-password及独立目标管理策略；version为目标资源版本；禁止本人、系统保留和tenant-admin账号；同事务强制改密并撤销目标全部设备")
    public ApiResponse.Success<Void> reset(@PathVariable UUID employeeId,@Valid @RequestBody ResetPasswordInput input,HttpServletRequest request,HttpServletResponse response){return execute(request,response,StaffSecurityOperations.Operation.RESET_PASSWORD,employeeId,version(input.version()),input.currentPassword(),input.newPassword());}
    @Parameters({@Parameter(name="X-CSRF-Token",in=ParameterIn.HEADER,description="WEB Cookie写入必须；MINIPROGRAM Token无需浏览器CSRF"),
    @Parameter(name="Origin",in=ParameterIn.HEADER,description="WEB必须匹配固定来源，缺失时提供同源Referer")})
    @PostMapping(value="/api/admin/identity/users/{employeeId}/revoke-sessions",consumes="application/json")
    @Operation(summary="授权管理员撤销目标员工全部会话",description="独立identity:user:revoke-sessions权限及目标管理范围；操作者当前密码确认；不改密码/角色/归属；version防止并发与旧请求重放")
    public ApiResponse.Success<Void> revoke(@PathVariable UUID employeeId,@Valid @RequestBody RevokeSessionsInput input,HttpServletRequest request,HttpServletResponse response){return execute(request,response,StaffSecurityOperations.Operation.REVOKE_SESSIONS,employeeId,version(input.version()),input.currentPassword(),null);}
    private ApiResponse.Success<Void> execute(HttpServletRequest request,HttpServletResponse response,StaffSecurityOperations.Operation op,UUID target,Long version,String confirmation,String replacement){
        var current=StaffHttpAuthentication.current(request);
        boolean complete;
        try{complete=security.execute(current,op,target,version,confirmation,replacement,request.getRemoteAddr());}
        catch(com.pet.platform.shared.exception.BusinessException failure){if(java.util.Set.of(com.pet.platform.shared.api.ErrorCode.SESSION_EXPIRED,com.pet.platform.shared.api.ErrorCode.SESSION_REVOKED).contains(failure.error().code()))auth.clearCurrentCookies(request,response);throw failure;}
        response.setHeader("X-Session-Cleanup",complete?"COMPLETE":"PENDING");
        if(target==null)auth.clearCurrentCookies(request,response);
        return ApiResponse.success(null);
    }
    private static long version(String value){try{return Long.parseLong(value);}catch(NumberFormatException invalid){throw new com.pet.platform.shared.exception.BusinessException(com.pet.platform.shared.api.ErrorCode.VALIDATION_FAILED);}}
}
