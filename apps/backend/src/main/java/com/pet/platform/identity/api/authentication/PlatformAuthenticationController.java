package com.pet.platform.identity.api.authentication;
import com.pet.platform.identity.application.authentication.*;
import com.pet.platform.shared.api.ApiResponse;
import com.pet.platform.shared.security.PlatformScopeGuard;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping(value="/api/platform/auth",produces="application/json")
@SecurityScheme(name="PlatformCookie",type=SecuritySchemeType.APIKEY,in=SecuritySchemeIn.COOKIE,paramName="__Secure-pet_platform_sid",description="仅PLATFORM WEB；写请求必须独立CSRF及同源来源")
@ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200",description="成功",useReturnTypeSchema=true),
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="400",description="格式或载体冲突",content=@Content(schema=@Schema(implementation=ApiResponse.Failure.class))),
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="401",description="未登录、失效或统一登录失败",content=@Content(schema=@Schema(implementation=ApiResponse.Failure.class))),
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="403",description="平台权限、CSRF或密码确认失败",content=@Content(schema=@Schema(implementation=ApiResponse.Failure.class))),
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="409",description="设备数上限",content=@Content(schema=@Schema(implementation=ApiResponse.Failure.class))),
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="422",description="校验失败",content=@Content(schema=@Schema(implementation=ApiResponse.Failure.class))),
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="429",description="频控，Retry-After",content=@Content(schema=@Schema(implementation=ApiResponse.Failure.class))),
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="503",description="依赖不可用；不自动重试写入",content=@Content(schema=@Schema(implementation=ApiResponse.Failure.class)))})
public class PlatformAuthenticationController {
    @Schema(name="PlatformLoginInput") public record PlatformLoginInput(@NotBlank @Size(max=128) String loginName,@NotNull @Size(min=12,max=256) @Schema(format="password",writeOnly=true) String password){@Override public String toString(){return "PlatformLoginInput[受限输入]";}}
    private final PlatformHttpAuthentication auth;private final PlatformSecurityOperations security;private final PlatformScopeGuard guard;
    public PlatformAuthenticationController(PlatformHttpAuthentication auth,PlatformSecurityOperations security,PlatformScopeGuard guard){this.auth=auth;this.security=security;this.guard=guard;}
    @GetMapping("/csrf") @Operation(summary="获取平台Web CSRF",description="匿名10分钟独立服务器预会话；已登录取当前设备绑定值，不续闲置")
    public ApiResponse.Success<StaffAuthenticationController.CsrfResult> csrf(HttpServletRequest r,HttpServletResponse response){var c=auth.csrf(r,response);return ApiResponse.success(new StaffAuthenticationController.CsrfResult(c.csrfToken(),c.expiresAt()));}
    @PostMapping(value="/login",consumes="application/json") @Operation(summary="平台Web登录",description="先取平台pre Cookie和CSRF；登录成功销毁pre，轮换Token/CSRF，只Set-Cookie，不签发小程序Token")
    @Parameters({@Parameter(name="X-CSRF-Token",in=ParameterIn.HEADER,required=true),@Parameter(name="Origin",in=ParameterIn.HEADER,description="固定同源，缺失时同源Referer")})
    public ApiResponse.Success<PlatformCurrentIdentity> login(@Valid @RequestBody PlatformLoginInput input,HttpServletRequest r,HttpServletResponse response){var issued=auth.login(input.loginName(),input.password(),r,response);return ApiResponse.success(PlatformCurrentIdentity.of(issued.identity(),issued.session()));}
    @GetMapping("/me") @SecurityRequirement(name="PlatformCookie") @Operation(summary="当前平台身份",description="platform:session:manage；每请求DB重验，tenantId/dataScope=null，不建立TenantContext")
    public ApiResponse.Success<PlatformCurrentIdentity> me(HttpServletRequest r){guard.requirePermission(PlatformPermissions.SESSION);var c=PlatformHttpAuthentication.current(r);security.cleanup(c.identity().id());return ApiResponse.success(PlatformCurrentIdentity.of(c.identity(),c.session()));}
    @PostMapping("/logout") @SecurityRequirement(name="PlatformCookie") @Operation(summary="退出当前平台会话",description="platform:session:manage；独立CSRF与同源来源；只退出当前平台设备")
    public ApiResponse.Success<Void> logout(HttpServletRequest r,HttpServletResponse response){guard.requirePermission(PlatformPermissions.SESSION);auth.logout(r,response);return ApiResponse.success(null);}
    @PutMapping(value="/password",consumes="application/json") @SecurityRequirement(name="PlatformCookie") @Operation(summary="本人修改平台密码",description="platform:credential:change；旧密码确认，同事务改密/代际/记录/撤销意图；全部旧平台会话失效，员工不受影响")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200",description="data=null；数据库已提交，物理清理状态在Header",useReturnTypeSchema=true,headers=@io.swagger.v3.oas.annotations.headers.Header(name="X-Session-Cleanup",schema=@Schema(type="string",allowableValues={"COMPLETE","PENDING"})))
    public ApiResponse.Success<Void> password(@Valid @RequestBody StaffSecurityController.ChangePasswordInput input,HttpServletRequest r,HttpServletResponse response){return execute(input.currentPassword(),input.newPassword(),r,response);}
    @PostMapping(value="/logout-all",consumes="application/json") @SecurityRequirement(name="PlatformCookie") @Operation(summary="本人退出全部平台会话",description="platform:session:manage；当前密码确认，递增数据库安全代际，不改变密码或STAFF会话")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200",description="data=null；数据库已提交，物理清理状态在Header",useReturnTypeSchema=true,headers=@io.swagger.v3.oas.annotations.headers.Header(name="X-Session-Cleanup",schema=@Schema(type="string",allowableValues={"COMPLETE","PENDING"})))
    public ApiResponse.Success<Void> logoutAll(@Valid @RequestBody StaffSecurityController.ConfirmationInput input,HttpServletRequest r,HttpServletResponse response){return execute(input.currentPassword(),null,r,response);}
    private ApiResponse.Success<Void> execute(String old,String next,HttpServletRequest r,HttpServletResponse response){
        try{boolean done=security.execute(PlatformHttpAuthentication.current(r),old,next,r.getRemoteAddr());response.setHeader("X-Session-Cleanup",done?"COMPLETE":"PENDING");auth.clearCookies(response);return ApiResponse.success(null);}
        catch(com.pet.platform.shared.exception.BusinessException failure){if(failure.error().code()==com.pet.platform.shared.api.ErrorCode.SESSION_REVOKED)auth.clearCookies(response);throw failure;}
    }
}
