# P04-01 可信租户上下文、数据范围模型与执行边界验证

日期：2026-10-08（Asia/Shanghai）。**P04-01 COMPLETE；G01～G15 PASS。P04整体 IN_PROGRESS。** 最终114项技术测试，原79项逐项回归，失败/错误/跳过均0。没有提交、推送或部署，没有自动执行P04-02。上下文和Guard不等于多租户数据隔离完成。

## 1. 前置事实、授权与冻结范围

用户明确授权本任务，替代AGENTS旧P02-01任务限制，其包名/技术/保全规则继续适用。直接在 `/Users/kimwell/work/pet-platform` 更新，后端固定com.pet.platform；未引入Product Delivery OS。已读取AGENTS/README、MODULE-BOUNDARIES/AUTHENTICATION/AUTHORIZATION/MULTI-TENANCY/CONFIGURATION、IDENTITY/API/DATA-TYPES、BACKEND/PERSISTENCE、VERSION-MATRIX/ROADMAP/DECISION-LOG、P03-03报告及ACCEPTANCE-MATRIX，检查身份/异常/trace/MDC/结构/测试注册/POM/Git。

P03-01/02/03及P03整体已COMPLETE，P03-03有效XML确为79项且0跳过。本轮开始时没有等价Java身份或租户上下文；复用原异常、错误码、响应、trace与ASM检查。P04尚未拆分，已补P04-01上下文与范围、P04-02 JPA/关联/PG隔离、P04-03 Redis/异步/阶段验收；原阶段目标、完成条件与A04-01～04要求全部保留。

当前Git实际已有60481ce提交且初始工作区干净，历史“尚无提交”不是现状。执行中另一个工作流更新了CI、VERSION-MATRIX和CI专用验证资料，本轮保留且不计入本轮文件清单；应用POM、pnpm锁、三端代码/生成产物与ui均保全。修改前SHA见 [baseline-files](evidence/P04-01/baseline-files.json)，最终逐文件/保全检查由 [审计脚本](evidence/P04-01/final-audit.py)生成。

沿用冻结应用依赖，无POM或锁修改。全局默认JDK与冻结发行版不同，实际检查通过命令专用JAVA_HOME使用已校验Temurin，不改全局设置。[工具链实测](evidence/P04-01/toolchain.json)、[完整输出](evidence/P04-01/toolchain.log)。精确版本唯一事实源仍是VERSION-MATRIX，当前改动仅为内部类型，不改变公开DTO/OpenAPI/error枚举，不无理由重生成三端产物。

## 2. 可信身份来源与默认行为

CurrentPrincipalProvider是内部服务器认证授权事实端口，默认生产Bean返回Optional.empty；无身份没有TenantContext。PLATFORM只用独立PlatformScopeGuard控制面入口，tenant必须null、租户grants/门店必须空，不自动转成任意租户身份。STAFF/CUSTOMER必须非空可信tenant；客户只支持明确SELF能力。同主体UUID跨域仍不同身份。

CurrentPrincipal只含主体域/ID、可信tenant、非凭据sessionId、authorizationVersion、操作权限、按permission的不可变范围及身份门店上限。不保存Token/密码/完整会话/客户资料，内部toString不输出授权内容。所有集合防御性复制。权限代码/授权版本/主体/租户/门店上限约束在构造时校验。客户端Header、Query、Body的tenantId/角色/权限/门店集合不被生产入口读取，tenantCode仍仅为候选登录入口。

P05负责真实Sa-Token域/载体/会话/权威安全及授权版本适配、公共端点无身份与适配器生命周期。本轮没有安装、调用或伪造Sa-Token会话。测试Provider和路径选择认证Filter只在src/test显式导入，生产包与默认扫描均无它们。

## 3. 生命周期、嵌套与同步后台

| 规则 | 实际行为 |
| --- | --- |
| 根入口 | 仅TenantContextFilter/TrustedTenantExecutor内部openIdentity；业务没有任意tenantId或Principal安装入口 |
| 身份根 | AUTHORITY_READ无业务权限/范围；预留身份owner安全查询，当前不实现认证DB查询 |
| 业务入口 | forPermission选择已授本permission；应用用例requirePermission核对操作；缺上下文/root业务访问404，缺具体权限403 |
| 同租户嵌套 | 同主体同权限保持或narrow到子集；forPermission不会恢复更大原grant |
| 跨租户/跨主体/扩大 | 拒绝且保持原上下文；业务内切换permission也拒绝，不借用list:TENANT执行update |
| 关闭 | 创建线程内LIFO；内层恢复外层，最外层清理；正常重复关闭幂等 |
| 顺序错误 | 抛生命周期错误，不改变内层，可以按正确顺序补关 |
| 非创建线程关闭 | 拒绝，不改变任一线程；owner仍能关闭 |
| 异常或内层遗漏 | 正常try-with-resources清理；根入口对未关内层先恢复/清理本次状态，再报告500内部错误 |
| 后台同步 | TrustedTenantExecutor只从注入可信Provider获取身份并执行明确permission；默认无身份401，平台/已有范围重开根拒绝 |

TenantContext不可变；Holder只公开current/required读方法，frame/replace及openIdentity/finishBoundary/withVerifiedStore不公开。不使用InheritableThreadLocal。没有上下文时租户业务Guard拒绝。后台SYSTEM登记/范围/权威重新授权/审计/描述传播尚缺，因此没有开放泛化系统runAs接口，后续P04-03/P05/P10承担接入责任。

## 4. 数据范围和门店行为

沿用冻结TENANT/STORES/SELF名称；DataScope保存tenant、主体域/ID、类型集合及允许storeIds。STORES空集合没有门店权限，TENANT只是本租户范围，SELF不默认createdBy也不单独授权整店。ScopeGrant.mergeForPermission要求同权限/同主体/同租户；同权限角色范围按原并集规则，TENANT归一化，否则STORES+SELF可并存，storeIds与身份上限相交。不同权限拒绝混合，未引入权限表达式引擎。

StoreScopeGuard只接收目标storeId，从StoreOwnershipReader内部端口查询归属，再核对当前tenant、当前permission范围和身份门店上限；不接受自称属于某租户的Store对象。shared不依赖platform/store实现。openStore经事实/授权检查才能写明确当前storeId，已有当前门店时内层不能静默换店。

| 场景 | 行为 |
| --- | --- |
| 门店不存在、属于他租户、本租户但未授权 | 同一404 RESOURCE_NOT_FOUND/“资源不存在或不可访问” |
| 本租户且STORES包含目标、身份门店上限允许 | 通过 |
| TENANT且真实本租户、身份门店上限允许 | 通过；TENANT不绕归属检查 |
| STORES空集合或仅SELF | 404，不解释成全部或本人经营门店 |
| 缺生产事实实现 | 默认503 DEPENDENCY_UNAVAILABLE；Guard真实调用默认Bean的拒绝已测试 |

正式Store数据源/有效状态检查后续由platform/store提供，当前无Store表、CRUD或生产归属事实。SELF业务归属谓词、数据库关联/行过滤在P04-02及相应用例实施。

## 5. HTTP、ERROR、ASYNC与MDC

TraceFilter（最高优先级）→可信认证适配器位置→TenantContextFilter（其后）→MVC。当前没有真实认证适配器，生产Provider为空，不将全部/api视为已登录。REQUEST仅由Provider建立身份根；Filter错误经现有HandlerExceptionResolver/GlobalExceptionHandler输出JSON状态/error/trace/no-store，不返回容器默认页或异常原文。成功、Controller异常、字段校验、Provider已知/未知异常、内层遗漏均已覆盖。

同步ERROR复用私有请求属性中的服务器身份快照，不重新读取Provider；普通再分发重建身份根，嵌套同步ERROR沿用当前收窄范围。真Tomcat sendError与自动Filter注册实测。REQUEST启动Servlet异步后移除该身份快照，后续异步ERROR不自动重绑；ASYNC不注册/不自动绑定，租户Guard默认拒绝，公共异步链继续工作。相关反例是边界拒绝测试，完整Servlet异步/流提交后的错误转换仍NOT_VERIFIED；现有trace仅同步REQUEST/ERROR，未扩展异步追踪。已提交响应不强行改写JSON。

Scope只写tenantId/operatorId，以及经过事实检查明确选择的storeId。授权storeIds不等于当前store，不任选一个写MDC。close恢复这三个键，保留traceId/其他组件MDC，不调用MDC.clear。未知错误日志仍只有trace和异常种类；不输出Token/密码/完整Principal/客户资料/授权集合。

## 6. 测试覆盖与证据

本轮新增35项，最终114项（Surefire91 + Failsafe23），失败/错误/跳过均0；原79项按class+case名称逐项对照，missingBaselineCases=[]。数据库原集成测试正常执行，包含真实PostgreSQL/Flyway/JPA事务、分页、标量/迁移反例，未以本轮不改数据库为由跳过。模型与MVC上下文本身使用独立Web上下文，不启动PG；真Tomcat自动注册/ERROR测试与既有全应用IT保持数据库自动配置。

| 用户要求场景 | 覆盖与依据 |
| --- | --- |
| 1～4 无身份、员工、客户、平台 | TenantExecutionTest/TenantMvcTest：匿名无上下文、可信租户/跨域主体、平台租户操作拒绝；默认Provider empty |
| 5～7 Header/Query/Body防伪造 | TenantMvcTest三个独立方法，各覆盖可信租户不被覆盖与匿名不被建立；Body仅测试意图DTO，不是生产业务DTO |
| 8～12 缺上下文、同租户/跨租户/扩大、异常 | 模型+MVC业务/root拒绝、nested恢复/收窄、不允许切permission/主体/租户、正常及遗漏内层清理 |
| 13 真实线程复用 | exactSameWorkerHandlesAThenBThenAnonymousWithoutResidue：同一个single-thread executor，A/B/匿名/A/匿名，实际线程ID集合恰1；每次核对上下文与MDC |
| 14 并发与不继承 | 两工作线程屏障同步观察A/B独立；owner范围中创建另一线程无上下文且不能关闭owner scope |
| 15～19 门店范围与事实源 | 空STORES、跨tenant、同tenant范围外、已授权、TENANT、SELF、缺默认Reader503、显式store/嵌套换店拒绝 |
| 20 Filter/ERROR协议/trace | MVC解析、同步再分发Provider不重读、嵌套ERROR保持scope；真Tomcat未知/503/sendError/漏关；ASYNC与异步ERROR不自动绑身份 |
| 21 测试Provider不进生产 | 生产JAR全部编译测试class/resource比对及字节码依赖规则，见第8节 |
| 补充 | 构造约束/集合复制、同权限合并/门店交集、异权限拒绝、LIFO/跨线程关闭、MDC外层恢复、存储/身份根入口ASM反例 |

每个待测请求返回后先断言TenantContext/MDC已清理；不先手动clear。需要清理的外层MDC测试先核对恢复值才兜底移除测试自己设置的键；测试身份ThreadLocal的清理属于测试认证适配器，不操作待测Holder。测试是技术夹具与本机协议/容器运行证据，不是正式身份、门店、业务或数据库隔离验收。

| 工作目录 | 实际命令 | 退出码/结果 | 证据 |
| --- | --- | --- | --- |
| apps/backend | ./mvnw -v（命令专用冻结JAVA_HOME） | 0，工具链实测 | [toolchain](evidence/P04-01/toolchain.json) |
| apps/backend | ./mvnw clean test -Dtest=TenantExecutionTest,TenantMvcTest,StructureRulesTest | 1，35项中1失败：Mock流默认编码导致中文变问号；夹具UTF-8修复，原失败保留 | [focused-first](evidence/P04-01/focused-first.json)、[日志](evidence/P04-01/focused-first.log) |
| apps/backend | ./mvnw clean verify（首轮） | 0；随后共享target被另一契约检查清理，首次XML提取只见10项，被识别为不完整，未作为门禁PASS证据 | [verify-first](evidence/P04-01/verify-first.json)、[不完整归档](evidence/P04-01/test-results-interfered.json) |
| apps/backend | ./mvnw clean verify（立即归档） | 0，113项、原79项全部回归，0失败/错误/跳过 | [verify-final](evidence/P04-01/verify-final.json)、[当次结果](evidence/P04-01/test-results-113.json) |
| apps/backend | ./mvnw clean verify（异步ERROR加固后最终） | 0，114项、原79项全部回归，0失败/错误/跳过；同一进程结束后立即保存XML/JAR审计 | [verify-async-final](evidence/P04-01/verify-async-final.json)、[完整日志](evidence/P04-01/verify-async-final.log)、[逐项汇总](evidence/P04-01/test-results.json) |
| 根目录 | python3 docs/testing/evidence/P04-01/final-audit.py | 0；文件SHA保全、文档链接、git diff --check、最终JAR与门禁结果一致 | [审计脚本](evidence/P04-01/final-audit.py) |

所有最终TEST-*.xml位于同一证据目录。失败、重复执行和共享target干扰如实记录；最后运行源码修改后重新完整verify，不用旧包或定向测试代替最终门禁。等级：规则DOCUMENTED，既有冻结依赖沿用RESOLVED，完整编译/JAR为COMPILED，明确模型/MVC/Tomcat/PG技术场景为RUNTIME_VERIFIED；这些等级不相互代替，不外推业务/生产安全。

## 7. 新增、修改文件

| 类型 | 文件与目的 |
| --- | --- |
| 新增生产security（4） | CurrentPrincipal、CurrentPrincipalProvider、PrincipalType、PlatformScopeGuard |
| 新增生产tenancy（13） | DataScope、DataScopeType、ScopeGrant、TenantContext、TenantContextHolder、TenantContextFilter、TenantExecutionScope、TenantPurpose、TenantScopeGuard、TrustedTenantExecutor、StoreOwnershipReader、StoreScopeGuard、TenancyConfiguration |
| 修改生产（1） | shared/exception/TenantAccessDeniedException，仅同步中文注释，不变404错误协议 |
| 新增测试（4） | com.pet.testing.tenancy下TenancyFixtures、TenantExecutionTest、TenantMvcTest、TenantHttpIT；无正式API/表 |
| 修改测试（1） | StructureRulesTest：底层存储/入口符号、模型/领域依赖、InheritableThreadLocal及反例/可见性检查 |
| 文档（11） | 本报告、README、MULTI-TENANCY、AUTHORIZATION、MODULE-BOUNDARIES、BACKEND、PERSISTENCE、IDENTITY、ROADMAP、DECISION-LOG、ACCEPTANCE-MATRIX |
| 证据 | baseline、工具链、真实命令/日志、最终XML/逐项对照、JAR审计、最终文件/链接保全脚本与结果 |

源码路径均在apps/backend/src/main/java/com/pet/platform/shared及src/test/java/com/pet/testing；完整逐文件SHA由审计脚本生成。无关CI/VERSION-MATRIX/CI验证报告的并行变更保留，非本轮所作。未修改ui、Web/小程序、公开契约生成文件、POM/锁或正式迁移。

## 8. 结构与生产产物隔离

继续执行原ASM字节码/泛型/注解/Repository继承检查。新增对调用/字段符号的规则：业务不能直接操作Holder底层存储，根身份建立只能由HTTP Filter/可信同步Executor发起；shared不依赖platform/store实现；上下文模型不依赖HTTP/Controller/微信SDK；domain不依赖tenancy；禁止InheritableThreadLocal。ASM违规技术class证明规则拒绝，不是只有空模块的文档检查。反射/被攻陷服务端代码/SQL内容/运行时权限不在结构证明范围。

最终生产JAR与全部52个编译测试class及2个测试资源比较，violations=[]；无测试Provider、认证选择Filter、测试Controller/门店实现/Entity/migration、测试配置或Testcontainers依赖。ProductionArtifactIT在package之后实际读取包，审计另保存JAR条目与SHA，[artifact-audit](evidence/P04-01/artifact-audit.json)。默认空身份Provider和拒绝门店事实查询属于生产安全默认行为，不是测试兜底。

## 9. G01～G15

| 门禁 | 结果/等级 | 依据 |
| --- | --- | --- |
| G01 前置与冻结 | PASS / DOCUMENTED / RESOLVED | 原79项/指定事实源核对，应用依赖未改，P04原条件保留 |
| G02 可信身份边界 | PASS / COMPILED / RUNTIME_VERIFIED | 默认Provider empty；平台/员工/客户约束；P05适配责任明确 |
| G03 客户端tenant不能建/改 | PASS / RUNTIME_VERIFIED | Header/Query/Body各有覆盖可信与匿名反例 |
| G04 无上下文拒绝 | PASS / RUNTIME_VERIFIED | Holder/业务/permission/TENANT/门店入口、匿名与平台/root业务访问拒绝 |
| G05 不可变与生命周期 | PASS / COMPILED / RUNTIME_VERIFIED | 防御性复制、内部存储、LIFO/同线程/异常/泄漏清理 |
| G06 嵌套不跨tenant/扩大 | PASS / RUNTIME_VERIFIED | 同权限收窄恢复、跨主体/租户/门店扩大/permission切换拒绝 |
| G07 空STORES拒绝 | PASS / RUNTIME_VERIFIED | 空集合不是全量，StoreGuard真实拒绝 |
| G08 归属与授权分开 | PASS / RUNTIME_VERIFIED | 可信端口归属、tenant范围与身份上限、本权限storeIds分别核对 |
| G09 缺事实不放行 | PASS / RUNTIME_VERIFIED | 默认Reader503，实际StoreGuard调用默认Bean拒绝 |
| G10 异常/线程复用清理 | PASS / RUNTIME_VERIFIED | MVC/真Tomcat异常与泄漏；同一线程A/B/匿名5次，线程ID恰1；并发独立 |
| G11 MDC与trace | PASS / RUNTIME_VERIFIED | 明确当前store、三个键恢复、trace/其他组件保留；同步ERROR协议 |
| G12 生产无测试身份/归属 | PASS / COMPILED / RUNTIME_VERIFIED | package后52测试class/2资源全比较、JAR条目无违规 |
| G13 原有与新增测试 | PASS / COMPILED / RUNTIME_VERIFIED | 最终clean verify exit0，114项，79项逐项回归，0跳过 |
| G14 未提前实现排除项 | PASS / DOCUMENTED / COMPILED | 无登录/正式表/CRUD/JPA租户查询/Redis/隐式异步，产物与变更核对 |
| G15 文档一致 | PASS / DOCUMENTED | 规范/决策/路线/矩阵/源码边界一致，命令证据/文件保全/链接审计 |

G01～G15只覆盖本任务，上下文技术门禁不能替代P04数据库/Redis/异步或P05真实认证门禁。

## 10. 尚未实现或未验证

| 项目 | 状态与后续任务 |
| --- | --- |
| 真实认证/Sa-Token/Redis会话、Cookie/CSRF、权限版本权威重验 | NOT_EXECUTED / NOT_VERIFIED，P05；当前只提供适配端口/模型 |
| 正式Tenant/Store/Employee表、Store归属与状态事实源 | 未实现；platform/store后续用例，默认不放行，不提供CRUD |
| JPA租户受控访问、关联复合约束、RLS/运行角色/SQL注册、PG越权读写、连接复用与REQUIRES_NEW租户隔离 | NOT_EXECUTED / NOT_VERIFIED，P04-02；原PG基础回归不等于这些隔离验收 |
| SELF实际业务归属谓词、权限实际数据库合并/撤销 | 未实现；P04-02/P05/业务owner；不是createdBy通用方案 |
| Redis租户缓存、异步Servlet/线程池/消息/任务的可信传递与重新授权、SYSTEM登记/审计 | NOT_EXECUTED / NOT_VERIFIED，P04-03/P05/P10；只验证不隐式继承与默认拒绝 |
| 完整异步/公共流真实运行、流提交后错误转换 | NOT_VERIFIED；本轮只实现同步REQUEST/ERROR边界与异步不重绑反例 |
| 生产部署/独立生产迁移、多OS、远程CI、正式业务/设备验收 | NOT_EXECUTED / NOT_VERIFIED；macOS arm64本机技术证据不替代这些事项 |

无关键本任务门禁失败，P04-01 COMPLETE。P04整体IN_PROGRESS，原跨租户查询/写入/Store/SELF、关联、运行角色/连接/异步等剩余必选条件仍需通过。**下一合法任务：P04-02：JPA租户受控访问、关联约束与PostgreSQL越权测试。** 只报告，不自动执行。
