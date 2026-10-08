package com.pet.platform.customeridentity.api;
import com.pet.platform.customeridentity.application.CustomerHttpAuthentication;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.enums.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
@RestController @RequestMapping("/api/customer/auth")
@SecurityScheme(name="CustomerToken",type=SecuritySchemeType.APIKEY,in=SecuritySchemeIn.HEADER,paramName="X-Customer-Token",description="Bearer加一个空格再加原始opaque Token；仅客户小程序，无客户Cookie")
@ApiResponses({@ApiResponse(responseCode="200",description="成功，统一响应信封",useReturnTypeSchema=true),@ApiResponse(responseCode="400",description="格式或载体错误",content=@Content(schema=@Schema(implementation=com.pet.platform.shared.api.ApiResponse.Failure.class))),@ApiResponse(responseCode="401",description="登录失败或会话无效",content=@Content(schema=@Schema(implementation=com.pet.platform.shared.api.ApiResponse.Failure.class))),@ApiResponse(responseCode="422",description="输入校验失败",content=@Content(schema=@Schema(implementation=com.pet.platform.shared.api.ApiResponse.Failure.class))),@ApiResponse(responseCode="429",description="频控，含Retry-After",content=@Content(schema=@Schema(implementation=com.pet.platform.shared.api.ApiResponse.Failure.class))),@ApiResponse(responseCode="503",description="依赖/微信不可用；结果不确定须重新取得code",content=@Content(schema=@Schema(implementation=com.pet.platform.shared.api.ApiResponse.Failure.class)))})
public class CustomerAuthenticationController {
 public record WechatLoginInput(@NotBlank @Size(max=32) String tenantCode,@NotBlank @Size(max=32) String entryId,@NotBlank @Size(max=256) @Schema(writeOnly=true) String code){@Override public String toString(){return "WechatLoginInput[受限输入]";}}
 @Schema(requiredProperties={"identity","token"}) public record CustomerTokenLoginResult(CustomerCurrentIdentity identity,CustomerTokenResult token){}
 @Schema(requiredProperties={"headerName","value","expiresAt"}) public record CustomerTokenResult(@Schema(allowableValues={"X-Customer-Token"}) String headerName,String value,Instant expiresAt){@Override public String toString(){return "CustomerTokenResult[受限凭据]";}}
 private final CustomerHttpAuthentication auth;
 public CustomerAuthenticationController(CustomerHttpAuthentication auth){this.auth=auth;}
 @PostMapping(value="/wechat/login",consumes="application/json") @Operation(summary="客户微信登录",description="服务端入口白名单解析有效租户和应用；一次code交换，不自动重试。共享AppID按租户独立注册，不提供员工权限。")
 @ApiResponse(responseCode="409",description="设备上限",content=@Content(schema=@Schema(implementation=com.pet.platform.shared.api.ApiResponse.Failure.class)))
 public com.pet.platform.shared.api.ApiResponse.Success<CustomerTokenLoginResult> login(@Valid @RequestBody WechatLoginInput input,HttpServletRequest request){var r=auth.login(input.tenantCode(),input.entryId(),input.code(),request);return com.pet.platform.shared.api.ApiResponse.success(new CustomerTokenLoginResult(CustomerCurrentIdentity.of(r.identity(),r.session()),new CustomerTokenResult("X-Customer-Token",r.token(),r.session().expiresAt())));}
 @GetMapping("/me") @Operation(summary="当前客户身份",description="真实会话及有效租户/客户/绑定/安全版本重验，不续闲置期限；不返回微信标识",security=@SecurityRequirement(name="CustomerToken"))
 public com.pet.platform.shared.api.ApiResponse.Success<CustomerCurrentIdentity> me(HttpServletRequest request){var c=CustomerHttpAuthentication.current(request);return com.pet.platform.shared.api.ApiResponse.success(CustomerCurrentIdentity.of(c.identity(),c.session()));}
 @PostMapping("/logout-all") @Operation(summary="退出客户全部会话",description="无需密码；有效当前客户Token再次验证，数据库同事务递增安全代际。旧会话即刻逻辑失效，物理清理失败标PENDING，后续新登录清旧代际；不能自动重放。",security=@SecurityRequirement(name="CustomerToken"))
 @ApiResponse(responseCode="200",description="全部旧客户会话已失效",useReturnTypeSchema=true,headers=@Header(name="X-Session-Cleanup",schema=@Schema(type="string",allowableValues={"COMPLETE","PENDING"})))
 public com.pet.platform.shared.api.ApiResponse.Success<Void> logoutAll(HttpServletRequest request,HttpServletResponse response){response.setHeader("X-Session-Cleanup",auth.logoutAll(request)?"COMPLETE":"PENDING");return com.pet.platform.shared.api.ApiResponse.success(null);}
 @PostMapping("/logout") @Operation(summary="退出当前客户会话",description="撤销当前客户设备，其他身份域不受影响；重复已退出请求401",security=@SecurityRequirement(name="CustomerToken"))
 public com.pet.platform.shared.api.ApiResponse.Success<Void> logout(HttpServletRequest request){auth.logout(request);return com.pet.platform.shared.api.ApiResponse.success(null);}
}
