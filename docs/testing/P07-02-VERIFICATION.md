# P07-02 员工列表与查询基础验证

日期：2026-10-09；根目录 `/Users/kimwell/work/pet-platform`。用户明确授权P07-02；沿用冻结版本与真实P07-01/P06契约，不使用Product Delivery OS，不执行下一任务，不提交、推送或部署。

**P07-02 COMPLETE；P07 IN_PROGRESS；G01～G14 PASS。** 正式员工列表读取、URL/Form/Query分工、服务端分页排序与授权清理通过。详情完整页面、写入/组织/角色仍是P07后续条件。历史P07-01 COMPLETE/完整后端455项保持；本轮后端未改，不重跑全套，只执行10项契约导出/模型检查。命令、退出码、原始失败、截图和精确集合见[证据目录](evidence/P07-02)、[命令索引](evidence/P07-02/command-index.json)、[文件清单](evidence/P07-02/file-manifest.json)、[最终审计](evidence/P07-02/final-audit.json)。

## 1. 实际事实与接口

已读取AGENTS/README、EMPLOYEE-MANAGEMENT/API/PAGINATION/DATA-TYPES、WEB-PAGES/WEB-STATE、AUTHORIZATION、P07-01-VERIFICATION/P06-ACCEPTANCE、ROADMAP/DECISION-LOG/ACCEPTANCE-MATRIX；进一步核对生成operations、正式Controller/查询/DTO、CurrentIdentity/授权范围、request/Query Key/SessionRuntime代际、路由、实际Vitest/Vite脚本、Git基线。版本没有升级，锁文件没有变化。

| 页面/接口 | 实际接入 | 权限与范围 |
| --- | --- | --- |
| `/admin/identity/users` | GET `/api/admin/identity/users`，生成operations/EmployeeView/EmployeePage | STAFF `identity:user:list`，逐权限TENANT/SELF/STORES，服务器最终授权 |
| 员工详情接口 `/{employeeId}` | 本轮未调用、无详情链接 | `identity:user:detail` 独立；后续P07-03 |
| `/admin/security` | 保持已有本人入口 | 与员工list独立 |

真实列表query是keyword/status/storeId/page/pageSize/**sortBy/sortOrder**；不是单个sort字符串。UI仅提供keyword/status；没有公开可信门店选择事实，不实现storeId控件，URL将storeId及未知参数移除提示。没有roleId/日期筛选。六字段只含id/loginName/displayName/status/createdAt/updatedAt；ACTIVE/DISABLED中文显示，无角色/门店/管理能力伪字段。后端keyword不trim，按字面量大小写不敏感匹配账号/姓名；前端保持首尾空白与大小写，不擅自改写输入。

## 2. URL、Form、分页与排序

[列表接入规则](../conventions/WEB-LIST-PAGES.md)是当前复用入口，模块在 `features/identity/users/{api,queries,pages}`；直接使用官方Form/Input/Select/Button/Table/Pagination/Alert/Empty/Tag，无BaseTable/透传Wrapper。

- URL只包含已提交的keyword/status/page/pageSize/sortBy/sortOrder。缺省page=1/pageSize=20/createdAt desc；默认字段省略，默认排序两字段一起省略，非默认排序成对编码。
- 原始URLSearchParams保留string/重复数组，`001`、`false`关键词不被JSON search转换。非法或重复已知参数整组恢复默认并提示；未知参数移除并提示，不能选择第一个重复值。空输入提交省略keyword，URL显式空值非法；keyword最多100 Unicode码点、与Java isBlank一致，拒绝非法代理字符。数字拒绝符号/空白/小数/指数/越界；前导零可安全规范化。
- 仅sortBy缺方向补desc，单独sortOrder非法。URL规范化用replace，首加载在Router初始规范化前保存提示到同一history条目，避免丢失原始重复/未知参数证据。用户查询/重置/分页/排序用push；前进后退覆盖Form旧草稿。输入不请求，Enter/查询提交校验后page回1；相同条件合并正在进行请求，否则刷新；没有重复提交产生额外页面请求。
- page是1～2147483647，pageSize是1～100，组件选项10/20/50/100。pageSize变化回1，重置默认条件/排序/大小；直接URL其他合法大小仍保留。
- total保持后端规范非负十进制**字符串**。正则→BigInt检查→只在MAX_SAFE_INTEGER内Number转换给Pagination；末页和JPA int offset上限也用BigInt计算，不clamp、不用当前页条数替代total。超过安全范围显示精确总数及“分页范围受限”，不渲染Pagination，不称“全部加载”。安全total的深页仍受int32页号/JPA offset能力约束；直接深页422 RESULT_TOO_LARGE可返回1。
- 根据真实total超过末页使用replace回有效页，total=0回1；history flag限制每个用户导航自动纠正一次。数据再次缩减时明确提示与显式返回第一页，不无限纠正。跨HTTP分页不保证并发快照。
- 六字段单列服务端排序，ascend→asc、descend→desc，清除恢复createdAt desc，排序变化回1；默认创建时间列desc→asc→清除（恢复desc），URL恢复箭头。稳定同向ID由后端追加，客户端不构造第二套串，不对当前页排序。rowKey=id，无行选择。

`9007199254740993`无损显示与无分页由技术测试/确定性组件验证，未在真实数据库造该数量；海量游标/快照/性能方案未实现。

## 3. 缓存、安全与查询状态

复用authKeys.protected，Key含STAFF、可信tenantId/principalId/sessionId、authorizationVersion/sessionEpoch、有效门店和逐权限dataScope、identity/employees/list资源、标准化条件；不只按租户，不含Cookie/Token/密码。当前正式me提供完整逐权限范围，核对其真实映射，未新建身份Store或推测范围。

queryFn传AbortSignal，经现有SessionRuntime/RequestClient执行并检查epoch。现有身份重验比较完整事实，即使版本未增也推进代际，取消旧请求、移除本空间旧缓存，原子投影新me；晚到旧结果不能写回。页面以主体/代际重新挂载，条件变化和跨身份均无placeholderData。PERMISSION_DENIED立即隐藏旧行、取消并清当前query；随后既有me重验更新导航/守卫。导航和beforeLoad共用sessionPages/canAccessPage；无list或仅PLATFORM身份不发STAFF列表请求，自己的安全入口独立。

staleTime30秒/gcTime5分钟；列表自身关闭focus自动刷新，既有身份focus/可见性规则先重验，范围改变新key立即重查。保留既有GET网络/超时/502～504最多2次（1秒/2秒）重试；4xx/取消不重试。不修改写入或敏感请求规则。前端缓存不是最终授权；没有实时推送/跨标签广播，未触发既有重验前已经取得的行可能暂时显示，不宣称撤回已完成读取。

| 状态 | 可观察行为与验证 |
| --- | --- |
| 首次加载/条件切换 | loading与文字，清旧条件行，不误报空态；组件受控延迟验证 |
| 同条件刷新 | 保留合法数据并标明“上次成功结果”；真实刷新按钮 |
| 无任何数据/筛选无结果 | 分开中文空态，真实STORES=0及不存在关键词=0 |
| 首次失败/刷新失败 | 首次无行/无假空态；刷新保留合法行+错误+重试；真实DB503均验 |
| 401 | 原统一会话失效/合法返回，不把503当401 |
| 403 | PERMISSION_DENIED撤权清理；PASSWORD_CHANGE_REQUIRED按既有受限流程；CSRF_INVALID沿用独立错误，不当未登录 |
| 422 | 关键词字段/查询参数信息；RESULT_TOO_LARGE独立深页提示；组件受控422验证 |
| 503/取消 | 503保留有效身份并重试，恢复无需登录；取消无失败提示 |
| 安全范围超限 | 精确total/限制提示，不传不安全Number |

时间按契约默认Asia/Shanghai格式化并标注；me无zoneId，不声称动态租户时区。空值“—”，姓名/账号保留完整title/tooltip且键盘可聚焦。没有dangerouslySetInnerHTML。390px表格横向滚动限表格区域，分页大小/跳页控件换行保留；没有整体溢出。未实现列显示配置/持久化。

## 4. 正式后端与真实浏览器

[环境初始化](evidence/P07-02/environment.json)、[目标数据准备](evidence/P07-02/data-preparation.json)记录真实命令及退出码：冻结PostgreSQL17.11/Redis8.2.10专用tmpfs容器，独立p07_02_acceptance库，正式角色脚本、V1～V5迁移与bootstrap、受限nosuperuser/nobypassrls runtime、既有生产JAR/local、唯一正式Provider。Web127.0.0.1:5173同源代理后端18090；无test profile/Gateway/内存数据库。正式bootstrap两租户操作者及平台操作者，凭据随机生成仅在忽略目录/进程中使用。

缺员工创建API，55名目标由隔离SQL准备（含长姓名、多门店/重复交集、ACTIVE/DISABLED、字面量特殊关键词）。这是数据准备，不是创建功能验收。主租户含操作者56名、DISABLED13名；第二租户仅1名。页面每场景的行ID、total、page/pageSize与同身份同条件正式API查询核对，另以隔离SQL总数/门店EXISTS核对范围，见[34个集合快照与79项场景](evidence/P07-02/browser-real.json)。

| 真实场景 | 结果 |
| --- | --- |
| TENANT / SELF / STORES | 56 / 1本人 / 20任一有效门店交集；多门店不重复；SQL及安全API集合一致 |
| 未增版本的scope/有效门店变化 | TENANT→SELF→STORES清旧缓存；撤销门店=0，恢复TENANT=56重新读取 |
| list权限撤销 | 真403 PERMISSION_DENIED，旧行隐藏/缓存移除；直接地址无列表请求；本人安全入口仍可用 |
| PLATFORM仅登录与跨租户 | 平台会话不能打开STAFF列表；同源另一标签退出再登录第二租户，旧页面重验后仅1条、无旧租户缓存 |
| 查询与排序 | keyword/status/字面量%_、草稿不请求、Enter一次、重置、page/pageSize；六字段asc/desc/清除/刷新箭头均验 |
| 地址与历史 | 带search直接地址、刷新、前进/后退与Form同步；重复/未知规范化提示、001字面量保持 |
| 空结果与末页 | 不存在关键词=0、page99→真实末页3且replace；total0→1，没有循环 |
| 真服务故障与恢复 | 临时撤销本轮受限角色对六列SELECT导致实际503 DEPENDENCY_UNAVAILABLE；首次与刷新各3次（现有GET重试）；恢复原列授权后无需重登恢复56/1条 |
| 桌面/390px/键盘 | 1280×900、390×844；Enter查询、页码键盘、大小切换/跳页；document宽390/scroll390，表格内部scroll1230/client358 |
| 控制台 | pageErrors=0，新增组件console error=0；保留10条浏览器资源错误：预期401×2/503×6/403×1及既有favicon404×1，不声称资源控制台为空 |

真实主套件79项PASS，390px补验4项PASS，退出均0。主套件列表响应92个200、6个503、1个403（含核对用安全查询）；没有HAR/响应头/凭据截屏。可复核脚本见[browser-real.mjs](evidence/P07-02/browser-real.mjs)、[窄屏脚本](evidence/P07-02/browser-narrow.mjs)，需重新建立专用资源及安全凭据；最终命令实际使用本轮忽略脚本，文件摘要与公开副本一致。

截图：[桌面](evidence/P07-02/desktop.png)、[390px最终](evidence/P07-02/list-390-final.png)、[真实刷新失败](evidence/P07-02/refresh-failure.png)。已目视核对。截图只有隔离验收员工/页面，不含密码、Cookie或CSRF。

## 5. 自动化和命令

现有Vitest工具新增61项，6文件**188项PASS**（原127保留）；覆盖默认/非法/重复URL、literal Unicode、提交重置/排序大小、total精度/offset/纠正一次、scope key/signal/旧响应/跨身份、权限撤销、503/401/首次刷新错误、DTO六字段/格式化（rowKey另有真实浏览器证据）。实际组件另13场景：加载/草稿/校验聚焦、超安全total/安全total超int32跳页拦截、二次缩减不循环、新条件清旧行与晚到旧响应、422提示、me撤权、同租户不同主体无占位；[组件结果](evidence/P07-02/browser-components.json)明确网络替身，只用于确定性行为，不充当正式业务联调。

所有命令cwd均 `/Users/kimwell/work/pet-platform`；使用子进程专用冻结Java21.0.12.1、Node24.21.0、pnpm10.34.6及实际Chrome154.0.8037.98，未改全局配置。完整开始结束时间、工具路径、退出码、日志见逐命令JSON与命令索引。

| 检查 | 实际命令 | 退出/数量 | 最终证据 |
| --- | --- | --- | --- |
| Web typecheck | pnpm --filter @pet/admin-web typecheck | 0 | [typecheck-final-accepted](evidence/P07-02/typecheck-final-accepted.json) |
| Web lint | pnpm --filter @pet/admin-web lint | 0 | [lint-final-accepted](evidence/P07-02/lint-final-accepted.json) |
| Web test | pnpm --filter @pet/admin-web test | 0，6文件188项 | [test-final-accepted](evidence/P07-02/test-final-accepted.json) |
| Web build | pnpm --filter @pet/admin-web build | 0，1704模块 | [build-boundary-final](evidence/P07-02/build-boundary-final.json) |
| 契约 | pnpm contracts:check | 0，10项后端导出/模型检查及共享类型；生成文件无漂移 | [contracts-check](evidence/P07-02/contracts-check.json) |
| 仓库 | pnpm check:repo | 0 | [check-repo-final](evidence/P07-02/check-repo-final-accepted.json) |
| diff | git diff --check | 0 | [diff-check-final](evidence/P07-02/diff-check-final-accepted.json) |
| 正式浏览器 | node .local-data/p07-02/browser-real.mjs | 0，79项 | [最终源码复验](evidence/P07-02/browser-real-boundary-final.json) |
| 窄屏补验 | node .local-data/p07-02/browser-narrow.mjs | 0，4项 | [browser-narrow-boundary-final](evidence/P07-02/browser-narrow-boundary-final.json) |
| 确定性组件 | node docs/testing/evidence/P07-02/browser-components.mjs | 0，13项 | [browser-components-boundary-final](evidence/P07-02/browser-components-boundary-final.json) |

最终build：入口719.32kB/gzip237.63kB；员工页独立chunk275.65kB/gzip90.07kB；CSS6.25kB/gzip2.03kB；SessionLayout94.89kB/gzip29.98kB。保留500kB警告，未提高阈值。不是生产部署或性能验收。

## 6. 原始失败、修正与收尾

原始失败/未执行边界全部保留，不覆盖或删除旧尝试。初始TS/lint/测试失败修正了search middleware类型、effect状态规则和断言。真实浏览器第1～11次及组件第1～5次失败有日志/观察：选择器/等待/脚本语法、初始网络拦截误截Vite模块、HMR裸模块导入导致观察另一AuthService、重复登录触发正式5会话上限、初始URL规范化提示被Router抢先消除、默认createdAt排序点击受控循环、测量表格外层误认未滚动。修正脚本精确观察实际资源模块和组件、复用/退出正式会话；修正正式代码默认列排序与初始history notice。第12次正式全套通过；深页边界修复后在新隔离环境中再次全套复验，最终组件13项实际通过；没有放宽鉴权或修改后端。

会话上限后仅重启本轮Redis清专用会话，随机映射端口变化首轮未更新造成503，随后更新本轮进程配置并恢复readiness200，见[临时Redis恢复](evidence/P07-02/temporary-redis-reset.json)、[后端恢复](evidence/P07-02/environment-recovery.json)。窄屏首脚本错误假设URL参数顺序导致跳页等待超时，改按URLSearchParams核对后4项通过，原[失败观察](evidence/P07-02/browser-narrow-first-observations.json)保留。收尾build-final失败发现原临时pnpm目录不存在/退回10.33.0；重取固定10.34.6到项目忽略目录、修正本轮PATH后build0，见[工具恢复](evidence/P07-02/toolchain-recovery.json)，未改变项目依赖或全局配置。最终复核另外发现安全total下组件可发出超int32页码，改为先计算可访问上限再拦截跳页（不抛未处理异常），新增第13个确定性场景通过，并重跑受影响Web检查及正式浏览器。清理首轮在专用后端停止后的killpg零信号探测出现EPERM，改用ps/端口核对；第二轮已停止容器、删除秘密输入，末尾全容器清单全等断言因其他项目新建容器失败。最终按专用资源名与原三个容器/卷子集核对通过，未操作其他项目资源；[首轮](evidence/P07-02/cleanup-first.json)、[第二轮](evidence/P07-02/cleanup-second.json)失败均保留。

浏览器finally恢复专用grant、原员工Store关联及受限列SELECT，正式退出STAFF会话。最终只停止本轮后端/Web进程和两个tmpfs容器，删除本轮秘密输入；日常三容器仍原exited状态、既有卷保留。生产JAR副本与当前产物摘要一致；contracts检查清理target后的原产物恢复不含源码变化。收尾命令/退出和端口/卷保全见[cleanup.json](evidence/P07-02/cleanup.json)，4505个Git基线文件的修改范围与历史证据/ui/后端/迁移/锁/生成类型保全见最终审计。

## 7. 文件与证据等级

新增模块API/queries/search/pagination/pages及61项测试，新增shared/format；修改SessionLayout、pageAccess、router、returnTo、global.css。新增WEB-LIST-PAGES、本报告/证据；更新WEB-STATE/WEB-PAGES/EMPLOYEE-MANAGEMENT/ROADMAP/DECISION-LOG/ACCEPTANCE-MATRIX/README。逐文件路径/字节/sha256见文件清单。未改后端源码/迁移/公开生成接口/依赖/锁/小程序/ui及历史证据。

| 等级 | 本轮成立内容 |
| --- | --- |
| DOCUMENTED | URL、权限/缓存、分页/排序/错误owner、接入步骤和未来边界 |
| RESOLVED | 固定工具链、既有生成类型与真实契约/完整范围、无依赖变化 |
| COMPILED | Web tsc/lint/build、188项测试、生成契约一致性 |
| RUNTIME_VERIFIED（真实） | 正式后端/PG/Redis/认证/隔离数据浏览器79+4项、各安全范围与503恢复 |
| RUNTIME_VERIFIED（技术） | 13项网络替身组件行为，超安全total/慢旧响应/422/连续缩减；与真实验收分开 |
| NOT_VERIFIED / NOT_EXECUTED | 生产部署/TLS/跨源/多OS/远程CI、真实海量total/性能、辅助技术全套/实时推送、详情页面/写入/批量/导出/附件/平台员工；本轮完整455后端测试NOT_EXECUTED（后端未改且无相应回归必要） |

## 8. 完成门禁

| Gate | 结果 | 依据 |
| --- | --- | --- |
| G01 实际读取契约/权限 | PASS | 实际生成类型、正式query/sort/default/keyword/me范围核对，storeIdUI未擅增 |
| G02 真实接口与生成类型 | PASS | list API generated导入、正式安全集合/六字段核对 |
| G03 导航/直接路由一致 | PASS | 共用pageAccess、无list/仅平台无列表请求、安全入口独立 |
| G04 URL/Form/历史同步 | PASS | 原始string/重复校验，真实直接/刷新/前后退/Enter/重置 |
| G05 服务端分页排序 | PASS | 真实page/size/六字段双向/清除与箭头，rowKey=id |
| G06 total无损安全 | PASS | BigInt边界/页运算及超限精确/超int32跳页组件场景，真实正常total56 |
| G07 取消/旧响应 | PASS | signal/epoch测试及受控迟到响应忽略，既有runtime清理 |
| G08 身份/范围清理 | PASS | 真实未增版本scope/门店/撤权、跨租户及同租户异主体组件 |
| G09 状态/错误恢复 | PASS | 真首次/刷新503恢复、空态、权限；受控loading/422/超限 |
| G10 真实安全范围 | PASS | TENANT56/SELF1/STORES20/撤门店0，API和SQL核对 |
| G11 桌面/390px/键盘 | PASS | 实际Chrome154.0.8037.98截图/宽度与键盘查询/页码/大小/跳页；无新增JS异常 |
| G12 静态/测试/build | PASS | 七项要求命令退出0、188项/10契约检查，体积警告保留 |
| G13 无假业务/死链接/写入提前 | PASS | 正式页无假数据、detail死链/写入/批量/导出/伪DTO字段 |
| G14 文档/证据/实际范围 | PASS | 文件摘要/命令/历史失败/等级/资源保全，P07整体仍IN_PROGRESS |

## 9. 下一合法任务

**P07-03：员工详情页与列表返回状态（建议，NOT_STARTED）。** 已核对原P07有详情范围、现有独立detail接口和权限可接入；后续确定详情路径、list/detail独立入口、scope外与不存在同404、合法列表search返回及重验清理。当前不放无效详情链接，不实现完整详情/写入，不自动执行下一任务。
