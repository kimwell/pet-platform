# P06 Web 阶段总验收

日期：2026-10-09。**P06-03 COMPLETE；P06 COMPLETE；G01～G14 PASS。** 原始路线与冻结提交 `60481ce` 的P06、A06-01/A06-02逐项对照；没有把未完成必选条件迁到P07。P06-01/P06-02 COMPLETE，历史失败及原生focus续验均保留。

## 已完成能力和覆盖矩阵

应用壳、中文Provider、Router/Query、统一fetch、两域Cookie/CSRF、登录恢复、权限/受限路由、本人密码及会话安全、页面加载/故障恢复。实际页面只有系统入口、两登录、两域身份壳/安全页、受限改密、403、NotFound和系统错误边界。没有业务Table、Dashboard或演示接口。

实现路径相对 `apps/admin-web/src/`。H1=[P06-01](P06-01-VERIFICATION.md)，H2=[P06-02及续验](P06-02-VERIFICATION.md)，N=[本轮原生补验](evidence/P06-03/browser-native-complete.json)。技术测试运行的COMPILED不能替代正式服务/浏览器的RUNTIME_VERIFIED。编码前覆盖计划已保全到[原矩阵](evidence/P06-03/coverage-before.md)。

| 需求 | 实现位置 | 有效证据 | 验证等级 | 本轮处理 | 缺口及原责任阶段 |
| --- | --- | --- | --- | --- | --- |
| Provider和中文配置 | app/providers/AppProviders.tsx、main.tsx | H1官方Form/入口；本轮源码核对 | COMPILED、RUNTIME_VERIFIED | 复用有效证据 | 无P06缺口；App上下文当前没有调用者 |
| 同源fetch、地址限制 | shared/api/request.ts、shared/config/api.ts | H1真实同源代理/地址拒绝；N真实正式接口 | COMPILED、RUNTIME_VERIFIED | 核对通过 | 跨源/生产反代独立验收 |
| data:null及响应/错误解析 | shared/api/request.ts、ApiError.ts | H1 null/非JSON/trace；本轮127测试含七种HTTP状态 | COMPILED（技术测试）、RUNTIME_VERIFIED（已有真实状态） | 新增400/401/403/409/422/429/503状态保真测试 | 真实业务409归原P07；并非全部状态均现场制造 |
| 网络、取消和超时 | shared/api/request.ts、ApiError.ts | H1确定性Body超时；N实际连接失败、恢复和Network canceled | COMPILED（超时/竞态）、RUNTIME_VERIFIED（连接/取消） | 真实浏览器缺项已补 | 不宣称取消证明服务端回滚 |
| Cookie/CSRF、不重放 | shared/auth/SessionRuntime.ts | H1/H2双Cookie与跨空间CSRF反例；N每个反例仅手动一次logout403/me200，共两个独立反例 | COMPILED、RUNTIME_VERIFIED | 缺失CSRF退出不重放且不宣称成功 | FormData仅透传技术测试；附件联调P08 |
| STAFF/PLATFORM隔离 | shared/auth/spaces.ts、SessionRuntime.ts | H1/H2双域保全；本轮局部双Cookie及原生平台/员工 | COMPILED、RUNTIME_VERIFIED | 复核通过 | Cookie名称/读取范围不改；无生产跨源证明 |
| 登录和恢复 | features/auth/api/AuthService.ts、pages/LoginPage.tsx | H1真实两域；N原生Enter、网络恢复、跨标签 | RUNTIME_VERIFIED | 已有及补充链路通过 | 成功必须重新me；不采用登录草稿构造身份 |
| 路由守卫 | app/guards/requireSession.ts、router/router.ts | H1直接URL/503；H2受限/403；N权限页/登录 | COMPILED、RUNTIME_VERIFIED | 复用并核对 | P07新页面需登记，未知页面仍404 |
| 合法返回目标 | shared/auth/returnTo.ts | H1/H2合法search/外站拒绝/受限returnTo；127测试 | COMPILED、RUNTIME_VERIFIED | 复用 | P07只扩已实现路由白名单 |
| 导航及操作权限 | pageAccess.ts、shared/auth/permissions.ts | H2权限下降/零管理权限本人能力；N受限导航/403 | COMPILED、RUNTIME_VERIFIED | 复用及窄屏复核 | 普通管理页面和权限契约归P07 |
| 权限刷新一致 | SessionLayout.tsx、AuthService.ts | H2 trusted visible focus、原生恢复撤权隐藏；N原生标签及平台403 | RUNTIME_VERIFIED | 没有新增广播的必要缺口 | 无主动推送或即时跨标签同步承诺 |
| 强制改密状态 | pageAccess.ts、AccountSecurity.tsx | H2正式限制/直达拒绝/改密恢复；N受限390px | RUNTIME_VERIFIED | 窄屏提示和必要本人入口通过 | PLATFORM无此字段，未伪造 |
| 本人修改密码 | features/account/AccountSecurity.tsx | H2两域实际PUT/旧密码拒绝/新密码登录；本轮敏感Form核对 | COMPILED、RUNTIME_VERIFIED（复用H2） | 复用正式改密主链 | 本轮原生UI没有输入新凭据；不宣称再次完成全部改密组合 |
| 退出当前和全部会话 | AuthService.ts、SessionLayout.tsx | H1未确认退出；H2全部设备；N两标签logout/logout-all/CSRF反例 | RUNTIME_VERIFIED | 一次错误呈现和原生跨标签撤销通过 | 退出未确认仍明确保留服务端会话可能有效 |
| Query范围和定向失效 | features/auth/api/queryKeys.ts、AuthService.ts | H1/H2作用域技术测试；N身份切换可见投影清理 | COMPILED（缓存）、RUNTIME_VERIFIED（身份投影） | scope含空间/主体/会话/版本/门店/权限范围 | 真实业务列表关联失效归原P07；动态导入缓存探针不作真实证据 |
| 旧响应及并发401 | SessionRuntime.ts、AuthService.ts | H1/H2可控旧成功/旧401/CSRF跨代际与双域并发；127测试 | COMPILED（确定性技术运行） | 复用必要回归 | 没有外推为任意生产网络竞态全部现场复现 |
| 跨标签退出/切账号/撤销 | SessionLayout.tsx focus、AuthService.ts authority | H2原生focus及改密后独立上下文401；N原生标签甲→乙/退出/全部撤销 | RUNTIME_VERIFIED；缓存另有技术测试 | 新身份确认后清旧表单、旧投影；原生补验通过 | 另标签改密整批脚本曾等待超时，保留；改密失效机制结合H2，不写该失败批次PASS |
| NotFound、系统错误、加载 | shared/SystemNotFound.tsx、SystemError.tsx、SessionFailure.tsx | H1/H2服务异常/404；N连接失败/403/390px；N系统边界技术故障 | COMPILED、RUNTIME_VERIFIED（分别注明技术/正式服务） | 已有页面及恢复入口核对 | 合法空身份为登录；无业务空集合页面，原P07实现 |
| 桌面和390px | styles/global.css、既有页面 | H1/H2桌面主壳/安全；本轮局部入口/登录与N错误/受限 | RUNTIME_VERIFIED | 发现平台页头裁切，最小CSS修复后补验 | 不代表所有尺寸/OS/浏览器；原失败截图保留 |
| 键盘、焦点、label、Enter | SystemEntry.tsx、LoginPage.tsx、AccountSecurity.tsx、SessionLayout.tsx | 本轮局部Tab/焦点/label/错误关联；N首字段focus、Enter、菜单Escape/Down | RUNTIME_VERIFIED（限定页面/操作） | 去嵌套交互、定位首错误、Dropdown autoFocus | 无Modal/Drawer；其关闭焦点归原P07。无真实辅助技术全标准声明 |
| 敏感输入/日志/持久化 | Form、Mutation、layoutState.ts | H1/H2限定扫描；本轮源码和已知技术秘密精确扫描 | RESOLVED、COMPILED、RUNTIME_VERIFIED（限定扫描） | 密码不入Mutation变量/持久缓存；权限无Zustand副本 | 不承诺JS物理擦除或未知第三方日志；Router库仅有非敏感reload标记 |
| 生成契约、CI和脚本 | contracts.ts、根scripts、.github/workflows/check.yml | 本轮类型/lint/127测试/build/10契约导出/check:repo | RESOLVED、COMPILED | 使用真实既有脚本，类型/契约无副本 | 远程CI、完整后端421回归、多OS本轮NOT_EXECUTED |
| total字符串安全适配 | PAGINATION、DATA-TYPES规范 | 后端和生成total:string；下文接入清单 | DOCUMENTED、RESOLVED | 明确安全范围算法 | 实际Table适配原P07；禁止任意Number(total) |
| 日期、金额、业务查询工具 | 当前无通用日期/金额/列表适配器 | DATA-TYPES与WEB规范；接入清单 | DOCUMENTED | 确认不是P06原完成必选 | P07按真实页面引入必要工具；不制造无行为通用工具 |
| 构建大小及警告 | Vite构建实际输出 | H1 711.10/234.59；H2 714.55/235.61；本轮最终build | COMPILED | 原始/gzip及阈值分别比较 | 500kB警告保留；没有首屏耗时测量 |

## 原始条件与阶段依据

原P06交付条件是Web基础和真实同源会话联通、清楚的错误/加载/空态。本轮结合H1/H2/N核对通过：初始加载与重验明确；无身份进入登录；未知路由404；依赖/网络失败可恢复；权限不足有合法离开入口。没有生产假实体空态。原A06-01补齐真实浏览器连接失败和signal取消；并发401/旧代际仍按可控技术证据记录。A06-02现有身份/安全操作按空间清理；普通员工列表的精确关联失效原本就属于P07，并未以延期降低P06门禁。

[P06-03 G01～G14](P06-03-VERIFICATION.md#完成门禁)与[最终保全审计](evidence/P06-03/final-audit.json)全部通过，P06-03及P06 COMPLETE。生产TLS、跨源、其他OS、远程CI始终是已知独立限制，配置存在不代表运行通过。

## 历史失败与本轮修复

H1的首次真实503误走React错误边界、H2的动态导入/加载时点、早期原生focus不可执行和随后可信focus续验保持原报告原样。本轮整批浏览器脚本也有退出1、无有效恢复事件的缓存探针及工具输入/视口问题，见[尝试审计](evidence/P06-03/browser-attempts-audit.json)，不称整批成功。实际原生逐场景结果另行留证。

本轮最小修复：入口按钮去除Link嵌套停止点；Form首错误聚焦；账号Dropdown autoFocus/Escape恢复；安全页按主体/会话/授权事实变化重建，普通同身份刷新保持草稿；当前退出错误单一owner；390px平台长名称页头改为自然换行/自适应高度。七种状态新增保真测试，不改测试预期掩盖缺陷。原窄屏裁切截图及[视觉失败记录](evidence/P06-03/visual-audit-before.json)保留。

## P07业务接入清单

本清单是任务接入索引；传输/缓存唯一规则在[WEB-STATE](../conventions/WEB-STATE.md)，页面规则在[WEB-PAGES](../conventions/WEB-PAGES.md)，协议来自[API](../contracts/API.md)、[分页](../contracts/PAGINATION.md)、[数据类型](../contracts/DATA-TYPES.md)。

| 接入事项 | 可执行做法和现有边界 |
| --- | --- |
| 1. request入口 | 业务服务取Router context中的auth，使用 `auth.runtime.request<T>(space, 契约相对路径, options)`；默认受保护。STAFF路径为 `/admin/...`、PLATFORM为 `/platform/...`，统一客户端再加 `/api`。queryFn传signal；禁止业务裸fetch/任意外域。直接依赖生成components/paths纯类型，不手写DTO副本。 |
| 2. Query Key | 使用 `authKeys.protected(identity, auth.runtime.epoch(space), module, resource, kind, normalizedParams)`；identity来自当前me。范围含tenant/principal/session/authorizationVersion、门店和dataScope，不能从URL授信。业务服务旁在P07声明模块key工厂；按合法URL规范化过滤/排序/数组，不能只用ID。 |
| 3. 写后失效 | mutation retry=false；只在确认成功后失效本完整scope的列表、详情及模块声明的关联查询前缀。失败不清普通草稿/不按成功失效。安全操作当前已有整个本空间清理；不能照搬为所有业务mutation全清。 |
| 4. 路由和操作权限 | 在sessionPages/pageAccess登记空间、真实权限和受限状态条件，导航共用元数据。按钮用当前身份的hasPermission/PermissionBoundary；本人能力用canManageSelf，不借普通管理权限阻断。后端逐接口/范围复核。 |
| 5. Form字段错误 | 已有登录/security为精确平字段白名单映射到Form.setFields。P07复杂表单按DTO登记允许的NamePath，将契约dot/bracket路径转换；拒绝原型属性和未知路径，未知消息由表单总错误一次展示。现无通用嵌套路径工具，不宣称已提供。 |
| 6. 409保留输入 | 普通业务Form保留当前输入、version和选择，提示显式重载后比较，不自动覆盖/重发；实际编辑页面和版本接口原P07实现。密码等敏感字段仍按既有安全规则清空。 |
| 7. 身份变化取消 | queryFn显式传signal；runtime统一空间代际、请求取消、旧响应拒绝和Query清理。页面敏感草稿/选择随主体或授权scope变化重置，同身份正常刷新不丢草稿。不要自行缓存身份副本或处理第二套401跳转。 |
| 8. total字符串 | DTO继续保留string。先校验规范非负十进制 `^(0|[1-9][0-9]*)$`；用字符串长度和同长度字典序确认不超过 `9007199254740991`，通过后才Number转换供AntD分页，并复核Number.isSafeInteger；非法/超限明确拒绝当前页码分页，保留原值、不截断。实际适配器/边界测试在原P07实施。 |
| 9. 日期和金额 | 当前没有统一业务日期/金额工具。纯日期保持YYYY-MM-DD语义，不用Date/UTC转换制造跨日；时间点按DATA-TYPES在页面约定时区格式化；金额/精确小数保持string，不以JS浮点计算。P07选择实际字段所需工具/精度规则后冻结其版本，先查VERSION-MATRIX，不提前创建无用途工具。 |
| 10. 空态/加载/错误owner | 首屏和查询失败由发起页面显示，合法空集合单独显示；后台刷新仅保留仍合法scope数据。字段错误在Form，总错误在页面，mutation只一次；统一401由runtime/守卫，网络/503不能视为退出。所有业务空态及Table模板原P07实现，当前无业务假页。 |

## 未验证限制

没有生产TLS/反代/跨源部署、Windows/Linux、多浏览器、远程CI、本轮完整后端421项重跑、真实辅助技术全标准、业务Table/CRUD/附件上传下载、小程序业务或首屏性能测量。FormData仅证明客户端透传和不手写boundary；不声明附件联调完成。

没有BroadcastChannel/推送。Cookie跨同源标签共享，但Query/CSRF内存各自独立。恢复focus/可见性或下一次保护路由重验前，旧标签可能暂时展示上次确认身份；成功确认后清旧scope和敏感草稿、路由导航同步；确认失败进入受控错误页；每次后端按真实Cookie/数据库授权，CSRF失配不自动重放写操作。不能把没有广播写成即时同步。

本轮原生同源标签退出、切主体、全部设备撤销通过；实际改密后独立上下文失效链复用H2。本轮改密整批脚本的标签A等待失败保留，不外推该具体整批场景。敏感信息只承诺限定扫描与应用引用清理，不承诺物理擦除或未知外部日志。

## 下一任务建议

P07当前NOT_STARTED。建议 **P07-01：员工管理后端查询与授权契约**，属于原P07“企业级页面与员工管理”：先定义并实现真实员工分页查询/详情DTO和权限/tenant/store/self范围、稳定排序/count(total:string)/错误及版本语义，生成OpenAPI三端类型，用真实PostgreSQL和HTTP做授权正反例，为企业列表提供可信接口。创建/修改/停用及组织/角色页面继续保留原P07必选，不因首项先只做查询而关闭P07。具体路径、权限代码及范围须在该任务从现有身份模型冻结；本轮没有创建任何管理API或Table，不自动执行。

P06-01/P06-02/P06-03及P06均COMPLETE；P07仍NOT_STARTED。最新阶段状态由[路线图](../development/ROADMAP.md)和本验收门禁对应，历史状态原样保留。
