# P01-01 项目事实、版本与兼容验证报告

日期：2026-10-07（Asia/Shanghai）。当前结论：**COMPLETE**（本轮13项完成条件已满足，P01整体仍IN_PROGRESS）。本报告是本轮结果事实源；不创建项目治理状态文件，不自动推进下一任务。

## 1. 当前目录与仓库事实

当前项目根目录固定为 `/Users/kimwell/work/pet-platform`。本次修订写入前，根目录只有 `.DS_Store` 和 `ui/`，273个既有文件；没有 AGENTS.md、根 README、docs、package.json、pom.xml、依赖锁文件或正式三端工程。当前根及四级父目录的 AGENTS.md 均不存在，没有与10份指定产物或证据目录重名的文件：[冲突检查](evidence/P01-01/conflict-check.json)。

本次重新执行 rev-parse、branch、status，均退出128并提示 not a git repository；当前不是 Git 仓库，分支及 tracked/dirty/untracked 分类不适用：[当前根与环境命令](evidence/P01-01/current-root-checks.json)。已检查既有 UI README；其视觉成果保留原位，不能作为通用模板业务或完整业务验收。

用户最新位置指令替代先前同级新目录选择：所有工程和文档直接在当前根创建或更新，不创建 `enterprise-app-scaffold` 子目录，不整体移动既有成果。只将前轮已产生的10份 P01-01 文档和相关验证证据复制并修订到当前根；先前同级目录未被修改或删除，不再是当前有效规范位置。证据中的原工作目录、命令、测试包和失败结果保留原样，明确作为历史执行记录；当前根的规则、结构、决策及本报告为修订后的事实源。

本次写入前的273个既有文件逐个与写入后核对路径、大小、SHA-256，均保持一致：[本次保全结果](evidence/P01-01/root-preservation.json)、[本次初始清单](evidence/P01-01/root-inventory-before.json)。前轮的[保全结果](evidence/P01-01/source-preservation.json)及清单亦作为原始证据保留。本轮未读取或更新 Product Delivery OS 状态、未引入其框架。

后端规划约束固定为基础包 `com.pet.platform`，源码目录 `apps/backend/src/main/java/com/pet/platform/`，启动类 `com.pet.platform.Application`。模块为 shared、platform、identity、customeridentity、attachment、audit、notification、modules，均位于该基础包下。本轮只更新这些规范，不创建源码目录或启动类。

## 2. 本机环境与目标环境

| 组件 | 本机事实 | 项目目标 / 本轮执行环境 |
| --- | --- | --- |
| Java | 默认 Azul Zulu 21.0.10+7；java_home 登记另列 OpenJDK21.0.1 | Temurin21.0.12.1+1，release21；临时下载并实际用于编译/测试 |
| Node.js | 22.23.2 | 24.21.0 LTS；临时工具链用于所有前端检查 |
| pnpm | 10.33.0 | 10.34.6；临时下载、校验和调用 |
| npm / Corepack | 10.9.8 / 0.34.6 | 仅记录可用性，不作为项目另一包管理器，未产生 npm/yarn 锁文件 |
| 系统 Maven | 3.9.15 | 项目使用 Wrapper3.3.4 only-script 下载 Maven3.9.16；不依赖全局 Maven |
| Docker | CLI29.5.2；daemon29.4.0，docker info 查询成功 | 本轮未启动/停止任何容器；目标镜像仅解析 manifest |
| Compose | 5.1.1 | 可用性确认；正式 infra/local 配置在P02创建 |
| OS / 架构 | macOS27.0.1 / arm64 | 本轮仅该平台运行；Linux/Windows NOT_EXECUTED |

Java 与 java_home 查询差异表明本机注册发现路径和当前 JAVA_HOME 使用路径不同；本轮没有修改任何全局配置。目标工具全部在临时目录运行；pnpm dlx 会使用用户缓存存放独立 CI 工具，未安装全局 CLI、未修改全局配置或其他项目文件。本次重新核对默认 Java、Node、pnpm、系统 Maven、Docker CLI/daemon及Compose，均与前轮记录相同，命令/退出码见 [当前环境核对](evidence/P01-01/current-root-checks.json)。前轮完整环境原始命令、工作目录、退出码和输出见 [environment.json](evidence/P01-01/environment.json)、[Docker daemon](evidence/P01-01/docker-daemon.json)。

## 3. 精确版本和来源

[版本矩阵](../development/VERSION-MATRIX.md) 逐项列出53条记录，覆盖全部指定后端、Web、小程序、必要测试/检查及驱动/BOM项，包含用途、精确版本、官方链接、2026-10-07查询日期、运行条件、兼容依据、验证等级及已知限制。

直接依赖精确，Spring Boot BOM/parent 管理项不重复覆盖；实际解析树与有效POM保留。选择 Java21、Boot4.0、Node24、pnpm10、React19.2、Vite7.3/插件5、TypeScript5.9、Ant Design6、ESLint10、Vitest4，避开全部最新版本叠加。WxJava采用正式4.8.0原始模块，不采用时间戳构建或额外SDK starter。

工具下载经过实际校验：[JDK/pnpm](evidence/P01-01/tool-integrity.json)、[Node官方SHA-256清单](evidence/P01-01/node-SHASUMS256.txt)、[Maven官方SHA-512及固定SHA-256](evidence/P01-01/maven-distribution-integrity.json)。Wrapper properties已加入 `distributionSha256Sum`，在 PATH 没有全局 Maven、空 Wrapper缓存下实际下载/执行成功。

## 4. 验证等级与边界

DOCUMENTED、RESOLVED、COMPILED、RUNTIME_VERIFIED、NOT_VERIFIED各自独立，不互相代替。JVM上下文/MockMvc、内存身份会话、SDK构造、React SSR、路由/缓存API、离线npm构建仅代表对应场景。

Java/SpringBoot/SaToken/springdoc与指定可选库的编译及基础加载已验证；JPA/Hibernate/驱动解析成功，但未建实体和数据库事务。Flyway load()不代表迁移通过。Testcontainers只构造容器对象，没有 start()。Web源码类型、构建、运行探针和静态检查通过；小程序源码类型和实际npm组件产物通过，微信运行时仍NOT_VERIFIED。

## 5. 实际命令、退出码和结果

以下兼容探针均为前轮真实执行记录，原始日志保持不变。本次修改项目位置和规划包名，不变更依赖、探针源码或工具链；没有重复执行兼容构建，不将前轮结果描述为本次重新运行。当前根环境核对及文档审计在本次实际执行。中性探针使用测试包 `probe`，不代表 `com.pet.platform.Application` 已创建或通过启动验收。

临时根：`/var/folders/5y/tt1690cx7k1gcq12252yf5rh0000gn/T/enterprise-scaffold-p01-01-plwt53cm`。后端工作目录：该根的 `backend-probe`；前端：`frontend-probe`；独立微信工具：`ci-probe`。以下短命令用于阅读；带全部临时绝对路径、完整参数和确切 cwd 的原始记录见 [all-command-records.json](evidence/P01-01/all-command-records.json)。`…`只是展示省略，不替代原始执行证据。

| 检查 | 命令摘要 | 退出码 | 结果/输出摘要 | 证据 |
| --- | --- | --- | --- | --- |
| wrapper-with-checksum | `./mvnw --version（无全局 mvn 的 PATH，空 Wrapper 缓存，SHA-256）` | 0 | PASS；工具分发及 checksum 通过 | [日志](evidence/P01-01/wrapper-with-checksum.log) |
| backend-resolve | `./mvnw … dependency:3.9.0:tree -Dverbose` | 0 | PASS；真实依赖解析成功，版本/覆盖可追踪 | [日志](evidence/P01-01/backend-resolve.log) |
| backend-effective-pom | `./mvnw … help:effective-pom` | 0 | PASS；BOM/parent 插件有效值确认 | [日志](evidence/P01-01/backend-effective-pom.log) |
| backend-tests | `./mvnw … test` | 0 | PASS；3 tests，0 failures/errors/skipped，Java testCompile 成功 | [日志](evidence/P01-01/backend-tests.log) |
| frontend-install-fixed | `pnpm install --reporter=append-only` | 0 | PASS；移除主 workspace CI 后 strict peer 安装成功 | [日志](evidence/P01-01/frontend-install-fixed.log) |
| frontend-lock-replay-r2 | `pnpm install --frozen-lockfile` | 0 | PASS；单份 workspace 锁文件复现成功 | [日志](evidence/P01-01/frontend-lock-replay-r2.log) |
| web-types-final | `pnpm exec tsc -p web/tsconfig.json` | 0 | PASS；strict 源码类型检查通过；skipLibCheck=true | [日志](evidence/P01-01/web-types-final.log) |
| frontend-lint-final | `pnpm exec eslint web/src mini/src --max-warnings=0` | 0 | PASS；0 errors、0 warnings | [日志](evidence/P01-01/frontend-lint-final.log) |
| web-build-final | `pnpm --dir web exec vite build` | 0 | PASS；3255 modules 构建成功；存在单 chunk >500kB 提示 | [日志](evidence/P01-01/web-build-final.log) |
| web-runtime-final | `pnpm exec vitest run web/src/compatibility.test.tsx --environment node` | 0 | PASS；2 tests 通过：Form/Button SSR；Router/Query/Zustand API | [日志](evidence/P01-01/web-runtime-final.log) |
| mini-types-baseline | `pnpm exec tsc -p mini/tsconfig.json` | 0 | PASS；原生 API/TDesign 使用类型通过；strict=true/skipLibCheck=true | [日志](evidence/P01-01/mini-types-baseline.log) |
| mini-npm-pack-api-final | `node ci-probe/pack-api.cjs` | 0 | PASS；官方 packNpmManually：miniProgramPackNum=1，warnList=[]；1177个组件文件实存 | [日志](evidence/P01-01/mini-npm-pack-api-final.log) |
| openapi-types-r2 | `pnpm exec openapi-typescript contract.json -o contract.generated.ts` | 0 | PASS；中性契约类型真实生成 | [日志](evidence/P01-01/openapi-types-r2.log) |
| openapi-types-compile-r2 | `pnpm exec tsc --noEmit --strict … contract.generated.ts` | 0 | PASS；生成类型编译成功 | [日志](evidence/P01-01/openapi-types-compile-r2.log) |

官方 PostgreSQL17.11-bookworm、Redis8.2.10-bookworm、RabbitMQ4.3.6-management 的manifest解析成功，含Linux arm64/amd64及精确多架构digest：[infra-images.json](evidence/P01-01/infra-images.json)。这属于RESOLVED，未拉取/启动镜像，不能标为RUNTIME_VERIFIED。

## 6. 失败尝试与最小调整

1. 主workspace安装miniprogram-ci2.1.31时，内部`@babel/eslint-parser7.22.10`的ESLint7/8 peer与项目ESLint10冲突，退出1。官方2.1.48仍有该peer；本轮将CI移出主依赖，作为精确版本的独立pnpm dlx工具。本次其隔离peer解析为ESLint8.57.1（EOL），不作为项目静态检查标准或默认初始化依赖；主workspace严格peer安装与frozen lockfile通过，没有关闭peer检查。
2. 首次驱动脚本把`--store-dir`传给pnpm exec/dlx，命令因不适用选项退出1，尚未运行相应检查；修正为`--config.store-dir=...`后重新实际执行，所有失败日志保留。
3. Web全面第三方声明检查退出2：image的GroupPreviewConfig与picker的SinglePickerPanelProps报TS2430；TDesign全面声明检查退出2，superComponent泛型报TS2344。采用Vite官方模板的`strict=true + skipLibCheck=true`边界后，应用源码公开API使用检查通过。**第三方声明全面检查仍为FAIL，不宣称已修复**。这是明确的编译基线选择，不关闭应用strict检查，不替换库或覆盖传递类型。
4. 初次小程序类型探针使用了未发布的路径/错误属性形状；按实际npm包的`miniprogram_dist/button/type`及属性描述符类型修正后通过。组件运行时构建路径与源码类型路径分别记录。
5. Web首次lint因Refresh规则要求组件独立导出而出现1条warning；拆开临时入口与Probe组件后严格0warning通过。
6. 微信CLI有退出0但预期目录无产物的尝试，没有据此确认构建验收。改用官方packNpmManually API；首次断言路径多拼一层miniprogram_npm导致退出1，保存失败；最终将dist参数指向小程序根，API返回1个小程序包、0个普通包、无warning，1177个TDesign组件文件及hash实存：[产物清单](evidence/P01-01/mini-pack-output-manifest.json)。
7. 部分官方网页/Adoptium API的urllib请求403、技能参考错误路径404；官方网页通过web工具读取，JDK通过官方GitHub资产下载/校验，技能参考正确官方路径读取。未将失败下载标成成功：[查询尝试](evidence/P01-01/official-source-retrieval.json)、[查询说明](evidence/P01-01/source-query-notes.json)。

剩余已知限制：Web探针未做分包优化，有>500kB chunk提示；Java测试有Mockito动态agent和macOS Netty DNS系统回退警告；这些不被说成生产验证通过。RabbitMQ4.3社区维护计划截至2026-11-30，P10启用前复核；其他支持窗口在P02/P12复核。编译边界内没有阻碍P01-02的关键peer/框架集成冲突；若后续要求skipLibCheck=false，需先解决上游声明并重新验证。

## 7. 未执行项与原因

| 项目 | 结果 / 等级 | 原因及后续 |
| --- | --- | --- |
| PostgreSQL真实连接、迁移、JPA/事务/锁 | NOT_EXECUTED / NOT_VERIFIED | 本轮必要范围是版本组合，未启动服务；P03/P04真实PostgreSQL测试 |
| Redis会话持久性、失效和I/O | NOT_EXECUTED / NOT_VERIFIED | 本轮仅Bean初始化与隔离内存会话探针；P05真实Redis验证 |
| RabbitMQ投递、确认、重试 | NOT_EXECUTED / NOT_VERIFIED | 可选能力当前不启用；P10启用时执行 |
| Testcontainers容器运行 | NOT_EXECUTED / NOT_VERIFIED | 不为本轮依赖/编译验证启动不必要容器；后续真实集成执行 |
| Web真实浏览器、Cookie/CSRF/跨源、权限、租户 | NOT_EXECUTED / NOT_VERIFIED | 当前未实现这些机制；P04～P07对应任务验证 |
| 小程序DevTools/真机/预览上传 | NOT_EXECUTED / NOT_VERIFIED | 本轮只做离线依赖和类型，无正式项目或微信配置；P09 |
| WxJava真实微信登录/支付/验签回调 | NOT_EXECUTED / NOT_VERIFIED | 不创建真实凭证，不实现业务；P10按需处理 |
| Linux/Windows工具链与三端完整验收 | NOT_EXECUTED / NOT_VERIFIED | 本轮macOS探针；后续按目标部署环境验证 |
| 正式三端初始化、当前启动类创建/启动、业务开发、提交/发布/部署 | NOT_EXECUTED | 用户明确禁止；包名/位置仅作为规划记录，本轮未自动执行 |

这些未执行场景不作为P01-01完成证据，也不是当前需要用户提供真实账号/密钥的事项。

## 8. 已确定与未细化事项

已确定：当前根目录、固定基础包/源码路径/启动类、固定技术体系、精确组合/BOM管理、单Module/Wrapper、pnpm单锁文件、官方UI组件直接使用、共享OpenAPI纯类型、分端传输、身份和租户方向、编译边界、独立CI工具以及P01～P12路线。[决策记录](../development/DECISION-LOG.md)包括事实依据和限制。

未细化：模块依赖/事务；fieldErrors、错误表、schema；ID算法/金额精度/日期输入规则；Cookie/CSRF与Token有效期/名称；租户执行路径及管理例外；文件生命周期；Web文件路由插件是否需要；小程序精确目标基础库/DevTools版本。由P01-02冻结，不把概要当详细接口已实现。

## 9. 新增/修改文件

在当前根目录新增以下10个指定产物，内容由前轮文档修订位置及包名；没有覆盖或移动任何既有文件：

- `AGENTS.md`
- `README.md`
- `docs/product/SCAFFOLD-SCOPE.md`
- `docs/architecture/TECHNICAL-BASELINE.md`
- `docs/architecture/PROJECT-STRUCTURE.md`
- `docs/contracts/CORE-CONTRACTS.md`
- `docs/development/VERSION-MATRIX.md`
- `docs/development/ROADMAP.md`
- `docs/development/DECISION-LOG.md`
- `docs/testing/P01-01-VERIFICATION.md`

另在`docs/testing/evidence/P01-01/`新增本轮日志、元数据、依赖树、有效POM、哈希、测试XML、命令记录及归档探针；不创建正式apps/packages/templates/scripts/infra空目录。[探针归档](evidence/P01-01/temporary-probes.tar.gz)与[复现说明](evidence/P01-01/PROBE-REPRODUCTION.md)只供证据核查。

## 10. G01～G13 完成条件

| 条件 | 结果 | 依据 |
| --- | --- | --- |
| G01 位置/已有成果检查 | PASS | 当前根重新检查、Git128、冲突检查、273个既有文件SHA一致、用户最新根目录指令 |
| G02 固定技术方向完整 | PASS | AGENTS、技术基线、范围，所有固定技术与排除项记录 |
| G03 主要组件精确版本 | PASS | 53条矩阵，BOM实际解析，前端直接精确版本 |
| G04 来源/兼容可追踪 | PASS | 官方URL/查询日/peer/engine，依赖树、有效POM、manifest、实际测试 |
| G05 必要兼容检查真实执行 | PASS | Java3项、Web2项、源码类型/构建/lint、mini离线构建、冻结安装；失败与未执行如实记录 |
| G06 无阻碍下一步的关键版本冲突 | PASS | 主peer及框架运行通过；标准源码编译边界与独立CI工具明确；不保证vendor声明全面通过 |
| G07 规划工程结构明确 | PASS | PROJECT-STRUCTURE区分规划/实际；当前根、单Module、com.pet.platform、源码路径/启动类及8个模块明确 |
| G08 核心协议概要明确 | PASS | 响应、HTTP/错误、分页、类型、身份、租户、OpenAPI事实源 |
| G09 路线P01～P12 | PASS | 每阶段目标/任务/依赖/交付/条件/验证/状态完整 |
| G10 文档完整/引用有效 | PASS | 10个指定文件齐全、53条版本记录、本地文档引用全部有效，详见最终审计 |
| G11 无提前正式业务实现 | PASS | 本轮仅新增规则/docs，既有ui保留；apps/packages/templates/scripts/infra未创建，测试源码仅临时/证据归档 |
| G12 未引入Product Delivery OS | PASS | 未安装/调用/创建框架或状态文件，未读取/更新其状态 |
| G13 未写入真实秘密 | PASS | 不配置真实微信/账号/密钥；目标文档和证据范围审计 |

本次文档审计命令：`python3 docs/testing/evidence/P01-01/audit-reproduction.py`，cwd为 `/Users/kimwell/work/pet-platform`，退出0；检查10个指定文件及新增证据内全部本地引用、精确版本/发布时间、273个既有文件SHA、当前根/固定包名一致、无正式工程或额外脚手架目录、秘密标识及探针归档边界：[本次审计](evidence/P01-01/document-audit.json)。前次审计另存为 `document-audit-prior-location.json`，只代表前次位置，不能替代本次检查。[位置修订说明](evidence/P01-01/ROOT-LOCATION-AMENDMENT.md)记录复用边界；所有当前文档和证据SHA-256见 [证据清单](evidence/P01-01/EVIDENCE-MANIFEST.json)。

本次审计首次退出1：链接检查早于生成其自身结果、SHA清单和保全结果，3个引用当时尚未存在。已修正执行顺序，先生成输出再检查全部引用；首次失败原样保存在 [首轮审计](evidence/P01-01/document-audit-first-attempt.json)，最终退出码以本次审计结果为准。此失败属于文档检查顺序，不改写版本探针结果。

## 11. 最终状态与下一任务

P01-01 最终 **COMPLETE**：G01～G13均PASS，必要兼容验证与文档审计实际通过；第三方声明全面检查的FAIL及未执行外部运行场景仍保留，不因任务完成改写为PASS。P01整体保持 **IN_PROGRESS**，P01-02和P02均 **NOT_STARTED**。

下一任务暂定 **P01-02：核心架构与三端契约详细冻结**。前置条件：P01-01最终条件通过、矩阵/证据可追踪、用户明确启动该任务；本轮不自动执行。P02正式初始化要等P01完成及对应任务授权。
