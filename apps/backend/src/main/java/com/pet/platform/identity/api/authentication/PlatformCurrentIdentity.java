package com.pet.platform.identity.api.authentication;
import com.pet.platform.identity.application.authentication.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.*;
/** 与STAFF独立判别DTO；tenant/dataScope始终null，无租户旁路。 */
@Schema(requiredProperties={"principalId","principalType","tenantId","displayName","sessionId","permissionCodes","dataScope","authorizedStoreIds","authorizationVersion","expiresAt","idleTimeoutSeconds"})
public record PlatformCurrentIdentity(UUID principalId,PlatformType principalType,Void tenantId,String displayName,UUID sessionId,
        List<String> permissionCodes,Void dataScope,List<UUID> authorizedStoreIds,@Schema(pattern="^(0|[1-9][0-9]*)$") String authorizationVersion,
        Instant expiresAt,@Schema(minimum="1",maximum="1800") int idleTimeoutSeconds) {
    public enum PlatformType { PLATFORM }
    static PlatformCurrentIdentity of(PlatformIdentity identity,IdentitySessionPort.Fact session){return new PlatformCurrentIdentity(identity.id(),PlatformType.PLATFORM,null,identity.displayName(),session.sessionId(),identity.permissions().stream().sorted().toList(),null,List.of(),Long.toString(identity.authorizationVersion()),session.expiresAt(),session.idleTimeoutSeconds());}
}
