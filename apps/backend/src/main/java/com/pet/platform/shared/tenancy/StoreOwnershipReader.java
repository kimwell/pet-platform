package com.pet.platform.shared.tenancy;

import java.util.Optional;
import java.util.UUID;

/** 内部事实查询端口，后续由platform/store实现；不能从调用者传入Store对象推断归属。 */
@FunctionalInterface
public interface StoreOwnershipReader {
    Optional<UUID> findTenantId(UUID storeId);
}
