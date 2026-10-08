# P03-02 PostgreSQL、JPA、Flyway 与持久化基础验证

日期：2026-10-08（Asia/Shanghai）。依据本轮明确授权直接在 `/Users/kimwell/work/pet-platform` 执行，基础包和启动类不变；未提交、推送、发布或部署。**P03-02 COMPLETE，G01～G15 PASS；P03整体 IN_PROGRESS。** 完整标量、实际 OpenAPI/生成类型/漂移与结构防绕过仍待后续任务，不能提前推进 P04。

## 1. 事实源与实施范围

已读取 AGENTS、README、TECHNICAL-BASELINE、MODULE-BOUNDARIES、CONFIGURATION、API/PAGINATION/DATA-TYPES、BACKEND、VERSION-MATRIX、ROADMAP、DECISION-LOG、LOCAL-DEVELOPMENT、P03-01报告、ACCEPTANCE-MATRIX 以及实际源码、测试、POM、Compose/数据库规范。当前授权替代 AGENTS 中旧 P02-01 的任务限制，其技术、安全及保全规则继续适用。

P03-01 实际为 COMPLETE，原34项测试全部回归；total 已为字符串，同步 REQUEST/ERROR trace保持原协议，异步传播未实现。修改前 main 尚无提交、现有文件均未提交，本轮没有覆盖 ui、其他端或旧报告。初始 SHA/资源状态见 [文件基线](evidence/P03-02/baseline-files.json)、[资源基线](evidence/P03-02/resource-baseline.json)。没有 Product Delivery OS、正式业务表、租户隔离、认证、Redis/AMQP/微信业务或生产演示接口。

## 2. 实现概要与文件

| 新增/修改 | 文件与行为 |
| --- | --- |
| 生产持久化代码 | [BaseEntity](../../apps/backend/src/main/java/com/pet/platform/shared/persistence/BaseEntity.java)、[PersistenceConfiguration](../../apps/backend/src/main/java/com/pet/platform/shared/persistence/PersistenceConfiguration.java)、[DatabaseConfigurationRules](../../apps/backend/src/main/java/com/pet/platform/shared/persistence/DatabaseConfigurationRules.java)、[JpaPageAdapter](../../apps/backend/src/main/java/com/pet/platform/shared/persistence/JpaPageAdapter.java) |
| 构建和配置 | [pom.xml](../../apps/backend/pom.xml)、[application.yml](../../apps/backend/src/main/resources/application.yml)、[prod模板](../../apps/backend/src/main/resources/application-prod.yml)、[输入示例](../../apps/backend/.env.example)、[CI](../../.github/workflows/check.yml) |
| 正式迁移目录 | [规则说明](../../apps/backend/src/main/resources/db/migration/README.md)，没有正式 SQL/Entity/Repository |
| 测试配置与技术夹具 | [容器支持](../../apps/backend/src/test/java/com/pet/testing/PostgresIntegrationSupport.java)、[测试配置](../../apps/backend/src/test/java/com/pet/testing/persistence/PersistenceFixtures.java)、[技术实体](../../apps/backend/src/test/java/com/pet/testing/persistence/PersistenceProbe.java)、[Repository](../../apps/backend/src/test/java/com/pet/testing/persistence/ProbeRepository.java)、[应用服务](../../apps/backend/src/test/java/com/pet/testing/persistence/ProbeApplicationService.java)、[测试迁移](../../apps/backend/src/test/resources/persistence-migrations/V1__shared_persistence_probe.sql) |
| 新测试与回归接入 | [JPA/事务IT](../../apps/backend/src/test/java/com/pet/testing/persistence/JpaPersistenceIT.java)、[迁移安全IT](../../apps/backend/src/test/java/com/pet/testing/persistence/MigrationSafetyIT.java)、[分页适配测试](../../apps/backend/src/test/java/com/pet/platform/shared/persistence/JpaPageAdapterTest.java)、[配置测试](../../apps/backend/src/test/java/com/pet/platform/shared/persistence/DatabaseConfigurationRulesTest.java)；原 ApplicationTest/ApiProtocolTest/ApiProtocolIT 接入独立真实 PostgreSQL，ApplicationTest 增加生产扫描无测试实体断言 |
| 文档 | 新增 [迁移](../conventions/DATABASE-MIGRATION.md)、[持久化](../conventions/PERSISTENCE.md)、本报告；更新 CONFIGURATION/PAGINATION/BACKEND/LOCAL-DEVELOPMENT/ROADMAP/DECISION-LOG/ACCEPTANCE-MATRIX/VERSION-MATRIX/README/PROJECT-STRUCTURE/TECHNICAL-BASELINE/infra说明 |
| 证据 | 本阶段 [目录](evidence/P03-02/)，包括命令、原始失败、JUnit逐项报告、运行记录、产物SHA与范围审计；完整新增/修改列表见 [最终审计](evidence/P03-02/final-audit.json) |

## 3. 实际解析版本

本表只记录实际解析结果，冻结值的唯一事实源仍是 [VERSION-MATRIX](../development/VERSION-MATRIX.md)。没有手工覆盖 BOM 或升级无关依赖，未增加 Module。

| 实际组件 | 解析值与核对 |
| --- | --- |
| Spring Boot / Flyway starter / Actuator starter | 4.0.8，同一 parent 管理 |
| Spring Data JPA / Hibernate ORM | 4.0.7 / 7.2.24.Final，与冻结值一致 |
| Flyway Core / PostgreSQL支持模块 | 11.14.1 / 11.14.1，与冻结值一致 |
| PostgreSQL JDBC | 42.7.13，与冻结值一致 |
| Testcontainers PostgreSQL / JUnit / Core | 2.0.5，使用2.x模块名，与冻结值一致 |
| PostgreSQL实际运行 | 17.11，既有 bookworm 标签和多架构 digest，日志真实识别 |
| HikariCP | 7.0.2，BOM传递依赖，未覆盖 |
| JUnit / Surefire / Failsafe | 6.0.3 / 3.5.6 / 3.5.6，由 Boot parent管理 |
| 本机工具 | Temurin 21.0.12.1+1、Wrapper对应 Maven 3.9.16；任务专用 JAVA_HOME/PATH，不改全局配置 |

证据：[完整依赖树](evidence/P03-02/backend-dependency-tree.txt)、[解析命令](evidence/P03-02/dependencies.json)、[工具执行](evidence/P03-02/toolchain.json)。Boot 4 的 Flyway 自动配置使用官方 starter，参见 [官方初始化说明](https://docs.spring.io/spring-boot/how-to/data-initialization.html)，实际集成结论以本轮冻结版本编译和运行为准。

## 4. 数据源、迁移、审计和事务

数据源 URL/用户名/密码必须明确注入，没有 local/生产可用回退；缺值在连接池创建前中文说明配置名，拒绝带凭据/查询参数的 URL。Hikari 最大10、最小空闲2、等待连接5秒、验证2秒、初始化失败10秒；驱动连接5秒、socket30秒。生产容量、凭据和角色权限未验收。

固定 OSIV=false、generate-ddl=false、ddl-auto=validate、SQL init=never；禁通过常见结构生成别名覆盖安全策略。local/test默认 Flyway启动迁移→JPA validate，prod按冻结规范先独立任务迁移、运行进程只validate，启动迁移被拒绝。正式目录只有说明，无业务SQL；应用空库只有Flyway自身history。校验和、命名与缺失locations检查启用；clean禁用、baseline与out-of-order关闭，不自动repair。测试迁移物理隔离，非空schema自动baseline与clean反例、临时副本checksum冲突均实际验证。

BaseEntity为MappedSuperclass，JDK生成UUID v4，Instant/timestamptz(3)存UTC时间点，JPA Auditing使用可替换Clock并截断毫秒。创建时两时间一致；dirty update更新时间，相同值不变；createdAt不可通过公共setter修改且不参加UPDATE。没有数据库时间默认值、假操作者、tenant/store基类、强制软删除或强制@Version。

事务位于测试专用Application Service，实际经过Spring代理；测试方法没有自动回滚事务。分别验证提交后独立查询可见、已flush写入遇运行时异常回滚、两笔写同事务回滚、唯一约束flush失败回滚之前写入。Controller不持有事务；readOnly不是权限/隔离；数据库事务不覆盖文件和远程请求。生产无通用BaseService/Repository或业务用例。

## 5. 分页与排序结果

复用PageQuery/SortRule/SortWhitelist/PageResponse；外部page-1→Pageable，默认1/20、最大100不变。offset先用long计算，超过JPA int能力返回422 RESULT_TOO_LARGE。适配器只从白名单转换属性，唯一排序条件由原逻辑追加一次；显式Sort NULLS LAST。实际冻结 [JPA源码](https://github.com/spring-projects/spring-data-jpa/blob/4.0.7/spring-data-jpa/src/main/java/org/springframework/data/jpa/repository/query/QueryUtils.java) 及本机类文件确认支持Jakarta Persistence 3.2空值次序，真实PostgreSQL升降序/Specification测试通过；不采用旧版本issue推断。Hibernate默认空值策略亦为last，原生SQL仍须显式实现。

应用服务Page.map成DTO后再fromPage，total来自Page.getTotalElements的long，不转int、不用本页条数代替。真实3条数据/每页2条返回total="3"；空查询total="0"，超末页保持请求页码、items=[]和实际总数。技术大整数序列化示例：

```json
{"items":["技术DTO"],"page":1,"pageSize":20,"total":"9007199254740993"}
```

该大整数是内存Page构造的精度技术测试，不伪称生成海量数据库记录；Page内部JSON字段不暴露。稳定次序只保证查询确定性，不宣称跨页并发插删快照一致。

## 6. 实际测试与健康启停

最终 **62 tests，0 failures、0 errors、0 skipped**：Surefire43，Failsafe19。其中新增专门PostgreSQL集成测试17项（JpaPersistenceIT12、MigrationSafetyIT5）；另外2项是原真实HTTP协议IT，使用独立数据库启动。原P03-01的34项全部通过，新增28项含配置/分页单元测试与扫描断言。逐项名称/数量见 [测试汇总](evidence/P03-02/test-results.json) 和同目录TEST-*.xml。

独立真实容器覆盖空库迁移/history/重复执行、JPA validate成功与缺列失败、UUID往返、时间精度/时区、应用服务事务和约束、条件组合/稳定排序/NULLS LAST/空页/超末页/count。生产扫描测试确认默认EntityManagerFactory不含测试实体；实际JAR无测试实体/Controller/Repository/配置/迁移/Testcontainers依赖。

本机生产JAR使用独立技术PostgreSQL分别local首次启动、local重复启动、prod只校验启动。启动日志迁移验证先于EntityManagerFactory。正常时总体/liveness/readiness均200 UP；总体端点可列出分组名称，隐藏连接详情。停止数据库后总体与readiness503 DOWN，liveness仍200 UP；没有Redis/RabbitMQ假健康。/actuator/env、/beans和测试接口均404。

prod缺URL/用户/密码、prod企图启动迁移、错误密码、不可用数据库6个独立进程都实际exit1，预期失败信息正确且日志无密码。三个成功进程SIGTERM后真实exit143、观察优雅关闭及连接池Shutdown completed、端口关闭，无遗留进程；信号退出不伪写exit0。临时容器/匿名技术卷已清理，既有三项P02容器和全部基线卷保持原状态。证据：[运行记录](evidence/P03-02/local-runtime.json)、[实际产物/SHA](evidence/P03-02/artifact-audit.json)、[最终运行命令](evidence/P03-02/runtime-third.json)。公开技术容器凭据不是实际业务账号或生产秘密。

## 7. 命令、退出码与失败修复记录

每个命令通过本阶段run-check.py记录cwd/argv/真实退出码/时间/日志摘要，原始失败保留，不以重试覆盖。命令表cwd省略根前缀 `/Users/kimwell/work/pet-platform`。

| cwd | 实际命令/运行 | 退出码与证据 |
| --- | --- | --- |
| apps/backend | ./mvnw --version | 0，[工具](evidence/P03-02/toolchain.json) |
| apps/backend | ./mvnw dependency:tree -DoutputFile=../../docs/testing/evidence/P03-02/backend-dependency-tree.txt | 0，[解析](evidence/P03-02/dependencies.json) |
| apps/backend | ./mvnw -DskipTests test-compile 首次 | 1，测试误用旧异常访问器，修正为现有error().code()；[原记录](evidence/P03-02/compile-first.json)；skipTests只用于编译探针，不计运行PASS |
| apps/backend | ./mvnw clean verify 首次/第二次/第三次 | 1/1/1：Testcontainers标签+digest需明确兼容postgres；Flyway空varargs重载歧义；容器URL自带loggerLevel参数不符合应用无参数URL规则，动态应用URL移除该技术参数。冻结镜像/版本、安全约束均不变；[一](evidence/P03-02/verify-first.json)、[二](evidence/P03-02/verify-second.json)、[三](evidence/P03-02/verify-third.json) |
| apps/backend | ./mvnw clean verify 第四次 | 1，迁移断言误把自动建schema技术history条目计为版本SQL，已按非空version计数；[原记录](evidence/P03-02/verify-fourth.json)。Failsafe在verify实际传播失败，未静默通过 |
| apps/backend | ./mvnw clean verify 最终 | 0，62项无失败/错误/跳过；[命令](evidence/P03-02/verify-final.json)、[日志](evidence/P03-02/verify-final.log) |
| 根 | python3 docs/testing/evidence/P03-02/runtime-audit.py 第一/第二次 | 1/1：探针误要求总体health没有合法groups字段；Docker随机宿主端口重启后改变，需要重新读取。原失败与运行输出保留在 [第一轮](evidence/P03-02/runtime-first/) / [第二轮](evidence/P03-02/runtime-second/)；是探针假设修正，不改生产行为 |
| 根 | 同一runtime-audit.py最终 | 0，JAR/health/配置负例/重复启动/停止清理通过；[命令](evidence/P03-02/runtime-third.json)、[日志](evidence/P03-02/runtime-third.log)；各docker命令cwd/argv/退出码也在local-runtime.json |
| 根 | python3 docs/testing/evidence/P03-02/final-audit.py | 0，文档链接/范围保全/资源恢复/产物一致性；[审计](evidence/P03-02/final-audit.json)，不是运行验收替代 |

实际测试日志包含预期缺列失败、唯一约束失败、故意内部错误和数据库断连堆栈，以及JVM类共享提示；不声称“日志零告警”。健康JSON不泄露内部信息，配置负例日志未输出密码。第三方异常日志的业务字段脱敏仍需后续有真实实体时继续验证，当前仅技术夹具。

## 8. G01～G15完成门禁

| 门禁 | 结果/等级 | 依据与真实边界 |
| --- | --- | --- |
| G01 冻结版本/P03-01契约 | PASS / DOCUMENTED / RESOLVED | 事实源读取、实际依赖树，原total/排序/trace未改 |
| G02 PostgreSQL数据源 | PASS / RUNTIME_VERIFIED | 独立容器真实认证连接与生产JAR启动，非日常库 |
| G03 Flyway安全集成 | PASS / COMPILED / RUNTIME_VERIFIED | 空库/重复、checksum/baseline/clean反例；prod独立迁移策略，生产角色本轮未验收 |
| G04 JPA结构校验 | PASS / RUNTIME_VERIFIED | 迁移后validate成功、缺列启动失败，无Hibernate建表 |
| G05 UUID/时间审计 | PASS / RUNTIME_VERIFIED | UUIDv4、Instant毫秒、UTC、created保持、实际更新与相同值不变 |
| G06 应用服务事务 | PASS / RUNTIME_VERIFIED | 非自动回滚测试，经服务代理提交/运行异常/多笔/flush约束失败 |
| G07 分页total字符串 | PASS / COMPILED / RUNTIME_VERIFIED | page-1、offset范围、实际count和>JS安全整数保真 |
| G08 白名单/稳定排序 | PASS / RUNTIME_VERIFIED | 固定属性、同向唯一补充、升降NULLS LAST、同值稳定、条件组合 |
| G09 Testcontainers实际执行 | PASS / RUNTIME_VERIFIED | 17专门PostgreSQL IT，冻结镜像实际17.11，0跳过 |
| G10 测试隔离 | PASS / COMPILED / RUNTIME_VERIFIED | 默认生产扫描无测试Entity，实际JAR无测试代码/资源/依赖，测试接口404 |
| G11 数据库健康与秘密 | PASS / RUNTIME_VERIFIED | db故障readiness/整体503但liveness200，隐藏details，6个启动负例无密码输出 |
| G12 原协议回归 | PASS / RUNTIME_VERIFIED | P03-01原34项继续通过，实际HTTP信封与ERROR trace保持 |
| G13 clean verify | PASS / COMPILED / RUNTIME_VERIFIED | 最终exit0、62项无失败/错误/跳过；第四次真实失败在Failsafe verify传播 |
| G14 无越界业务 | PASS / DOCUMENTED / COMPILED / RUNTIME_VERIFIED（产物） | 生产无业务表/Entity/Repository/演示Controller，其他端/ui未改，未推进P04 |
| G15 文档与证据 | PASS / DOCUMENTED / RESOLVED | 使用/限制/状态/命令/版本、链接与最终SHA/资源一致，历史失败保留 |

DOCUMENTED是规则说明，RESOLVED是实际依赖/文件解析，COMPILED是编译与打包，RUNTIME_VERIFIED只覆盖本报告明确的本机技术场景；等级不互相代替，技术数据不作为业务验收。

## 9. 未执行项、阶段状态与下一范围

| 项目 | 真实状态/原因 |
| --- | --- |
| Tenant/Store/员工/角色/正式CRUD、无上下文拒绝、RLS/范围查询、异步上下文 | NOT_EXECUTED / NOT_VERIFIED，P04及后续；本轮明确禁止 |
| 生产迁移独立任务、迁移/运行角色权限与生产部署 | NOT_EXECUTED / NOT_VERIFIED，只有冻结策略和prod配置反例；没有生产环境授权或凭据，不把管理员技术容器当生产权限证明 |
| 完整UUID/时间/金额/日期/null/version JSON序列化、DST | NOT_EXECUTED / NOT_VERIFIED，P03剩余；本轮只覆盖UUID/Instant数据库和total序列化 |
| 实际OpenAPI导出/三端生成/漂移CI、完整结构防绕过 | NOT_EXECUTED / NOT_VERIFIED，P03剩余，当前生产包隔离断言不是完整模块安全验收 |
| Redis、RabbitMQ、登录、微信、真实文件/业务验收 | NOT_EXECUTED / NOT_VERIFIED，不在本轮范围 |
| 远程CI、Windows/Linux、真机、发布/部署 | NOT_EXECUTED / NOT_VERIFIED，本轮仅更新Docker前置CI配置，未远程执行；未修改其他端或发布 |

没有剩余P03-02完成阻塞。P03-02 COMPLETE，P03整体仍IN_PROGRESS。按 [路线图](../development/ROADMAP.md) 下一合法范围是 **P03剩余：完整标量序列化、实际OpenAPI/生成类型/漂移检查与结构防绕过验证**，具体任务编号在后续授权中细化；本轮不自动执行下一任务或提交/推送/部署。
