package com.pet.platform.identity.infrastructure.bootstrap;

import java.net.URI;
import java.sql.*;
import java.util.Map;

/** 独立命令的数据库输入，只报告名称和固定错误；不启动Web/JPA/Redis。 */
final class CommandDatabase {
    private CommandDatabase() { }
    static String required(Map<String,String> environment,String name) {
        String value=environment.get(name);
        if(value==null || value.isBlank()) throw new IllegalArgumentException("缺少命令配置："+name);
        return value;
    }
    static String url(Map<String,String> environment,String prefix) {
        var url=required(environment,prefix+"_DATABASE_URL");
        try {
            var uri=URI.create(url.substring(5));
            if (!url.startsWith("jdbc:postgresql://") || uri.getHost()==null || uri.getUserInfo()!=null || uri.getQuery()!=null || uri.getFragment()!=null || !uri.getPath().matches("/[A-Za-z0-9_-]+") || uri.getPort()==0 || uri.getPort()>65535) throw new IllegalArgumentException();
        } catch (RuntimeException failure) { throw new IllegalArgumentException("命令数据库地址必须为无凭据和参数的PostgreSQL地址"); }
        return url;
    }
    static Connection connect(Map<String,String> environment,String prefix,String role) throws SQLException {
        var properties=new java.util.Properties();
        properties.setProperty("user",required(environment,prefix+"_DATABASE_USERNAME"));
        properties.setProperty("password",required(environment,prefix+"_DATABASE_PASSWORD"));
        properties.setProperty("connectTimeout","5");properties.setProperty("socketTimeout","30");
        var connection=DriverManager.getConnection(url(environment,prefix),properties);
        try {
            verifyLogin(connection,role);
            return connection;
        } catch (SQLException | RuntimeException failure) { connection.close();throw failure; }
    }
    static void verifyLogin(Connection connection,String role) throws SQLException {
        try(var q=connection.prepareStatement("select rolsuper or rolbypassrls or rolcreaterole or rolcreatedb,pg_has_role(current_user,?,'MEMBER'),pg_has_role(current_user,'pet_auth_owner','MEMBER'),pg_has_role(current_user,'pet_bootstrap_owner','MEMBER'),pg_has_role(current_user,'pet_migrator','MEMBER'),pg_has_role(current_user,'pet_platform_auth_owner','MEMBER'),pg_has_role(current_user,'pet_platform_bootstrap_owner','MEMBER'),pg_has_role(current_user,'pet_bootstrap','MEMBER'),pg_has_role(current_user,'pet_platform_bootstrap','MEMBER') from pg_catalog.pg_roles where rolname=current_user")) {
            q.setString(1,role);
            try(var r=q.executeQuery()) {
                if(!r.next() || r.getBoolean(1) || !r.getBoolean(2) || ((role.equals("pet_bootstrap") || role.equals("pet_platform_bootstrap")) && (r.getBoolean(3)||r.getBoolean(4)||r.getBoolean(5)))) throw new IllegalStateException("命令执行账号权限不符合要求");
                if(role.equals("pet_platform_bootstrap") && (r.getBoolean(6)||r.getBoolean(7)||r.getBoolean(8)))throw new IllegalStateException("平台初始化账号不得兼具租户初始化或函数owner能力");
                if(role.equals("pet_bootstrap") && (r.getBoolean(6)||r.getBoolean(7)||r.getBoolean(9)))throw new IllegalStateException("租户初始化账号不得兼具平台初始化或函数owner能力");
            }
        }
    }
}
