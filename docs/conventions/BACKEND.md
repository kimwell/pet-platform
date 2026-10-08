# 后端工程约定

冻结日期：2026-10-07，P01-02。本文拥有编码/事务/校验约定，模块/安全/协议规则分别引用，不再复制。正式代码P02以后按任务创建。

固定一个Module、基础包与启动类见 [结构](../architecture/PROJECT-STRUCTURE.md)，精确依赖见 [矩阵](../development/VERSION-MATRIX.md)。Spring Boot BOM管理依赖不重复覆盖版本；Hutool仅core必要工具，不替代认证/JSON/存储/加密/租户。不得引入H2作为PostgreSQL验收替代。

Controller只处理输入/HTTP/DTO，调用application用例；返回DTO，不暴露Entity/Repository/SDK。职责、允许依赖、事务见 [模块](../architecture/MODULE-BOUNDARIES.md)。业务规则/注释/错误/文案中文，技术标识英文。没有行为不创建多层空抽象。

输入DTO分create/replace/patch/query，Bean Validation配合跨字段应用校验，禁止Entity直接反序列化；未知字段拒绝。PATCH显式记录字段presence，null规则见 [API](../contracts/API.md)。JSON序列化按语义使用统一编解码：时间/ID采用全局规则，金额/Long计数采用公共专用注解，不散落不同字段实现；测试往返真实HTTP JSON和OpenAPI一致。

application显式事务，经范围入口，禁止OSIV。更新从有范围查询加载managed实体、@Version并发检查；批量/原生SQL须登记，见 [隔离](../architecture/MULTI-TENANCY.md)。Flyway维护结构、ddl-auto=validate，迁移/运行凭据分开。业务表标准createdAt/updatedAt/version等按实际数据需要，不为所有技术表机械继承万能BaseEntity。

权限代码在代码声明，数据库关系不伪造额外权限；认证空间与会话读取见 [认证](../architecture/AUTHENTICATION.md)。安全状态当前值来自权威记录，Redis不可用不降级绕过。

审计记录稳定操作码、主体域/ID、Tenant、资源标识、结果、traceId、必要字段差异摘要；不记密码/Token/Cookie/微信code/密钥、全请求体或下载票据。业务成功审计同事务；失败安全审计短事务，注意避免敏感资源存在性泄露。

远程调用和文件长I/O在DB事务外，失败用意图/补偿，不把catch吞错当成功。Outbox/任务线程必须显式上下文及finally清理；禁默认公共线程池继承认证。配置与开关见 [配置](../architecture/CONFIGURATION.md)。

测试分层：JUnit技术单元夹具清楚标识；真实PostgreSQL/Testcontainers测试SQL/事务/RLS；真实Redis测试跨实例会话；HTTP/浏览器/DevTools/真机测试传输。结构测试与运行反例共同验收，命令/工作目录/退出码/输出/产物摘要留证。不得把MockMvc、内存会话、文档扫描单独标为业务PASS。完整任务映射见 [矩阵](../testing/ACCEPTANCE-MATRIX.md)。

## 当前协议实现（P03-01）

公共响应/错误/分页类型和参数语义见 [API](../contracts/API.md) 与 [分页](../contracts/PAGINATION.md)，使用示例与例外由这两份权威文档拥有。Controller 显式返回 ApiResponse，成功 null 必须有 data:null；错误由统一异常转换，禁止每个 Controller catch 后返回 200。只对明确的公开业务异常传入中文信息，内部错误使用固定提示。

DTO 上使用 `@Valid` 与约束注解；嵌套对象/集合显式 `@Valid`。HTTP 方法参数约束使用 MVC 内建验证，Controller 不加类级 `@Validated`。约束消息只能是源码内静态中文，不用 validatedValue 插值；不要把密码、Token 或其他原值加入 FieldErrorDetail。对象约束的总提示不造字段路径。

列表 Controller 使用 PageQuery 原始标量校验、接口静态 SortWhitelist、PageResponse 保真计数；P03-02 已使用 JpaPageAdapter 接入 Spring Data，沿用白名单/唯一补充/NULLS LAST 与字符串 total，具体用法由分页文档拥有。

当前请求追踪只支持同步 REQUEST/ERROR，异步不得假设 ThreadLocal/MDC 自动继承。统一错误日志不记录原始异常内容；工具/回调专用处理和流写出后的错误归其所属适配器。校验失败不含 rejectedValue。执行记录与测试边界见 [P03-01](../testing/P03-01-VERIFICATION.md)；MockMvc 是真实 MVC 协议处理链测试，随机端口是本机 HTTP 技术验证，二者均不是业务/认证/数据库验收。

## 当前持久化实现（P03-02）

[持久化](PERSISTENCE.md) 拥有 BaseEntity、JPA 审计、Clock、应用服务事务与分页适配约定；[迁移](DATABASE-MIGRATION.md) 拥有正式/测试 SQL 和执行规则。只接入 PostgreSQL/JPA/Flyway/Actuator，不提供正式 Entity、业务 CRUD 或通用 BaseService/Repository。prod 不用本地凭据回退，迁移/运行角色实际分离仍需 P04 验证；当前没有租户隔离。旧协议测试已经连接独立技术 PostgreSQL，未关闭数据库自动配置来维持回归。

## P03-03 标量、OpenAPI与结构门禁

实际规则唯一见 [DATA-TYPES](../contracts/DATA-TYPES.md)，生成/文档策略唯一见 [OPENAPI-GENERATION](../contracts/OPENAPI-GENERATION.md)。金额BigDecimal只标注CnyAmount，请求input=true，输出默认固定两位；非金额BigDecimal不标注。无界Long计数/version用DecimalCounter，不把所有整数都字符串化。HTTP规则不作用于JPA类型；不接收Entity作为输入。输出无效值视为内部错误，不静默舍入或截断。

Controller声明具体ApiResponse<T>，schema解析保留实际Success<T>与可空data；错误模型用实际Failure。DTO区分输入输出、可空/可省略，集合不返回null；页面模型total始终string。不要用ApiResponse<Object>或裸泛型逃避类型契约。生产不创建为了导出模型的示例Controller，显式注册真实公共类型；测试Controller只在src/test导入。

`./mvnw clean verify`执行字节码门禁与JAR测试资源隔离，覆盖/限制见 [模块边界](../architecture/MODULE-BOUNDARIES.md)。本轮结构检查不替代租户范围、SQL注册和真实数据库越权测试。测试环境配置仅在src/test/resources；测试profile不是生产JAR支持的部署环境。完整证据见 [P03-03](../testing/P03-03-VERIFICATION.md)。

## P04-01 上下文用法与责任（2026-10-08）

内部安全类型不作为Controller输入DTO，也不直接公开到OpenAPI。当前默认Provider返回Optional.empty，未经可信认证不能建立TenantContext；不要从Header/Query/Body传入tenantId、角色或授权集合，不要注入开发默认租户。P05适配器负责域/载体/会话/权威版本验证与公共端点返回无身份。

应用用例在可信身份根边界内使用try-with-resources选择本permissionCode，再检查TenantScopeGuard.requirePermission；需要目标tenant一致性时requireTenant。StoreScopeGuard通过内部事实查询校验门店，并可openStore绑定明确当前门店。底层set/clear不可公开；普通业务不能new身份并安装上下文，不能借用list范围执行update。嵌套只保持/收窄同权限，关闭必须在创建线程按LIFO；异常或内层遗漏由入口最终清理并拒绝。后台同步调用只能注入TrustedTenantExecutor，从可信Provider执行明确权限，无任意tenantId/runAs入口。

日志上下文只恢复本次tenantId/operatorId/storeId，保留traceId；不能全局MDC.clear或记录完整Principal/授权门店集合。授权集合不等于当前门店。ASYNC、线程池、Redis、消息和系统任务不自动传播身份，公共异步功能不全局禁用。当前只有同步REQUEST/ERROR及同步Executor技术边界；完整持久化范围/JPA/RLS/连接池隔离待P04-02，Redis/异步待P04-03。

本轮MVC上下文测试无需PostgreSQL；完整verify仍执行原真实PostgreSQL迁移/JPA/事务测试。线程复用用同一个单线程Executor逐次A/B/匿名，返回并核对实际线程ID，每次先断言上下文/MDC已清理，不在请求后先clear掩盖问题；测试身份只在测试包。[验证](../testing/P04-01-VERIFICATION.md)区分技术场景与未来业务/生产验收。

P04-01异步边界加固：REQUEST启动Servlet异步后移除同步ERROR使用的服务端身份快照；后续ASYNC/异步ERROR不自动重绑身份，公共处理链继续运行。此反例仅验证拒绝隐式传播，不代表已实现或验证完整Servlet异步协议。
