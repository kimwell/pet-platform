# 配置、能力选择与迁移

冻结日期：2026-10-07，P01-02。本文拥有部署配置、初始化能力和迁移规则。P02-01 已创建工程环境文件与本地 Compose；P03-02 已接入 PostgreSQL/JPA/Flyway 和仅含说明的生产迁移目录；业务能力与安装清单仍为规划。版本见 [版本矩阵](../development/VERSION-MATRIX.md)，异步运行见 [异步事件](ASYNC-EVENTS.md)。

## 配置边界

生产产物profile为local/prod，test配置只位于src/test/resources供测试源码使用；local 不隐式提供真实账号/密码/密钥。公开配置与秘密分离，环境变量只作为输入，不打印全量 env。配置启动时校验，失败中文说明缺少的配置**名称**，不输出值。前端仅公开 API 相对根路径、能力展示等，微信 AppSecret/支付密钥从不进入客户端。

| 拟定配置 | 默认/约束 | owner / 实施 |
| --- | --- | --- |
| pet.environment / public-origin | environment 必填；生产 HTTPS 精确 origin | shared；P02/P05 |
| pet.auth.cookie-secure / cors-enabled | prod 强制 true；cors 默认 false，细则见认证文档 | shared.security；P05 |
| pet.tenancy.mode | SINGLE/MULTI 只影响入口，不关闭隔离 | shared.tenancy/platform；P04 |
| spring.jpa.open-in-view / hibernate.ddl-auto | false / validate；无自动 create/update | shared.persistence；P03 |
| 运行/迁移 DataSource | 同一主库、不同受限角色/凭据；prod 启动不自动用 owner 迁移 | shared.persistence；P03/P04 |
| pet.storage.root | 必填私有目录、非静态站点根，不使用源码目录 | attachment；P08 |
| pet.capabilities.rabbitmq.enabled | 默认 false，不创建连接/监听器/健康依赖 | shared.messaging；P10 |
| pet.capabilities.wechat-miniapp.enabled / wechat-pay.enabled | 默认 false；开启前校验服务端配置 | shared.wechat；P10 |
| springdoc.api-docs.enabled / swagger-ui.enabled | local/contract-export 可开；prod 默认 false | shared.api；P03 |

Sa-Token 的准确配置/API 定义在 [认证](AUTHENTICATION.md)；本表不复制数值或 Cookie 规则。P02-01 的配置只校验工程环境和公开 origin，profile 文件与输入方法见 [本地开发](../development/LOCAL-DEVELOPMENT.md)。生产模板没有秘密或开发回退，不代表已实现安全配置。Redis 会话依赖在 P05 接入，届时仍是必需能力，不是生产可降级的可选内存存储。P03-02 数据库与P04-03资源Redis自动配置已接入；AMQP/微信未接入，没有排除自动配置掩盖依赖失败。Redis资源端口不提供会话DAO，认证会话仍归P05。

## 初始化选择与运行时启停

初始化清单区分 required 基础（协议、隔离、身份、附件/审计所选实现）、客户端能力 CUSTOMER/STAFF/BOTH、RabbitMQ、微信登录、微信支付。选择会影响依赖、适配器和模板，不能静默裁剪依赖其他能力的接口；生成器验证组合。默认关闭 RabbitMQ/WxJava 真实外部接入，客户页面可显示“当前未启用微信登录”，不得提供假账号兜底。

运行时 enabled 只控制**已安装**能力。未安装能力不能仅改开关开启；需要受控升级、依赖/迁移/验证。关闭返回 503 CAPABILITY_DISABLED，停止新工作、等待或取消既有任务按任务策略；不自动删表、历史数据、绑定文件或消费者幂等记录。能力查询不泄露外部凭据。

## Flyway 组织与升级

规划 `src/main/resources/db/migration/{core,attachment,notification,wechat,messaging}`，由安装清单确定 locations。所有版本迁移用全应用唯一编号 `V<递增整数>__<module>_<中文可读含义的英文标识>.sql`，不得各模块都 V1。已应用版本不可改名/改内容，checksum 校验失败阻止启动；ddl-auto 仅 validate。重复迁移只用于明确登记的视图/函数定义，不绕过表版本管理。

platform 的 installed_module 记录 moduleCode、installedVersion、installedAt、enabled、configurationVersion；Flyway history 是 SQL 执行事实，installed_module 是能力安装事实，两者核对。首次迁移/升级事务更新安装版本，失败不宣告安装完成；需非事务 DDL 时专项说明恢复步骤。

locations 包含所有**已安装**模块，即使运行关闭；不得因关闭从 locations 移走历史迁移导致验证失配。新增模块在唯一编号后追加迁移、更新安装清单；禁止事后插入低编号或开启 out-of-order 来掩盖冲突。禁用/裁剪只对全新生成项目可排除可选模块；既有项目升级保留历史。

P02 本地 infra 固定镜像及卷路径，生产迁移独立执行任务；P03/P10 验证空库、重复启动、旧库升级、关闭保留数据、校验失败、可选组合。P12 恢复测试必须包含 PostgreSQL 与文件目录，不能只恢复数据库。

## P02-02 当前工程输入与验证

实际必填/可选变量、优先级、端口、代理、数据目录和AppID入口统一列在 [LOCAL-DEVELOPMENT](../development/LOCAL-DEVELOPMENT.md#当前实际配置来源)。这不是未来能力配置已实现的声明。Java不加载dotenv；无profile/prod缺值进入既有中文构造校验，空占位不是可用默认值；local/test/prod不能混用，profile与pet.environment不一致拒绝启动。Web开发来源只被Vite dev读取、校验且不入bundle；preview固定回环4173、无开发代理，生产需SPA fallback与未来API反向代理。

本地Compose仍是开发配置；RabbitMQ固定hostname及30秒停止宽限，停止脚本检查节点真实退出。旧节点目录保留、消息迁移未验证；不删除卷作为修复。此为 P02 历史边界。P03-02 已增加必需的应用数据库连接；P04-03增加Redis资源连接，AMQP和本地业务存储仍未实现。证据与限制见 [P02-02](../testing/P02-02-VERIFICATION.md)。

## P03-02 数据库与健康配置（2026-10-08）

当前输入由 [本地开发](../development/LOCAL-DEVELOPMENT.md) 唯一列明。`spring.datasource.url/username/password` 必填，没有可用默认值；URL 只接受不含凭据/查询参数的 PostgreSQL 地址。额外 JDBC 设置通过 `spring.datasource.hikari.data-source-properties` 提供，秘密只经独立配置输入。BeanFactoryPostProcessor 在连接池、迁移及 JPA 创建前校验，失败中文说明配置名，不打印值。

Hikari 保守默认最大10、最小空闲2、连接等待5秒、验证等待2秒、初始化失败等待10秒；驱动 connectTimeout=5秒、socketTimeout=30秒，不宣称已完成生产容量调优。`open-in-view=false`、`generate-ddl=false`、`ddl-auto=validate`、SQL init=never；禁止使用别名覆盖成创建/更新结构。UTC JDBC 与审计毫秒策略见 [持久化](../conventions/PERSISTENCE.md)。

local/test 默认 Flyway 先迁移再 JPA 校验；prod 强制关闭启动迁移，使用独立迁移任务与凭据，不覆盖冻结的生产方案。危险迁移默认值不能由外部配置打开，正式/测试 locations 物理隔离，详见 [迁移](../conventions/DATABASE-MIGRATION.md)。生产角色分离和迁移任务实际运行仍未验证，P04/部署阶段负责。

Actuator 仅暴露 health，details/components 均 never。`/actuator/health/liveness` 只有 livenessState；P03-02当时 `/actuator/health/readiness` 为 readinessState+db；P04-03增加redis贡献，总体与readiness检查真实db/redis。任一依赖不可用时总体/readiness返回503，liveness仍只反映进程。默认其他健康贡献关闭，RabbitMQ未接入。健康返回工具格式 `{"status":"UP"}`，总体端点还可列出 liveness/readiness 分组名称，不包装业务信封；无连接信息。真实启动/故障/停止证据见 [P03-02](../testing/P03-02-VERIFICATION.md)，公开健康策略不代表已有认证或生产上线。

P03-03文档与UI策略已实施，实际输入/过滤/扫描/生产覆盖拒绝和运行证据唯一见 [OpenAPI生成](../contracts/OPENAPI-GENERATION.md)。不在本文重复schema或版本定义。

## P04-03 Redis与有限执行资源

Redis资源访问为当前后端连接能力，host/password必填无回退，prefix/模块段校验，database/port及连接/命令超时有界；只允许单节点配置，repositories自动发现关闭。输入、默认值与支持范围唯一见[本地开发](../development/LOCAL-DEVELOPMENT.md#p04-03-redis与进程内执行器)，内容/编码/TTL/错误规则见[REDIS](../conventions/REDIS.md)。启动前校验只输出配置名；应用构建连接能力，实际依赖可用性由health与操作检查，不把Bean创建当Redis连接成功。

总体health与readiness新增真实redis贡献，故障503，liveness不依赖Redis；隐藏details/components。完整verify使用隔离认证容器，不连接开发Redis，生产JAR local/prod测试均注入独立服务输入。标准配置没有开发host/密码或生产降级。Redis ACL/TLS/网络与容量的生产配置未验收。

TenantTaskExecutor只有显式Bean，线程/队列/快照期限/停止等待有硬边界，没有@EnableAsync、全局第三方池自动包装或CallerRuns。生命周期、事务提交时机与当前授权限制唯一见[ASYNC-EXECUTION](../conventions/ASYNC-EXECUTION.md)。尚无真实会话、MQ、Outbox或调度平台；后续P05/P10不得仅靠修改开关冒充接入完成。

## P05-01 正式身份执行配置

现有数据库输入新增显式独立迁移PET_MIGRATION_DATABASE_*，启用启动迁移时必填且目标URL与运行URL一致；禁止凭据回退。prod仍独立命令迁移、普通Application只validate。初始化另用PET_BOOTSTRAP_DATABASE_*，只独立bootstrap命令读取；初始密码不来自应用环境或参数，普通启动没有自动初始化能力。真实输入和命令见[初始化说明](../development/IDENTITY-BOOTSTRAP.md)、[本地开发](../development/LOCAL-DEVELOPMENT.md)。容器验证不代表正式部署；未创建真实本地管理员。
