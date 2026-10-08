package com.pet.platform.identity.api.authentication;

import com.pet.platform.identity.application.authentication.*;
import io.swagger.v3.oas.annotations.enums.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;

@RestController @RequestMapping("/api/admin/auth")
@SecuritySchemes({@SecurityScheme(name="StaffCookie",type=SecuritySchemeType.APIKEY,in=SecuritySchemeIn.COOKIE,paramName="__Secure-pet_staff_sid",description="生产Cookie；local HTTP使用pet_dev_staff_sid，写请求另需X-CSRF-Token及固定Origin/Referer"),
    @SecurityScheme(name="StaffToken",type=SecuritySchemeType.APIKEY,in=SecuritySchemeIn.HEADER,paramName="X-Staff-Token",description="Bearer加一个空格再加原始opaque Token；与Cookie不能并存")})
@ApiResponses({@ApiResponse(responseCode="200",description="成功，统一响应信封",useReturnTypeSchema=true),
    @ApiResponse(responseCode="400",description="非法格式或凭据混用",content=@Content(schema=@Schema(implementation=com.pet.platform.shared.api.ApiResponse.Failure.class))),
    @ApiResponse(responseCode="401",description="认证失败、未登录或会话失效",content=@Content(schema=@Schema(implementation=com.pet.platform.shared.api.ApiResponse.Failure.class))),
    @ApiResponse(responseCode="403",description="CSRF或权限拒绝",content=@Content(schema=@Schema(implementation=com.pet.platform.shared.api.ApiResponse.Failure.class))),
    @ApiResponse(responseCode="422",description="输入校验失败",content=@Content(schema=@Schema(implementation=com.pet.platform.shared.api.ApiResponse.Failure.class))),
    @ApiResponse(responseCode="429",description="频控，响应Retry-After",content=@Content(schema=@Schema(implementation=com.pet.platform.shared.api.ApiResponse.Failure.class))),
    @ApiResponse(responseCode="503",description="数据库或Redis不可用",content=@Content(schema=@Schema(implementation=com.pet.platform.shared.api.ApiResponse.Failure.class)))})
public class StaffAuthenticationController {
    public record LoginInput(@NotBlank @Size(max=64) String tenantCode,@NotBlank @Size(max=128) String loginName,
        @NotNull @Size(min=12,max=256) @Schema(format="password",writeOnly=true) String password) {
        @Override public String toString(){return "LoginInput[受限输入]";}
    }
    @Schema(requiredProperties={"csrfToken","expiresAt"}) public record CsrfResult(String csrfToken,Instant expiresAt) { }
    @Schema(requiredProperties={"identity","token"}) public record TokenLoginResult(CurrentIdentity identity,TokenResult token) { }
    @Schema(requiredProperties={"headerName","value","expiresAt"}) public record TokenResult(String headerName,String value,Instant expiresAt) { @Override public String toString(){return "TokenResult[受限凭据]";} }
    private final StaffHttpAuthentication auth;
    private final StaffSecurityOperations security;
    public StaffAuthenticationController(StaffHttpAuthentication auth,StaffSecurityOperations security){this.auth=auth;this.security=security;}
    @GetMapping("/csrf") @Operation(summary="获取员工Web CSRF凭据",description="匿名创建10分钟服务器预会话；已登录绑定当前WEB设备；no-store，不续闲置期限")
    public com.pet.platform.shared.api.ApiResponse.Success<CsrfResult> csrf(HttpServletRequest request,HttpServletResponse response){var c=auth.csrf(request,response);return com.pet.platform.shared.api.ApiResponse.success(new CsrfResult(c.csrfToken(),c.expiresAt()));}
    @Parameters({@Parameter(name="X-CSRF-Token",in=ParameterIn.HEADER,required=true,description="GET csrf取得的服务器绑定凭据"),@Parameter(name="Origin",in=ParameterIn.HEADER,description="必须匹配固定来源；缺失时必须提供同源Referer")})
    @PostMapping(value="/login",consumes="application/json") @Operation(summary="员工Web账号密码登录",description="必须先获取预会话Cookie与CSRF；提交X-CSRF-Token和固定Origin或Referer。成功轮换会话与CSRF，仅Set-Cookie，不返回Token。")
    @ApiResponse(responseCode="409",description="设备数上限",content=@Content(schema=@Schema(implementation=com.pet.platform.shared.api.ApiResponse.Failure.class)))
    public com.pet.platform.shared.api.ApiResponse.Success<CurrentIdentity> web(@Valid @RequestBody LoginInput input,HttpServletRequest request,HttpServletResponse response){var issued=auth.login(new StaffHttpAuthentication.Login(input.tenantCode(),input.loginName(),input.password()),StaffSessionPort.Channel.WEB,request,response);return com.pet.platform.shared.api.ApiResponse.success(CurrentIdentity.of(issued.identity(),issued.session()));}
    @PostMapping(value="/token/login",consumes="application/json") @Operation(summary="员工小程序账号密码登录",description="不得提交Web会话/预会话Cookie或身份Header；成功建立独立MINIPROGRAM设备，仅此响应返回原始Token。")
    @ApiResponse(responseCode="409",description="设备数上限",content=@Content(schema=@Schema(implementation=com.pet.platform.shared.api.ApiResponse.Failure.class)))
    public com.pet.platform.shared.api.ApiResponse.Success<TokenLoginResult> mini(@Valid @RequestBody LoginInput input,HttpServletRequest request,HttpServletResponse response){var issued=auth.login(new StaffHttpAuthentication.Login(input.tenantCode(),input.loginName(),input.password()),StaffSessionPort.Channel.MINIPROGRAM,request,response);return com.pet.platform.shared.api.ApiResponse.success(new TokenLoginResult(CurrentIdentity.of(issued.identity(),issued.session()),new TokenResult("X-Staff-Token",issued.token(),issued.session().expiresAt())));}
    @GetMapping("/me") @Operation(summary="当前员工身份",description="每请求读取正式有效租户、员工、角色、权限和门店授权；不续闲置期限",security={@SecurityRequirement(name="StaffCookie"),@SecurityRequirement(name="StaffToken")})
    public com.pet.platform.shared.api.ApiResponse.Success<CurrentIdentity> me(HttpServletRequest request){var c=StaffHttpAuthentication.current(request);security.cleanup(c.identity().tenantId(),c.identity().employeeId());return com.pet.platform.shared.api.ApiResponse.success(CurrentIdentity.of(c.identity(),c.session()));}
    @PostMapping("/logout") @Operation(summary="退出当前员工设备",description="Cookie需CSRF与来源校验，Token渠道无需浏览器CSRF。只撤销当前设备并删除对应Cookie/CSRF；重复无有效凭据返回401。",security={@SecurityRequirement(name="StaffCookie"),@SecurityRequirement(name="StaffToken")})
    public com.pet.platform.shared.api.ApiResponse.Success<Void> logout(HttpServletRequest request,HttpServletResponse response){auth.logout(request,response);return com.pet.platform.shared.api.ApiResponse.success(null);}
}
