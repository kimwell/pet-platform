# P02-01 根目录工程骨架、构建工具与本地基础设施初始化

日期：2026-10-07（Asia/Shanghai）。**P02-01 COMPLETE；P02整体 IN_PROGRESS；P02-02 NOT_STARTED。** G01～G16 必选条件通过，条件性未执行项单列，不转换为 PASS。本报告是本轮事实源；P01 报告保持历史原样。

## 1. 项目事实与前置条件

实际读取用户指定的 AGENTS、README、技术基线/结构/模块/配置、版本矩阵/路线/决策、P01 两份验证和验收矩阵，并核对契约索引、OpenAPI 生成、三端约定和认证的同源开发规则。P01-01、P01-02及P01整体 COMPLETE；未发现影响工程初始化的核心设计阻塞。

本轮开始时为 Git 仓库，分支 main 尚无提交，AGENTS.md、README.md、docs/、ui/ 均未提交；没有 apps、packages、infra、工程 package.json、pom.xml或锁文件。P01 当时“非 Git”的记录不改写，当前 README/结构据实更新。写入前建立 [430文件清单](evidence/P02-01/inventory-before.json)，最终逐文件保全与变更见 [审计](evidence/P02-01/final-audit.json) 和 [变更清单](evidence/P02-01/change-summary.json)。既有 ui 与 P01 历史证据原样保留。

工具当前执行环境为 macOS arm64，使用 P01 已下载并校验的隔离 Temurin、Node、pnpm，不改全局配置。本机默认 Node/pnpm/Java 不是项目精确目标；临时 PATH 包含同一 pnpm，避免根脚本内调用本机旧版本。Docker CLI、daemon、Compose均可用。实际已安装微信开发者工具低于矩阵目标，没有实际 AppID；不安装其他工具线来绕过冻结条件。工具输出见 [environment](evidence/P02-01/environment.json)。

未安装、调用或引入 Product Delivery OS，未创建/读取/更新其状态；没有脚手架子目录、移动成果、自动提交/推送/部署/发布。

## 2. 实际版本与接入边界

精确版本唯一文档事实源仍为 [VERSION-MATRIX](../development/VERSION-MATRIX.md)，应用版本没有重选。实际采用 Java/Temurin、Maven/Wrapper、Boot、Node/pnpm、React/DOM、TS、Vite/React插件、Ant Design、Router/Query/Zustand、ESLint相关工具、Vitest、TDesign/微信API类型均与该表一致。20项前端直接依赖真实解析见 [直接依赖树](evidence/P02-01/frontend-direct-tree.log)；BOM 管理项见 [实际后端树](evidence/P02-01/backend-dependency-tree.txt)。

后端只加入 spring-boot-starter-webmvc 与 test starter；编译插件、Surefire/Failsafe、JUnit、Mockito及HTTP运行依赖由 Boot parent/BOM管理。Failsafe 已绑定 integration-test/verify，但目前没有 IT 用例；“No tests to run”不代表数据库集成 PASS。2项有效单元/启动测试实际执行、0失败/错误/跳过，报告XML已归档。Mockito显式agent使用BOM版本，未动态自附加。

JPA/Flyway/PG驱动/springdoc在P03，Sa-Token/Redis会话在P05，AMQP/WxJava在P10按需加入；Testcontainers跟随真实数据库集成任务加入。Hutool没有当前用例，不预装。没有通过自动配置排除伪装已集成能力，没有H2、业务表、正式迁移或健康Controller。

P01未指定CI动作版本，按用户允许的最小构建决策补齐官方Actions并固定完整SHA，依据与解析结果追加到唯一版本矩阵；未新增应用框架/包管理器/测试体系。现有Maven镜像配置被正常沿用，没有修改全局settings；POM未写私有仓库或凭据。

## 3. 工程结构和三端完成内容

实际目录树见 [PROJECT-STRUCTURE](../architecture/PROJECT-STRUCTURE.md)，规划树另列。根目录建立唯一 pnpm workspace/lock、精确 engines/packageManager/.nvmrc、EditorConfig、gitignore/gitattributes、统一ESLint和有效脚本。Java仍是独立单Module，未创建根聚合POM或Node化后端。

后端为 `com.pet.platform.Application`，源码路径符合固定包名；only-script Wrapper包含Unix/Windows脚本、properties、分发SHA-256和Unix执行位。配置包括base/local/test/prod及环境输入示例，base无开发profile回退，公开origin验证且prod要求HTTPS。产物为可启动Boot JAR，Start-Class及Java21 bytecode见 [产物证据](evidence/P02-01/backend-artifact.json)。不声称认证、数据库、健康组件或生产部署完成。

Web包含React入口、Ant Design中文、QueryClientProvider、代码路由、系统壳、错误和NotFound。正式登录、guard、Dashboard、假列表、透传组件、无用途Zustand Store均未创建。开发服务器仅回环监听、`/api`同源代理，不开放任意CORS；客户端没有秘密。测试验证真实路由NotFound渲染及返回链接、入口在官方配置/provider下渲染，浏览器另行检查。

小程序是原生TS工程：合法app/page/sitemap，TDesign官方button，noEmit类型检查与ESLint。miniprogramRoot、npm源/产物关系、TS插件、目标基础库和私有配置按 [微信官方项目配置](https://developers.weixin.qq.com/miniprogram/dev/devtools/projectconfig.html)、[npm](https://developers.weixin.qq.com/miniprogram/dev/devtools/npm.html)、[TS](https://developers.weixin.qq.com/miniprogram/dev/devtools/compilets.html)核对；HTTP200资料内容存档。AppID留空，私有文件覆盖方式有说明，未编造AppID/Secret。主workspace不加入miniprogram-ci。

packages/api-contracts只有package.json/README，无正式后端OpenAPI、业务DTO或冒充生成声明；计划生成入口遵循P01-02，不放入必须check链。正式功能均未提前实现。

## 4. 本地基础设施与运行状态

Compose包含P01冻结的PostgreSQL、Redis、RabbitMQ标签+多架构digest，三个named volumes、可调回环端口、有效健康检查、Redis密码保护和RabbitMQ管理UI。`.env.example`密码为空，缺值明确拒绝启动；真实本地配置被忽略，不打印解析后的秘密。详细使用、备份/保留/清理说明见 [infra](../../infra/local/README.md)。

本轮开始无已运行容器；验证使用独立项目 `pet-platform-p02-validation`，公开技术夹具凭据、回环25432/26379/25672/25673，不是真实业务账号或生产秘密。三镜像真实拉取、启动并全部healthy；PostgreSQL认证SQL成功、Redis认证PING成功/无认证NOAUTH、RabbitMQ节点正常/管理API认证HTTP200。没有创建Tenant/Store业务表，也没有数据库应用集成或消息投递测试。

验证结束停止本轮专用容器，保留新建named volumes；没有删除容器/数据库/卷或处理其他项目资源。普通stop命令0，但RabbitMQ首次容器退出137且非OOM，原状态不改写。原卷再次启动healthy后用官方节点shutdown，容器最终退出0；同容器exec客户端随PID1终止返回137，该命令保留FAIL。结束状态见 [第一次停止](evidence/P02-01/infra-stop.json)、[原状态](evidence/P02-01/runtime-final.json)、[最后状态](evidence/P02-01/runtime-final-r2.json)与 [节点退出](evidence/P02-01/rabbit-exit-final.json)。后端18080/Web5173临时检查进程也已停止，启动证据保留；日常环境须按说明自行填写 `.env` 后启动，不使用验证夹具冒充日常配置。

## 5. 新增和修改文件

逐文件大小/SHA与分类见 [change-summary](evidence/P02-01/change-summary.json)，本轮没有删除既有文件。

新增：根工程配置/单锁/ESLint，apps/backend真实入口与配置/测试/Wrapper，apps/admin-web真实入口/路由/provider/错误/样式/测试，apps/wechat-miniprogram原生入口/私有示例，api-contracts包标识/说明，两个工程检查脚本，infra/local三文件，CI workflow、LOCAL-DEVELOPMENT、本报告及P02证据。

修改：AGENTS.md、README.md、PROJECT-STRUCTURE、TECHNICAL-BASELINE、CONFIGURATION、VERSION-MATRIX、ROADMAP、DECISION-LOG、ACCEPTANCE-MATRIX。只更新当前授权/实际工程状态和最小CI工具补充，不改P01历史门禁结论或核心业务契约。

## 6. 实际验证命令与结果

完整argv/cwd/退出码/摘要在 [all-command-records](evidence/P02-01/all-command-records.json)。以下pnpm使用矩阵目标工具，省略任务临时工具的绝对路径；Maven工作目录apps/backend，其他为项目根，干净安装/离线夹具的内层cwd和参数均在原始记录。

| 检查 | 命令 | 退出码 | 实际结果 / 等级 | 证据 |
| --- | --- | --- | --- | --- |
| 空缓存Wrapper | `MAVEN_USER_HOME=<专用空缓存> ./mvnw --version` | 0 | PASS；真实下载/校验Maven / RUNTIME_VERIFIED | [记录](evidence/P02-01/wrapper-empty-cache.json) |
| 后端 | `./mvnw --batch-mode clean verify` | 0 | PASS；2 tests，0失败/错误/跳过，JAR打包 / COMPILED及启动测试运行 | [日志](evidence/P02-01/backend-verify.log) |
| 安装 | `pnpm install --reporter=append-only` | 0 | PASS；4 workspace、272解析包 / RESOLVED | [日志](evidence/P02-01/frontend-install.log) |
| 冻结重装 | `pnpm install --frozen-lockfile`，当前根及干净临时workspace | 0 / 0 | PASS；无node_modules的新副本安装成功，锁SHA不变 / RESOLVED | [当前](evidence/P02-01/frontend-lock-replay.json)、[干净](evidence/P02-01/frontend-clean-install.json) |
| Web类型 | `pnpm --filter @pet/admin-web typecheck`，根check中的最终执行 | 0 | PASS；strict源码/skipLibCheck / COMPILED | [完整最终check](evidence/P02-01/check-root-r3.log) |
| Web lint | `pnpm --filter @pet/admin-web lint` | 0 | PASS；0错误/警告 | 同上 |
| Web test | `pnpm --filter @pet/admin-web test` | 0 | PASS；2项入口/路由技术测试 / RUNTIME_VERIFIED限定场景 | 同上 |
| Web build | `pnpm build:web` | 0 | PASS；真实Vite产物，有chunk提示 / COMPILED | [日志](evidence/P02-01/web-build-r2.log) |
| mini类型/lint | `pnpm --filter @pet/wechat-miniprogram typecheck` / `lint` | 0 / 0 | PASS；非工具编译 / COMPILED及静态检查 | [类型](evidence/P02-01/mini-typecheck.json)、[lint](evidence/P02-01/mini-lint.json) |
| 根聚合命令 | `pnpm check` | 0 | PASS；真实仓库/配置/Web/mini链 | [记录](evidence/P02-01/check-root-r3.json) |
| 最终版本/秘密/配置 | `pnpm check:repo` | 0 | PASS；精确值对矩阵、单锁、原生引用/结构、秘密特征检查 | [记录](evidence/P02-01/repo-final.json) |
| 脚本静态规则 | `pnpm exec eslint scripts eslint.config.mjs --max-warnings=0` | 0 | PASS；工程检查代码无lint警告 | [记录](evidence/P02-01/repo-lint.json) |
| Compose | `docker compose --env-file .local-data/p02-infra.env -p pet-platform-p02-validation -f infra/local/docker-compose.yml config --quiet` | 0 | PASS；实际解析校验，不打印凭据 | [记录](evidence/P02-01/compose-config.json) |
| 容器启动 | 同配置 `up -d --wait --wait-timeout 180` | 0 | PASS；三项healthy / RUNTIME_VERIFIED | [记录](evidence/P02-01/infra-start.json) |
| Rabbit停止核对 | Compose stop；原卷重启；容器内 `rabbitmqctl shutdown`；宿主inspect | 0 / 0 / 137 / 0 | stop/重启/inspect命令PASS；shutdown exec命令FAIL；最终节点退出0，两个退出层次分别记录 | [调查](evidence/P02-01/rabbit-stop-investigation.json)、[重启](evidence/P02-01/rabbit-restart.json)、[shutdown](evidence/P02-01/rabbit-graceful-shutdown.json)、[最终退出](evidence/P02-01/rabbit-exit-final.json) |
| 基础认证连接 | `python3 <临时>/infra-smoke.py`，内层真实Compose exec/管理HTTP | 0 | PASS；PG SELECT、Redis认证/拒绝无认证、Rabbit管理200 / RUNTIME_VERIFIED | [日志](evidence/P02-01/infra-smoke.log) |
| 产物/开发启动 | `java -jar ... --spring.profiles.active=local --server.port=18080` / `pnpm dev:web`，随后HTTP检查 | 启动为长驻进程；HTTP检查0 | PASS；后端真实404与Web200，启动后受控停止 | [进程](evidence/P02-01/backend-start-process.json)、[HTTP](evidence/P02-01/startup-http.json) |
| 浏览器 | CUA打开入口→未知路由→返回 | 不适用进程退出码 | PASS；真实页面和返回链接 / RUNTIME_VERIFIED | [浏览器记录](evidence/P02-01/web-browser.json)、[入口](evidence/P02-01/web-entry.jpg)、[NotFound](evidence/P02-01/web-not-found.jpg) |
| mini离线npm | `node <临时>/pack-api.cjs`，官方隔离packNpmManually | 0 | PASS；miniProgramPackNum=1，warnList=[]，1177实存文件；非TS工具编译 | [记录](evidence/P02-01/mini-npm-pack.json)、[产物](evidence/P02-01/mini-npm-output-manifest.json) |
| 配置反例 | 无profile/必填环境后端启动；空密码Compose校验 | 1 / 1（均预期1） | PASS反例；确实拒绝，未当意外构建成功 | [后端](evidence/P02-01/backend-missing-environment.json)、[Compose](evidence/P02-01/compose-empty-password.json) |
| CI配置 | `python3 <临时>/check-ci.py` | 0 | PASS；YAML/官方SHA/实际脚本对应，不代表远程CI运行 | [记录](evidence/P02-01/ci-source-check.json) |
| 最终审计 | `python3 evidence/P02-01/audit-reproduction.py` | 0 | PASS；保全/引用/范围/秘密特征/文件清单，不替代业务验收 | [审计](evidence/P02-01/final-audit.json) |

## 7. 失败尝试、修复与未执行项

Web首轮typecheck退出2/test退出1，原因是测试用了当前Router不存在的状态字段；一次改成另一个不存在字段也退出2。读取真实类型/实现后改用实际NotFound SSR和返回链接断言，最终typecheck/lint/test/build通过。不是删除失败测试或关闭strict，所有失败记录保留。

最终文档审计首轮退出1，发现报告引用了尚未写出的审计自身输出/运行结束记录；先写真实运行状态、在审计启动时建立明确NOT_EXECUTED的待计算产物，再完整复核引用并生成最终结论。原失败日志保留，没有删除引用检查。

根check首轮退出1，临时runner虽然外层调用冻结pnpm，嵌套脚本PATH仍找到本机旧pnpm；在任务临时bin绑定已校验精确pnpm后通过，不改全局或放宽engine。初次安装提示esbuild脚本未批准，按官方pnpm设置只允许已解析的esbuild；干净frozen install真实执行其安装脚本。CI版本API首次403限流，官方git ls-remote后续成功。首张浏览器截图样式尚未稳定，重新获取可见状态和截图确认正常后保存最终证据，没有把异常截图当布局验收。

| 未执行项 | 结果 / 等级 | 原因、影响和后续 |
| --- | --- | --- |
| 目标微信开发者工具npm构建/TS转换/模拟器编译 | NOT_EXECUTED / NOT_VERIFIED | 本机旧工具、无实际AppID；离线组件输出不能替代。G09是配置符合原生要求，已通过；工具真实编译不是本轮G08静态检查。P02-02解决环境后验收，P02整体未完成 |
| 真机/预览/上传/发布 | 同上 | 无微信运行配置，且本轮不发布；对应P09/P12 |
| 远程GitHub CI | 同上 | 没有提交/推送授权；CI命令/配置已校验，平台运行待后续 |
| Windows/Linux工具链与mvnw.cmd运行 | 同上 | 本轮仅macOS arm64；已有脚本/CI配置不代替实际多OS运行 |
| PostgreSQL应用集成/Flyway/JPA/Testcontainers、Redis会话、RabbitMQ业务消息 | 同上 | 按阶段尚未接入，不用三容器健康冒充应用/业务验收 |
| 认证/权限/租户越权、真实OpenAPI、附件/异步、真实微信 | 同上 | 当前授权明确排除正式功能，留在对应后续阶段 |

## 8. G01～G16完成门禁

| Gate | 条件 | 结果 | 当前证据及边界 |
| --- | --- | --- | --- |
| G01 | P01实际事实已读取 | PASS | 原报告/契约/版本/限制已核对，无核心设计阻塞 |
| G02 | 当前根直接建设 | PASS | 实际目录与清单，无脚手架子目录 |
| G03 | 后端package固定 | PASS | 入口源码/JAR Start-Class为com.pet.platform.Application |
| G04 | Maven Wrapper可用 | PASS | only-script、校验和、Unix权限、空缓存真实调用 |
| G05 | 后端clean verify | PASS | 退出0，2有效测试0跳过，真实HTTP启动 |
| G06 | Web typecheck | PASS | 最终tsc退出0，不关闭源码strict |
| G07 | Web lint/test/build | PASS | 三命令最终0；2测试，真实Vite产物 |
| G08 | 小程序typecheck/lint | PASS | 两命令0，仅静态检查边界 |
| G09 | 原生小程序配置 | PASS | 官方配置/路径/TS插件/AppID方法；真实离线TDesign产物，工具编译未执行另列 |
| G10 | Compose配置 | PASS | config --quiet退出0，缺秘密反例拒绝 |
| G11 | 依赖健康状态如实记录 | PASS | 三项真实healthy及基础认证连接；验证后停止，未称业务集成完成 |
| G12 | CI对应有效命令 | PASS | YAML/SHA/脚本核对；本地相同命令通过，远程CI仍未执行 |
| G13 | 单锁可重复安装 | PASS | 一份根锁，当前与干净workspace frozen install成功且SHA不变 |
| G14 | 配置与仓库无真实秘密 | PASS | 公开技术夹具明确标注，本地文件忽略；秘密特征扫描及人工增量核对无真实秘密发现 |
| G15 | 文档与工程一致 | PASS | 当前事实/实际与规划/命令/未执行边界一致，引用及清单审计通过 |
| G16 | 未提前正式功能 | PASS | 无业务Controller/CRUD/表/会话/附件/Outbox/生成假DTO |

## 9. 当前真实问题、最终状态和下一任务

P01上游第三方声明全面检查历史FAIL未修复，当前沿用strict源码+skipLibCheck边界。Web构建输出约689kB单chunk并给出>500kB提示，构建通过，但未作性能验收；后续按页面模块分包。微信目标工具与实际AppID缺失阻止目标工具编译，远程CI/多OS仍未运行。日常infra `.env`尚待开发者本机填写；本轮只用了独立技术夹具。RabbitMQ普通停止出现137，原卷重启健康后节点shutdown最终退出0；容器内exec仍137，停止链路需P02-02进一步核对，不声称其完全验收。没有隐藏为默认值的关键版本或契约冲突。

**P02-01 COMPLETE**：G01～G16在各自定义边界内均PASS；条件性未执行项原样列出，不从静态/离线结果推导运行PASS。**P02整体仍IN_PROGRESS**，本任务不表示数据库集成、认证、多租户、小程序发布或生产完成。

下一合法任务经路线图核对并补齐为 **P02-02：三端启动联调、环境配置校验与工程基础验收**，范围见路线图；当前NOT_STARTED，只报告，不自动执行。

## 10. 复现与证据

先按矩阵准备任务专用工具路径并在根执行 frozen install、check、build:web，在backend执行Wrapper clean verify；日常infra按专用.env启动。离线npm可在独立已冻结miniprogram-ci安装目录调用归档pack-api.cjs；替换脚本中临时绝对路径为实际工具模块路径，不加入主workspace。verify.py是本轮证据记录器，不是根开发命令依赖；其路径与完整原始命令保留以复现边界。

[证据manifest](evidence/P02-01/EVIDENCE-MANIFEST.json)保存大小/SHA并排除manifest自身，包含测试XML、JAR元数据、依赖树、命令日志、保全/变更、官方资料、组件清单和浏览器截图。本地target/dist/组件产物按gitignore排除，不将运行生成物直接当源文件提交。
