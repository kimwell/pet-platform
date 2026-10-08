# 数据库迁移约定

P03-02 实施日期：2026-10-08。版本由 [唯一矩阵](../development/VERSION-MATRIX.md) 管理；配置 owner 为 [CONFIGURATION](../architecture/CONFIGURATION.md)。

生产 SQL 只放 `apps/backend/src/main/resources/db/migration/`。当前没有正式业务表，目录只有规则说明；Flyway 自身的 `flyway_schema_history` 是技术执行记录，不是业务表。不得为了启动或验收新增无用途的表。后续已安装模块按冻结的 locations 组织，运行关闭的模块仍保留历史迁移。

文件名沿用全应用唯一递增整数 `V<整数>__<module>_<英文含义>.sql`，例如未来有效用途对应的 `V1__platform_...sql`。这不是允许本轮创建平台表；各模块不能独立从 V1 开始。新迁移在最后编号后追加，不事后插入低编号；已执行版本禁止改名、改内容或删除。重复迁移只用于明确登记的视图/函数。

local/test 默认在应用启动时迁移，Spring Boot 的数据库初始化依赖保证 Flyway 先于 JPA `validate`。没有迁移时仍执行 Flyway 验证并建立必要的 history；Hibernate 不创建或更新结构。`validate-on-migrate=true`、`validate-migration-naming=true`、`baseline-on-migrate=false`、`out-of-order=false`、`clean-disabled=true`、空忽略模式以及 `fail-on-missing-locations=true` 固定在工程配置中。危险配置覆盖被启动校验拒绝。

校验和冲突必须阻止启动。不能自动 `repair`、自动 baseline 非空数据库、忽略缺失迁移或在应用流程 `clean`。发现失败时保留数据库、history 和原 SQL，比较实际结构、执行历史与源文件；修复策略必须针对真实差异，不能删库重建掩盖错误。需要非事务 DDL 时说明失败恢复步骤。

生产沿用冻结方案：独立迁移任务使用独立的迁移角色，先执行与当前产物一致的迁移，然后运行进程使用受限角色启动并 `validate`。`prod` 禁止启动迁移，不回退本地 URL/凭据。本轮无正式 SQL，不新增无用途的生产迁移执行器；生产任务、角色权限和 RLS 的实际配置/验收仍归 P04 及部署阶段，不能以本轮本地管理员技术容器宣称完成。

本地启动方式见 [本地开发](../development/LOCAL-DEVELOPMENT.md)：向 Java 进程注入数据库配置后使用 Wrapper 或生产 JAR。关闭 local 迁移开关只用于结构已由独立任务更新的场景，JPA 仍必须 `validate`，不能以关闭迁移跳过结构检查。

测试 SQL 在 `src/test/resources/persistence-migrations/`，测试实体、Repository 和服务在 `src/test/java/com/pet/testing/persistence/`。测试配置显式指定 `classpath:persistence-migrations`，生产默认 locations 不含此目录；测试实体位于生产包扫描之外。生产 JAR 不包含测试源码/资源/依赖，产物审计作为本阶段门禁。

`./mvnw clean verify` 使用冻结 PostgreSQL Testcontainers 镜像，必须有可用 Docker；没有 Docker 则失败并记录 NOT_EXECUTED，不能改用 H2 或 `disabledWithoutDocker`。校验和冲突只修改临时迁移副本，结构反例只改独立测试容器中的 schema。迁移、重复执行、拒绝 baseline/clean、结构不匹配及实际启动证据见 [P03-02](../testing/P03-02-VERIFICATION.md)。
