# P07-03 员工详情页与列表返回状态验证

日期：2026-10-09；工作目录 `/Users/kimwell/work/pet-platform`。用户明确授权本任务，直接在根目录实施，使用冻结依赖与现有工程；没有引入 Product Delivery OS，没有执行下一任务或提交、推送、发布、部署。

**P07-03 IN_PROGRESS；P07 IN_PROGRESS。** 页面实现、244项Web测试、41项确定性页面场景和43项正式浏览器场景通过；G13的规定 `pnpm contracts:check` 未通过，不能标本项 COMPLETE。P07-01/P07-02仍 COMPLETE。后端455项是P07-01历史基线，本轮后端未改，完整后端回归 NOT_EXECUTED；本轮契约检查实际注册10项、环境初始化 Errors=10、通过0项，不能借历史10项通过或生成文件未变替代当前结果。

可复核入口：[命令索引](evidence/P07-03/command-index.json)、[新增/修改文件清单](evidence/P07-03/file-manifest.json)、[最终审计](evidence/P07-03/final-audit.json)、[资源清理](evidence/P07-03/cleanup.json)。失败尝试和旧阶段证据均保留。

## 1. 真实依据与契约

已读取实际 AGENTS.md、EMPLOYEE-MANAGEMENT/API/PAGINATION/DATA-TYPES、WEB-LIST-PAGES/WEB-PAGES/WEB-STATE、P07-01/P07-02验证、P06-ACCEPTANCE及认证/授权/会话规范、ROADMAP/ACCEPTANCE-MATRIX/DECISION-LOG；核对正式 EmployeeController、查询服务/范围、EmployeeView、CurrentIdentity投影、生成operations、Web列表/路由/请求/SessionRuntime/Query Key和现有测试。进入本项前实际重跑Web基线：[6文件188项、退出0](evidence/P07-03/test-baseline.json)。报告与拥有实现的文件在本任务所依赖的读取契约上没有实质冲突。

| 事实 | 实际值与实现 |
| --- | --- |
| 页面 | `/admin/identity/users/$employeeId`，既有TanStack Router同级STAFF保护路由 |
| 正式接口 | `GET /api/admin/identity/users/{employeeId}`；不发送详情search给后端，后端拒绝详情query参数 |
| 权限 | `identity:user:detail`，与 `identity:user:list` 完全独立；PLATFORM不能借STAFF入口 |
| 生成类型 | `operations['getEmployee']['parameters']['path']['employeeId']`、`components['schemas']['EmployeeView']`、`SuccessEmployeeView['data']`；没有手写平行响应接口 |
| 六字段 | id/loginName/displayName/status/createdAt/updatedAt；EmployeeView六字段均必填、非null，成功信封data可null但详情将null视为协议错误 |
| 标量 | 小写UUID v4；ACTIVE/DISABLED；UTC固定3位毫秒输出，页面按既有Asia/Shanghai并标注 |
| 范围 | 后端按detail自己的TENANT/SELF/STORES授权，同一权限范围并集；list范围不扩detail |

只读公开六字段；未添加角色、门店、内部版本、最近登录、管理能力或审计轨迹；没有写入、启停、删除、重置、批量、导入导出、选择框、BaseDetail/BaseTable或配置驱动框架。后端、冻结迁移、生成契约、依赖、锁文件与ui保全。

## 2. 入口、查询与授权失效

列表只在有detail权限时追加官方Button“查看详情”，可键盘操作，没有整行隐式点击。详情 `navigation:false` 不制造无目标菜单；route/pageAccess/queryFn独立验证detail，只有list或只有PLATFORM不发送详情请求。只有detail的STAFF可在合法直接地址读取授权目标，不被list守卫拦截。

ID格式在页面与API入口校验；非法反馈中文且零请求，合法格式不证明存在或可访问。目标独立Query使用正式接口，无列表DTO、placeholder、预热或导航携带详情对象。AbortSignal传递到既有RequestClient/SessionRuntime；响应复核目标ID、六字段类型/非null、枚举及时间，协议异常保留合法traceId。

Query Key沿用authKeys.protected，包含STAFF、可信tenantId/principalId/sessionId、真实authorizationVersion/sessionEpoch、有效门店和逐权限dataScope、identity/employees/detail及employeeId，不含凭据。实际me已有完整逐权限grants；不从扁平权限集合推范围、不虚构版本。即使版本未变，既有完整事实比较也会推进代际、取消旧请求并清当前空间缓存。员工切换用独立key和目标重新挂载；离开后等待观察者卸载再取消/移除精确详情query，避免StrictMode探测误清。旧响应不能覆盖当前员工/身份。

详情staleTime30秒、gcTime5分钟，自身不先触发focus自动刷新，沿用P06保护路由、焦点与可见性重验。真实离线检查发现默认online模式会暂停刷新，已将详情设 `networkMode:'always'`，明确尝试传输后按既有GET重试策略显示失败；网络/超时/502～504最多两次自动重试（1秒/2秒），4xx/取消/协议错误不自动重试。没有跨标签即时广播、撤权推送或“已完成读取立刻被撤回”的承诺。

| 页面状态 | 行为 |
| --- | --- |
| 首次加载 | 官方Spin、中文状态，无旧详情或假数据 |
| 成功 | 官方Descriptions六字段与Tag中文状态，复用displayText/displayInstant |
| 同身份同目标后台刷新 | 仍合法旧内容保留，明确“正在刷新，以下仍为上次成功结果” |
| 无权限 | route和query默认拒绝，无详情请求；当前身份等既有安全去向保留 |
| 401 | 复用按空间/代际合并的会话失效、取消、清缓存和登录返回，不重复全局通知 |
| 403 | 既有PERMISSION_DENIED/PASSWORD_CHANGE_REQUIRED触发重验；所有正式403停止展示旧详情并清数据，不把CSRF_INVALID当退出 |
| 404 | 统一“员工不存在或不可访问”，不存在、跨租户、范围外不分辨；清除旧详情数据 |
| 503/网络/超时/响应异常 | 页面ErrorNotice及定位编号、明确重试，不自动退出；已有合法数据标“刷新失败，以下仍为上次成功结果” |
| 查询取消 | 静默，不产生错误通知 |

401/403/404一出现即停止渲染旧详情，并移除当前query的数据，保留错误和trace供定位。普通错误只由页面展示一次；会话通知归既有会话层。长账号、姓名、UUID完整换行并可聚焦/title查看；时间标Asia/Shanghai（me未提供动态zoneId）。null是协议异常，不假造可空性；空字符串技术反例沿用“—”，与数据库实际约束分开。无未转义HTML。详情加载/切换/错误聚焦标题，返回列表聚焦列表标题；重试、返回、详情入口均官方Button。

## 3. 列表返回方案与边界

详情URL保存受限 `returnTo`，事实只来自当前规范化列表URL的keyword/status/page/pageSize/sortBy/sortOrder。复用P07-02的parseWebSearch/normalizeSearch/employeeHref；不建第二套列表状态、不存Zustand、不采纳Form未提交草稿、不用location.state任意地址跳转、不带详情对象或秘密。

原始返回路径必须恰为 `/admin/identity/users`，长度≤2048，拒绝外部、协议相对、其他空间/业务、反斜线、控制字符、hash及点段别名。仅六参数白名单，非法、未知或重复参数整组拒绝；外层重复returnTo也拒绝。合法前导零和默认值按既有规范化。登录返回白名单支持合法UUID详情并重验嵌套返回；既有全局2048字符上限保留，超长登录返回安全回当前身份。

“返回列表”使用确定目标，不用history.back：有list权限且合法返回则恢复它；无/非法返回则去默认员工列表；没有list权限则不展示列表导航，提供 `/admin` “返回当前身份”。返回前撤销list并重验，入口及时替换；原生后退仍经列表守卫。URL保存可承受详情刷新和原生前后退。返回时total变化/页越界复用P07-02的一次replace纠正，未另写循环。开发StrictMode可出现一次已取消的初始探测请求，实际纠正只完成一次，不能把取消探测计作重复成功或跳转循环。

本项只保证**已提交URL查询恢复**；滚动位置、未提交草稿和完整视觉位置恢复 NOT_VERIFIED/未实现。

## 4. 确定性测试与正式浏览器

Web现有Vitest新增56项，7文件**244项通过**，原188保留。覆盖权限不互授/查询guard、UUID/受限返回/重复/界限、正式生成目标请求/signal、统一404、响应/trace/时间/非null、完整scope key/不增版本变化/旧响应、取消/401/503/网络与实际RequestClient受控超时、重试策略。最终[typecheck](evidence/P07-03/typecheck-final-accepted.json)/[测试](evidence/P07-03/test-final-244.json)退出0。

[确定性页面41项](evidence/P07-03/browser-components.json)使用测试脚本中的网络替身，专门稳定验证加载/后台刷新、401/403/404清数据、503/网络/协议错误与恢复、旧响应/切目标、六字段/空文本/长文本、URL与Form草稿边界、刷新/历史/撤list/恶意返回/一次页纠正；pageErrors=0，17条预期负向资源错误单独记录。它是技术夹具，不替代真实认证/业务联调。

[正式浏览器43项](evidence/P07-03/browser-real.json)使用未修改的生产JAR/local、真实冻结PostgreSQL17.11/Redis8.2.10、独立tmpfs库、正式V1～V5迁移/受限nosuperuser+nobypassrls运行角色及正式STAFF认证。仅本轮两个隔离租户和技术验收身份，经正式bootstrap与显式employee-read-upgrade准备；凭据随机且只在忽略目录/进程中使用，已清理。55名目标、长文本与授权/门店变更由受控SQL准备，**不是员工创建/编辑/启停功能验收**。环境命令/退出见[environment.json](evidence/P07-03/environment.json)，目标准备见[data-preparation](evidence/P07-03/data-preparation.json)。

当前Docker Desktop默认socket及宿主机发布端口失败。正式联调用仅127.0.0.1的临时字节隧道经raw引擎docker exec直连真实容器TCP，原样转发PG/Redis字节，不解释、替换SQL/认证/协议或响应，没有Mock/Test Gateway/内存数据库。该恢复方法只能支持本轮隔离浏览器，**没有证明标准Docker/Testcontainers正常**；后者仍在G13失败，未擅自重启既有Docker或其他项目。

| 正式场景 | 结果与证据 |
| --- | --- |
| A list+detail | 真实筛选/排序/page2后键盘进入；六字段逐一与正式详情API比对；返回六查询、详情刷新后返回、原生后退/前进通过 |
| B list-only | 列表没有详情入口；直接详情由独立权限保护且零详情请求 |
| C detail-only | 合法直达授权目标成功，无列表菜单/返回；当前身份去向可用；返回前撤list重验替换入口 |
| D 详情范围 | TENANT、SELF本人、STORES有效门店、STORES+SELF并集（本人无门店仍可读）；列表TENANT而详情SELF范围外、不存在、跨租户均同404 |
| E 撤权/身份 | 打开详情撤有效门店→404清旧数据；撤detail→403重验清缓存；其他标签正式退出后重验失效；重新登录sessionId变化不复用；换租户清旧身份；PLATFORM不能进入STAFF详情 |
| E 故障/目标 | 实际浏览器离线与恢复键盘重试；只暂停本轮Redis产生实际503、保留会话/trace，恢复无需重登；CDP延迟下实际接口快速切目标、旧请求被取消、只显示新员工 |
| 返回页越界 | 隔离SQL改变状态导致已提交筛选total=0，返回原page2只replace到1，无重复纠正/循环；不计作员工启停功能 |
| F 页面质量 | 1280×900与390×844；真实长账号64字符/长姓名/UUID完整；390px document client=scroll=390；键盘详情/返回/重试及标题焦点通过 |
| F 控制台 | pageErrors=0，新增JS/组件错误0；保留39条预期401/403/404/503/离线等资源错误（含favicon），不声称资源控制台为空 |

截图已目视检查：[桌面](evidence/P07-03/detail-desktop.png)、[390px](evidence/P07-03/detail-390.png)、[Redis刷新失败](evidence/P07-03/real-redis-failure.png)。无Cookie/Token/密码或秘密截屏；没有保存HAR/认证头。

## 5. 规定命令、数量与警告

所有检查cwd为 `/Users/kimwell/work/pet-platform`；runner仅对子进程采用冻结Java21.0.12.1、Node24.21.0和pnpm10.34.6，未改全局配置。开始结束时间、精确工具路径、命令及退出码见逐命令JSON和命令索引。正式服务启动进程记STARTED/RUNTIME_VERIFIED，不虚构成功退出，结束核对归cleanup。

| 检查 | 实际命令 | 退出/实际数量 | 最终证据 |
| --- | --- | --- | --- |
| 类型 | `pnpm --filter @pet/admin-web typecheck` | 0 | [typecheck-final-accepted](evidence/P07-03/typecheck-final-accepted.json) |
| lint | `pnpm --filter @pet/admin-web lint` | 0 | [lint-final-accepted](evidence/P07-03/lint-final-accepted.json) |
| Web测试 | `pnpm --filter @pet/admin-web test` | 0；7文件244项，0失败 | [test-final-244](evidence/P07-03/test-final-244.json) |
| 构建 | `pnpm --filter @pet/admin-web build` | 0；1706模块 | [build-final-accepted](evidence/P07-03/build-final-accepted.json) |
| 契约默认 | `pnpm contracts:check` | 1；默认socket持续无响应，仅终止本轮测试子JVM；没有完成的测试体 | [contracts-check](evidence/P07-03/contracts-check.json)、[中止](evidence/P07-03/contracts-interruption.json) |
| 契约恢复1 | 同命令，子进程DOCKER_HOST指raw引擎 | 1；10项注册、0通过、10 Errors/0 Failures/0 Skipped，Ryuk socket挂载初始化失败 | [contracts-raw-attempt](evidence/P07-03/contracts-raw-attempt.json)、[原始JUnit报告](evidence/P07-03/contracts-raw-failed-reports) |
| 契约恢复2 | 同命令，raw加标准VM socket override | 1；卡于Ryuk启动/宿主机端口转发，终止本轮子JVM，退出143；没有完成的测试体 | [contracts-raw-socket](evidence/P07-03/contracts-raw-socket.json)、[中止](evidence/P07-03/contracts-socket-interruption.json) |
| 仓库 | `pnpm check:repo` | 0 | [check-repo-final](evidence/P07-03/check-repo-final.json) |
| diff | `git diff --check` | 0 | [diff-check-final](evidence/P07-03/diff-check-final.json) |
| 正式浏览器 | `node docs/testing/evidence/P07-03/browser-real.mjs` | 0；43项 | [browser-real-second](evidence/P07-03/browser-real-second.json) |
| 确定性页面 | `node docs/testing/evidence/P07-03/browser-components.mjs` | 0；41项 | [browser-components-final](evidence/P07-03/browser-components-final.json) |

最终build入口720.72kB/gzip238.08kB，详情chunk3.42kB/gzip1.67kB，员工共享12.77/5.26，列表264.72/86.31，SessionLayout95.33/30.08，CSS6.62/2.12。保留500kB警告，未提高阈值或借机拆包。不是生产部署/性能验收。

## 6. 历史失败、修复与保全

原始Web test-first有2失败/241通过；typecheck-second退出2，原因是it.each把权限数组当多参数展开，修正测试参数为对象后243项通过，补超时测试后最终244项通过。lint各轮退出0；不删除失败日志。确定性页面前3轮因AntD中文双字自动间距与定位名称差异等待失败，生产重试按钮补显式aria-label；观察者卸载清理补StrictMode保护。第4轮技术me替身错误返回成功null，修正为正式401形态；第5/6轮及页纠正探针对API query与规范化URL的断言/异步等待不准确，改为观察实际完成page1后验证一次纠正，第7轮与最终41项通过，未改P07-02纠正算法。正式浏览器首轮离线发现真实Query暂停问题，修复详情networkMode后第二轮43项通过。

专用环境创建的run/start客户端45秒超时与引擎HTTP start退出28仍保留；inspect确认本轮容器实际running后继续。初期bash字节中继在Flyway5迁移已应用后的连接关闭处超时，换为透明Perl Socket字节转发后正式migration/bootstrap均退出0。各environment-first～sixth、resume-first及failure文件保留，重建只触及本轮专用空tmpfs资源，未重启Docker/已有服务。契约三次失败单列，不当业务错误；只终止明确属于本轮的测试子JVM，未降低检查条件。

收尾恢复本轮授权、门店关联和技术目标状态，核对list/detail各一条TENANT、操作者门店1、DISABLED13；只停止本轮Web/后端/隧道与两个tmpfs容器，清本轮Ryuk/临时秘密输入；四端口18092/5175/18094/18095关闭。既有11个容器保持原状态，原卷子集保全，没有删除命名卷或真实/历史数据。contracts的clean删除临时target产物后已恢复同一原JAR，SHA256与正式联调副本一致。新/修改127个当时公开文件做本轮已知临时凭据精确值扫描、0匹配，截图另目视核对，见[秘密值扫描](evidence/P07-03/secret-value-scan.json)和cleanup。

起始Git工作区clean，4688个基线文件；最终审计逐文件比较，既有ui、后端、冻结迁移、生成契约、POM/package/锁/版本矩阵、小程序、P06与P07-01/02历史证据保持。第一次审计将HEAD原始内容摘要与工作树逐字节全等比较失败，定位为mvnw.cmd及三份旧日志/XML的既有CRLF文本规范化差异；逐项核对Git规范化blob与HEAD一致后按Git内容比较并另存原始摘要差异，没有改这些旧文件。原失败元数据保留。没有自动提交。

## 7. 新增、修改文件

| 类别 | 精确文件（模块路径相对根目录） |
| --- | --- |
| 新增Web | `apps/admin-web/src/features/identity/users/pages/EmployeeDetailPage.tsx`；`queries/detailSearch.ts`；`queries/detail.test.ts`（后两项同模块） |
| 修改Web | `app/layout/SessionLayout.tsx`、`app/router/pageAccess.ts`、`app/router/router.ts`；员工 `api/employees.ts`、`pages/EmployeeListPage.tsx`、`queries/employees.ts`；`shared/auth/returnTo.ts`、`shared/format.ts`、`styles/global.css`（均位于apps/admin-web/src） |
| 新增规范/报告 | `docs/conventions/WEB-DETAIL-PAGES.md`、本报告 |
| 更新现有文档 | README.md、EMPLOYEE-MANAGEMENT.md、WEB-PAGES.md、WEB-STATE.md、WEB-LIST-PAGES.md、ROADMAP.md、DECISION-LOG.md、ACCEPTANCE-MATRIX.md；按各自现有目录更新，不建立平行规范 |
| 新增证据 | `docs/testing/evidence/P07-03/`：脚本、所有命令/失败/观察、截图、清理、索引及审计；技术夹具只在测试/验收脚本，未进入生产源码/构建 |

文件清单包含全部新增/修改路径、状态、字节及SHA256；索引/清单/审计自身列出但不递归计算自身摘要。

## 8. G01～G14逐项门禁

| Gate | 结果 | 验证等级与逐项证据 |
| --- | --- | --- |
| G01 正式契约/未扩字段 | PASS | RESOLVED + RUNTIME_VERIFIED；正式Controller/生成EmployeeView，浏览器六字段比对；契约自动导出失败独立归G13 |
| G02 请求层/生成类型 | PASS | COMPILED + RUNTIME_VERIFIED；getEmployee生成path/data、Signal、正式接口与trace测试/浏览器 |
| G03 独立权限/导航直达 | PASS | RUNTIME_VERIFIED；正式list-only/detail-only/PLATFORM及41项技术页面，不互授、无权零请求 |
| G04 目标/返回安全 | PASS | COMPILED + RUNTIME_VERIFIED；UUID v4、路径六参数白名单、长度/重复/外部/协议相对/跨空间拒绝，56项新增测试及正式直达 |
| G05 六字段/加载/长文本 | PASS | RUNTIME_VERIFIED（真实+技术分记）；正式API六字段和desktop/390截图；技术延迟/空文本/协议null |
| G06 统一404 | PASS | RUNTIME_VERIFIED；不存在/跨租户/detail范围外同中文、trace且清旧详情，后端实际404 |
| G07 取消/旧响应/目标 | PASS | RUNTIME_VERIFIED + COMPILED；真实延迟目标切换/中止，确定性迟到响应/Signal，无占位或取消通知 |
| G08 身份/租户/范围清理 | PASS | RUNTIME_VERIFIED；SELF/STORES/未增版本门店撤销/detail撤权、换租户/重登清缓存，完整authKeys/epoch测试 |
| G09 401/403/503/网络/重试 | PASS | RUNTIME_VERIFIED（真实）；退出401、撤权403、实际Redis503/离线恢复；技术超时/响应格式、trace/无重复通知及terminal清数据 |
| G10 URL往返/刷新/历史 | PASS | RUNTIME_VERIFIED；正式已提交六查询、详情刷新、原生后退前进、无/非法返回、撤list安全去向/总数变零一次纠正；草稿明确不恢复 |
| G11 正式认证范围/权限组合 | PASS | RUNTIME_VERIFIED（真实）；受限PG/Redis/正式STAFF，TENANT/SELF/STORES/同detail并集、list与detail不互扩，43项结果 |
| G12 桌面/390px/键盘焦点 | PASS | RUNTIME_VERIFIED；截图及width=scroll=390，键盘进入/返回/重试、标题焦点、0新增JS/组件错误 |
| G13 规定检查/秘密保全 | **PARTIAL（环境阻塞）** | Web类型/lint/244测试/build、repo/diff退出0；资源/秘密/ui/后端等保全PASS。contracts:check三次退出1，10环境Errors且0通过，标准Docker/Testcontainers NOT_VERIFIED；不得标PASS |
| G14 文档/清单/命令/证据 | PASS | DOCUMENTED + RESOLVED；本报告/现有规范/路线、精确清单/命令/原始失败/JUnit/截图/审计/清理齐备，状态与未执行项明确 |

DOCUMENTED不替代COMPILED，生成文件不变不替代本轮契约导出，确定性夹具不替代真实正式认证；本项仍IN_PROGRESS。

## 9. 未执行项与下一合法建议

当前唯一未关闭的本项规定门禁是G13标准Docker/Testcontainers契约检查，恢复前保持IN_PROGRESS；既有资源保全要求下没有重启全局Docker/已有进程或绕过Testcontainers。下一建议仅为**P07-03续验：恢复标准连接后重跑contracts:check、关闭G13并重新汇总门禁**，不自动执行新阶段。通过后仍需重读实际路线与P07剩余条件，再提出下一任务；不预设员工写入或角色管理已授权。

本轮完整后端455项 NOT_EXECUTED（源码未改）；生产部署/TLS/代理/跨源、多OS/远程CI、真实性能/海量数据、全套辅助技术、跨标签即时同步、完整视觉位置恢复 NOT_VERIFIED。创建/编辑/启停/删除/重置、角色/门店关系、批量/导入导出不在本项，没有实施或验收。原P07完整范围和历史限制保留，不因详情实现完成将P07标COMPLETE。
