package com.pet.platform.platform.application;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;
/** 控制面元数据投影；不公开身份凭据及安全代际。 */
public final class ControlModels {
 private ControlModels() { }
 @Schema(requiredProperties={"id","code","name","status","initialized","version","createdAt","updatedAt"})
 public record TenantView(UUID id,String code,String name,@Schema(allowableValues={"ACTIVE","DISABLED"}) String status,boolean initialized,String version,Instant createdAt,Instant updatedAt) { }
 @Schema(requiredProperties={"id","tenantId","code","name","status","version","createdAt","updatedAt"})
 public record ControlStoreView(UUID id,UUID tenantId,String code,String name,@Schema(allowableValues={"ACTIVE","DISABLED"}) String status,String version,Instant createdAt,Instant updatedAt) { }
 public record CreateTenant(@NotNull String code,@NotNull String name) { }
 public record EditControlName(@NotNull String version,@NotNull String name) { }
 public record ControlStatus(@NotNull String version,@NotNull @Pattern(regexp="ACTIVE|DISABLED") String status) { }
 public record CreateControlStore(@NotNull String code,@NotNull String name) { }
 @Schema(requiredProperties={"id","version","sessionCleanupComplete"})
 public record ControlMutation(UUID id,String version,boolean sessionCleanupComplete) { }
}
