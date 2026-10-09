package com.pet.platform.identity.application.management;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
public final class ControlAccountModels {
 private ControlAccountModels() { }
 @Schema(requiredProperties={"id","loginName","displayName","status","version","permissions","createdAt","updatedAt"})
 public record ControlAccountView(UUID id,String loginName,String displayName,@Schema(allowableValues={"ACTIVE","DISABLED"}) String status,String version,List<String> permissions,Instant createdAt,Instant updatedAt) { }
 public record CreateControlAccount(@NotNull String loginName,@NotNull String displayName,@NotNull @Size(min=12,max=256) @Schema(format="password",writeOnly=true) String initialPassword,@NotNull @Size(max=64) List<@NotNull String> permissions){@Override public String toString(){return "CreateControlAccount[受限输入]";}}
 public record EditControlAccount(@NotNull String version,@NotNull String displayName) { }
 public record ControlAccountGrants(@NotNull String version,@NotNull @Size(max=64) List<@NotNull String> permissions) { }
 public record ControlAccountReset(@NotNull String version,@NotNull @Size(min=12,max=256) @Schema(format="password",writeOnly=true) String newPassword){@Override public String toString(){return "ControlAccountReset[受限输入]";}}
 public record ControlAccountRevoke(@NotNull String version) { }
 @Schema(requiredProperties={"code","grantable"}) public record ControlPermissionOption(String code,boolean grantable) { }
}
