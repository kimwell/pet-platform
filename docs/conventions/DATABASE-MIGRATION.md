# 数据库迁移约定

当前实施状态见本文P05-01章节及[P05-01验证](../testing/P05-01-VERIFICATION.md)。旧阶段“尚未实现”描述保留为历史范围；冻结安全契约不变。
P03-02 实施日期：2026-10-08。版本由 [唯一矩阵](../development/VERSION-MATRIX.md) 管理；配置 owner 为 [CONFIGURATION](../architecture/CONFIGURATION.md)。

生产 SQL 只放 `apps/backend/src/main/resources/db/migration/`。当前没有正式业务表，目录只有规则说明；Flyway 自身的 `flyway_schema_history` 是技术执行记录，不是业务表。不得为了启动或验收新增无用途的表。后续已安装模块按冻结的 locations 组织，运行关闭的模块仍保留历史迁移。

文件名沿用全应用唯一递增整数 `V<整数>__<module>_<英文含义>.sql`，例如未来有效用途对应的 `V1__platform_...sql`。这不是允许本轮创建平台表；各模块不能独立从 V1 开始。新迁移在最后编号后追加，不事后插入低编号；已执行版本禁止改名、改内容或删除。重复迁移只用于明确登记的视图/函数。

local/test 默认在应用启动时迁移，Spring Boot 的数据库初始化依赖保证 Flyway 先于 JPA `validate`。没有迁移时仍执行 Flyway 验证并建立必要的 history；Hibernate 不创建或更新结构。`validate-on-migrate=true`、`validate-migration-naming=true`、`baseline-on-migrate=false`、`out-of-order=false`、`clean-disabled=true`、空忽略模式以及 `fail-on-missing-locations=true` 固定在工程配置中。危险配置覆盖被启动校验拒绝。

校验和冲突必须阻止启动。不能自动 `repair`、自动 baseline 非空数据库、忽略缺失迁移或在应用流程 `clean`。发现失败时保留数据库、history 和原 SQL，比较实际结构、执行历史与源文件；修复策略必须针对真实差异，不能删库重建掩盖错误。需要非事务 DDL 时说明失败恢复步骤。

生产沿用冻结方案：独立迁移任务使用独立的迁移角色，先执行与当前产物一致的迁移，然后运行进程使用受限角色启动并 `validate`。`prod` 禁止启动迁移，不回退本地 URL/凭据。本轮无正式 SQL，不新增无用途的生产迁移执行器；生产任务、角色权限和 RLS 的实际配置/验收仍归 P04 及部署阶段，不能以本轮本地管理员技术容器宣称完成。

本地启动方式见 [本地开发](../development/LOCAL-DEVELOPMENT.md)：向 Java 进程注入数据库配置后使用 Wrapper 或生产 JAR。关闭 local 迁移开关只用于结构已由独立任务更新的场景，JPA 仍必须 `validate`，不能以关闭迁移跳过结构检查。

测试 SQL 在 `src/test/resources/persistence-migrations/`，测试实体、Repository 和服务在 `src/test/java/com/pet/testing/persistence/`。测试配置显式指定 `classpath:persistence-migrations`，生产默认 locations 不含此目录；测试实体位于生产包扫描之外。生产 JAR 不包含测试源码/资源/依赖，产物审计作为本阶段门禁。

`./mvnw clean verify` 使用冻结 PostgreSQL Testcontainers 镜像，必须有可用 Docker；没有 Docker 则失败并记录 NOT_EXECUTED，不能改用 H2 或 `disabledWithoutDocker`。校验和冲突只修改临时迁移副本，结构反例只改独立测试容器中的 schema。迁移、重复执行、拒绝 baseline/clean、结构不匹配及实际启动证据见 [P03-02](../testing/P03-02-VERIFICATION.md)。

## P04-02 安全测试迁移与关联约束（2026-10-08）

新增测试migration仅 `src/test/resources/security-migrations/V1__tenant_security_fixtures.sql`，由专用测试上下文选择独立locations；它与原persistence-migrations分别运行在隔离Testcontainer，因此测试V1不进入全应用生产版本号序列。原测试migration与所有历史失败证据保留，生产db/migration无新SQL，Hibernate仍validate。

本安全夹具包含safety_parent/child/store_resource/owned_resource/store_fact，五表tenant_id非空、UNIQUE(tenant_id,id)，父子与门店引用为复合FK并ON DELETE RESTRICT；业务代码的唯一约束按tenant_id+code。没有cascade。JPA保存parentId/storeId标量引用，关联目标从受控查询加载/校验，与DDL使用同一tenant_id关联；不依赖隐式懒加载授权。

五表均ENABLE/FORCE RLS，USING/WITH CHECK使用事务局部pet.tenant_id。临时容器角色security_probe_runtime与迁移owner分离，前者NOSUPERUSER/NOBYPASSRLS/NOINHERIT/NOCREATEDB/NOCREATEROLE，只有技术资源DML和门店事实SELECT，PUBLIC撤销schema CREATE；Flyway专用连接以容器owner执行后，JPA以runtime连接validate。角色不进入生产migration，临时凭据不保存或复用。

实际pg_class/pg_policies/pg_constraint查询与范围外native/JPQL更新、无GUC、WITH CHECK伪造写入、DDL/TRUNCATE/SET ROLE拒绝见[P04-02](../testing/P04-02-VERIFICATION.md)。每个失败后的结果用独立owner连接读取。触发器故意跳过一行制造真实影响数异常，应用回滚全部变更；只在该隔离容器中使用TRUNCATE重置技术夹具，没有删除日常数据库卷或历史数据。

未来正式模块按[持久化接入清单](PERSISTENCE.md#关联与新模块接入清单)创建正确复合约束与RLS，生产迁移编号仍唯一递增；运行/迁移角色实际权限、独立生产迁移执行和部署验证仍NOT_VERIFIED/NOT_EXECUTED，不能从技术容器结论推导生产账号权限验收。

## P05-01 首次正式V1与独立执行（2026-10-08）

新增唯一正式V1__platform_identity_foundation.sql，建立七表、索引、复合约束、FORCE RLS与三个受限函数。此前生产目录没有SQL，本轮空库迁移与已有空history升级均真实测试；不存在未测试的历史业务SQL升级路径。原测试persistence/security migrations保持src/test独立locations、独立EntityScan，正式迁移不引用测试角色/表。

管理员显式执行infra/database/provision-identity-roles.sql预配置NOLOGIN能力角色；SQL不创建登录账号、密码或租户，已有角色冲突失败，不自动修复。正式迁移在开头SET ROLE pet_migrator；Flyway历史由继承迁移能力的独立登录身份维护，避免依赖会被恢复的init-sql role。函数由NOLOGIN、无BYPASSRLS的读/写owner分别拥有，PUBLIC EXECUTE撤销、固定search_path、显式限定表，不使用动态SQL。前置权限、成员配置及命令见[身份初始化](../development/IDENTITY-BOOTSTRAP.md)。

生产JAR提供scripts/backend-identity.sh migrate独立入口；prod Application仍不迁移，只validate。local启用迁移时PET_MIGRATION_DATABASE_*显式必填且URL必须与运行数据库一致，不回退运行凭据。仍禁止clean、baseline非空库、repair、out-of-order或忽略checksum；生产原迁移不得改写。

容器验证正式迁移/JPA validate/重复无变更及角色权限；正式部署、真实生产账号权限、备份恢复、既有生产数据升级仍NOT_VERIFIED/NOT_EXECUTED。本轮没有实际本地/生产管理员初始化。

## B02 控制面和组织（2026-10-09）

新增 V7__control_plane_management.sql / V8__tenant_organizations.sql。先在目标库显式预配置 infra/database/provision-control-management-roles.sql，再用独立迁移任务执行全部8个版本。旧 V1～V6 不改。V7 创建固定控制面管理函数/事件及 Tenant initialized 状态，V8 创建独立 Organization；结构检查按具体受限 owner、运行权限与 FORCE RLS 扩展。没有 repair/baseline/忽略checksum开关。B02 开发中一次完整回归与同步修改尚未发布的新V7发生checksum冲突，失败保留；之后Maven/迁移源编辑顺序执行，最终空库正式迁移及完整回归通过。本轮专用无持久卷联调环境在新函数修复后重新正式迁移/页面创建；没有改既有库、repair历史或把SQL准备当管理功能成功。实际部署仍须独立角色预配置、备份及生产升级验证，见 [B02](../testing/B02-ACCEPTANCE.md)。
