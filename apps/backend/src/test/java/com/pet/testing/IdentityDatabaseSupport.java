package com.pet.testing;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** 仅临时容器的登录身份；正式脚本不生成登录账号和密码。 */
public final class IdentityDatabaseSupport {
    public static final String RUNTIME="p05_probe_runtime",MIGRATION="p05_probe_migration",BOOTSTRAP="p05_probe_bootstrap";
    private IdentityDatabaseSupport() { }
    public static void provision(PostgreSQLContainer database) {
        try(var c=DriverManager.getConnection(database.getJdbcUrl(),database.getUsername(),database.getPassword());var s=c.createStatement()) {
            s.execute(Files.readString(Path.of("../../infra/database/provision-identity-roles.sql")));
            for(String role:java.util.List.of(RUNTIME,MIGRATION,BOOTSTRAP)) {
                s.execute("CREATE ROLE "+role+" LOGIN NOSUPERUSER NOBYPASSRLS NOCREATEROLE NOCREATEDB "+(role.equals(BOOTSTRAP)?"NOINHERIT":"INHERIT"));
                try(var q=c.prepareStatement("select format('ALTER ROLE %I PASSWORD %L',?::text,?::text)")) {
                    q.setString(1,role);q.setString(2,database.getPassword());
                    try(var r=q.executeQuery()) { r.next();s.execute(r.getString(1)); }
                }
            }
            s.execute("GRANT pet_runtime TO "+RUNTIME+" WITH INHERIT TRUE, SET FALSE");
            s.execute("GRANT pet_migrator TO "+MIGRATION+" WITH INHERIT TRUE, SET TRUE");
            s.execute("GRANT pet_bootstrap TO "+BOOTSTRAP+" WITH INHERIT FALSE, SET TRUE");
        } catch(Exception failure) { throw new IllegalStateException("临时身份数据库角色预配置失败",failure); }
    }
}
