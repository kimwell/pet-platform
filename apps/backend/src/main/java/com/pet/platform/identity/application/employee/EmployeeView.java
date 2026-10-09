package com.pet.platform.identity.application.employee;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** 员工读取白名单；列表和详情一致，不输出角色、门店关系或管理能力。 */
@Schema(requiredProperties={"id","loginName","displayName","status","createdAt","updatedAt"})
public record EmployeeView(UUID id, String loginName, String displayName,
        @Schema(allowableValues={"ACTIVE","DISABLED"}) String status, Instant createdAt, Instant updatedAt) { }
