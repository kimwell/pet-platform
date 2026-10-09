package com.pet.platform.identity.infrastructure.bootstrap;
import java.util.Map;
/** 显式初始化能力升级，不读取密码、不启动HTTP或自动赋权。 */
public final class PlatformManagementUpgradeCommand {
 private PlatformManagementUpgradeCommand() { }
 public static void main(String[] args){System.exit(run(args,System.getenv()));}
 public static int run(String[] args,Map<String,String> env){try{if(args.length!=0)throw new IllegalArgumentException();int count=new PlatformBootstrapJdbc(env).upgradeManagement();System.out.println("平台管理权限升级成功：本次新增="+count);return 0;}catch(RuntimeException e){System.err.println("平台管理权限升级失败；未确认成功，请核查独立初始化角色及目标状态");return 2;}}
}
