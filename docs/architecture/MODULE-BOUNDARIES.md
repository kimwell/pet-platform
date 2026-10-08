# 后端模块边界

冻结日期：2026-10-07，P01-02。本文拥有职责、数据归属、依赖和事务边界。一个 Spring Boot 应用、一个 Maven Module、一个 PostgreSQL 主库；使用 JPA/Flyway，不增加服务或独立数据库。包及目录见 [工程结构](PROJECT-STRUCTURE.md)。

接口名均是拟定契约名，不代表已有 Java 类型；只在真实用例需要时创建，输出稳定 DTO，不泄露 Entity/Repository/凭据或 SDK 对象。

| 模块 | 职责及拥有的数据 | 公开应用/查询接口 | 允许编译依赖 | 禁止依赖/访问 | 事务边界 / 阶段 |
| --- | --- | --- | --- | --- | --- |
| shared | 响应、异常、上下文、范围事务入口、技术配置；Outbox/任务调度技术记录 | TenantExecution、ScopedPersistence、CurrentPrincipal、任务/消息/存储端口 | JDK、冻结基础库，不依赖具体模块 | 全部具体模块、行业实体；不拥有员工/客户/租户业务 Repository | 仅技术事务入口；P03/P04/P10 |
| platform | 平台管理员、Tenant、Store、能力安装记录、平台控制面 | TenantDirectory、StoreDirectory、PlatformAccountService、InstalledModuleQuery | shared、audit.application | 其他身份内部数据；任意租户业务 Repository | 平台控制面或一个租户目录事务；P04/P05/P10 |
| identity | 员工、密码凭据、角色、授权、Organization、员工门店关系 | StaffAuthentication、StaffAuthorizationQuery、StaffDirectory、StaffManagement | shared、platform.application、audit.application | 客户/平台认证内部数据、微信 SDK、其他 Repository | 员工/角色授权与版本同事务；P05/P07 |
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
