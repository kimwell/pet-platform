# 本地开发与检查

当前实施状态见本文最新P05章节及[P05-03验证](../testing/P05-03-VERIFICATION.md)。旧阶段“尚未实现”描述保留为历史范围；冻结安全契约不变。
日期：2026-10-08。精确版本以 [VERSION-MATRIX](VERSION-MATRIX.md) 为唯一文档事实源。历史初始化见 [P02-01](../testing/P02-01-VERIFICATION.md)，本轮实际启停与限制见 [P02-02](../testing/P02-02-VERIFICATION.md)。以下命令从项目根执行，后端命令单独进入 apps/backend。ui 保留原位，不加入工程构建。

## 工具与安装

使用矩阵中固定的 Temurin、Node、pnpm，不改全局工具配置。已有 nvm 可在本项目目录按 .nvmrc 使用；也可把官方固定版本发行包放入任务专用目录，通过本终端 PATH/JAVA_HOME 使用。嵌套脚本调用的 pnpm 也必须来自同一版本。隔离目录被清理后须从已校验分发恢复，不能因旧路径仍存在就推导工具完整。

```sh
node --version
pnpm --version
java --version
pnpm install --frozen-lockfile
pnpm check
pnpm build:web
```

engine-strict 会拒绝不匹配的 Node/pnpm。前端仅根 pnpm 安装及一份根锁，不在应用目录另行 npm install；只允许冻结 esbuild 的安装脚本。根 check 执行仓库/小程序配置、Web typecheck/lint/test、小程序 typecheck/lint；不启动后端/容器，也不执行微信真实编译。Web build 和后端 Wrapper 独立运行。

## 当前实际配置来源

| 配置 | 来源、必填及默认值 | 当前边界 |
| --- | --- | --- |
| SPRING_PROFILES_ACTIVE | Java进程环境或 --spring.profiles.active；生产产物启动显式选local/prod，测试源码使用test | 无默认开发profile；local/test/prod只能启用一个并须与pet.environment一致；不自动读取.env |
| PET_ENVIRONMENT | 没有profile提供环境时必填local/test/prod；对应local/prod profile已设置pet.environment；test只在测试资源 | 空值拒绝启动，不静默local回退 |
| PET_PUBLIC_ORIGIN | Java进程环境/外部Spring配置；local默认http://localhost:5173，test固定公开测试来源；prod/无profile必填 | HTTP/HTTPS精确来源，无凭据/路径/查询/片段；显式端口1～65535；prod必须HTTPS |
| SERVER_ADDRESS / SERVER_PORT | Java进程环境；默认127.0.0.1 / 8080；test配置端口0供随机端口测试 | 当前无认证，禁止把local监听开放当生产部署；非法类型、占用均失败 |
| DEV_BACKEND_ORIGIN | Web .env.local / .env.development.local 等Vite标准文件与进程变量，进程变量优先；未设置时http://127.0.0.1:8080 | 仅dev服务器使用；显式空值或非法origin失败；保留/api路径，不注入客户端，不用于build/preview |
| Web监听与静态base | vite.config.ts：dev127.0.0.1:5173、preview127.0.0.1:4173，strictPort=true；Vite默认base=/ | 端口占用明确失败；当前部署目标为站点根路径，子路径部署须另行修改/验证base与路由 |
| VITE_* | 当前没有使用的VITE配置；所有VITE_*都公开 | 不放Token、密码、微信AppSecret或支付密钥 |
| Compose必填 | infra/local/.env 或 --env-file：POSTGRES_DB、POSTGRES_USER、POSTGRES_PASSWORD、REDIS_PASSWORD、RABBITMQ_DEFAULT_USER、RABBITMQ_DEFAULT_PASS | 示例密码为空，必须由开发者本机填写本地专用值；进程变量优先于env-file；不复用生产值 |
| Compose可选端口 | POSTGRES_PORT=15432、REDIS_PORT=16379、RABBITMQ_PORT=15672、RABBITMQ_MANAGEMENT_PORT=15673 | 四项仅绑定127.0.0.1；格式错误提示具体变量；默认项目名pet-platform-local |
| 本地数据 | Compose named volumes：postgres-data、redis-data、rabbitmq-data，由项目名加前缀 | 在Docker数据目录/VM内，不在源码中；.local-data仅任务临时配置/进程记录且忽略；业务文件存储目录在P08实现 |
| 微信AppID | apps/wechat-miniprogram/project.private.config.json；由私有示例复制并填实际授权AppID | 公共appid为空；私有文件忽略，不填AppSecret；公共根/TS/npm配置已提供 |

P03-02 已接入必需 PostgreSQL 数据源、JPA/Flyway 与 Actuator；P04-03已接入Redis资源客户端；AMQP/认证/文件/微信客户端仍未接入，不能仅修改开关启用。生产不能直接使用本地Compose；prod profile只有工程公开配置校验，未实现Cookie/CSRF、身份或生产部署方案。配置反例使用临时进程变量，不覆盖用户文件、不打印完整解析配置。

## 后端启动、访问与停止

```sh
cd apps/backend
./mvnw clean verify
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Windows用同目录mvnw.cmd。首次Wrapper下载需要网络与JDK，无需全局Maven。矩阵固定分发与SHA；Windows/Linux运行未执行，CI配置不等于跨平台PASS。

另一个终端检查 `curl -i http://127.0.0.1:8080/` 和不存在路径：两者目前都是404。后端没有业务 Controller；P03-02 已提供 Actuator 健康接口。local 现在需要真实 PostgreSQL、Redis及各自配置，`/actuator/health/readiness` 验证数据库与Redis就绪，`/actuator/health/liveness` 验证进程存活；404 只表明该路径不存在。P04-03已接入Redis资源端口；RabbitMQ未接入。

运行终端Ctrl+C正常停止，确认出现优雅停止日志、8080不再监听；只结束自己启动的进程。P02-02用本轮记录的独立进程组SIGTERM验证停止与无遗留进程。也可 `java -jar target/pet-platform-backend-0.0.0-SNAPSHOT.jar --spring.profiles.active=local`。apps/backend/.env.example只描述输入，Java不自动加载它；可通过进程环境或外部Spring配置注入。

无profile/必填公开origin、非法origin、prod使用HTTP、profile混用/环境不一致、server.port非法类型及端口占用均会失败，并给出对应配置名/端口。项目自定义错误使用中文；Spring/Tomcat原生诊断保持官方输出。JUnit真实随机HTTP端口测试与配置反例已执行；Failsafe已在P03接入真实PostgreSQL与HTTP IT，完整结果见各P03报告。

## Web开发、代理、构建和预览

```sh
pnpm dev:web
# 另一个终端执行检查/构建
pnpm check:web
pnpm build:web
pnpm preview:web
```

开发入口 http://127.0.0.1:5173，预览入口 http://127.0.0.1:4173。复制apps/admin-web/.env.example为同目录.env.local可设置开发后端来源，修改后重启Vite。开发/API相对路径为/api，代理保留路径与Origin，不配置任意CORS。当前后端无业务接口；实际/api未知路径透传404只能证明代理连接，不证明业务/认证联调。停止后端时代理网络失败也不是应用自带健康接口。

只有/系统入口和NotFound，/missing/deep属于未知路径。浏览器直接访问、刷新未知深层路径、点击“返回系统入口”已实测。中文ConfigProvider和Query provider保留；当前没有App消息/通知/弹窗调用，所以移除了无用途App上下文，后续按实际需要接入官方能力。

build产物在apps/admin-web/dist，JS/CSS路径为/assets/...。preview独立回环/固定端口、不继承开发/api代理，也不加载开发来源。**Vite preview通过不等于生产部署验收**。生产静态服务器须对前端页面使用SPA fallback：已有资源正常返回，前端路由回退到index.html；/assets缺失资源返回404，未来/api由独立反向代理处理，不回退成HTML。当前默认base=/；部署到子路径需要对应配置和新验证，不能直接宣称已支持。

本轮JS从689.22kB降到621.32kB（gzip206.36kB）；仍超过500kB警告阈值。按实际模块记录确认核心React/Router/Query及官方组件占用，无全量命名空间导入，不通过提高阈值隐藏提示。当前只有一个真实入口，不造业务路由分包；P06/P07有实际页面时按路由dynamic import，性能目标另行验收。

## 原生微信小程序

```sh
pnpm check:miniprogram
```

1. 在本机准备矩阵固定的微信开发者工具Stable，导入 **apps/wechat-miniprogram**。
2. 私有文件不存在时，从project.private.config.json.example创建project.private.config.json；已经存在时只合并修改appid，保留其他私有开发设置。公共appid为空，私有文件被Git忽略。[官方规则](https://developers.weixin.qq.com/miniprogram/dev/devtools/projectconfig.html)支持私有AppID覆盖；不要填编造AppID或AppSecret。本次实际AppID只写入本机私有文件，账号开发权限仍需实际验证。
3. 确认基础库与公共配置一致、miniprogramRoot=miniprogram/、TypeScript插件启用、URL校验开启。依赖只由根pnpm安装。
4. 在微信工具“工具 → 构建 npm”，手动关系为packageJsonPath=./package.json、miniprogramNpmDistDir=./miniprogram/；检查miniprogram/miniprogram_npm/tdesign-miniprogram/button/button.wxml等真实产物。
5. 再执行工具真实编译/TypeScript转换，模拟器打开系统入口，点击“查看工程状态”并确认中文提示；记录工具/基础库版本、步骤、截图与日志。真机需要实际AppID、登录及设备条件，独立记录，不能冒充PASS。

P02-02首轮历史：当时已安装工具低于目标且没有实际AppID，工具npm构建、真实编译、真机均NOT_EXECUTED；原始记录保留。本次续验见下段，不能由新准备工作倒改历史。

2026-10-08准备轮历史：冻结Stable 2.02.2608080已从官方macOS ARM64包独立安装到 `/Users/kimwell/Applications/WechatDevTools-2.02.2608080.app`，旧 `/Applications/wechatwebdevtools.app` 2.01.2510260保留。没有执行修改全局命令链接的安装脚本；实际工具版本通过运行包manifest和界面核对，不能以Electron Info.plist的宿主版本代替。实际AppID已写入忽略的私有配置，公共配置未变。目标工具使用独立资料，目前在登录二维码界面，用户须用有该AppID权限的微信扫码；旧工具登录不代表新工具登录或当前工程授权。

目标CLI help可用，但状态探针exit246报告“服务端口已关闭”；首次GUI启动前的exit255资料目录失败也保留。不反复运行相同必然失败命令，不开启服务端口或关闭安全检查。后续可直接通过工具界面导入当前目录、构建npm、编译和检查模拟器；[官方CLI](https://developers.weixin.qq.com/miniprogram/dev/devtools/cli.html)没有独立compile子命令，open触发刷新也需要界面/日志验证。若以后采用CLI，服务端口入口为“设置 → 安全设置 → 服务端口”，须按实际接口配置核对；不把接口开启或open退出码当源码编译成功。

准备轮当时typecheck、lint、结构检查均exit0；工具npm/源码编译/模拟器/预览/真机仍NOT_EXECUTED，登录和开发权限待完成。原有miniprogram_npm是历史离线输出，不能冒充本次工具构建。前两项继续阻塞P02完成；真机完整业务验证仍归P09。最新命令/版本/状态见[P02-02第12节](../testing/P02-02-VERIFICATION.md)。

2026-10-08登录后最新结果：同一冻结工具已通过真实npm构建（1569毫秒、1177个文件）、源码编译（清除文件缓存后日志 `【idle-compile】 all done`）、模拟器入口和TDesign按钮点击；控制台基础库3.17.2、0 errors，2条内部资源preload警告原样保留。实际AppID和应用名称由工具基本信息核对，开发版预览二维码生成成功，当前账号具备本轮开发/预览权限。用户反馈真机“已打开且按钮正常”；仅此基础范围通过，P09完整验收未执行。P02-02/P02 COMPLETE，P03 NOT_STARTED，见[报告第13节](../testing/P02-02-VERIFICATION.md#13-2026-10-08-登录后续验与p02关闭)。

**导入后的配置核对必需：** 本次工具导入虽然识别私有AppID，仍自动把实际AppID写到公共project.config.json。导入后检查公共文件；保留工具自动写入的观察证据，将公共appid恢复为空并保留既定构建设置，私有文件只保留实际本机绑定且不覆盖其他开发字段。然后“项目 → 重新打开此项目”，核对工具仍使用私有AppID，编译及运行正常。本次已恢复完整原始公共SHA并复验通过。依赖仍只有根pnpm单锁，npm构建输出仍在miniprogram/miniprogram_npm。

服务端口当前仍关闭（CLI状态探针exit246），使用GUI“工具 → 构建 npm / 编译 / 预览”足以完成本阶段。没有独立compile CLI子命令；GUI结果记步骤/截图/日志，退出码不适用。工具预览成功不自动算真机通过；只有本次用户反馈支持有限真机结论。预览面板的编译提示/代码质量建议另行保留，不宣称全量质量通过，也没有临时改变P02门禁。

官方配置/npm/TS资料在P02-01归档。已有独立冻结miniprogram-ci的packNpmManually离线检查已重新运行，输出1个包、1177个实际组件文件、无warning；它不需要AppID/私钥，不加入主workspace，也不替代微信工具编译或真机。P02证据中的pack-api.cjs包含当次隔离工具绝对路径，复现时替换为同版本独立工具位置，不创建第二份仓库锁。共享契约包暂无生成代码；P03才接入正式OpenAPI。

## 基础设施和结束状态

先按 [infra说明](../../infra/local/README.md) 填本机专用.env，再执行pnpm check:infra、pnpm dev:infra、pnpm stop:infra。可用 --env-file 指定临时配置，--project 指定本项目验证名称；禁止操作他项目同名资源或删除卷。

stop核对每个本项目容器的Status/ExitCode/OOMKilled；异常会非零退出，不因为Compose命令0就宣称节点正常。RabbitMQ固定hostname与30秒宽限。旧验证节点目录仍留在卷内；既有真实RabbitMQ迁移需要专项操作，不能改节点名后假定旧消息自动恢复。

本轮开始/结束均无运行容器；保留全部已有卷与历史证据，后端/Web/preview进程全部结束。日常.env仍未创建；本轮使用旧报告明确标注的公开技术夹具，不能直接当日常账号或生产配置。第三方声明全面检查历史FAIL仍保留，源码strict+skipLibCheck基线未变。远程CI、Windows/Linux、生产发布/部署及业务集成都未执行。

## P03-02 数据库输入与执行条件

| 输入 | 默认与约束 | 当前作用 |
| --- | --- | --- |
| PET_DATABASE_URL | 必填，无默认值；`jdbc:postgresql://<host>:<port>/<database>`，无凭据/查询参数 | 对应 spring.datasource.url，不能回退本地库；额外 JDBC 设置使用 hikari.data-source-properties |
| PET_DATABASE_USERNAME / PET_DATABASE_PASSWORD | 必填，无默认值；本机专用值，不提交、不输出 | 对应数据源独立凭据；生产运行角色不得复用 owner，实际角色权限归 P04 |
| PET_DATABASE_POOL_MAX / PET_DATABASE_POOL_MIN_IDLE | 10 / 2 | 保守单体连接池初值，按真实负载再调优 |
| PET_DATABASE_CONNECTION_TIMEOUT_MS | 5000 | 等待连接上限；验证等待2000ms、初始化失败等待10000ms |
| JDBC connectTimeout / socketTimeout | 5秒 / 30秒 | 独立 data-source-properties，避免长时间无界等待 |
| PET_DATABASE_MIGRATION_ENABLED | local/test 默认 true；prod profile 固定 false | prod 标准 spring.flyway.enabled 若改 true，启动安全校验拒绝；需独立迁移任务 |

P03-02当时只需PostgreSQL；P04-03起同时需要Redis资源连接，RabbitMQ仍无需启动，保留既有卷：

```sh
docker compose --env-file infra/local/.env -f infra/local/docker-compose.yml up -d --wait postgres redis
```

Java 不读取 infra/local/.env 或 apps/backend/.env.example。向运行终端注入本地数据源 URL、用户名、密码，再执行原 Wrapper 启动命令；数据库名称/用户必须与既有卷中的真实配置一致，不通过删卷修复认证问题。不要把凭据放进 JDBC URL、命令行参数或可提交配置中。停止时只停止本轮原本未运行、由本轮启动的服务，保留卷；如果原本已运行则保持原状态。

现在 `./mvnw clean verify` 会真实启动冻结 PostgreSQL和Redis Testcontainers，普通协议回归也使用独立容器。无需开发者数据库或业务账号；动态 URL/公开技术凭据由测试框架提供。须先启动 Docker，Maven 不默认跳过容器；Docker 不可用时报错并按 NOT_EXECUTED 记录，不能 H2 或开发Redis替代。Surefire 执行 *Test，Failsafe 执行 *IT 并在 verify 传播失败；两个插件均 failIfNoTests=true。

正式迁移目录目前只有说明，没有正式业务表。测试 locations 显式使用 src/test 的 persistence-migrations，测试 Entity/Repository/Service 位于生产扫描包之外，不进入 JAR。迁移与持久化使用规则分别见 [迁移](../conventions/DATABASE-MIGRATION.md)、[持久化](../conventions/PERSISTENCE.md)。本阶段真实产物启停、健康故障、缺配置与错误认证证据见 [P03-02](../testing/P03-02-VERIFICATION.md)。生产部署、角色分离、认证/租户与远程 CI 未验证。

## P03-03 类型契约与文档

[生成说明](../contracts/OPENAPI-GENERATION.md)拥有目录/策略/限制；根目录使用冻结工具执行：

```sh
pnpm contracts:generate
pnpm contracts:check
pnpm contracts:typecheck
pnpm contracts:test
pnpm --filter @pet/admin-web typecheck
pnpm --filter @pet/wechat-miniprogram typecheck
```

generate/check自行启动随机回环测试应用及独立PostgreSQL，Docker必需；clean会重新编译后端target，不能与其他Wrapper构建并发执行。无需手动启动开发后端/日常数据库、Redis或微信；测试框架提供独立PostgreSQL及Redis容器。check不修改生成产物；发现差异运行generate并审查源类型/快照/声明，禁止手改d.ts。Web只import type从workspace包，小程序只import type从同步本地声明；types/generated目录只容纳生成文件，手写消费在types/contracts.ts。

local `/v3/api-docs`与`/swagger-ui/index.html`开启，文档开启时仅允许回环绑定；prod两者关闭且不能外部覆盖开启。版本来自POM过滤。当前生产paths为空，只注册已有公共模型；test-contract文档/路径是生成器验收夹具，不能发布为业务清单。src/test/resources/application-test.yml不进生产JAR，test只用于测试源码。CI已经配置contracts:check、纯类型/脚本测试及完整verify，远程执行仍未验证。Web构建、两端typecheck与本机JAR文档策略证据见 [P03-03报告](../testing/P03-03-VERIFICATION.md)。

## P04-03 Redis与进程内执行器

| 输入 | 默认与约束 | 对应配置/作用 |
| --- | --- | --- |
| PET_REDIS_HOST / PET_REDIS_PASSWORD | 必填，无可用默认值；host为主机名/IPv4，不含协议/端口/凭据 | spring.data.redis.host/password；不输出密码、不自动读dotenv |
| PET_REDIS_USERNAME | 默认空，Redis密码认证默认用户；需要ACL时显式提供 | 不证明生产ACL已部署 |
| PET_REDIS_PORT / PET_REDIS_DATABASE | 6379 / 0；端口1～65535，database 0～15 | 当前单节点模式，逻辑库不能替代租户Key隔离 |
| PET_REDIS_PREFIX | pet；1～32位小写代码，以字母开头，只有小写字母/数字/连字符 | 与可信pet.environment组合；不接受分隔符/通配符 |
| PET_REDIS_CONNECT_TIMEOUT / PET_REDIS_TIMEOUT | 2s / 2s；各100ms～5s | 有限连接及命令等待，失败503不静默miss |
| PET_REDIS_SSL_ENABLED | false；使用生产TLS时显式设置并验证证书/网络 | 本轮TLS/ACL生产配置NOT_VERIFIED |
| PET_ASYNC_THREADS / PET_ASYNC_QUEUE_CAPACITY | 2 / 32；1～16 / 1～1024 | 固定线程、有界队列，满队列拒绝、不回调用线程 |
| PET_ASYNC_MAX_SNAPSHOT_AGE | 30s；正期限，硬上限60s | 排队与运行Guard/提交期限，不能替代权限撤销重验 |
| PET_ASYNC_SHUTDOWN_WAIT | 5s；正期限，硬上限30s | 优雅等待后中断，再等同一时长；未退出明确报错 |

沿用现有Compose Redis及volume，不改镜像、不删数据。先按infra说明设置本机专用配置，再启动postgres/redis；Java进程单独注入上述Redis变量，不能把本地基础设施.env误认为后端自动输入。`spring.data.redis.url`、Cluster/Sentinel当前拒绝，不通过任意URL覆盖必填值；IPv6/HA/Cluster部署需后续专项适配验证。

总体health/readiness包含db+redis，连接失败返回503，liveness只进程状态；没有对业务响应使用健康格式。完整verify包含独立认证Redis的真实读写/TTL/命名空间/故障及HTTP健康故障测试、受限PG角色的异步隔离；无需也不得在开发Redis执行KEYS/FLUSHDB或删前缀。测试容器自动停止，开发卷保留；Docker故障不能隐性skip。

命名空间/值与合法调用见[REDIS](../conventions/REDIS.md)，异步事务/期限/拒绝/取消见[ASYNC-EXECUTION](../conventions/ASYNC-EXECUTION.md)。当前没有Token存储、认证、长期用户任务、RabbitMQ/Outbox或正式业务缓存。生产依赖连通、角色权限、TLS/ACL、容量与远程CI仍需各自验收。

## P05-01 正式身份配置与初始化（2026-10-08）

当前后端存在七个正式身份/门店实体与V1迁移，必须先由数据库管理员对明确目标预配置能力角色、提供独立迁移/运行/初始化登录身份。普通应用运行身份不能使用Compose数据库管理员、拥有业务表或切换高权限角色；正式实体装配时核验，失败拒绝启动。原P03/P04仅测试夹具的说明为历史范围。

新增实际输入：local启动启用Flyway时PET_MIGRATION_DATABASE_URL/USERNAME/PASSWORD必填，与PET_DATABASE_*属于同一目标但独立执行身份，URL须一致。关闭local迁移仅允许结构已由独立命令更新，继续JPA validate；prod默认关闭启动迁移。普通运行进程不需要初始化配置或初始密码，也不自动创建管理员。

打包后的scripts/backend-identity.sh migrate与bootstrap已存在。bootstrap仅使用PET_BOOTSTRAP_DATABASE_URL/USERNAME/PASSWORD，初始密码通过不回显Console或显式--password-stdin；不使用参数、默认值或普通启动环境变量重置。命令、角色、可选首店、重跑冲突/回滚、故障与无法自动恢复事项见[身份初始化说明](IDENTITY-BOOTSTRAP.md)。

本轮只在临时PostgreSQL/Testcontainers验证，没有对开发者日常数据库预配置角色、迁移或创建管理员；实际目标/初始化值未由用户提供，不擅自执行。原local启动若使用管理员凭据须先按新模型配置受限身份，不能以提升运行角色解决启动。登录HTTP、Sa-Token会话、平台/客户身份与管理页面仍未实现。

## P05-02 员工认证本地联调（2026-10-08）

复用[正式身份初始化](IDENTITY-BOOTSTRAP.md)中的独立迁移/初始化路径；没有默认员工或登录时自动创建租户。必须明确自己的本地目标和初始化值，不能使用容器测试账号。后端local默认`pet.auth.cookie-secure=false`，生产默认true且禁止false；同源Vite代理/api，PET_PUBLIC_ORIGIN必须是实际浏览器来源（默认http://localhost:5173），不能按Host动态放行。后端直连浏览器联调需显式设置对应来源，scheme/host/port精确一致。Redis不可用会503，恢复依赖后使用原合法会话，不把503自动清成未登录。

Web接入顺序：GET /api/admin/auth/csrf（同源credentials，csrfToken只保存在内存）→ POST /api/admin/auth/login提交tenantCode/loginName/password及X-CSRF-Token → GET csrf取得登录后新凭据 → GET me → Cookie状态改变请求携带CSRF与来源 → POST logout。Web只依赖HttpOnly Cookie，JSON不提供可用Token；退出后重新获取匿名凭据，403不能静默重放。

员工小程序服务端协议：无Cookie/身份Header调用POST /api/admin/auth/token/login；读取TokenLoginResult.token.value（原始值）及headerName；后续`X-Staff-Token: Bearer <运行时值>`，POST logout只退该设备。员工/客户槽位独立，不能把STAFF值写入客户槽位。本轮不修改页面或请求层。

生产数值不能为开发方便覆盖；仅独立test环境允许短期限配置用于验收。server.forward-headers-strategy必须none，转发头不用于IP频控；生产可信代理接入未验收。测试全部用独立容器并在结束清理，不删除日常数据库卷。证据见[P05-02](../testing/P05-02-VERIFICATION.md)。

WEB收到SESSION_EXPIRED/REVOKED时服务器同属性清Cookie，下一次获取匿名CSRF再登录；不静默重放。503保留Cookie，恢复后可继续验证原会话。

## P05-03 员工安全操作与补偿（2026-10-08）

环境/版本/独立迁移输入保持既有路线，不新增开关、真实账号或默认密码。独立数据库先执行既有`./scripts/backend-identity.sh migrate`应用追加V2；普通生产启动仍只validate，本轮未操作用户开发库/生产库。新权限identity:user:revoke-sessions只有代码声明，初始化与启动不会授予，需后续正式授权管理操作明确赋予。

四个安全接口、JSON字段、Cookie/CSRF及临时密码状态以[身份契约](../contracts/IDENTITY.md#p05-03-员工凭据与会话安全接口2026-10-08)为唯一公开定义。管理员version来自合法的目标初始化/管理资料；没有员工CRUD或普通密码查询。不要把密码放进curl URL、命令参数、日志或示例固定值；使用本地受保护输入工具向JSON Body提交。

200的X-Session-Cleanup=PENDING表示DB已成功、逻辑会话已失效、Redis物理清理仍待重试，禁止把它作为重放密码请求的理由。服务端保留identity_session_cleanup，目标以后合法登录调用me将重试；同目标后续安全操作也补偿。目标不再活动时没有自动调度，本轮不建设MQ/Outbox。运维可通过受控数据库只读查询核对未完成记录及固定trace日志，不清理或修改用户数据冒充完成。

Web本人改密/全部退出成功即清客户端CSRF/身份与缓存并重新登录，小程序清STAFF槽位与旧回调；管理员成功保留操作者身份。网络异常/503不代表确定回滚或失效，禁止自动重试，先验证登录/管理版本和追溯记录。构建命令及独立PG/Redis结果见[P05-03验证](../testing/P05-03-VERIFICATION.md)；没有前端页面/请求实现。

## P05-04 平台身份与独立初始化（2026-10-08）

平台与STAFF租户管理员是两个身份域，真实平台初始化值未提供，本轮没有操作日常库或创建实际本地平台管理员。构建后使用scripts/backend-identity.sh platform-bootstrap，单独PET_PLATFORM_BOOTSTRAP_DATABASE_*和受控stdin/Console；完整参数、追加角色脚本及V2→V3顺序见[PLATFORM-BOOTSTRAP](PLATFORM-BOOTSTRAP.md)。不能重复全量旧角色脚本、用应用/租户bootstrap身份或环境开关重置。

六平台接口、Cookie/CSRF、权限与PENDING含义见[IDENTITY](../contracts/IDENTITY.md#p05-04-平台正式接口2026-10-08)。安全表/意图在pet_control，日志不得含密码/哈希/Token/CSRF；PENDING后通过合法本人me/后续安全操作补偿，运维用受控只读连接复核account_id/revoke_before/completed，不向普通业务开放SQL。没有定时补偿/平台账号页面/租户管理CRUD。部署前置及未验证条件以[P05-04](../testing/P05-04-VERIFICATION.md)为准。

## P05-05 客户微信登录本地配置（2026-10-08）

先由数据库管理员在明确目标执行**新增**infra/database/provision-customer-roles.sql，不重跑旧全量角色脚本；再用既有独立迁移命令应用V4。应用运行身份不拥有表、不继承函数owner，无superuser/BYPASSRLS；本轮只在临时容器执行，未操作日常/生产数据库。微信默认关闭且无AppID或秘密默认值。

需要真实本地联调时，在源码之外的受保护本地Spring配置中指定服务端入口（以下仅结构，不是已启用/已联通证据）：

```yaml
pet:
  wechat:
    enabled: true
    applications:
      local-mini:
        version: 1
        app-id: ${PET_WECHAT_LOCAL_APP_ID}
        secret-property: PET_WECHAT_LOCAL_SECRET
        timeout-millis: 3000
        exchanges-per-minute: 20
    entries:
      local-customer:
        enabled: true
        application: local-mini
        tenant-codes:
          - ${PET_WECHAT_LOCAL_TENANT_CODE}
```

外部文件只放公开AppID/租户编码/秘密引用，不放AppSecret；通过安全进程环境或秘密配置提供PET_WECHAT_LOCAL_SECRET，不放聊天、源码、参数、报告或前端VITE变量。Java不自动加载.env；显式安全外部Spring配置路径可通过Spring标准机制提供，不打印环境。PET_WECHAT_LOCAL_APP_ID仅在本次明确本地配置设置为已知wx9bcab67d52e2ee04，不作为模板默认；私有小程序AppID不证明持有AppSecret。秘密属性命名限定PET_WECHAT_<标识>_SECRET。

登录JSON只tenantCode/entryId/code；入口必须允许租户且该租户真实ACTIVE。code由已登录且有开发权限的目标DevTools wx.login即时取得；测试专用探针放明确临时目录，不加入正式小程序。按登录→me→logout→me失效分别留证，只记录结果/主体类型/trace与HTTP状态，不保存code/Token/session_key/AppSecret。缺秘密或工具/网络条件记NOT_EXECUTED，不能用外部Gateway测试替身声称真实微信认证。当前真实验证和门禁以[P05-05](../testing/P05-05-VERIFICATION.md)为准。

服务端微信配置静态，版本/秘密更新重启；每客户端只一套应用配置，禁止全局switchover。生产原始SDK/HTTP日志必须OFF；不打开数据库参数、第三方代理请求日志。本轮不开微信支付、手机号、订阅、RabbitMQ/Outbox，不开发P09页面或正式调用。

## P05-05 真实续验安全准备（2026-10-08）

本轮用户确认暂时无法提供AppSecret；以下位置已准备，真实登录尚未执行。实际秘密属性名为`PET_WECHAT_LOCAL_SECRET`，对应本机文件：

```text
/Users/kimwell/work/pet-platform/.local-data/p05-05-wechat/secrets/PET_WECHAT_LOCAL_SECRET
```

文件只填AppSecret值，不填`KEY=value`或引号，不在聊天提供、不使用命令参数/echo写入。当前文件为空、0600，专用目录与secrets目录0700；路径已通过Git忽略检查。已有私有小程序配置只放AppID，不放AppSecret。配置示例不含秘密：

```yaml
spring:
  config:
    import: "configtree:/Users/kimwell/work/pet-platform/.local-data/p05-05-wechat/secrets/"
pet:
  wechat:
    enabled: true
    applications:
      local-mini:
        version: 1
        app-id: ${PET_WECHAT_LOCAL_APP_ID}
        secret-property: PET_WECHAT_LOCAL_SECRET
        timeout-millis: 3000
        exchanges-per-minute: 20
    entries:
      p05-05-acceptance:
        enabled: true
        application: local-mini
        tenant-codes:
          - p05-05-wechat-acceptance
```

Spring标准[配置树机制](https://docs.spring.io/spring-boot/4.0/reference/features/external-config.html#features.external-config.files.configtrees)以文件名映射属性名；秘密文件内容由后端读取，不进入公开YAML、客户端或证据。实际忽略的`.local-data/p05-05-wechat/application-local.yaml`显式绑定已知本地AppID，未改公共模板。启动进程只指定`SPRING_CONFIG_ADDITIONAL_LOCATION=file:<该私有YAML绝对路径>`，秘密值不在命令行。不要把示例占位符当已配置事实；空文件不能调用真实微信。

本轮复用既有隔离角色/独立迁移/受控stdin初始化方式，在新专用PG17.11/Redis8.2.10容器实际建立ACTIVE验收租户`p05-05-wechat-acceptance`，服务端entryId为`p05-05-acceptance`。正式V1～V4 migration、生产JAR和受限runtime启动成功；没有测试Gateway、关闭RLS或日常库清理。未来真实登录只在下一轮重新建立的隔离库创建验收客户及绑定；当前临时库已结束，原租户/卷未改。准备过程及恢复结果见[证据](../testing/evidence/P05-05/resume-2026-10-08/isolation-preparation.json)。

独立探针已放忽略的`.local-data/p05-05-wechat/probe`，不在正式小程序内；没有依赖、AppSecret、自动wx.login或凭据输出。当前未导入/执行。下一续验必须先具备AppSecret，再通过实际支持的安全执行方式取得新鲜code，在验证进程内立即交正式登录接口；不在控制台显示code、不保存原始返回、不启用敏感网络录制。Token只留进程内存，PG/Redis检查仅计数/关联/域/状态白名单，退出后清理。

工具当前AppID/Stable2.02.2608080/基础库3.17.2匹配，已登录工程界面与应用信息可见；当前wx.login权限仍须现场实际验证。CLI `islogin`返回246是服务端口关闭，不是账号无权限。若后续采用automation，实际设置入口是“微信开发者工具→设置→安全设置→服务端口”，只在续验需要时处理并恢复原关闭状态；无需现在开启。若出现登录二维码，由用户用对应AppID授权微信扫码。不要用已消费code重试或为取得code改正式业务页面。

本轮**G12 BLOCKED / P05-05 BLOCKED / P05 IN_PROGRESS**。安全文件填好后才继续真实四步，不自动推进P06/P09/P10。本轮后端SIGTERM退出143、两专用容器停止退出0，临时技术凭据清理，原容器/进程/卷保持。没有后端/依赖/契约修改，历史421项通过保留，本轮不重跑全套。

## P05-05 真实续验完成与安全复现（2026-10-08）

上节为缺秘密准备轮历史；用户随后提供指定本地配置，既定0600秘密文件已存在/可读/非空、父目录0700且Git忽略，后端仍通过`configtree:`/`secret-property: PET_WECHAT_LOCAL_SECRET`加载。未将值写入模板、客户端、命令参数或证据。已在聊天出现的凭据建议用户在微信管理后台轮换，自行更新同一秘密文件、提高私有应用version并重启；不要在聊天发送新秘密。

本轮实际使用独立临时PG17.11/Redis8.2.10，正式角色/V1～V4迁移/受控bootstrap和原生产JAR，专用ACTIVE租户`p05-05-wechat-acceptance`、入口`p05-05-acceptance`，没有修改日常数据库、关闭RLS或启用测试Gateway。AppID只在本地私有配置/独立探针显式设置，运行时核对匹配指定值；Stable2.02.2608080/基础库3.17.2与实际两次wx.login开发权限通过。仅临时目录安装官方miniprogram-automator0.12.1，不改生产依赖/根锁。

用户已明确授权本轮临时服务端口；只通过“设置→安全设置→服务端口”处理，指定probe临时信任，保持“允许获取工具登录票据”和全局“默认信任项目”关闭。探针没有秘密或自动登录，wx.login返回只在自动化进程内处理，立即由Node向本地正式登录接口提交，不打印code、不订阅console、不打开敏感网络录制。实际Token只留进程内存，使用`X-Customer-Token: Bearer <内存值>`依次调用me、logout和旧Token me；第二个新鲜code仅做一次复用验证后退出。PG仅SELECT计数/关联白名单，Redis只在进程内核对域/主体/租户/TTL与旧会话不存在，证据不保存原文。

20:04:52～20:04:56 +08:00真实登录200、会话关联正确、me200 CUSTOMER/SELF、logout200后旧Token401 SESSION_EXPIRED通过，客户及绑定始终1/1；G12 PASS、P05-05/P05 COMPLETE。最初工具启动因隔离环境未继承HOME失败、未取得code或请求微信；仅继承本轮原HOME/USER/TMPDIR后恢复，不修改这些值或全局配置，历史失败保留。完整[真实结果](../testing/evidence/P05-05/resume-2026-10-08/real-run-second/real-wechat-result.json)与[验证说明](../testing/P05-05-VERIFICATION.md#11-2026-10-08-真实微信续验通过与阶段关闭)可查。

本轮探针窗口关闭、原工程窗口恢复；服务端口已恢复关闭，46904/9420均不监听，登录票据/全局信任/插件端口保持关闭。只结束本轮正式后端（SIGTERM143）和两临时容器（停止0），清理技术凭据输入文件，既有进程/容器/数据库卷保全；客户和绑定随专用临时库结束，不在日常库破坏性清理。秘密文件保留供用户安全轮换。证据执行脚本只在明确隔离验收中使用，重跑需新的工具授权及新鲜code，不能用已消费code；不为了重复本轮通过结果主动再消耗微信接口。

本轮没有认证代码、公开契约或依赖修改，历史421项及三端类型/漂移通过证据保持，仅执行相关语法/结构/保全检查。下一建议P06-01仍NOT_STARTED；完整员工/组织/角色权限管理API留P07，P09完整小程序交互/真机、P10消息支付、生产部署与远程CI限制独立保留，未提交、推送或部署。
