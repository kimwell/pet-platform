package com.pet.platform.identity.application.bootstrap;
import com.pet.platform.identity.application.*;
import java.util.UUID;
/** 无启动钩子；初始化首个平台账号，不修复、重置或补授权。 */
public final class PlatformBootstrap {
    private final PlatformBootstrapWriter writer;
    private final PasswordService passwords;
    public PlatformBootstrap(PlatformBootstrapWriter writer,PasswordService passwords){this.writer=writer;this.passwords=passwords;}
    public UUID initialize(String login,String name,char[] password){return writer.initialize(IdentityNames.loginName(login),IdentityNames.name(name),passwords.hash(password));}
}
