package com.pet.platform.identity.application.authentication;
import com.pet.platform.identity.application.PasswordService;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import java.util.UUID;
/** 无哈希getter；仅密码匹配和不可变安全代际。 */
@JsonIgnoreType public final class PlatformCredential {
    private final UUID id;private final String hash;private final long security;
    public PlatformCredential(UUID id,String hash,long security){this.id=id;this.hash=hash;this.security=security;}
    public UUID id(){return id;}public long securityVersion(){return security;}
    public boolean matches(PasswordService p,char[] raw){return p.matches(raw,hash);}
    @Override public String toString(){return "PlatformCredential[受限凭据]";}
}
