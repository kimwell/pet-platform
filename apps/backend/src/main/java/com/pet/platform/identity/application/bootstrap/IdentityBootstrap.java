package com.pet.platform.identity.application.bootstrap;

import com.pet.platform.identity.application.PasswordService;

/** 密码派生在事务之前；数据库单函数原子创建跨owner基础关系。非Spring启动钩子。 */
public final class IdentityBootstrap {
    private final BootstrapWriter writer;
    private final PasswordService passwords;
    public IdentityBootstrap(BootstrapWriter writer,PasswordService passwords) { this.writer=writer;this.passwords=passwords; }
    public BootstrapResult initialize(BootstrapRequest input) {
        var encoded=passwords.hash(input.password());
        return writer.initialize(input.tenantCode(),input.tenantName(),input.loginName(),encoded,input.storeCode(),input.storeName());
    }
}
