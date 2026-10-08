package com.pet.platform.identity.application.authentication;
import java.util.*;
public record PlatformIdentity(UUID id,String displayName,long securityVersion,long authorizationVersion,Set<String> permissions) {
    public PlatformIdentity{permissions=Set.copyOf(permissions);if(!PlatformPermissions.DECLARED.containsAll(permissions))throw new IllegalArgumentException("未登记的平台权限");}
    @Override public String toString(){return "PlatformIdentity[受限控制面事实]";}
}
