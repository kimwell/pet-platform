package com.pet.platform.identity.infrastructure.bootstrap;

import org.flywaydb.core.Flyway;

/** 产物内正式独立迁移入口，和普通Application/prod启动分离。 */
public final class MigrationCommand {
    private MigrationCommand() { }
    public static void main(String[] args) {
        try {
            if(args.length!=0) throw new IllegalArgumentException("迁移命令不接受参数");
            var env=System.getenv();
            try(var connection=CommandDatabase.connect(env,"PET_MIGRATION","pet_migrator")) { /* 先核对真实连接身份 */ }
            var flyway=Flyway.configure().dataSource(CommandDatabase.url(env,"PET_MIGRATION"),CommandDatabase.required(env,"PET_MIGRATION_DATABASE_USERNAME"),CommandDatabase.required(env,"PET_MIGRATION_DATABASE_PASSWORD"))
                .locations("classpath:db/migration").defaultSchema("public")
                .cleanDisabled(true).baselineOnMigrate(false).validateOnMigrate(true).validateMigrationNaming(true).outOfOrder(false)
                .ignoreMigrationPatterns(new String[0]).failOnMissingLocations(true).load();
            var result=flyway.migrate();
            System.out.println("正式迁移成功：本次执行数量="+result.migrationsExecuted);
        } catch(Exception failure) { System.err.println("正式迁移失败；请核查目标、迁移身份、角色预配置及历史校验和；不会自动repair、baseline或clean");System.exit(2); }
    }
}
