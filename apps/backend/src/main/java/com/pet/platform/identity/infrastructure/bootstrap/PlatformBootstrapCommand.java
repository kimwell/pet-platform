package com.pet.platform.identity.infrastructure.bootstrap;
import com.pet.platform.identity.application.PasswordService;
import com.pet.platform.identity.application.bootstrap.PlatformBootstrap;
import java.io.*;
import java.util.*;
/** 密码仅不回显Console或显式受控stdin；普通启动不调用。 */
public final class PlatformBootstrapCommand {
    private PlatformBootstrapCommand() { }
    public static void main(String[] args){int code=run(args,System.getenv(),System.in,System.out,System.err);if(code!=0)System.exit(code);}
    public static int run(String[] args,Map<String,String> env,InputStream input,PrintStream out,PrintStream err){
        char[] password=null;
        try {
            var options=new HashMap<String,String>();
            for(int i=0;i<args.length;i++){
                String key=args[i];if(!Set.of("--admin-login","--admin-name","--password-stdin").contains(key))throw new IllegalArgumentException("平台初始化选项不合法；禁止参数密码");
                String value=key.equals("--password-stdin")?"true":(++i<args.length && !args[i].startsWith("--")?args[i]:null);
                if(value==null || options.putIfAbsent(key,value)!=null)throw new IllegalArgumentException("平台初始化选项缺值或重复");
            }
            if(!options.containsKey("--admin-login") || !options.containsKey("--admin-name"))throw new IllegalArgumentException("必须提供平台登录名与显示名");
            if(options.containsKey("--password-stdin"))password=BootstrapCommand.readPassword(input);
            else {var console=System.console();if(console==null)throw new IllegalArgumentException("没有交互控制台；须显式使用--password-stdin");password=console.readPassword("平台初始密码（不回显）：");}
            UUID id=new PlatformBootstrap(new PlatformBootstrapJdbc(env),new PasswordService()).initialize(options.get("--admin-login"),options.get("--admin-name"),password);
            out.println("平台初始化成功：principalId="+id);return 0;
        }catch(IOException failure){err.println("平台密码输入读取失败");return 2;}
        catch(IllegalArgumentException | IllegalStateException failure){err.println(failure.getMessage());return 2;}
        finally{if(password!=null)Arrays.fill(password,'\0');}
    }
}
