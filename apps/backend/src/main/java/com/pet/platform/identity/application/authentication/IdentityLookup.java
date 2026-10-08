package com.pet.platform.identity.application.authentication;

import java.util.Optional;
import java.util.UUID;

/** 固定认证读端口，无筛选/分页/实体返回；结构门禁限制到身份认证适配器。 */
public interface IdentityLookup {
    Optional<AuthenticationCandidate> candidate(String normalizedTenantCode,String normalizedLogin);
    Optional<StaffIdentity> load(UUID tenantId,UUID employeeId);
}
