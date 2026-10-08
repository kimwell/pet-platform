package com.pet.platform.identity.infrastructure.bootstrap;

import com.pet.platform.identity.application.PasswordService;
import com.pet.platform.identity.application.bootstrap.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** 显式命令主入口，普通Application不调用；密码只从不回显Console或受控stdin输入。 */
public final class BootstrapCommand {
    private BootstrapCommand() { }
    public static void main(String[] args) {
        int code=run(args,System.getenv(),System.in,System.out,System.err);
        if(code!=0) System.exit(code);
    }
    public static int run(String[] args,Map<String,String> environment,InputStream input,PrintStream out,PrintStream error) {
        char[] password=null;
        try {
            var options=parse(args);
            if(options.containsKey("password-stdin")) password=readPassword(input);
            else {
                var console=System.console();
                if(console==null) throw new IllegalArgumentException("没有交互控制台；自动化须显式使用--password-stdin");
                password=console.readPassword("管理员初始密码（不回显）：");
            }
            try(var request=new BootstrapRequest(options.get("tenant-code"),options.get("tenant-name"),options.get("admin-login"),password,options.get("store-code"),options.get("store-name"))) {
                var result=new IdentityBootstrap(new BootstrapJdbc(environment),new PasswordService()).initialize(request);
                out.println("初始化成功：tenantId="+result.tenantId()+" employeeId="+result.employeeId()+" storeId="+result.storeId());
            }
            return 0;
        } catch (IllegalArgumentException | IllegalStateException | IOException failure) {
            // 只输出本项目固定安全消息，不输出JDBC原异常或参数。
            error.println(failure instanceof IOException ? "密码输入读取失败" : failure.getMessage());return 2;
        } finally { if(password!=null) Arrays.fill(password,'\0'); }
    }
    private static Map<String,String> parse(String[] args) {
        var options=new HashMap<String,String>();
        var allowed=Set.of("tenant-code","tenant-name","admin-login","store-code","store-name","password-stdin");
        for(int i=0;i<args.length;i++) {
            if(!args[i].startsWith("--") || !allowed.contains(args[i].substring(2))) throw new IllegalArgumentException("初始化命令选项不合法；禁止通过参数传密码");
            var key=args[i].substring(2);
            String value=key.equals("password-stdin") ? "true" : (++i<args.length && !args[i].startsWith("--") ? args[i] : null);
            if(value==null || options.putIfAbsent(key,value)!=null) throw new IllegalArgumentException("初始化选项缺值或重复");
        }
        for(var required:List.of("tenant-code","tenant-name","admin-login")) if(!options.containsKey(required)) throw new IllegalArgumentException("缺少初始化选项："+required);
        return options;
    }
    static char[] readPassword(InputStream input) throws IOException {
        var reader=new InputStreamReader(input,StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT));
        char[] buffer=new char[258];int length=0;
        try {
            int ch;
            while((ch=reader.read())!=-1 && ch!='\n') {
                if(length==buffer.length) throw new IllegalArgumentException("密码超过允许长度");
                buffer[length++]=(char)ch;
            }
            if(length>0 && buffer[length-1]=='\r') length--; // 仅移除stdin行结束符，不trim密码。
            return Arrays.copyOf(buffer,length);
        } finally { Arrays.fill(buffer,'\0'); }
    }
}
