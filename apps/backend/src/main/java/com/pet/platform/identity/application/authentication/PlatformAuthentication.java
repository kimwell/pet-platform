package com.pet.platform.identity.application.authentication;
import com.pet.platform.identity.application.*;
import java.util.*;
import org.springframework.stereotype.Service;
@Service public final class PlatformAuthentication {
    private final PlatformIdentityStore store;private final PasswordService passwords;private final String dummyHash;
    public PlatformAuthentication(PlatformIdentityStore store,PasswordService passwords){this.store=store;this.passwords=passwords;char[] raw=UUID.randomUUID().toString().toCharArray();try{dummyHash=passwords.hash(raw);}finally{Arrays.fill(raw,'\0');}}
    public Optional<PlatformIdentity> verify(String name,char[] password){
        var c=store.candidate(IdentityNames.loginName(name));
        if(c.isEmpty()){passwords.matches(password,dummyHash);return Optional.empty();}
        var fact=c.orElseThrow();if(!fact.matches(passwords,password))return Optional.empty();
        return store.load(fact.id()).filter(i -> i.securityVersion()==fact.securityVersion());
    }
    public Optional<PlatformIdentity> load(UUID id){return store.load(id);}
}
