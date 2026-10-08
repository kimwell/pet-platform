package com.pet.platform.identity.application.authentication;
import java.util.*;
import java.util.function.Supplier;
/** 固定控制面查询与本人安全事务，不向普通业务开放。 */
public interface PlatformIdentityStore {
    Optional<PlatformCredential> candidate(String normalizedLogin);
    Optional<PlatformIdentity> load(UUID trustedId);
    <T> T transaction(UUID trustedId,Supplier<T> work);
    PlatformCredential lockCredential();long revoke(String replacementHash);
    void event(UUID actor,String operation,String result,String trace);
    long pending(UUID id);void completed(UUID id,long cutoff);
}
