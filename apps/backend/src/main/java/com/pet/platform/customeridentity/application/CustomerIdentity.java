package com.pet.platform.customeridentity.application;
import java.util.UUID;
/** 权威最小客户事实，不含微信标识或权限提升字段。 */
public record CustomerIdentity(UUID customerId,UUID tenantId,long securityVersion,long tenantSecurityVersion) {}
