# P04-02 JPA租户受控访问、关联约束与PostgreSQL越权验证

日期：2026-10-08（Asia/Shanghai）。**P04-02 COMPLETE，G01～G15 PASS；P04整体IN_PROGRESS。** 最终167项测试，原114项逐项全部回归，失败/错误/跳过均0；新增48项真实PostgreSQL安全测试、4项结构门禁测试、1项缓存配置拒绝测试。本报告只证明本轮受控基础能力和技术模型，不等于真实认证、所有未来业务或生产账号权限验收。

## 1. 前置事实与范围

已读取AGENTS/README、MULTI-TENANCY/AUTHORIZATION/MODULE-BOUNDARIES、API/IDENTITY/PAGINATION、BACKEND/PERSISTENCE/DATABASE-MIGRATION、ROADMAP/VERSION-MATRIX/DECISION-LOG、P04-01-VERIFICATION/ACCEPTANCE-MATRIX；检查BaseEntity、TenantContext/DataScope、TenantScopeGuard/StoreScopeGuard、CurrentPrincipalProvider、事务/JPA/分页/结构/migration与JAR隔离。P04-01确有114项且0跳过；[基线测试](evidence/P04-02/baseline-tests.json)与最终逐项对照无缺失。

用户明确授权P04-02替代AGENTS旧P02-01阶段边界，其技术/包名/保全规则仍适用。初次Git检查干净，当前HEAD为dcf7e3feb18e9d65094e620ca4dfb0794c46c849；[基线](evidence/P04-02/baseline-git.txt)中的untracked证据目录是本轮创建后采集的，不是外部改动。没有提交、推送或部署，未使用Product Delivery OS，未执行P04-03。保留ui、前端/小程序、CI、根锁、POM、冻结矩阵、原migration和P04-01证据；[审计](evidence/P04-02/final-audit.json)核对343个受保护文件，无变化。

应用依赖不变，沿用冻结BOM；命令专用JAVA_HOME使用Temurin 21.0.12.1+1，不修改全局Java/Node/包管理配置。Maven Wrapper 3.9.16；PostgreSQL采用既有17.11标签+digest，Testcontainers 2.0.5。[工具链](evidence/P04-02/toolchain.json)、[版本事实源](../development/VERSION-MATRIX.md)。公开DTO/OpenAPI/错误枚举/三端产物无变化，不无理由重生成契约或运行无关三端检查。

## 2. 受控访问方案与防御边界

模块infrastructure继承ScopedPersistence<T>，通过自己的固定查询/命令提供能力；application声明实际代理事务，api仅DTO。每个适配器实例固定permissionCode、ResourceAccessPolicy和批量可编辑字段；共享类所有数据操作protected final，没有公共save/merge或底层句柄输出。模块不能向HTTP透传任意Entity、Supplier/Consumer/Specification、字段名或SQL。

安全查询始终为 `tenantCondition AND dataScopeCondition AND businessCondition`。BusinessCondition只提供Root/CriteriaBuilder，不能替换外层安全条件；业务OR整组参与AND。find/require/page/count/exists/findIds使用同一策略。require通过有范围SQL定位，不存在/范围外统一TenantAccessDeniedException（404 RESOURCE_NOT_FOUND及既定文案）；exists为false，count/page.total不包含范围外行。未声明组合拒绝，不使用无条件安全回退。

同时保留P01冻结RLS方向：ScopedTransaction在应用JPA事务首次访问时，用Hibernate实际连接执行参数化 `set_config('pet.tenant_id', ?, true)`。事务绑定完整TenantContext，后续受控操作、实体写入及beforeCommit必须一致；REQUIRES_NEW挂起/恢复分别绑定。业务可在事务开始前收窄；已访问数据库的同一事务中改变范围会拒绝，不能复用宽范围实体写回。提交/回滚后局部GUC消失。禁OSIV、二级缓存和查询缓存，危险配置覆盖也被拒绝。

安全封装与结构检查不能阻止所有直接数据库访问；RLS只负责tenant底线，不负责门店/owner。持有数据库凭据的恶意代码可自行改GUC，运行角色也不是抵御被攻陷应用的凭据沙箱。当前不提供普通业务native SQL、字符串JPQL或任意bulk扩展；未来新增自定义SQL必须登记并真实越权测试。**测试容器角色权限已验证，生产数据库角色权限仍NOT_VERIFIED，独立生产迁移仍NOT_EXECUTED。**

## 3. 实体与资源范围

| 实现 | 实际责任 |
| --- | --- |
| TenantScopedEntity | MappedSuperclass继承BaseEntity的UUID/时间；tenantId私有、非空、updatable=false，无公共setter；业务构造从可信BUSINESS初始化，JPA无参构造不授归属 |
| StoreScopedEntity | 同时tenantId/storeId；业务initializeStore和insert均经StoreScopeGuard权威事实/归属/授权；普通更新不能迁移store |
| 生命周期/受控更新 | 创建及PostLoad记原归属/执行范围，persist/update/remove回调及update显式校验防字段伪造与执行范围串用；不以ORM只读列单独证明SQL安全 |
| tenantOnly | 仅TENANT；没有store/owner映射的STORES或SELF拒绝 |
| stores | TENANT+当前身份门店上限；STORES+本权限集合，空集合SQL为false |
| owned | TENANT或模块明确principalType+ownerId的SELF；不使用createdBy |
| storesAndOwned | 同上两种映射；STORES+SELF按同权限并集，仍受外层tenant条件 |

测试Owner字段不可普通更新；员工和客户即使同UUID也不能互认。SELF只授权owned行，不授权整个Store，StoreScopeGuard仍拒绝仅SELF创建整店资源。缺正式Store事实源的默认503沿用P04-01，没有用测试事实冒充生产platform/store模块。

## 4. 查询、更新、删除与批次

合法代码和新模块8项接入清单见[持久化约定](../conventions/PERSISTENCE.md#p04-02-当前受控持久化能力2026-10-08)，真实适配器见[SafetyRepositories](../../apps/backend/src/test/java/com/pet/testing/tenantpersistence/SafetyRepositories.java)。创建通过insertNew内部工厂，tenant从执行上下文初始化，拒绝有ID的detached对象；新行保存后还检查资源策略。单条update在范围内加载managed实体，使用模块固定业务方法，只改允许字段，显式校验后flush；具体模型@Version。单条delete使用保留安全条件的固定CriteriaDelete。

内部updateBatch/deleteBatch输入1～100个非空ID（提交条数限制），重复去重；同事务PESSIMISTIC_WRITE定位全部去重目标，任何不存在/范围外目标整批统一拒绝且不指出具体ID。DML自身保留tenant+范围+ID集合条件，更新只开放静态白名单字段、维护UTC毫秒updatedAt及long version递增；实际影响数与目标数不一致标记rollbackOnly并抛异常。DML前flush、成功后clear，后续不得直接复用旧managed引用。数据库FK限制删除；bulk不调用实体回调，其安全靠独立谓词/白名单/锁/影响数/事务/RLS。

冻结[API](../contracts/API.md#批量部分失败)已有公开batch-actions逐项独立事务契约，本轮没有改写或实现该HTTP接口。此处是模块内部原子集合命令，专用于证明整批拒绝/回滚；未来公开逐项成功必须按原API实现独立用例、version和响应计数，不能偷换成内部原子语义。客户端version、幂等及完整公开批处理仍未实现，未生成新的三端协议。

## 5. 跨实体数据库约束

全部技术表在src/test的[安全migration](../../apps/backend/src/test/resources/security-migrations/V1__tenant_security_fixtures.sql)：safety_parent（租户父）、safety_child（parentId）、safety_store_resource、safety_owned_resource和safety_store_fact。A/B两个租户，A1/A2/B1门店，同租户不同owner、STAFF/CUSTOMER同UUID、跨租户相同code均实际入库。

五表tenant_id NOT NULL、UNIQUE(tenant_id,id)。child以(tenant_id,parent_id)引用parent；store_resource以(tenant_id,store_id)引用事实表，均ON DELETE RESTRICT，无级联。应用先通过目标受控查询验证当前范围/tenant一致，然后保存引用ID。owner连接直接绕过应用建立A→B关联，数据库返回23503且没有新增行；Store跨租户FK同样拒绝。合法父子提交/受控投影通过，存在child的父删除被拒绝且父子保留。

当前实体保存标量parentId，父投影重新经父模型受控query取得，没有隐式JPA导航。试验使用复合ManyToOne且LAZY仍提前解析父实体，verify-third的实测断言失败保留；改为ID引用是实际安全边界选择，未关闭检查。初轮final getter代理警告经最小修正解除。未来join/fetch/lazy投影仍须单独验证被引用资源的STORES/SELF，不从注解、FK或tenant RLS推导完整关联授权。

五表真实ENABLE/FORCE RLS、USING/WITH CHECK检查局部pet.tenant_id；runtime非owner/SUPERUSER/BYPASSRLS/CREATEROLE/CREATEDB且不继承owner，只获技术资源DML与事实SELECT。无GUC读0行、写42501；A执行针对B的native/JPQL更新影响0行；native把不冲突的A记录移到B、伪造B新行均42501，数据库未改变。DDL/TRUNCATE/SET ROLE均42501。事实表不是生产Store，角色和临时凭据不进入生产SQL。

## 6. 28项要求与数据库结果

48项安全IT实际执行，完整逐项XML见[PostgreSQL安全报告](evidence/P04-02/verify-complete-reports/failsafe-reports/TEST-com.pet.testing.tenantpersistence.TenantPersistenceIT.xml)，独立数据库快照见[快照目录](evidence/P04-02/verify-complete-reports/p04-02-database-observations)。测试方法没有自动回滚事务，应用服务代理真实提交/回滚，owner每次以独立连接读取验证，不仅检查异常。技术夹具不作为业务验收。

| 用户要求编号 | 核心测试方法/额外覆盖 | 实际结果 |
| --- | --- | --- |
| 1～2 | pagesExcludeOtherTenantAndUseSecuredActualCount | A分页total=2，B=1，items排除B |
| 3～4 | idDetailUnifiesMissingAndOtherTenant；existsCountAndIdSetsDoNotLeakOtherTenant | 隐藏/不存在同错误；B exists=false，A count=2，ID集只A |
| 5 | businessOrCannotEscapeOuterTenantAndCount | OR列表及total仍在A |
| 6～8 | emptyStoresHaveNoRowsTotalsOrExistence；storeRangeExcludesSameTenantOtherStoreAndOtherTenant | 空集0；A1只1行，A2/B1不可见 |
| 9 | selfUsesExplicitOwnerAndPrincipalTypeAcrossSameUuid；selfWritesUseOwnerTypeAndIdAndDoNotAffectOtherOwners | STAFF/OWNER只自身；OTHER及同UUID CUSTOMER拒绝，合法自身更新提交 |
| 10 | tenantLevelResourceRejectsUndeclaredStoresAndSelf | 租户级不机械套store，也不放行未映射组合 |
| 11～12 | creationAssignsOnlyCurrentTenantAndTenantBusinessCodeIsNotGlobal；reflectedForgeryBeforePersistIsRejectedAndNothingInserted | 新建归属A；跨租户相同code可同时存在；伪造不新增 |
| 13～15 | updateOtherTenantDoesNotChangeDatabase；deleteOtherTenantDoesNotChangeDatabase；detachedEntityCannotBeInsertedOrMergedThroughControlledPort | B仍原值，记录未删除/新增 |
| 16～17 | crossTenantAssociationRejectedByApplicationWithoutWrite；compositeForeignKeyRejectsAssociationEvenWhenApplicationIsBypassed | 应用404、DB23503，child=0 |
| 18～19 | storeCreateRejectsSameTenantUnauthorizedAndForeignStore；ordinaryUpdatesCannotMoveTenantOrStoreEvenUsingReflection | 门店写拒绝；tenant/store原归属和业务值保留 |
| 20～23 | validAtomicBatchUpdatesActualUniqueRowsAndVersion；mixedTenantBatchUpdatesNothing；mixedUnauthorizedStoreBatchUpdatesNothing；duplicateBatchIdsAreDeduplicatedBeforeCountAndWrite | 合法2行version+1；混合0写；重复3个ID实际1行 |
| 24～25 | alreadyFlushedWriteRollsBackAtApplicationFailure；affectedRowMismatchRollsBackRealPartialDml | 已flush异常及触发器实际仅影响1行时，全批回滚原值/version=0 |
| 26 | noContextAndAuthorityReadCannotUseControlledPersistence；authorityRootWithoutBusinessPurposeRefusesDatabaseAccess | 无上下文、AUTHORITY_READ均拒绝，数据未增 |
| 27 | repeatedTenantsDoNotReusePredicatesEntitiesOrConnectionGuc | A/B反复分别2/1，事务外RLS读0；48份快照GUC均EMPTY |
| 28 | nestedNarrowRepositoryCannotRecoverOuterGrant | narrow A1后forPermission不能恢复TENANT，只A1可见 |

额外覆盖：STORES+SELF并集、TENANT门店上限、门店单条更新/删除、SELF写、缺失目标整批拒绝、合法单条删除、合法批量删除/非法门店删除、批次数量/安全字段白名单、应用事务强制、跨权限借用拒绝、宽范围实体写回和commit前范围变化拒绝、REQUIRES_NEW恢复、FORCE策略/复合键实际目录检查、RLS的native/JPQL/WITH CHECK与受限角色反例。

[影响数异常快照](evidence/P04-02/verify-complete-reports/p04-02-database-observations/affectedRowMismatchRollsBackRealPartialDml.json)显示A两条父记录均“初值”、version=0，B亦原值，证明真实部分DML被回滚；[混租户批次](evidence/P04-02/verify-complete-reports/p04-02-database-observations/mixedTenantBatchUpdatesNothing.json)同样无修改。SQL日志用于辅助定位，不作为唯一证据。

## 7. 结构防绕过与产物隔离

字节码门禁实际9项测试，[结构XML](evidence/P04-02/verify-complete-reports/surefire-reports/TEST-com.pet.testing.architecture.StructureRulesTest.xml)。新增覆盖api/application/domain直连EntityManager/Factory、JdbcTemplate/JDBC/DataSource/Query/Session；api/application直接引用受控基础实现；裸Repository继承；跨模块Repository；infrastructure自行native/JPQL/merge/find/getReference/bulk；bootstrap Handle/ConstantDynamic/方法引用绕过。违规夹具真实进入ASM分析，合法infrastructure受控适配器有正例，不因没有生产业务类而零匹配宣称有效。底层允许类仅ScopedPersistence/ScopedTransaction，字符串native/JPQL及无范围实体入口全局禁止。

当前规则不解析任意反射、恶意字节码/外部动态SQL，也不能证明所有未来自定义查询安全；名称含Tenant或位于infrastructure不等于安全，新增查询仍需真PG越权反例。当前没有业务类，未来模块仍须跑这些规则及其实际行为测试。

ProductionArtifactIT在package后核对所有编译测试class/resources/dependencies；[额外JAR审计](evidence/P04-02/artifact-audit.json)比较66个测试class和3项测试资源，无测试配置/安全实体/应用服务/事实源/接口/migration/Testcontainers。生产无@Entity类、无生产SQL、无safety_parent字符串。五个生产新类型都是共享能力，不是假业务模块。JAR SHA记录于该审计，未发布/部署。

## 8. 命令、退出码和失败记录

所有Maven命令工作目录为 `/Users/kimwell/work/pet-platform/apps/backend`；实际argv/JAVA_HOME/UTC起止时间/退出码/输出摘要完整保存在对应JSON，原始输出在同名log，XML/快照立即归档。文档/审计工作目录为项目根目录。

| 检查 | 退出码与结果 | 证据 |
| --- | --- | --- |
| ./mvnw -v（冻结JAVA_HOME） | 0，Temurin/Wrapper确认 | [toolchain](evidence/P04-02/toolchain.json) |
| 初次根目录Wrapper启动 | 127，未执行编译，纠正工作目录 | [原始错误元数据](evidence/P04-02/initial-launch-failure.json) |
| ./mvnw -q -DskipTests test-compile | 1，测试Runnable/错误访问器编译问题，非测试PASS | [compile-first](evidence/P04-02/compile-first.json) |
| ./mvnw clean verify（first） | 1，访问器名未修正完整，未运行测试 | [verify-first](evidence/P04-02/verify-first.json) |
| ./mvnw clean verify（second） | 0，159项0跳过；发现final getter代理警告，继续修正 | [verify-second](evidence/P04-02/verify-second.json) |
| ./mvnw clean verify（third） | 1，160项，其中1项LAZY断言失败；保留失败XML，不关闭检查 | [verify-third](evidence/P04-02/verify-third.json) |
| ./mvnw clean verify（fourth） | 0，162项；标量受控关联与补强通过 | [verify-fourth](evidence/P04-02/verify-fourth.json) |
| ./mvnw clean verify（final） | 0，166项；48项安全IT与独立快照通过 | [verify-final](evidence/P04-02/verify-final.json) |
| ./mvnw clean verify（complete，最终源码） | **0，167项：Surefire96、Failsafe71，0失败/错误/跳过；安全PG48项** | [最终元数据](evidence/P04-02/verify-complete.json)、[完整日志](evidence/P04-02/verify-complete.log)、[逐项对照](evidence/P04-02/test-results.json) |
| 证据辅助脚本 | 1，json import遗漏已纠正，未影响产品或Maven运行 | [辅助错误](evidence/P04-02/evidence-helper-failure.json) |
| 首次文档/JAR审计 | 1，报告尚未创建的链接拒绝；HEAD比较逻辑误包含本轮untracked行，实际HEAD未变；已修正 | [原始审计](evidence/P04-02/final-audit-incomplete.json)、[原因](evidence/P04-02/audit-incomplete-notes.json) |
| python3 docs/testing/evidence/P04-02/final-audit.py（最终） | 0，原114全保留、48独立快照、生产隔离/保全/链接/差异/HEAD通过 | [最终审计](evidence/P04-02/final-audit.json) |

等级：规则与接入方式DOCUMENTED；冻结依赖沿用RESOLVED，完整编译及生产JAR COMPILED；具体PostgreSQL/事务/角色/结构反例为RUNTIME_VERIFIED。未执行项保留NOT_EXECUTED/NOT_VERIFIED，等级不相互代替。没有Docker默认跳过、H2替换、Hibernate自动建表或关闭结构门禁；只有明确test-compile阶段使用skipTests，它不是最终验收命令。

## 9. 文件变更

完整新增/修改文件清单见[changed-files](evidence/P04-02/changed-files.json)，包含源码、规定文档和本轮完整证据。主要文件：

- main新增shared/persistence中的TenantScopedEntity、StoreScopedEntity、ResourceAccessPolicy、ScopedPersistence、ScopedTransaction；修改DatabaseConfigurationRules和application.yml以禁共享实体缓存。
- src/test新增tenantpersistence下9个安全模型/适配器/应用服务/事实配置/故意绕过/IT文件及security-migrations；修改StructureRulesTest、DatabaseConfigurationRulesTest。没有新增Controller。
- 创建本报告；更新MULTI-TENANCY/AUTHORIZATION/MODULE-BOUNDARIES、PERSISTENCE/DATABASE-MIGRATION/BACKEND、ROADMAP/DECISION-LOG/ACCEPTANCE-MATRIX和README最新状态。
- 新增本轮基线、命令日志/元数据、各轮报告、48份数据库快照、JAR/保全/逐项测试审计；历史失败和其他阶段证据未覆盖。

## 10. G01～G15

| 门禁 | 结果/等级 | 证据与实际判断 |
| --- | --- | --- |
| G01 前置/实际范围契约 | PASS / DOCUMENTED | 已读冻结文档和代码；TENANT/STORES/SELF、114基线、公开逐项批处理边界确认 |
| G02 创建归属不可由客户端决定 | PASS / RUNTIME_VERIFIED | 可信initializeTenant/Store、私有字段、伪造前persist拒绝与DB未新增 |
| G03 强制租户与范围查询 | PASS / RUNTIME_VERIFIED | 全查询外层AND，OR/未映射组合实测 |
| G04 分页/count/exists范围一致 | PASS / RUNTIME_VERIFIED | A/B、门店/owner的items/total/count/exists/ID集一致 |
| G05 单条错误统一 | PASS / RUNTIME_VERIFIED | 范围内SQL，隐藏/不存在同404异常及既定消息/错误码 |
| G06 受控增改删不能跨租户 | PASS / RUNTIME_VERIFIED | 伪造/detached/跨ID、store/tenant迁移及独立DB结果 |
| G07 STORES/SELF真数据库 | PASS / RUNTIME_VERIFIED | 空STORES/A1/A2/B1/门店上限、跨owner/主体域及读写 |
| G08 混合批次无部分写 | PASS / RUNTIME_VERIFIED | 全目标校验、混租户/门店/缺失目标无写；真实影响数异常回滚 |
| G09 应用与数据库关联 | PASS / RUNTIME_VERIFIED | 受控父查询、复合唯一/FK、23503、RESTRICT，无级联 |
| G10 结构约束与违规测试 | PASS / RUNTIME_VERIFIED | 业务直连/裸Repository/跨模块/infra原生/JPQL/方法引用真实反例 |
| G11 真PostgreSQL执行 | PASS / RUNTIME_VERIFIED | 48项安全IT，17.11隔离容器/受限runtime，48独立快照 |
| G12 原测试回归 | PASS / COMPILED / RUNTIME_VERIFIED | 167项0失败/错误/跳过，原114逐项无缺失 |
| G13 生产无安全测试产物 | PASS / COMPILED | package后ProductionArtifactIT及JAR额外审计，无测试migration/实体/接口 |
| G14 无正式业务/登录管理 | PASS / DOCUMENTED / COMPILED | 无生产Entity或SQL、无业务Controller、新身份/正式Store实现 |
| G15 文档边界/接入规则 | PASS / DOCUMENTED | 规定文档/清单/合法用法/禁止方式/批次与生产限制；最终链接/保全审计通过 |

## 11. 未实现与未验证

真实Sa-Token认证、权限管理和权威撤销/提交前实时安全版本重验未实现。正式Tenant/Store/Employee及所有业务表/CRUD、生产Store事实/有效状态实现未创建；测试事实不会进入生产扫描。所有未来模块、join/native/bulk/附件/导出查询安全必须另行登记和验证。

生产迁移/运行数据库角色实际权限与独立生产迁移NOT_VERIFIED/NOT_EXECUTED；当前仅技术容器权限测试。Redis命名空间、线程池/Servlet ASYNC/消息/系统任务、真实附件/导出/缓存等隔离没有完成。当前作用于同步JPA事务，不能携带Entity或上下文自动进入异步。READ_COMMITTED分页多语句不保证并发快照，内部批次需明确flush/clear；公开batch-actions/version/幂等未实现。远程CI、多OS、生产部署和正式业务验收未执行。

## 12. 状态与下一合法任务

P04-02 COMPLETE；P04整体IN_PROGRESS，P04-03 NOT_STARTED。完成受控JPA基础与技术模型测试不自动关闭P04、不表示认证或所有业务已安全。下一合法任务为 **P04-03：Redis命名空间、异步执行边界与多租户阶段验收**，本轮只报告，未执行。没有提交、推送、发布或部署。
