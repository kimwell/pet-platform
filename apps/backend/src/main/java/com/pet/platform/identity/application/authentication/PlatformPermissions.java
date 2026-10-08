package com.pet.platform.identity.application.authentication;
import java.util.Set;
/** 仅当前控制面能力，不包含租户业务或通配符。 */
public final class PlatformPermissions {
    private PlatformPermissions() { }
    public static final String SESSION="platform:session:manage",PASSWORD="platform:credential:change",REDIS="platform:redis:operate";
    public static final Set<String> DECLARED=Set.of(SESSION,PASSWORD,REDIS);
}
