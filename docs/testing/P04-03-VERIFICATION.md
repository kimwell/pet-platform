# P04-03 Redis命名空间、异步执行边界与多租户阶段验收

日期：2026-10-08（Asia/Shanghai）。**P04-03 COMPLETE；G01～G15 PASS；P04整体 COMPLETE，限定工程基础。** 最终222项测试（Surefire124、Failsafe98），原167项逐项回归，新增55项，失败/错误/跳过均0。没有提交、推送或部署，没有进入P05。真实认证/正式Store/权威撤销重验/生产角色与未来业务限制完整保留，见[P04总验收](P04-ACCEPTANCE.md)。

## 1. 前置事实与范围

用户明确授权本任务，替代AGENTS旧P02-01阶段限制；技术、固定包、保全和验证规则继续适用。直接在 `/Users/kimwell/work/pet-platform` 更新，后端 `com.pet.platform`；不使用Product Delivery OS，未读取或修改其状态。初始Git干净，HEAD保留，ui及其他端、根锁、CI、infra、历史证据和原测试migration未动；[基线文件](evidence/P04-03/baseline-files.json)、[Git基线](evidence/P04-03/baseline-git.txt)。

读取AGENTS/README、MULTI-TENANCY/AUTHORIZATION/MODULE-BOUNDARIES/CONFIGURATION/ASYNC-EVENTS、BACKEND/PERSISTENCE、VERSION-MATRIX/ROADMAP/DECISION-LOG、P04-01/P04-02报告及ACCEPTANCE-MATRIX。此前没有REDIS约定文件，本轮创建。实际检查TenantContext/嵌套/可信入口、DataScope/SELF主体域、Store事实默认拒绝、ScopedTransaction与实体/谓词、生产配置、trace/MDC/Executor使用、ASM及实际RLS migration/角色配置，不仅依据报告摘要。

P04-01/02实际已完成；原最终XML为167项、0失败/错误/跳过，保存为[逐项基线](evidence/P04-03/baseline-tests.json)。确认原公开逐项批处理与内部原子集合不同，SELF是主体域+owner ID，不改原契约。默认Provider无身份、正式Store事实源未实现，生产角色未验证；没有登录、会话Token存储、锁/MQ/Outbox/定时平台/正式业务表或UI修改。

只增有实际用途的BOM管理Redis starter，未手工指定受管理版本或升级基线。实际解析与矩阵一致，[依赖树](evidence/P04-03/backend-dependency-tree.txt)、[解析命令](evidence/P04-03/dependencies.json)。VERSION-MATRIX仅追加接入事实，冻结正文逐字保留。用命令专用已核验Temurin与Wrapper，[工具链](evidence/P04-03/toolchain.json)；没有修改全局Java/Node/包管理器。公开DTO/OpenAPI/错误枚举不变，原导出/协议测试全部回归，不重生成三端产物。

## 2. Redis封装与安全边界

生产shared.redis新增RedisKeyBuilder、RedisKey、TenantRedisAccess、PlatformRedisAccess、包内RedisValueStore与RedisConfiguration。只开放read/write(正TTL)/delete；业务不接受裸地址，不注入裸RedisTemplate/Connection/Lettuce。命名空间详细格式及合法代码见[REDIS](../conventions/REDIS.md)：配置prefix+可信environment+v1，tenant/store/platform的raw资源空间各自明确。

租户ID从当前BUSINESS取得，不提供传tenantId的构造方法；无上下文/仅身份目的拒绝。门店Key签发先核对真实内部事实、本permission范围和身份上限；显式当前门店不能另选，使用时再核对事实。缺事实默认503，foreign/未授权统一资源不可访问。平台独立入口要求PLATFORM及固定platform:redis:operate，在创建和每次操作时复核，不接纳任何租户范围或租户Key；租户接口也不接纳平台Key，空tenant不降级。

prefix/module为有限小写代码，拒绝分隔符/通配符/超长。业务标识允许1～128字节合法Unicode非敏感文本，拒绝空/null/控制字符/坏代理项；严格UTF-8逐字节Base64 URL编码，不trim/替换/归一化，分隔符/通配符/花括号不成为地址结构，不造成替换碰撞。地址上限512 ASCII字节；不使用hash tag，当前没有Cluster/跨槽/多Key原子操作支持。

RedisKey构造包内，绑定签发builder与不可变TenantContext；操作验证来源和完整当前范围。A创建后B使用、宽范围到窄范围使用、另造builder选择其他prefix/环境均拒绝；地址不作为跨请求或未来MQ授权凭证。Key没有公开原始地址getter，toString不输出内容。禁止在标识中放Token/密码/手机号等；编码不是保密措施，内容分类仍由模块保证。

值最大64KiB UTF-8，空字符串合法，null拒绝；读取miss为Optional.empty。线格式v1:加严格文本bytes，未知版本、坏UTF-8或超限值明确基础设施错误。私有template显式string Key/byte[] value，禁默认Java serializer和事务排队，未做Java原生反序列化。TTL为整毫秒1ms～24h，没有永久写入；不开放Lua、KEYS、FLUSHDB、删前缀/批量删、通用RedisUtil或任意重试。

真实Redis不可用/超时/错误及损坏值都抛既有DEPENDENCY_UNAVAILABLE，不当正常miss。未实现缓存降级/填充/DB与Redis原子性；特定只读降级由未来业务显式决定。总体health/readiness新增Redis贡献，真实Redis停止后503、liveness仍200，隐藏details/components。

## 3. 缓存数据范围规则

租户命名空间不是数据权限。当前只允许共享原始资源，命中后消费owner仍按本permission与当前TENANT/STORES/SELF检查归属，再返回/使用。RedisIsolationIT真实写入原始A2资源后，STORES A1虽然命中同raw bytes，仍被当前Store Guard拒绝消费；当前完整范围Key对象不能跨范围复用。

**已按权限过滤的列表、统计、allowedActions或授权结果当前禁止缓存**，没有此类地址API，也禁止通用Spring Cache/@Cacheable绕过。没有假造权威权限版本或不完整范围指纹；现有authorizationVersion字段不证明撤销失效已解决。静态检查不能判断任意字符串是否列表，模块必须遵守原始资源分类；基础端口不能替代具体资源owner规则。未来需要范围结果缓存时先落实身份域/主体/permission/规范化范围/权威版本与撤销方案，再专项验收。

## 4. 异步执行、事务与授权时效

TenantTaskExecutor是显式有限进程内池，不开启@EnableAsync、不包装第三方池或Servlet ASYNC，不使用公共ForkJoinPool。工作线程构造明确禁继承第三方inheritable变量，项目不使用InheritableThreadLocal传播身份。默认2线程/32队列，硬边界1～16线程/1～1024队列；AbortPolicy满队列/停止拒绝，不CallerRuns回退。

submit只从可信当前BUSINESS捕获私有不可公开构造Snapshot；保留当前tenant、主体域/ID、permission/dataScope、必要门店上限/明确门店、既有授权版本元数据与trace。用taskId替代sessionId，不带全部grants、Token/密码/Request/Session/EntityManager/连接/事务资源或可变Entity。普通业务不能new任意tenant快照或构造未登记可信入口安装身份，嵌套选择只保持/收窄当前权限。

默认30秒，硬上限60秒单调期限，从捕获计时，包含排队；开始前、运行范围读取/Guard/嵌套权限/受控beforeCommit和action完成时检查。过期不执行排队action或拒绝提交/结果；嵌套任务继承剩余期限，不能续期。无检查点的CPU/I/O不会被其他线程强制终止，已完成的独立事务不能逆转。

快照是捕获时授权，不代表执行时账号/权限仍有效；本轮没有真实状态重校验，明确排除长期/持久化/跨进程任务和需要权威执行时授权的敏感写。P05提供正式状态/版本/授权源，P10按当前与提交范围交集重验，不能把本Snapshot当MQ凭证。

submit拒绝调用线程active transaction或synchronization，推荐先由代理应用服务完整返回/提交，再提交不依赖未提交数据的工作；没有afterCommit helper，也不承诺在资源仍绑定的afterCommit回调可直接使用。工作线程先建立新范围，再调用代理新应用事务，沿用唯一P04-02 ScopedTransaction在同一JPA连接set_config(local)，没有第二套RLS或事务传播。进程内提交仍可能拒绝/崩溃丢失，不是可靠消息；可靠投递归P10 Outbox。完整调用与边界见[ASYNC-EXECUTION](../conventions/ASYNC-EXECUTION.md)。

## 5. 清理、拒绝、取消与停止

正常/异常/漏关由实际线程finally结束根范围及TraceScope；tenantId/operatorId/storeId/traceId只处理允许字段，调用线程trace和其他MDC不改变。Future正常结果或ExecutionException可观察；固定日志只记录任务技术ID与异常类型，不输出原文/堆栈，取消后的晚到异常也有日志。

进入和退出任务核对租户、trace、管理MDC及事务资源干净；残留拒绝并记录，失败Future可观察，当前工作线程自行退出、后续任务换新线程。测试通过故意污染内部池证明进入前拒绝，未由测试手工clear待测Holder掩盖。

| 场景 | 验证结果 |
| --- | --- |
| 同worker A/B/无身份、并发 | 实际threadId复用与屏障分别证明无身份/MDC串用 |
| 异常/漏关 | Future失败后同worker探针干净；漏关根兜底恢复后拒绝 |
| 拒绝 | 队列满/停止立即异常，action不在调用线程执行，调用范围/trace保持 |
| 排队取消 | action不运行、无身份安装、Future取消，释放队列位置 |
| 运行中协作取消 | Future可已done但action仍持有A，实际安全退出后才清理；排队探针此前不运行 |
| stopNow | 排队Future取消、运行线程中断请求，实际线程finally负责清理 |
| close | 有限优雅等待/中断/再等；正常排队任务排空，后续提交拒绝 |
| 不协作停止 | close明确报尚未退出，运行身份未被另一线程提前清理；测试释放后真正退出 |
| 期限 | 确定性测试时钟验证排队不运行、运行Guard/嵌套不续期、真实DB提交过期回滚 |

取消不能逆转提交，也不等于立即停止；没有用户任务状态机/可靠结果存储。Servlet ASYNC保持既有明确限制，未顺手扩展。

## 6. RLS实际角色与异步数据库结果

读取并复用[原安全migration](../../apps/backend/src/test/resources/security-migrations/V1__tenant_security_fixtures.sql)，没有修改其内容。P04-02真实security_probe_runtime为NOSUPERUSER/NOBYPASSRLS/NOCREATEDB/NOCREATEROLE/NOINHERIT，非五表owner，无owner成员关系，只获显式技术DML/事实SELECT；迁移使用独立容器owner凭据。五表ENABLE/FORCE及USING/WITH CHECK；原48项RLS/应用/复合约束反例全部回归，含native/JPQL/bulk、无GUC、WITH CHECK、DDL/TRUNCATE/SET ROLE拒绝。

新增独立PG异步IT用同样受限角色与独立迁移owner重新核对，连接池最大1确保实际复用：

- 工作线程开始无应用事务/资源，代理方法开启新的txid；与已提交调用方txid不同。
- 同worker A/B/无身份、并发任务的GUC与tenant/count对应，单连接pid复用；事务外GUC EMPTY且raw读取0行。
- 异步跨tenant查询不可见/更新拒绝，合法A/B更新分别提交；独立owner查询确认实际数据。
- flush后异常回滚，后续B不继承A；提交时快照过期回滚，后续B连接安全。
- 空STORES、A1门店、SELF STAFF/CUSTOMER同owner UUID均按原真实SQL规则执行。
- 在真实JPA事务中submit拒绝，调用事务范围不变；代理返回后提交的数据在工作新事务可见。

[9份异步独立数据库快照](evidence/P04-03/verify-complete-reports/p04-03-database-observations)与[48份原安全快照](evidence/P04-03/verify-complete-reports/p04-02-database-observations)均无GUC/角色错误。应用受控谓词、DB RLS、容器角色配置与生产部署权限分开记录。**生产角色权限/独立迁移/TLS/ACL仍NOT_VERIFIED/NOT_EXECUTED**，未执行生产数据库操作；上线前清单见[PERSISTENCE](../conventions/PERSISTENCE.md#p04-03-异步事务与生产rls前置)。

## 7. 测试数量、命令与证据

最终222项：原167项全部按class+case名称保留，missingBaselineCases=[]；新增55项为RedisConfigurationTest 2、TenantTaskExecutorTest 23、RedisIsolationIT 17、AsyncTenantPersistenceIT 9、RedisHealthIT 1、结构规则新增3。[逐项结果](evidence/P04-03/test-results.json)。Surefire124，Failsafe98，0失败/错误/跳过；真实PostgreSQL与Redis均执行，没有Docker隐性skip或内存数据库替代。技术Provider/Store/安全表只供工程反例，不作为真实业务验收账号。

| 工作目录 | 实际命令 | 退出码与结论 | 证据 |
| --- | --- | --- | --- |
| apps/backend | ./mvnw -v（命令专用冻结JAVA_HOME） | 0，冻结工具可用，不改全局 | [toolchain](evidence/P04-03/toolchain.json) |
| apps/backend | ./mvnw -q -DskipTests test-compile | 0，仅编译探针，不作为最终PASS | [compile-first](evidence/P04-03/compile-first.json) |
| apps/backend | ./mvnw test -Dtest=TenantTaskExecutorTest,RedisConfigurationTest,StructureRulesTest | 1，31项中的结构白名单内部调用误报，收窄修正；原失败保留 | [focused-first](evidence/P04-03/focused-first.json) |
| apps/backend | ./mvnw clean verify（first） | 1，217项：2失败13错误；旧两处PG测试缺Redis输入、编码字符断言及TTL轮询线程无身份，未当PASS | [verify-first](evidence/P04-03/verify-first.json) |
| apps/backend | ./mvnw clean verify（second） | 1，测试初始化器lambda变量重名编译失败；修正名称 | [verify-second](evidence/P04-03/verify-second.json) |
| apps/backend | ./mvnw clean verify（third） | 0，220项0跳过；随后补HTTP健康故障 | [verify-third](evidence/P04-03/verify-third.json) |
| apps/backend | ./mvnw clean verify（final） | 0，221项0跳过；随后明确禁第三方inheritable变量并补反例 | [verify-final](evidence/P04-03/verify-final.json) |
| apps/backend | **./mvnw clean verify（complete，最终源码）** | **0，222项，124+98，0失败/错误/跳过** | [最终元数据](evidence/P04-03/verify-complete.json)、[完整日志](evidence/P04-03/verify-complete.log)、[报告目录](evidence/P04-03/verify-complete-reports) |
| apps/backend | ./mvnw org.apache.maven.plugins:maven-dependency-plugin:3.9.0:tree -DoutputFile=项目绝对路径/docs/testing/evidence/P04-03/backend-dependency-tree.txt | 0，实际精确argv见JSON，与冻结值一致 | [dependencies](evidence/P04-03/dependencies.json) |
| 根目录 | python3 docs/testing/evidence/P04-03/final-audit.py（报告未创建时） | 1，仅文档链接尚缺，222/生产包/保全/角色/资源均已通过；原记录保留 | [首次审计](evidence/P04-03/final-audit-incomplete.json) |
| 根目录 | python3 docs/testing/evidence/P04-03/final-audit.py（最终） | 0，逐项回归/JAR/原文保全/链接/HEAD/资源通过 | [最终审计](evidence/P04-03/final-audit.json) |

完整cwd/argv/起止UTC/JAVA_HOME/退出码/输出摘要/log在同名JSON，XML/快照于命令结束立即归档；[命令索引](evidence/P04-03/COMMAND-INDEX.json)。历史失败没有覆盖。初次TTL Awaitility默认轮询线程无身份，失败正确证明不隐式传播；修正为当前受控线程有界poll，不额外造身份或用固定长sleep。

等级分别为DOCUMENTED（规则）、RESOLVED（冻结解析）、COMPILED（源码/生产包）、RUNTIME_VERIFIED（明确Redis/PG/HTTP/线程反例）；生产、远程、多OS与未实现业务保持NOT_VERIFIED/NOT_EXECUTED，不能互相替代。

## 8. 结构、产物与资源保全

ASM只扫描项目生产字节码，追加裸Redis/私有驱动/Cache、未登记Executor/@Async/Thread/公共异步、Key构造、内部snapshot入口、业务无身份提交及构造未登记可信根入口规则；泛型和bootstrap Handle违规夹具证明有效，合法受控端口引用有正例，第三方库内部Executor不扫描。它不分析任意反射、闭包图、动态内容或恶意DB/Redis凭据代码，不宣称所有动态绕过都可发现。

ProductionArtifactIT在package后执行；额外[JAR审计](evidence/P04-03/artifact-audit.json)逐项比较全部编译测试class和测试资源、Testcontainers及角色/安全表字符串，violations=[]。生产没有测试Provider、角色/migration、测试Entity/Controller、生产业务@Entity或新正式SQL；JAR SHA和比较数量以该JSON为准。

[最终保全审计](evidence/P04-03/final-audit.json)核对1464个受保护文件，ui/Web/小程序/生成产物/根锁/CI/infra、旧阶段证据、P04-01/02报告与原安全migration均未变，HEAD未变。测试只在独立Redis/PostgreSQL容器操作，未连接开发Redis、执行FLUSHDB或删前缀，未删除开发卷。[最终资源](evidence/P04-03/resources-final.json)无运行容器，最终前基线12个卷全部保留，测试进程/容器自动退出。

## 9. 新增与修改文件

完整逐文件清单见[changed-files](evidence/P04-03/changed-files.json)，最终审计sourceFiles列出全部源码/文档。主要内容：

| 类型 | 文件/目的 |
| --- | --- |
| 新增生产Redis（6） | shared/redis/RedisKeyBuilder、RedisKey、TenantRedisAccess、PlatformRedisAccess、RedisValueStore、RedisConfiguration |
| 新增生产异步/trace（4） | shared/tenancy/TenantTaskExecutor、TaskDeadline、AsyncExecutionConfiguration；shared/observability/TraceScope |
| 修改生产与配置 | TenantExecutionScope增加内部任务根/期限继承；pom.xml仅加BOM Redis starter；application.yml及.env.example增加Redis/有限池/健康输入 |
| 新增测试（7） | RedisConfigurationTest、RedisIsolationIT、TenantTaskExecutorTest、RedisTestSupport、RedisHealthIT、AsyncDatabaseProbe、AsyncTenantPersistenceIT |
| 修改既有测试（6） | PostgresIntegrationSupport、JpaPersistenceIT、MigrationSafetyIT、TenantPersistenceIT、PackagedDocumentPolicyIT补独立Redis输入；StructureRulesTest补边界/违规反例 |
| 新增文档（4） | 本报告、P04-ACCEPTANCE、REDIS、ASYNC-EXECUTION |
| 更新文档（12） | MULTI-TENANCY、CONFIGURATION、ASYNC-EVENTS、MODULE-BOUNDARIES、PERSISTENCE、LOCAL-DEVELOPMENT、ROADMAP、DECISION-LOG、ACCEPTANCE-MATRIX，以及README/BACKEND/VERSION-MATRIX引用一致性 |
| 证据 | 基线SHA、工具链、实际命令/失败/日志、最终XML/独立DB快照、依赖树、JAR/资源/文件与链接审计 |

## 10. G01～G15

| 门禁 | 结果/等级 | 实际依据 |
| --- | --- | --- |
| G01 前置实现与契约 | PASS / DOCUMENTED | 实际代码/迁移/角色/原167 XML核对；原P04条件和SELF/逐项批处理保持 |
| G02 可信Key与无歧义编码 | PASS / COMPILED / RUNTIME_VERIFIED | 当前BUSINESS、严格段、UTF-8 Base64 URL往返/碰撞/来源/环境反例 |
| G03 门店/平台独立边界 | PASS / RUNTIME_VERIFIED | 事实/授权/缺事实拒绝、平台固定权限、无tenant降级/交叉Key |
| G04 操作当前范围校验 | PASS / RUNTIME_VERIFIED | 每次签发者/完整范围/门店事实复核，A→B及宽→窄拒绝 |
| G05 权限结果不错误共享 | PASS / DOCUMENTED / RUNTIME_VERIFIED | 结果缓存禁止/无API，raw命中后的当前Store拒绝；静态内容识别限制单列 |
| G06 真实Redis读写/TTL/隔离 | PASS / RUNTIME_VERIFIED | 17 Redis IT + 1实际健康HTTP故障；错误不miss |
| G07 不可变不可伪造快照 | PASS / COMPILED / RUNTIME_VERIFIED | private Snapshot/current-only grants、复制集合、无session凭据/任意tenant入口，ASM反例 |
| G08 复用/异常清理 | PASS / RUNTIME_VERIFIED | 23生命周期技术测试中的同worker/异常/漏关/trace/残留拒绝等反例 |
| G09 拒绝/取消/停止 | PASS / RUNTIME_VERIFIED | 队列满、排队释放、运行实际生命周期、有限close及不协作退出明确错误 |
| G10 异步事务/PG隔离 | PASS / RUNTIME_VERIFIED | 9真PG IT、新txid、A/B读写、单pid复用、flush/超龄提交回滚 |
| G11 受限RLS角色/生产区分 | PASS / RUNTIME_VERIFIED / DOCUMENTED | 非owner/超权/继承、五表FORCE，48原PG反例回归；生产NOT_VERIFIED |
| G12 全测试回归 | PASS / COMPILED / RUNTIME_VERIFIED | 222项/原167逐项保留，0失败/错误/跳过 |
| G13 生产包隔离测试夹具 | PASS / COMPILED | package后ProductionArtifactIT及完整测试class/resource/角色字符串审计 |
| G14 无提前认证/MQ/业务 | PASS / DOCUMENTED / COMPILED | 无生产Entity/SQL/测试身份/Token DAO/MQ/Outbox/业务API或UI变化 |
| G15 P04总验收/限制 | PASS / DOCUMENTED | 三任务/原阶段条件与后续owner核对、合法规则/清单/链接及真实限制归档 |

## 11. 未实现、未验证与状态

真实Sa-Token认证/三域会话/Cookie/CSRF、正式Tenant/Store/身份表/初始化/角色授权、账号停用及权限撤销重验、认证候选函数和正式Store有效状态源未实现。Redis资源端口不等于Token存储、业务缓存、跨实例会话或通用锁；当前没有授权结果缓存/降级或Cluster多Key。长期/持久化/跨进程任务、RabbitMQ/Outbox、可靠投递、Servlet ASYNC和所有未来业务/附件/导出/公开批处理仍未实现或不在支持范围。

生产角色/独立迁移部署/TLS/ACL/容量、远程CI、Windows/Linux、真实账号/端到端业务/生产发布均NOT_VERIFIED或NOT_EXECUTED。本地容器/工程技术夹具不能替代；任务取消或期限也不能逆转已提交副作用。

核对原路线条件和三项P04后，**P04-03与P04整体COMPLETE，P05 NOT_STARTED**。[P04总验收](P04-ACCEPTANCE.md)逐项区分当前证据及未来owner，没有改门禁以完成状态。下一合法 **P05-01：正式身份数据基础、初始化路径与认证接入依赖**，必须先建立正式数据/安全初始化/权威身份与Store/认证查找依赖，不能继续用测试身份完成登录验收。本轮只报告下一任务，未执行。
