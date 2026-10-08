package com.pet.platform.platform.application;

import java.util.Set;

/** 租户内门店目录的基础管理权限，不属于平台管理员跨租户权限。 */
public final class PlatformPermissions {
    private PlatformPermissions() { }
    public static final Set<String> STORE_MANAGEMENT = Set.of("platform:store:list","platform:store:update");
}
