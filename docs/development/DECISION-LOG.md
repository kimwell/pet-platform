# 工程决策记录

日期：2026-10-07。决定来自本次明确需求及官方/实际验证，不继承其他项目的版本或状态。这里只记录工程决定，不创建审批工作流。D01～D18为P01-01历史决策；本轮D19起记录P01-02详细冻结，当前阶段状态以验证报告为准。

| 编号 | 已确定决定 | 理由与证据 | 适用限制/后续事项 |
| --- | --- | --- | --- |
| D01 | 当前根目录 `/Users/kimwell/work/pet-platform` 是项目位置 | 用户最新指令替代先前同级目录选择；重新检查无文件冲突 | 所有工程/文档直接位于当前根，不创建 enterprise-app-scaffold 子目录，不整体移动既有成果；只新增本轮文档 |
| D02 | Java 21 LTS / Temurin | 长期维护、生态兼容；官方支持页与实际临时工具链 | 安全补丁复核，不将本机旧 JDK 当目标发行版 |
| D03 | Spring Boot 4.0 + Sa-Token Boot 4 + springdoc 3.0 | 官方匹配关系；Maven 和运行探针 | 使用精确矩阵，不选择不匹配的 Boot 3 starter 或 springdoc 2 |
| D04 | 一个 Maven Module 的模块化单体，Maven Wrapper only-script | 固定需求；官方 Wrapper；实际分发下载与构建 | 系统 Maven 仅用于本次临时引导；后续开发/CI 使用 Wrapper |
| D05 | PostgreSQL / Flyway PostgreSQL 模块 / Boot BOM | 官方数据库支持方式和真实依赖树 | DB 运行和迁移在 P03 真正验证，不用 H2 替代 |
| D06 | WxJava 原始 miniapp/pay 模块可选；Hutool 仅 core | 避免 SDK starter 重新引入框架版本及工具范围扩张 | 依赖并存不代表微信 API/支付真实可用 |
| D07 | Node.js 24 LTS、pnpm 10、Vite 7.3/React 插件 5 | Node 维护期、Vite 支持线和 npm engine/peer；隔离工具链 | 不改变全局 Node/pnpm；Vite/插件按组合升级 |
| D08 | React/DOM 同版本、Ant Design 6 | 官方 React 19 支持条件，不额外加入 v5 补丁 | 支持现代浏览器，浏览器实测在后续任务 |
| D09 | TypeScript 5.9、ESLint 10、Vitest 4 | TS 7 不满足 openapi-typescript/静态工具 peer；ESLint 9 已 EOL | 所有直接工具依赖精确；不加入无需求的大量工具 |
| D10 | pnpm 单 workspace/单锁文件、原生小程序 | 固定需求；离线 npm 打包验证布局 | 不引入第二包管理器；DevTools/真机仍需独立验证 |
| D11 | OpenAPI 为事实源，生成纯类型；请求实现分端 | 固定协议方向和实际生成探针 | 类型生成不替代运行时校验/权限验证 |
| D12 | 服务端会话、三类身份空间、安全 Cookie + 独立 CSRF | Sa-Token 官方多账号/配置可行性 | 有效期、轮换、CSRF 细节在 P01-02，真实防护在 P05 |
| D13 | 可信 tenantId，默认拒绝，单/多租户共用隔离 | 固定需求，涵盖数据和非 DB 资源 | P01-02 详细冻结，P04 起真实越权读写验收 |
| D14 | Redis 8.2 Extended，RabbitMQ 4.3 可选 | 官方支持策略及发行信息 | RabbitMQ 2026-11-30 前后复核；Redis 分发许可单独检查 |
| D16 | Web/小程序 strict 源码检查 + skipLibCheck | Vite 官方模板；真实库声明检查分别 TS2430/TS2344，日志保留 | 不保证第三方声明全面检查；公开 API 的应用使用仍严格检查 |
| D17 | miniprogram-ci 作为隔离 pnpm dlx 工具，不加入主 workspace | 2.1.31/2.1.48 内部 parser 仅支持 ESLint7/8；主工程使用10；离线 API 打包及实际文件检查通过 | 隔离工具内部 ESLint8.57.1已EOL；不作为项目静态标准，不产生第二份仓库锁文件 |
| D15 | P01-01当时只覆盖技术基线，P01-02需明确启动 | 历史用户范围；当前已明确授权P01-02 | 不自动P02、提交/推送/部署，不使用Product Delivery OS |
| D18 | 当前基础包 `com.pet.platform`、源码 `apps/backend/src/main/java/com/pet/platform/`、启动类 `com.pet.platform.Application` | 用户最新明确约束；结构与根规则同步更新 | 所有模块位于此包下；只记录规划，不提前创建启动类/正式三端工程 |

## 位置修订记录

先前同级目录选择及 `com.company.scaffold` 占位包名已被本次用户指令替代，不再是当前规范。旧目录文件保留为历史成果；当前有效事实源为本项目根目录中的文档。原始探针命令、日志和归档保留原工作目录及技术测试包，不能据此声称当前启动类已建立或运行。

## P01-02 已关闭的细化决策

| 编号 | 冻结结论 | 理由 / 唯一权威定义 | 验证边界 |
| --- | --- | --- | --- |
| D19 | 模块数据owner、无环依赖、应用接口/同库事务 | [模块边界](../architecture/MODULE-BOUNDARIES.md) | DOCUMENTED；结构与真实事务测试P03/P04 |
| D20 | 三StpLogic、显式载体适配、设备独立、绝对/闲置期限 | [认证](../architecture/AUTHENTICATION.md)、[身份](../contracts/IDENTITY.md) | 精确源码/API编译和内存技术探针；Redis/HTTP未验证 |
| D21 | 同源Cookie、服务器CSRF预会话/设备绑定、来源校验 | [认证](../architecture/AUTHENTICATION.md) | DOCUMENTED；浏览器/代理P05 |
| D22 | 权限代码声明、每权限范围并集、权威安全/授权版本 | [授权](../architecture/AUTHORIZATION.md) | DOCUMENTED；撤销/并发P05/P07 |
| D23 | ScopedPersistence + PostgreSQL RLS、运行/迁移角色分离 | [隔离](../architecture/MULTI-TENANCY.md) | DOCUMENTED；真实JPA/PG防绕过P04，不靠单Filter |
| D24 | 统一HTTP错误/fieldErrors/trace/PATCH/version/批量 | [API](../contracts/API.md) | DOCUMENTED；JSON/业务P03/P07 |
| D25 | UUID字符串、大整数string、金额精度/舍入、UTC与业务时区 | [数据类型](../contracts/DATA-TYPES.md)、[分页](../contracts/PAGINATION.md) | 生成类型技术编译；DB/时区业务未执行 |
| D26 | 代码路由，无文件插件；Query/URL/Form/Zustand各自owner | [结构](../architecture/PROJECT-STRUCTURE.md)、[Web状态](../conventions/WEB-STATE.md) | DOCUMENTED；浏览器P06/P07 |
| D27 | 小程序双会话槽位、员工复用admin、合法返回/竞态 | [小程序](../conventions/MINIPROGRAM-PAGES.md) | DOCUMENTED；DevTools/真机P09 |
| D28 | 授权下载临时图片、video短票据，不假设组件header | [小程序](../conventions/MINIPROGRAM-PAGES.md)、[存储](../architecture/FILE-STORAGE.md) | 官方HTTP资料已读；设备/Range未运行 |
| D29 | 附件一个资源绑定、文件/DB补偿、联合备份 | [存储](../architecture/FILE-STORAGE.md)、[附件](../contracts/ATTACHMENTS.md) | DOCUMENTED；P08故障及恢复 |
| D30 | 初始化vs运行开关、唯一Flyway编号/安装历史保留 | [配置](../architecture/CONFIGURATION.md) | DOCUMENTED；P02/P10迁移组合 |
| D31 | 至少一次JSON、Outbox租约/fence/确认、任务重新授权 | [异步](../architecture/ASYNC-EVENTS.md)、[任务](../contracts/ASYNC-TASKS.md) | DOCUMENTED；真RabbitMQ/PG P10 |
| D32 | OpenAPI实际导出→稳定化→类型→mini复制→漂移CI | [生成](../contracts/OPENAPI-GENERATION.md) | 临时生成/strict及故意漂移拒绝已执行；正式CI P03 |
| D33 | 微信精确基础库/DevTools目标已选，未安装 | [唯一版本矩阵](VERSION-MATRIX.md) | 官方发布/元数据RESOLVED；运行NOT_VERIFIED |
| D34 | JDK PBKDF2算法/参数/Unicode与密码策略 | [身份](../contracts/IDENTITY.md) | JDK技术探针已运行；生产性能/限流P05 |
| D35 | 本轮仅设计冻结，P02-01为下一合法任务 | [P01-02报告](../testing/P01-02-VERIFICATION.md) | 设计门禁与实现/运行验收分别记录 |

本轮无需要用户再次启动或提供真实秘密的设计阻塞。尚未执行的实施验证有明确owner/阶段：[验收矩阵](../testing/ACCEPTANCE-MATRIX.md)。不将其写成已实现能力，不以“以后考虑”代替核心协议；P02如出现具体兼容失败，记录证据、做最小调整并更新受影响规范。

## P02-01 实施决策（2026-10-07）

以下依据用户本轮明确授权，替代前轮“本轮只设计”的任务范围，不改 P01 历史报告。

| 编号 | 决策与原因 | 依据 / 结果边界 |
| --- | --- | --- |
| D36 | 当前 main 尚无提交、已有文件未提交；直接在根建设并保全 ui/历史证据 | 当前 inventory 与 Git 记录见 [P02-01](../testing/P02-01-VERIFICATION.md)，旧“非仓库”事实保持历史原样 |
| D37 | 后端只加入 Boot WebMVC 和测试 starter；不预装 JPA/Flyway/认证/Redis/AMQP/WxJava | 既定阶段 P03/P05/P10；无自动配置排除、假业务表或假健康端点；BOM 管理测试/编译版本 |
| D38 | local/test/prod 配置只处理环境与公开 origin；默认无 profile 不能静默开发回退 | EnvironmentSettings 启动校验与真实 HTTP/JUnit；prod 强制 HTTPS，安全机制尚未实现 |
| D39 | pnpm 根锁、精确 engines/.nvmrc、strict peer；只批准 esbuild 安装脚本 | [pnpm 10 官方设置](https://pnpm.io/10.x/settings#onlybuiltdependencies)，真实冻结安装；本机旧 pnpm 的嵌套调用通过任务专用 PATH 修正，不改全局 |
| D40 | 原生小程序手动 npm 路径与 TypeScript 插件；AppID 留空、私有文件覆盖 | [微信项目配置](https://developers.weixin.qq.com/miniprogram/dev/devtools/projectconfig.html)、[npm](https://developers.weixin.qq.com/miniprogram/dev/devtools/npm.html)、[TS](https://developers.weixin.qq.com/miniprogram/dev/devtools/compilets.html)；离线 packNpmManually 与工具编译分别记录 |
| D41 | infra 固定标签+多架构 digest、认证、回环端口及 named volumes；验证使用独立项目/公开技术夹具 | P01 镜像证据与本轮 Compose/认证基础连接；不删除既有资源、不把本地管理员当应用受限数据库角色 |
| D42 | 最小 CI 官方 Actions 用完整 SHA；P01 缺少的构建工具记录在唯一矩阵 | [GitHub 官方固定 SHA 依据](https://docs.github.com/en/actions/reference/security/secure-use)，实际官方 git ls-remote；首次 API403保留说明，远程 CI 未执行 |
| D43 | api-contracts 仅建立包标识与说明，无空 DTO/生成假文件/无效 check | 正式导出和生成 P03；三端没有依赖不存在的产物 |
| D44 | P02-01 必选门禁与微信/多OS/远程 CI 条件项分别验收，P02整体保持 IN_PROGRESS | [当前报告](../testing/P02-01-VERIFICATION.md)；下一 P02-02 明确为工程基础验收，本轮不执行 |

## 研究中排除的组合

不采用 Vite 7 配 React 插件 6（插件 6 的 peer 要求 Vite 8）；不采用 TypeScript 7 配当前 OpenAPI 类型生成器（peer 为 TypeScript 5）；不采用已停止维护的 ESLint 9；不使用 React 19 + Ant Design 5 的补丁路径作为新模板默认。

## P02-02 工程基础验收（2026-10-08）

| 编号 | 决策与依据 | 验证边界 |
| --- | --- | --- |
| D45 | 用户明确授权 P02-02；复用 P02-01 有效证据、保全历史失败及 ui，未升级依赖 | [P02-02](../testing/P02-02-VERIFICATION.md)，P02整体不由单任务静态检查推导完成 |
| D46 | RabbitMQ 显式 stop_grace_period=30s；原容器实际 StopTimeout=1，事件 SIGTERM→约1秒SIGKILL→137，非 OOM | 修复后节点退出0、无SIGKILL/OOM，原1秒设置的历史来源无法追溯；不增加资源要求 |
| D47 | 根 infra 脚本保留 Docker/Compose 检查、真实失败传播；stop 核对节点状态/退出码；端口格式错误通过 Compose 探针定位变量名 | 未忽略非零码，不执行 docker kill、down --volumes；CI 使用同一 check:infra |
| D48 | DEV_BACKEND_ORIGIN 仅开发服务器读取，拒绝空值/非法来源/凭据；preview 独立回环4173且不继承开发代理 | 配置反例、14项Web测试与真实浏览器；生产仍须静态服务器fallback及实际API反向代理 |
| D49 | 移除无当前用途的 Ant Design App 消息/弹窗上下文，保留官方 ConfigProvider/Query provider | JS 689.22→621.32 kB，gzip 228.03→206.36 kB；保持500kB警告阈值，不新增拆包依赖 |
| D50 | 无profile及prod缺公开origin时用空输入交给既有中文校验；限制公开origin端口范围，并拒绝local/test/prod混用及profile/environment不一致 | 空输入不构成可用默认值；未提供生产秘密、安全或业务能力；真实失败反例通过 |
| D51 | 固定本地 RabbitMQ hostname=rabbitmq，避免重建后节点数据目录随容器ID改变 | 两次容器ID、同一节点名/同一named volume；旧验证节点目录保留，未迁移、删除或证明消息恢复 |
| D52 | 目标微信工具npm构建/真实编译未执行，P02-02 BLOCKED；完整真机业务验收仍归P09 | 沿用路线的三端可独立构建与目标工具验收条件，不以离线组件输出临时降低完成规则，不自动P03 |

## P03-01 公共协议实现（2026-10-08）

| 编号 | 决策与原因 | 证据与边界 |
| --- | --- | --- |
| D53 | 当前明确 P03-01 授权替代 AGENTS 中 P02-01 旧任务限制；直接在根目录执行，保全 ui/其他端/历史证据 | P02 最新实际报告已 COMPLETE；[P03-01](../testing/P03-01-VERIFICATION.md)，不推进持久化/认证/前端 |
| D54 | Controller 显式 ApiResponse 工厂；成功/失败封闭分支，不使用全局自动包装 | P01 未冻结自动包装机制；工厂取当前 trace，文件/ResponseEntity 原格式保留；尚未接入的工具/回调须专用适配 |
| D55 | 增加 BOM 管理 validation starter，严格输入 JSON；继承框架异常基类保留状态/Allow 等头 | [版本矩阵](VERSION-MATRIX.md)，实测只读异常头必须复制；Jackson 文本转换需显式 coercion，枚举特性位于 EnumFeature；原失败日志保留 |
| D56 | 字段路径/同字段多错/去重/稳定顺序及静态中文模板安全规则按 API 落地；对象级错只用总提示 | [API](../contracts/API.md)，DTO与MVC方法校验422、语法400、返回值/未知错误500；不把所有 IllegalArgumentException 当输入错误 |
| D57 | 最小 trace Filter 支持 REQUEST/同步 ERROR，请求属性复用；新增框架 /error 安全兜底 | 真 Tomcat sendError 分发通过；只恢复本次设置的上下文/MDC键；异步/消息/跨服务未实现 |
| D58 | 原始分页参数验证、独立 sortBy/sortOrder、固定白名单、同向唯一补充；total 保真字符串 | [分页](../contracts/PAGINATION.md)；持久化适配/NULLS LAST/真实 count 归下一任务，不预装JPA或造伪适配 |
| D59 | P03-01 由协议运行、完整Wrapper构建、生产包/启动与文档门禁共同完成；P03整体 IN_PROGRESS | [G01～G15](../testing/P03-01-VERIFICATION.md)，下一合法P03-02持久化基础只报告、不执行 |

## P03-02 持久化基础（2026-10-08）

| 编号 | 决策与依据 | 验证边界 |
| --- | --- | --- |
| D60 | 当前 P03-02 明确授权替代 AGENTS 的旧 P02-01 限制；保留未提交成果、ui 和历史证据 | 不创建正式业务表、不推进 P04/认证/下一任务；[报告](../testing/P03-02-VERIFICATION.md) |
| D61 | Boot BOM 管理 JPA/Flyway PostgreSQL/JDBC/Actuator 与 Testcontainers；使用 Boot 4 Flyway starter 接入模块自动配置 | 精确解析与冻结值一致，无升级/覆盖/任意排除 |
| D62 | 必填数据源无回退，启动前只按名称报错；安全结构/迁移策略禁止危险覆盖 | 缺配置/错误凭据/不可用数据库独立进程；不输出密码；生产凭据/权限未验收 |
| D63 | local/test Flyway 先迁移再 validate；prod 独立迁移任务、运行进程只 validate | 沿用冻结生产方案；生产角色分离/P04未完成；本轮正式目录仅说明 |
| D64 | JDK UUID v4 + Instant 毫秒 + 可替换 Clock 的 JPA Auditing，基类不加入租户/@Version/软删除/假操作者 | PostgreSQL 往返、实际 dirty update、相同值不更新时间；[持久化](../conventions/PERSISTENCE.md) |
| D65 | 分页只接受原白名单，page-1、JPA int offset边界、Sort NULLS LAST、Page total long→string | 冻结 JPA 4 已支持 Jakarta Persistence 3.2 空值次序，当前类文件和真实查询确认；旧版本 issue 不作为当前事实 |
| D66 | 真实 PostgreSQL 技术夹具隔离生产扫描与产物；应用服务代理事务，测试不自动回滚 | 提交、flush后异常/多笔/约束回滚、Flyway副本校验和冲突；不提供生产演示接口 |
| D67 | 仅 health，readiness包含db，liveness仅进程状态，隐藏详细配置，不添加未接入服务健康 | 生产JAR+独立容器故障探针；非远程CI或生产部署验收 |
| D68 | P03-02完成后 P03仍IN_PROGRESS；剩余标量/OpenAPI生成/结构防绕过按后续授权实施 | 不自动下一任务、提交、推送或部署 |

## P03-03 标量与类型契约（2026-10-08）

| 编号 | 决策/原因 | 证据与边界 |
| --- | --- | --- |
| D69 | 当前P03-03授权替代AGENTS旧阶段范围；直接根目录保全ui/未提交成果 | [报告](../testing/P03-03-VERIFICATION.md)，未开发业务/认证/租户、未自动下一任务 |
| D70 | 金额与Long计数使用公共语义注解；Instant/LocalDate/UUID按类型统一HTTP编解码 | 不全局改变BigDecimal/Long，不影响JPA；输入金额省略尾零、时间0～3小数等最小细节见DATA-TYPES，原信封/total不变 |
| D71 | 实际Success/Failure开放为schema类型、构造器仍私有；解析ApiResponse<T>保留泛型和null联合 | 不手写竞争DTO，不把失败描述成data:null；schema/HTTP逐字段断言 |
| D72 | 生产公共模型显式注册，无生产假接口；两份独立测试上下文分别导出生产/测试schema | 冻结springdoc全局过滤先于分组；test-contract只用于生成器验收、公共包不导出测试路径 |
| D73 | local文档/UI回环启用，test导出只开启文档，prod关闭且拒绝开启覆盖 | 生产JAR真实HTTP通过；标题代码定义、版本POM过滤；不提供假安全方案 |
| D74 | generate/check临时导出+规范化+类型，check只比较、不写仓库；小程序固定目录SHA同步 | 未提交main也可执行；真实total变number反例exit1；不是完整破坏性分析 |
| D75 | 复用已有完整ASM测试依赖做字节码/泛型检查，不新增架构库 | shared/domain/跨模块/Repository/测试产物等反例；SQL/租户/反射运行时不在其证明范围 |
| D76 | application-test.yml移动至测试资源，package后核对所有编译测试class和资源 | 生产无测试配置、Controller、Entity、migration、Testcontainers；不改变local/prod部署配置来源 |
| D77 | 核对P03三项及原阶段条件后关闭P03；生产权限/独立迁移运行仍归A04/部署，远程CI/多OS保持未验证限制 | [路线图](ROADMAP.md)、[门禁报告](../testing/P03-03-VERIFICATION.md)，未临时降低必选门禁，下一合法P04不自动执行 |

## P04-01 可信上下文与执行边界（2026-10-08）

| 编号 | 决策/依据 | 验证与后续责任 |
| --- | --- | --- |
| D78 | 用户P04-01明确授权替代AGENTS旧任务边界；沿用冻结版本；补三项P04拆分不改原阶段条件 | 当前Git已有60481ce提交、初始工作区干净，纠正历史未提交描述；不提交/推送/部署，不修改无关UI |
| D79 | CurrentPrincipal为内部最小可信认证授权事实，Provider默认empty；平台无租户、员工/客户必须租户、客户只SELF | P05真实Sa-Token/域/版本适配尚未执行；没有默认租户/测试身份兜底 |
| D80 | 根身份AUTHORITY_READ与具体权限BUSINESS分开；只从已绑定范围选择/收窄，内部安装不公开 | 同主体/租户/权限嵌套保持或收窄，LIFO/同线程关闭，边界异常/泄漏最终清理；数据库目的白名单P04-02 |
| D81 | 沿用STORES和已冻结的按permission并集，而非额外STORE_SET；ScopeGrant合并显式验证同权限/主体，STORES与身份上限相交 | 空集合无门店；SELF业务映射待P04-02/业务owner；不使用createdBy通用映射 |
| D82 | StoreOwnershipReader为shared内部事实端口，StoreScopeGuard检查真实tenant与范围/门店上限；默认缺事实503 | 后续platform/store实现；不存在/跨租户/未授权统一404，TENANT不是平台权限 |
| D83 | TraceFilter先行，可信认证适配器在租户Filter之前；REQUEST/同步ERROR复用服务器身份，不采信HTTP tenant；异常复用MVC解析协议 | ASYNC不注册/不继承，公共流仍可继续；完整异步传播P04-03；已提交流不改写协议 |
| D84 | MDC只管理tenant/operator/明确当前store并恢复；同步Executor只从可信Provider取身份，不接任意tenantId | SYSTEM授权登记/重验/审计后续接入，无系统超权运行入口 |
| D85 | 复用P03 ASM扩展存储/入口符号与模型边界反例，生产JAR全测试class/resource隔离继续执行 | 结构不证明运行时或数据库越权安全；完整verify保留原PG测试，内部类型无理由改三端产物 |

## P04-02 受控JPA与PostgreSQL越权（2026-10-08）

| 编号 | 决策/理由 | 验证与边界 |
| --- | --- | --- |
| D86 | 用户明确授权P04-02替代AGENTS旧任务边界，沿用冻结版本/固定包；初始Git干净并保留ui/三端/CI/历史证据 | [验证](../testing/P04-02-VERIFICATION.md)，不提交/推送/部署或推进P04-03 |
| D87 | 模块ScopedPersistence适配器固定权限，protected final查询/写入，强制tenant AND scope AND business；不引入通用DSL或Spring Data全量CRUD | 不接受替代安全Specification或任意detached merge；真实OR/count/exists/单条范围测试 |
| D88 | 资源显式声明tenantOnly/stores/owned/storesAndOwned，未知组合拒绝，SELF映射principalType+ownerID | 空STORES无行；TENANT门店受身份上限；不能用createdBy或没有store就全量 |
| D89 | 归属MappedSuperclass + 创建上下文注入 + 回调/受控更新显式校验 + 事务完整范围固定 | 不把updatable=false当bulk安全；JPA getter不final以兼容代理，警告证据保留 |
| D90 | 沿用P01受控入口+RLS，在真实JPA事务同连接参数化set_config(local)，同步挂起恢复支持REQUIRES_NEW；禁OSIV/二级/查询缓存 | 真PG连接/GUC/提交回滚/旧范围实体反例；不是异步或任意直接DB防护 |
| D91 | 内部原子集合固定Criteria DML，1～100提交ID去重、全目标锁定/范围校验、实际影响数、rollbackOnly、version/audit及clear | 公开API逐项成功契约保持原样且未实现；混合不可见、缺失、触发器部分影响均数据库无部分写入 |
| D92 | 应用受控关联+tenant复合唯一/FK+RESTRICT；测试模型采用引用ID，再经父资源受控query投影 | 初始复合ManyToOne的LAZY实测提前解析，改为显式ID避免隐式导航授权；失败报告保留，无级联 |
| D93 | 测试表/门店事实/角色/适配器/故意绕过和migration全src/test，独立PG容器运行角色与owner分离，五表FORCE RLS | 实际生产角色权限、正式模块SQL/认证/Store仍未验证，不将容器权限当生产验收 |
| D94 | ASM增加低层持久化、裸Repository、infra native/JPQL/merge及bootstrap方法引用反例，保留跨模块和产物隔离 | 不能证明反射、恶意代码、未来自定义SQL安全；新增模型与查询需范围测试 |

## P04-03 Redis、异步及阶段验收（2026-10-08）

| 编号 | 决策/依据 | 验证与后续责任 |
| --- | --- | --- |
| D95 | 当前明确授权替代AGENTS旧任务限制；根目录/固定包/冻结版本/旧成果保全，不用Product Delivery OS | [P04-03](../testing/P04-03-VERIFICATION.md)，不提交/推送/部署或自动P05 |
| D96 | 增BOM管理Redis starter；环境+版本+tenant/store/platform原始资源空间；严格模块/前缀，UTF-8 Base64 URL业务标识无替换碰撞 | 之前未冻结字节格式，本轮细化不改变认证空间；无hash tag/Cluster多Key能力 |
| D97 | 不可公开构造的RedisKey绑定签发builder及完整当前范围，操作复核；store再查事实，platform独立固定权限 | 真实A/B/不同门店/无上下文/事实缺失/跨范围/不同builder拒绝 |
| D98 | raw资源命中再验数据权限，禁止权限过滤结果/通用Spring Cache；不伪造权限版本或范围指纹解决撤销 | [REDIS](../conventions/REDIS.md)，静态规则不理解任意字符串内容；未来owner负责策略 |
| D99 | GET/带1ms～24h TTL的SET/DEL，64KiB严格UTF-8 v1格式，无Java反序列化/任意Lua/KEYS/FLUSHDB | 真实CRUD/有界TTL/损坏值/连接故障；失败503不静默miss，无业务降级 |
| D100 | 显式有限TenantTaskExecutor，从当前BUSINESS捕获不可伪造最小快照；taskId替代sessionId，只当前permission/range | 有限资源/AbortPolicy，不CallerRuns、不继承第三方InheritableThreadLocal，不全局包装线程池 |
| D101 | 默认30s且硬上限60s单调期限，排队及运行Guard/提交/完成校验，嵌套不续期 | 无权威撤销重验，不支持长期/持久化/跨进程；P05/P10必须接当前状态/交集 |
| D102 | 活动事务/同步期提交拒绝，代理用例完整返回后提交；工作端新事务沿用唯一ScopedTransaction/RLS | 真PG独立tx、单连接复用、跨行拒绝、flush/超龄提交回滚；不是afterCommit可靠消息 |
| D103 | 实际线程finally清理身份/允许MDC/trace；残留拒绝并废弃线程；Future+固定脱敏日志观察异常 | 排队取消释放容量、运行取消不中断时仍运行、停止有限等待/协作请求，不能从其他线程清身份 |
| D104 | 复用ASM补裸Redis/Cache/未登记Executor/内部snapshot/Key伪造/无身份入口与方法句柄反例 | 只扫描项目class；闭包/反射/动态内容/恶意凭据代码不在静态证明范围，产物继续隔离测试 |
| D105 | 实际受限运行角色与迁移owner关系重新核对；三项P04和原完成条件综合关闭 | [P04总验收](../testing/P04-ACCEPTANCE.md)；生产角色/认证/正式Store/MQ/未来业务限制保留 |
| D106 | 下一P05-01先正式身份数据基础/安全初始化/权威状态与Store/认证查找依赖 | 禁止以测试身份完成登录验收；P05仍NOT_STARTED，本轮只报告 |
