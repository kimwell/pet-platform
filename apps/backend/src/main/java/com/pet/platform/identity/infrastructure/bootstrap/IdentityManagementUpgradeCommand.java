package com.pet.platform.identity.infrastructure.bootstrap;

import java.io.PrintStream;
import java.util.Map;
import java.util.UUID;

/** 显式权限升级；不注册Bean、不在普通启动或迁移自动调用。 */
public final class IdentityManagementUpgradeCommand {
    private IdentityManagementUpgradeCommand() { }
    public static void main(String[] args) {
        int code=run(args,System.getenv(),System.out,System.err);
        if(code!=0) System.exit(code);
    }
    public static int run(String[] args, Map<String,String> environment, PrintStream out, PrintStream error) {
        try {
            if(args.length!=2 || !args[0].equals("--tenant-id") || !args[1].matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}"))
                throw new IllegalArgumentException("用法：identity-management-upgrade --tenant-id <规范UUID v4>");
            int changed=new BootstrapJdbc(environment).upgradeIdentityManagement(UUID.fromString(args[1]));
            out.println("员工与角色管理权限升级完成，新增范围条数="+changed);return 0;
        } catch(IllegalArgumentException | IllegalStateException failure) {
            error.println("员工与角色管理权限升级失败；核对命令选项、初始化角色、迁移及保留角色");return 2;
        }
    }
}
