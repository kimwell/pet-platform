package com.pet.platform.identity.application.authentication;
import java.util.Set;
/** 平台固定权限目录，不包含租户业务或通配符。 */
public final class PlatformPermissions {
 private PlatformPermissions() { }
 public static final String SESSION="platform:session:manage",PASSWORD="platform:credential:change",REDIS="platform:redis:operate";
 public static final Set<String> DECLARED=Set.of("platform:session:manage","platform:credential:change","platform:redis:operate","platform:tenant:list","platform:tenant:detail","platform:tenant:create","platform:tenant:update","platform:tenant:enable","platform:tenant:disable","platform:account:list","platform:account:detail","platform:account:create","platform:account:update","platform:account:enable","platform:account:disable","platform:account:grant","platform:account:reset-password","platform:account:revoke-sessions","platform:store:control-list","platform:store:control-create","platform:store:control-update","platform:store:control-status");
}
