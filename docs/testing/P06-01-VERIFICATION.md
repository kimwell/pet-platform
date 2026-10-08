# P06-01 Web 应用壳、请求与同源会话基础

2026-10-08；**P06-01 COMPLETE，P06整体 IN_PROGRESS，G01～G14 PASS**。P05 COMPLETE 以 [总验收](P05-ACCEPTANCE.md#2026-10-08-真实微信通过后的阶段验收)为依据。421项后端通过是既有完整基线，本轮未重跑全套；真实浏览器与正式后端联调已实际执行。不使用 Product Delivery OS，不自动下一任务，不提交、推送或部署。

## 编码前的实际映射（G01）

已核对 AUTHENTICATION、IDENTITY、生成类型及两个正式 AuthenticationController；路径采用 PROJECT-STRUCTURE 冻结前缀。

| 空间 | 页面 | 实际 HTTP | 输入 / data / 语义 |
| --- | --- | --- | --- |
| STAFF | /admin/login | GET /api/admin/auth/csrf | CsrfResult；独立预会话或当前设备 |
| STAFF | /admin/login | POST /api/admin/auth/login | LoginInput：tenantCode/loginName/password；返回 CurrentIdentity，仅 Set-Cookie |
| STAFF | /admin | GET /api/admin/auth/me | CurrentIdentity；权威重载，401 与依赖故障区分 |
| STAFF | /admin 的退出 | POST /api/admin/auth/logout | 无 Body；data:null；仅当前设备，重复无效401 |
| PLATFORM | /platform/login | GET /api/platform/auth/csrf | 独立 CsrfResult，不共享 STAFF |
| PLATFORM | /platform/login | POST /api/platform/auth/login | PlatformLoginInput：loginName/password；PlatformCurrentIdentity，仅 Set-Cookie |
| PLATFORM | /platform | GET /api/platform/auth/me | PlatformCurrentIdentity；tenantId/dataScope 为 null |
| PLATFORM | /platform 的退出 | POST /api/platform/auth/logout | 无 Body；data:null；platform:session:manage；STAFF 保留 |

local Cookie 为 pet_dev_staff_sid/pre（Path=/api/admin/）、pet_dev_platform_sid/pre（Path=/api/platform/）；生产使用冻结 __Secure 名称。前端不读取 Cookie。写请求使用 GET csrf 返回的 X-CSRF-Token，Origin/Referer 由浏览器真实来源提供，Vite changeOrigin=false 保留；固定 PET_PUBLIC_ORIGIN 必须匹配浏览器。登录后重新 GET csrf。

IdentityNames 使用 Java strip 后 ASCII 小写；账号/租户编码仅允许已登记 ASCII。密码保持原输入。401 LOGIN_FAILED 不触发受保护会话失效；403 CSRF_INVALID 与 PERMISSION_DENIED 分开；429 使用正整数 Retry-After；503 不退出。没有租户名称读取接口，产品只能展示真实 me.tenantId，不能把登录草稿当租户身份事实。

历史 ui 为宠物寄养/微信视觉资产，本轮管理壳没有已确认复用用途，保留原位。初始工作区与保护摘要见 [baseline](evidence/P06-01/baseline.json)。

## 实际页面与实现方式

| 路由 | 访问条件 | 本轮功能 |
| --- | --- | --- |
| `/` | 公开，不主动查询全部身份 | 系统入口；员工、平台真实入口 |
| `/admin/login` | STAFF公开登录；已有身份时转合法返回路径 | 租户编码、账号、密码，Ant Design Form |
| `/platform/login` | PLATFORM公开登录；已有身份时转合法返回路径 | 账号、密码，独立空间 |
| `/admin` | STAFF beforeLoad完成身份判断 | 当前真实员工名称、身份空间、tenantId、刷新、菜单折叠、当前退出 |
| `/platform` | PLATFORM beforeLoad完成身份判断 | 当前真实平台名称、身份空间、刷新、菜单折叠、当前退出 |
| 其他路径 | NotFound | 中文404与可用系统入口 |

Provider为中文ConfigProvider、QueryClientProvider和TanStack Router。没有message/modal的上下文调用需求，保持不引入无用途Ant Design App；错误采用内联Alert/Result。布局直接使用Layout/Menu/Breadcrumb/Dropdown/Avatar/Button；登录和布局懒加载。不存在员工/角色/租户管理、企业Table、业务Dashboard、假统计、微信Web登录或小程序页面。

当前身份只存在Query缓存；Zustand只保存两个空间各自菜单折叠，未持久化。没有tenantName读取契约，所以员工首页展示真实tenantId。强制改密状态仅给出提示，完整改密流程尚未实现，不声称个人安全中心完成。

请求客户端默认`/api`、15秒超时、same-origin credentials、no-store和redirect:error；处理Query编码、JSON、FormData透传、AbortSignal、响应体等待超时和标准信封。成功返回data，允许data:null；traceId经ApiError或受限回调保留。HTTP业务错误、网络、取消、超时、协议格式错误分开，HTML响应不会整段显示给用户。地址先校验再取CSRF，拒绝绝对URL、协议相对地址、越界路径和秘密Query字段；空间客户端只能访问自己的API前缀。没有下载接口联调；后续独立原始Response客户端需另行定义，不能把二进制响应套入JSON信封。

登录名/租户编码按实际Java strip与ASCII小写规则规范化，密码不trim或转换。Form支持label、autocomplete及键盘提交，同步提交锁与Mutation状态阻止重复提交。Mutation变量为undefined，敏感草稿只在Form及短期函数局部，成功/失败清密码、成功/离开清Form；不提供记住密码。字段错误映射已知字段，其他安全字段消息置于顶层；认证失败使用后端安全中文，网络故障独立提示，429按实际正整数Retry-After提示。登录结果不会直接构造用户，必须重新GET me。

## Cookie、CSRF、Query和代际

两种Cookie允许同浏览器并存，前端不读取或保存Token/Cookie。CSRF只通过对应GET获取，在内存按空间独立保存；同空间并发合并，登录前带到POST、成功后重新取，当前写请求携带、退出后清理。到期重新获取；CSRF_INVALID只清当前CSRF并提示原操作失败，PERMISSION_DENIED保留凭据，写Mutation默认不重试、不自动重放。

Query Key的第一项包含principalType/sessionEpoch；受保护模块Key另含真实tenantId/principalId/sessionId/authorizationVersion/Store与范围。不含Token、密码。me是staleTime:0/gcTime:0/retry:false，在守卫、刷新及恢复时权威确认；其余Query只有网络/超时及502/503/504有限重试，401/403/422不重试，Mutation不重试。查询传递AbortSignal，没有本地缓存持久化。权限变化在me重新获取后清旧权限范围缓存，后端仍即时执行最终授权；没有实时推送撤权承诺。

每空间独立sessionEpoch、请求控制器、过渡锁与失效标记。同一空间登录/退出互斥，确认身份等待过渡结束；所有响应在回写前核对代际。统一受保护401只失效一次，登录LOGIN_FAILED不进入全局跳转。取消请求是优化，旧响应即使不能及时取消也不能覆盖新CSRF、写回旧me或清新身份。当前空间推进仅取消/移除自己的Query和临时折叠状态，不撤销或清除另一个空间。

退出真实调用后端：200/data:null或契约已失效401才进入相应登录；其他错误显示“退出未确认，服务端会话可能仍有效，请重试退出”，重新确认me并允许明确重试。Cookie仍由服务端处理，503不能被解释为已退出。会话启动分加载中、未登录、已登录、服务异常；预期网络/503/权限错误走可重试受控页，401才转未登录，不以React渲染后检查替代beforeLoad。

返回目标只允许当前已实现的同空间合法首页，保留安全search；拒绝外站、协议相对、登录循环、另一空间、hash、秘密参数及嵌套跳转。路由上下文与身份变化会invalidate重算。未来新增受保护路由必须同步扩展合法目标校验。

## 实际检查命令

以下工作目录均为`/Users/kimwell/work/pet-platform`，使用隔离冻结Node24.21.0、pnpm10.34.6和Temurin21.0.12.1+1，无全局工具配置修改。每个JSON记录命令、退出码、摘要、日志和时长。

| 命令 | 退出码 | 实际结果与证据 |
| --- | --- | --- |
| `pnpm --filter @pet/admin-web typecheck` | 0 | [typecheck-final](evidence/P06-01/typecheck-final.json) |
| `pnpm --filter @pet/admin-web lint` | 0 | [lint-final](evidence/P06-01/lint-final.json) |
| `pnpm --filter @pet/admin-web test` | 0 | 4文件、77项通过、0失败；[test-strengthened](evidence/P06-01/test-strengthened.json) |
| `pnpm --filter @pet/admin-web build` | 0 | 编译与生产构建；[最终构建](evidence/P06-01/build-responsive.json) |
| `pnpm contracts:check` | 0 | 10项导出/模型检查，生成产物一致及类型通过；[contracts-check](evidence/P06-01/contracts-check.json) |
| `pnpm check:repo` | 0 | 结构、冻结依赖、单锁、生产特征与小程序结构；[repo-check](evidence/P06-01/repo-check.json) |
| `git diff --check`及文档/保全审计 | 0 | [final-audit](evidence/P06-01/final-audit.json) |

后端代码未修改；上述10项不等于全套421项回归。77项是受控技术测试，不是后端/浏览器业务验收。覆盖data:null、标准/字段错误、非JSON、trace、网络、取消/响应体超时、同源限制、名称/密码、CSRF隔离与过期、LOGIN_FAILED、403区分、并发401、旧响应、退出失败与单空间清理、权限变化、合法返回、守卫服务故障/未登录、官方Form/密码存储边界。实际调用AuthService登录/退出验证A退出后B登录、旧me成功、旧401、CSRF跨轮换、双空间并发；没有用技术夹具声称真实链路通过。

`build-final`保留修正窄屏前构建，最终样式构建见`build-responsive`。入口JS约711.10kB（gzip234.59kB），仍存在默认500kB警告；历史P02入口621.32kB（gzip206.36kB）仅作旧基线比较。登录/布局/服务错误已正常懒加载，未提高警告阈值；没有全面bundle分析或生产性能验收。

## 正式后端与真实浏览器联调

[环境记录](evidence/P06-01/environment.json)保存冻结PostgreSQL17.11/Redis8.2.10专用tmpfs容器、正式三个角色SQL、受限runtime（非superuser/非BYPASSRLS）、正式V1～V4迁移、`scripts/backend-identity.sh bootstrap --password-stdin`与`platform-bootstrap --password-stdin`。两种测试账号仅在隔离数据库初始化，没有修改日常账号。生产JAR使用local配置，readiness 200 UP，无测试profile/Gateway；微信关闭，本轮不续做微信任务。

实际浏览器为Codex In-app Browser，Origin`http://127.0.0.1:5173`，经Vite同源代理保留Origin/Cookie Path和渠道。可选本地透明观察器仅记录method/path/status、CSRF头存在布尔值、trace与Cookie名/属性，未改请求/响应/后端CORS；没有保存Body、Cookie值、CSRF值、密码、Token或HAR。正式POST的Origin及CSRF、Cookie Path/HttpOnly/SameSite可见[HTTP白名单证据](evidence/P06-01/http-observations.json)。

| 场景 | 实际结果 | 证据 |
| --- | --- | --- |
| STAFF登录/me/刷新/当前退出 | PASS；真实身份、tenantId，退出200后对应登录 | [首轮](evidence/P06-01/browser-results-first.json)、[最终复验](evidence/P06-01/browser-results-final.json) |
| PLATFORM登录/me/刷新/退出 | PASS；独立真实平台身份，退出200后对应登录 | 同上及HTTP记录 |
| 同浏览器双空间、员工退出后平台有效 | PASS；平台继续me200 | 最终复验/HTTP记录 |
| 直接访问保护路径、只恢复当前空间 | PASS；未登录转对应入口；独立员工访问没有平台API请求 | [空间恢复专验](evidence/P06-01/space-only-browser.json) |
| 错误密码与重复Enter | PASS；LOGIN_FAILED安全中文、密码清空；两个Enter仅一条login POST | 首轮/HTTP记录 |
| Cookie/CSRF正常 | PASS；独立pre/sid、Path、每次POST有CSRF、登录轮换重新取 | HTTP白名单记录 |
| Redis真实故障、退出未确认、恢复重试 | PASS；暂停专用Redis导致实际logout/me503；保护路径与错误保留，恢复后原Cookie me200，无需重新登录 | 最终复验与pause/unpause命令记录 |
| 合法search与外站返回拒绝 | PASS；`/admin?tab=profile`保留，外站归一到员工首页 | 最终复验 |
| NotFound、受控系统错误页、控制台 | PASS；最终故障页与最终正常页dev.logs均空 | 最终复验/截图 |
| 窄屏主布局与折叠菜单 | PASS；修正Ant Design优先级，实际窄屏宽度不溢出、折叠可用 | [窄屏复验](evidence/P06-01/browser-responsive.json) |

截图只在清空登录表单或登录完成后获取：[空白登录页](evidence/P06-01/login-final.png)、[员工窄屏壳](evidence/P06-01/staff-shell-responsive.png)、[员工桌面壳](evidence/P06-01/staff-shell-desktop.png)、[平台壳](evidence/P06-01/platform-shell.png)、[最终故障页](evidence/P06-01/service-error-final.png)、[NotFound](evidence/P06-01/not-found.png)。修正前staff-shell.png与service-error.png保留作为首轮记录，不作为最终无缺陷截图。已实际视觉复核登录、故障和员工页面。

本轮临时Web/后端/观察器与两个专用容器已停止，端口5173/18086/18087关闭；0600技术凭据文件删除，账号随tmpfs结束，原有三个exited容器/卷保留。[初次清理](evidence/P06-01/cleanup.json)与[窄屏复验收尾](evidence/P06-01/cleanup-responsive.json)留证。长进程启动以readiness和RUNTIME_VERIFIED表示，不伪填进程退出码。

## 失败、修正与证据等级

首次隔离环境错误使用pg_isready时数据库尚未建立，正式角色命令退出2；[environment-first](evidence/P06-01/environment-first.json)保留，清理本轮专用容器后改为目标库真实SELECT1就绪，正式迁移/初始化通过。早期类型检查和测试失败保留；SSR路由在缺省returnTo时发生额外canonical redirect，修正为保留undefined，并按当前TanStack Router SSR结果验证。最初73项测试最终补强为77项，最终数量取test-strengthened。

首次真实503走React错误边界，产生error/warn；首轮browser-results-first与service-error保留。修正为beforeLoad受控sessionError，最终重复真实Redis故障时无新增控制台错误。UI工具默认等待曾在后端故障超时前结束，随后实际页面/HTTP确认；临时浏览器页被工具回收后另建页完成NotFound/返回校验，未把工具等待或页回收当产品PASS。首次cleanup的进程路径校验失败保留，未误停其他进程；修正路径匹配后只停止本轮资源。

截图复核又发现窄屏Ant Design侧栏优先级覆盖了项目flex-direction，已提高目标选择器精度、限定内容border-box/min-width，并另建隔离后端与真实窄屏浏览器复验；保留旧截图和首次清理记录，不覆盖历史失败。

窄屏环境重建首次因contracts:check清理target、正式初始化脚本缺少生产JAR返回2；[记录](evidence/P06-01/environment-responsive-first.json)保留。恢复本轮同一既有生产JAR到本地产物目录后正式迁移/初始化通过，没有改源码或重复全套回归。首次窄屏修正还需覆盖Ant Design为横向布局设置的子Layout width:0，最终390像素展开/折叠均无溢出、桌面1280断点正常。末次审计初次在写出自指证据前检查其链接返回1，先生成证据再核对，原记录保留为final-audit-first.json。

| 等级 | 本轮事实 |
| --- | --- |
| DOCUMENTED | 接口/路由映射、调用策略、边界、决策、路线与门禁同步 |
| RESOLVED | 生成类型与实际接口一致、冻结依赖不变、早期问题修正 |
| COMPILED | TypeScript/lint/77受控测试/生产构建/10契约检查通过 |
| RUNTIME_VERIFIED | 正式PG/Redis/初始化/生产JAR、真实浏览器既定场景与故障恢复 |
| NOT_VERIFIED / NOT_EXECUTED | 生产TLS/部署/跨源/可信反代、远程CI、多OS；完整安全中心/下载/业务页面不在范围；421全套本轮未执行 |

## G01～G14与文件范围

| 门禁 | 结果 | 依据 |
| --- | --- | --- |
| G01 契约和真实接口 | PASS | 编码前8接口映射、生成类型、P05总验收、contracts:check |
| G02 请求协议/取消/错误 | PASS | request层、受控测试、真实null/401/503 |
| G03 Cookie/CSRF空间隔离 | PASS | runtime双状态、真实Cookie属性/CSRF POST、并发测试 |
| G04 两类真实登录与me | PASS | 正式初始化、浏览器与HTTP记录 |
| G05 会话恢复区分故障 | PASS | 401保护跳转、真实Redis503与无需重登恢复 |
| G06 守卫与返回地址 | PASS | beforeLoad、受控路由测试、真实直达/search/外站校验 |
| G07 退出作用域/失败 | PASS | 员工退出平台有效、实际logout503/重试、已失效契约测试 |
| G08 并发与旧响应 | PASS | 77项中的实际AuthService受控竞态；真实双域并发基础 |
| G09 真实身份应用壳 | PASS | 真实me、官方组件、窄屏复验、无假业务 |
| G10 真实浏览器后端 | PASS | 专用正式环境、实际操作/HTTP/截图/最终空控制台 |
| G11 静态/测试/构建 | PASS | 所需命令退出0，500kB警告原样记录 |
| G12 无秘密持久化 | PASS | 密码/凭据扫描、源代码无存储/日志调用、Mutation无密码变量、白名单证据 |
| G13 既有成果保全 | PASS | 506文件SHA256对比，无UI/后端/契约/小程序/锁文件变化 |
| G14 文档一致 | PASS | 本报告及约定/本地/结构/路线/决策/矩阵、链接与diff审计 |

完整新增/修改路径见[changed-files](evidence/P06-01/changed-files.json)。代码集中在`apps/admin-web/src`的providers/router/guards/layout、features/auth/account、shared/api/auth/config；4个测试文件全部执行，其中本轮新增/修改3个。文档为README、PROJECT-STRUCTURE、WEB-STATE、WEB-PAGES、LOCAL-DEVELOPMENT、ROADMAP、DECISION-LOG、ACCEPTANCE-MATRIX及本报告/证据。没有修改依赖、pnpm-lock、Vite配置、后端、公开契约、小程序或ui；已有Vite代理足够，未改变CORS。初始工作区记录见baseline，最终变更与保全见final-audit。

## 当前限制与下一合法任务

当前仅已实现首页被返回目标允许；没有后续业务导航/按钮权限完整呈现、本人改密/全设备退出页面、管理CRUD、下载、跨Tab主动身份同步或实时撤权推送。Cookie生产安全/TLS/跨源/可信代理/部署、远程CI与多OS仍独立未验证；本地Browser PASS不能替代这些。入口包仍有500kB警告，生产性能没有验收。

**P06-01 COMPLETE；P06整体IN_PROGRESS。** 按[路线图](../development/ROADMAP.md#p06-01-当前结果与下一合法任务2026-10-08)，下一合法任务建议为**P06-02：Web权限呈现、强制改密与本人会话安全最小流程**，基于既有me/password/logout-all契约完善P06剩余范围。这是后续拆分建议，NOT_STARTED，需要下一次明确任务授权；不代表完整个人安全中心、P07管理API或业务页面已进入范围。本轮只报告，没有执行下一任务、提交、推送或部署。
