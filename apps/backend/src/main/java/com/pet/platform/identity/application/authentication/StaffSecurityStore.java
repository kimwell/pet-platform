package com.pet.platform.identity.application.authentication;

import com.pet.platform.identity.application.PasswordService;
import java.util.*;
import java.util.function.Supplier;
import com.fasterxml.jackson.annotation.JsonIgnoreType;

/** 本轮固定安全事务端口，不开放Entity绑定、任意SQL或跨租户入口。 */
public interface StaffSecurityStore {
    @JsonIgnoreType
    final class Credential {
        private final String hash;
        public final long securityVersion,version;
        public final boolean mustChange,reserved,tenantAdmin,active;
        public Credential(String hash,long security,long version,boolean mustChange,boolean reserved,boolean tenantAdmin,boolean active) {
            this.hash=hash;this.securityVersion=security;this.version=version;this.mustChange=mustChange;this.reserved=reserved;this.tenantAdmin=tenantAdmin;this.active=active;
        }
        public boolean matches(PasswordService service,char[] value){return service.matches(value,hash);}
        @Override public String toString(){return "Credential[受限凭据]";}
    }
    <T> T transaction(UUID tenant,Supplier<T> work);
    void lockTenant();
    Optional<Credential> lockEmployee(UUID employee);
    Set<UUID> employeeStores(UUID employee);
    void change(UUID employee,String encoded,boolean mustChange);
    long revoke(UUID employee);
    void event(UUID operator,UUID target,String operation,String result,String traceId);
    long pending(UUID employee);
    void completed(UUID employee,long cutoff);
}
