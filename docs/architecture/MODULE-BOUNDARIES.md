# 后端模块边界

P07-01身份模块已实现EmployeeDirectory/EmployeeQueryStore固定应用读取契约、EmployeeView投影及identity.infrastructure.EmployeeQueries；shared只提供无具体模块依赖的范围/DTO查询基础。实际字段与权限见[员工读取契约](../contracts/EMPLOYEE-MANAGEMENT.md)。

当前员工读取实施见[P07-01验证](../testing/P07-01-VERIFICATION.md)，身份装配沿用文末P05章节。旧阶段“尚未实现”描述保留为历史范围；冻结安全契约不变。
冻结日期：2026-10-07，P01-02。本文拥有职责、数据归属、依赖和事务边界。一个 Spring Boot 应用、一个 Maven Module、一个 PostgreSQL 主库；使用 JPA/Flyway，不增加服务或独立数据库。包及目录见 [工程结构](PROJECT-STRUCTURE.md)。

接口名均是拟定契约名，不代表已有 Java 类型；只在真实用例需要时创建，输出稳定 DTO，不泄露 Entity/Repository/凭据或 SDK 对象。

| 模块 | 职责及拥有的数据 | 公开应用/查询接口 | 允许编译依赖 | 禁止依赖/访问 | 事务边界 / 阶段 |
| --- | --- | --- | --- | --- | --- |
| shared | 响应、异常、上下文、范围事务入口、技术配置；Outbox/任务调度技术记录 | TenantExecution、ScopedPersistence、CurrentPrincipal、任务/消息/存储端口 | JDK、冻结基础库，不依赖具体模块 | 全部具体模块、行业实体；不拥有员工/客户/租户业务 Repository | 仅技术事务入口；P03/P04/P10 |
| platform | Tenant、Store、能力安装记录、平台控制面目录；平台凭据见P05-04 identity归属 | TenantDirectory、StoreDirectory、PlatformAccountService、InstalledModuleQuery | shared、audit.application | 其他身份内部数据；任意租户业务 Repository | 平台控制面或一个租户目录事务；P04/P05/P10 |
| identity | 员工与平台管理员凭据/认证、角色、授权、Organization、员工门店关系 | StaffAuthentication、StaffAuthorizationQuery、StaffDirectory、StaffManagement | shared、platform.application、audit.application | 客户/平台认证内部数据、微信 SDK、其他 Repository | 员工/角色授权与版本同事务；P05/P07 |
| customeridentity | 客户、AppID/OpenID 映射、手机号关联 | CustomerAuthentication、CustomerDirectory、CustomerAuthorizationQuery | shared、platform.application、audit.application | identity 内部数据；客户不能继承员工角色 | 身份映射/手机号补充各自事务；微信 HTTP 在事务外；P05/P09/P10 |
| attachment | 元数据、临时状态、单资源绑定、访问票据、文件补偿记录 | AttachmentApplication、AttachmentQuery、AttachmentAccessPolicy 注册接口 | shared、audit.application | modules 实现/Repository；不自行定义业务所有权 | 绑定与业务同一 DB 事务；文件 I/O 补偿；P08 |
| audit | 追加审计、身份/租户/trace 关联、受限查询 | AuditAppender、AuditQuery | shared | 其他 Repository、业务写回调；不存请求秘密 | 成功审计随用例提交；失败事件独立短事务；P08，P05 先接端口 |
| notification | 通知意图、收件对象引用、渠道、投递与幂等记录 | NotificationApplication、NotificationQuery、渠道端口 | shared、audit.application | 员工/客户 Repository、业务写接口 | 意图事务内、外部发送事务外；P10 |
| modules.<实际模块> | 本模块业务数据与规则；模板没有行业模块 | 明确 Command/Query 接口 | shared；按用例依赖上述 application 契约 | 其他 infrastructure/Repository/Entity；循环依赖 | 一用例、一可信租户、一事务；P07/P11 扩展 |

业务调用方实现附件/通知的策略端口并注册，提供方不反向 import 业务模块。shared 身份读取端口由三个身份模块实现；shared 只定义稳定协议。audit/notification 不递归调用上游写接口。模块编译依赖必须有向无环。

## 层次与事务

api 负责 DTO、输入校验、HTTP；application 负责授权、事务、范围与协调；domain 表达有行为的规则；infrastructure 负责本模块持久化/外部适配。domain 不依赖 Controller、HTTP、线程上下文或微信 SDK；允许领域实体使用 JPA 映射，不向 api/其他模块泄露。简单 CRUD 不强制空领域服务/接口。

跨模块只能引用 application 下明确公开的契约/DTO。需要跨数据查询时由数据 owner 提供投影接口，不让消费者 join 他人私表。共享数据库不意味着共享 Repository。

application 用例声明事务，只读查询也经范围事务入口；关闭 OSIV。同步接口默认 REQUIRED，沿用同一 TenantContext，禁止切换租户/扩大范围。业务写、附件绑定、审计和 Outbox 意图可同事务；远程微信、消息发送和长文件 I/O 不在长 DB 事务内，不引入分布式事务。恢复分别见 [文件存储](FILE-STORAGE.md)、[异步事件](ASYNC-EVENTS.md)。

账号停用/密码重置同事务更新安全版本和撤销意图；权威安全版本保证 Redis 删除失败仍拒绝旧会话。权限撤销对提交后开始的新请求生效；已开始的敏感写在提交前再检查安全版本，变化则回滚，见 [认证](AUTHENTICATION.md)。

## 防绕过检查

P03-03已建立JUnit编译字节码检查，复用已有测试依赖ASM，未新增架构库。StructureRulesTest分析class常量池符号、描述符、泛型签名及注解，并按本表模块映射检查shared依赖具体模块、domain依赖api/HTTP/具体WxJava/持久化适配/TraceContext、跨模块非application或Repository、api直连持久化、未登记模块依赖与生产引用测试类型。Repository继承按字节码递归读取，不能靠改类名或放到application绕过。当前允许依赖图本身有向无环，未实现动态插件/反射依赖推断。

ASM生成的违规class夹具覆盖普通字段及只有泛型签名的引用，确认规则能拒绝；不是因为没有业务类就声称安全。ProductionArtifactIT在package之后比较全部编译测试class与测试资源、测试依赖名称；反例包含测试Controller和migration。原application-test.yml已移至src/test/resources，生产JAR不再包含该测试环境配置。

范围Repository、租户表/原生SQL注册表与数据库角色/RLS属于P04持久化防绕过实施：本轮没有生产租户表或业务Repository，也没有实现这些行为门禁。类扫描不检查运行时反射字符串、SQL内容、动态依赖或数据授权，不等于租户隔离完成，不能替代A04的数据库越权读写测试。后续实际公开application契约仍由owner登记；包位置门禁本身不能证明契约行为正确。

结构检查与 [验收矩阵](../testing/ACCEPTANCE-MATRIX.md) 的真实 PostgreSQL、Redis、HTTP、文件故障测试共同证明行为，文档或扫描不能单独证明隔离。

## P04-01 实际契约与结构边界（2026-10-08）

shared.security公开最小CurrentPrincipalProvider/CurrentPrincipal、PrincipalType及PlatformScopeGuard；shared.tenancy公开只读Holder、TenantContext、DataScope/ScopeGrant、执行范围/Guard、StoreOwnershipReader及可信同步Executor。上下文的构造不授予安装权限；根openIdentity、底层frame/replace与finishBoundary仅包可见，业务调用只能从已建立边界选择/收窄权限范围。生产默认身份为空、默认门店事实查询抛503；测试Provider/认证Filter/门店事实夹具只在src/test。

StoreOwnershipReader端口在shared，由未来platform/store数据owner实现；不反向引用实现、Entity或Repository。P05三个身份owner按认证与IDENTITY契约提供权威事实；当前未提供登录、会话或业务API。AUTHORITY_READ与BUSINESS的持久化目的白名单尚待P04-02，不能把当前Guard称为数据库防绕过。

原P03编译字节码检查新增：禁止生产InheritableThreadLocal、领域依赖tenancy、内部上下文模型依赖HTTP/Controller/微信SDK；扫描调用/字段符号，只有Holder/Scope能操作底层存储，只有TenantContextFilter/TrustedTenantExecutor能调用根openIdentity。反例用ASM生成，不放入生产。另验证修改方法的非public可见性。生产/测试class、资源、依赖的JAR隔离检查继续执行，包含新增测试身份/门店夹具。结构规则不分析反射/恶意代码/运行时归属，不能防止所有越权；数据库和真实认证测试仍需后续完成。

## P04-02 受控适配器与防绕过规则（2026-10-08）

shared.persistence新增TenantScopedEntity/StoreScopedEntity、ResourceAccessPolicy、ScopedPersistence，以及包可见ScopedTransaction。业务实体继承归属基类；domain只使用实体基类，不直接访问tenancy线程范围。模块infrastructure构造受控适配器，application调用本模块固定查询/命令方法并声明代理事务；api仍只接DTO。ScopedPersistence的受保护方法不是模块公开API，不向跨模块消费者暴露Entity或Repository。

StructureRulesTest新增业务api/application/domain对EntityManager/Factory、Query/TypedQuery、Hibernate Session、JdbcTemplate/JDBC、Connection和DataSource的禁止；api/application不能直接引用受控基础实现。业务模块不能继承裸Spring Data Repository全量API。跨模块Repository/非application引用继续拒绝。基础设施位置只允许构造基类所需句柄，不能调用原生Query、merge/find/getReference或自行创建JPQL/bulk；底层调用仅登记ScopedPersistence/ScopedTransaction，字符串JPQL/native/无范围find等仍全局禁止，当前没有其他底层适配器白名单。

规则读取编译字节码的字段/泛型/注解/继承及直接调用、bootstrap Handle/ConstantDynamic/方法引用；违规夹具包括api/application直连、裸Repository、infrastructure中的native/merge/JPQL以及原生方法引用，合法基础适配器也有正例。生产class与全部测试class/resource/JAR依赖比对继续执行。测试模型、应用服务、门店事实源、故意绕过入口和migration全在src/test，没有生产测试Controller。

这些规则不解析任意反射字符串、恶意代码或外部动态SQL，不证明未来自定义查询安全。新能力只能经明确登记、代码审查与真实PostgreSQL范围反例接入；名称含Tenant、位于infrastructure或通过静态检查都不构成授权证据。接入清单与具体限制见[持久化](../conventions/PERSISTENCE.md)、[P04-02](../testing/P04-02-VERIFICATION.md)。

## P04-03 Redis与异步入口

shared.redis拥有RedisKeyBuilder/RedisKey、TenantRedisAccess、PlatformRedisAccess及包内RedisValueStore/配置；模块infrastructure仅注入受控地址与端口，再提供固定资源方法并执行当前数据权限。禁止模块直接裸Redis/客户端/私有驱动、创建Key对象、通用Spring Cache/@Cacheable或将授权列表放入raw空间。数据语义限制、版本与TTL由[REDIS](../conventions/REDIS.md)拥有。

shared.tenancy新增唯一TenantTaskExecutor/AsyncExecutionConfiguration与包内TaskDeadline；shared.observability增加只管理traceId的TraceScope。任务根openTask、时效捕获和底层状态读取仍包内且仅批准执行器使用。业务不可构造未登记执行器/可信身份根入口、调用无身份技术submitUnscoped、使用@Async/公共ForkJoinPool/Executors/自行启动Thread绕过。先结束调用方事务，工作端代理新事务沿用ScopedTransaction。快照只当前权限、不可伪造或序列化成未来消息凭证；P05/P10责任见[ASYNC-EXECUTION](../conventions/ASYNC-EXECUTION.md)。

ASM只扫描本项目生产class，第三方内部Executor不进入检查；新增泛型引用/直接调用/bootstrap Handle反例证明Redis、Key伪造、异步入口、无身份任务及未登记可信入口规则有效。生产继续禁止引用测试Provider、角色/Entity/SQL夹具，并对全部测试class/resources/dependencies做JAR比对。扫描不理解任意闭包/反射/动态字符串或缓存内容，真实范围与部署权限不能以静态PASS替代。

## P05-01 正式数据owner与受限入口（2026-10-08）

platform.domain拥有Tenant/Store（控制面Tenant继承BaseEntity，Store继承TenantScopedEntity）；identity.domain拥有Employee/Role与三关系，均为真实JPA映射而非空分层类。platform.application.PlatformPermissions声明租户门店管理能力，identity仅依赖该公开契约。Organization/平台账号/客户身份未在本轮创建，留待其真实用例。

identity.application.authentication公开StaffAuthentication/StaffIdentity及内部IdentityLookup/AuthenticationCandidate，只有identity认证应用与登记AuthenticationJdbc，以及未来identity/api/authentication适配器可以访问；其他业务/api引用被编译字节码拒绝。候选无凭据getter、Jackson禁止可见性，实体不返回Controller，身份事实不等于已验证会话。

新增底层白名单仅AuthenticationJdbc（两个固定只读函数）、IdentityRuntimePermissions（系统权限目录检查）、CommandDatabase/BootstrapJdbc/MigrationCommand（独立命令）。普通EntityManager/native/JPQL/JDBC禁令保留，白名单不是给模块infrastructure全面放行。反例仍检查方法引用和泛型；Store事实投影只能登记PostgresStoreOwnershipReader调用，shared不引用具体实体。

显式identity.application.bootstrap协调一次跨owner基础初始化，经专用数据库函数原子创建platform与identity基础记录；这是单一初始化用例，不是跨模块Repository或通用写接口。初始化命令不注册Spring Bean，不被Application调用，不开放HTTP。普通模块/api不能引用bootstrap端口或底层类。正式表/角色/命令及安全责任见[初始化说明](../development/IDENTITY-BOOTSTRAP.md)。

## P05-02 真实认证边界（2026-10-08）

identity/api/authentication暴露五项正式STAFF接口；identity/application/authentication定义HTTP安全用例和StaffSessionPort，身份查找复用P05-01窄范围函数。只有identity/infrastructure/session可以调用Sa-Token、官方Redis DAO及原始认证Redis，装配Application仅排除未受限官方DAO自动Bean；业务不直接调用StpUtil/SaHolder或认证存储。StaffAuthenticationFilter是唯一生产CurrentPrincipalProvider，shared通过现有端口消费，不反向依赖identity。SessionPrincipalProvider标记会话来源，TenantExecutionScope禁止捕获其异步任务；不创建第二套Provider或放宽RLS。生产JAR不包含测试Probe或临时浏览器页面。


## P05-03 固定安全适配与任务重验（2026-10-08）

四个公开DTO/Controller仅调用StaffSecurityOperations，application通过StaffSecurityStore端口使用固定安全SQL。StaffSecurityJdbc作为唯一新增登记SQL调用方，负责可信STAFF同租户GUC、行锁及原子变更/记录/清理意图；没有放行整个infrastructure或普通业务native/JDBC。AuthenticationJdbc读取V2授权投影新增强制改密字段，密码哈希仍仅受限内部类型，不公开查询。runtime权限核验扩展两张FORCE RLS表与受限列权限。

shared.security.TaskAuthority是无具体模块依赖的内部端口，StaffTaskAuthority在identity实现私有真实会话证明和执行前重验，TenantTaskExecutor不直接引用identity/Sa/Redis；按原范围与当前授权取交集。测试故障注入/屏障仅在src/test，生产无测试开关。生产接口总计九条路径；不实现平台/客户认证与完整audit模块。

## P05-04 平台凭据owner与受限装配（2026-10-08）

本任务明确允许平台账号归identity，故集中由identity拥有PlatformAccount、初始化和PLATFORM认证/安全事务；platform继续拥有Tenant/Store控制面目录。此为原表“平台管理员”职责的具体凭据归属细化，不增加platform对identity内部实现的依赖，不放宽shared/模块图。shared只消费原CurrentPrincipalProvider/PlatformScopeGuard；两域路由复用唯一生产Provider。

PlatformIdentityJdbc只登记七个固定运行函数调用/局部本人事务；PlatformBootstrapJdbc只登记专用首次创建，普通业务/Controller/租户适配器不能引用平台候选或底层SQL、选择认证空间。SaIdentitySessions和WebCookieSecurity只共享已验证的设备与传输安全语义，不建通用认证框架。结构规则精确追加两个SQL适配器，并增加平台凭据/bootstrap负例；不开放全部infrastructure。没有平台账号列表/通用Repository/HTTP bootstrap/租户冒用。

## P05-05 客户owner与会话技术复用（2026-10-08）

customeridentity独立拥有正式模型、四个公开接口、最小身份/配置/Gateway/会话端口及固定CustomerIdentityJdbc；WechatBinding不是Employee关联。只有WxJavaMiniProgramGateway及其私有登录客户端依赖SDK，业务/API不使用WxJava类型。客户认证结果、配置、注册存储不是Controller登录凭据；字节码规则拒绝API/普通业务访问，register仅CustomerHttpAuthentication调用，底层SQL白名单只追加CustomerIdentityJdbc。

现有唯一认证Filter/装配位于identity/infrastructure/session，作为应用技术装配精确引用CustomerHttpAuthentication路由；客户SaCustomerSessions/CustomerConfiguration精确复用已有SaIdentitySessions、AuthenticationRedis及IdentitySessionPort/StaffSessionPort.Channel技术类型。这是现有引擎复用登记例外，不开放identity账号/密码/授权查询、Entity/Repository、其他认证内部接口或模块通用循环调用；新规则反例覆盖客户适配器试图访问IdentityLookup。当前Filter仍唯一生产CurrentPrincipalProvider，shared不依赖客户模块。后续独立提取技术包可以集中重构，但本轮保持已验证员工/平台框架及端口。
