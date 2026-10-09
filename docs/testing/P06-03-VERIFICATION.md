# P06-03 Web综合验收与P07接入准备

日期：2026-10-09；根目录 `/Users/kimwell/work/pet-platform`。用户明确授权本任务，冻结版本、固定包名及根目录规则继续适用。**P06-03 COMPLETE；P06 COMPLETE；G01～G14 PASS。** 不使用Product Delivery OS；没有进入P07、提交、推送、发布或部署。

## 前置恢复及覆盖矩阵

实际读取AGENTS/README、ROADMAP/DECISION-LOG/LOCAL-DEVELOPMENT、WEB-PAGES/WEB-STATE、AUTHENTICATION/AUTHORIZATION/CONFIGURATION、IDENTITY/API/PAGINATION/DATA-TYPES、P06-01/P06-02及续验、ACCEPTANCE-MATRIX。核对实际请求、Cookie/CSRF、Query key/代际、路由/权限、Form安全、错误、CI/脚本及git；不读取旧PDOS状态。原始P06/A06条件与冻结提交60481ce对照。

开始git干净，HEAD为 `2687e8adca1b586b5bebfb6c7c772d28c57c620e`；4020项tracked摘要含270项ui，见[基线](evidence/P06-03/baseline.json)。P06-01/P06-02 COMPLETE、P06原IN_PROGRESS；H2 G01～G14已有PASS、5文件120项及六检查只作为历史。最近完整后端421项没有在本轮重新执行。

先建立[补验前矩阵](evidence/P06-03/coverage-before.md)，逐项需求/实现/证据/等级/补验/owner已收敛到[P06总验收](P06-ACCEPTANCE.md#已完成能力和覆盖矩阵)。没有缺陷或证据缺口的主链复用H1/H2，不机械重复真实改密/微信/后端完整套件。

## 实际修复

| 问题及影响 | 最小变更 | 验证 |
| --- | --- | --- |
| 系统入口Link包Button有重复停止点 | 官方Button用Router navigate | 390px两个Tab停止点、Enter |
| 空表单错误虽有文字，首错误定位不足 | Login/Security官方Form scrollToFirstError focus | 原生空提交焦点到第一字段；文字及关联 |
| 账号菜单未自动聚焦，Escape/键盘菜单恢复不足 | 官方Dropdown autoFocus | 原生focus菜单项、Escape回按钮；Down/Enter退出 |
| 同源标签换主体/授权后安全Form可能复用旧主体草稿 | AccountSecurity key绑定主体/租户/会话/授权版本/权限/门店/dataScope/受限状态 | 原生甲→乙后现有确认密码草稿清空；普通同身份成功刷新不清 |
| 当前退出失败通知可能两层重复 | notice与logout ErrorNotice互斥，使用同一标题 | 正式logout缺CSRF403，me200，只有一次未确认通知及trace |
| 390px平台长账号挤压空间文字，固定页头行高裁切 | 仅现有窄屏media query允许header换行、自适应高度和正常行高 | 原失败截图保留；最终390px/scrollWidth390、页头101px且内容完整，补验PASS |
| 完整七HTTP状态缺少一张统一保真测试表 | 新增400/401/403/409/422/429/503参数化状态/code/trace测试 | 原120保留，最终127项，0跳过 |

只有上述Web源码/测试/样式和本任务文档/证据变化。没有改后端、公开schema/生成契约、依赖/锁、CI或检查脚本；没有无意义Wrapper、生产Mock、新业务页或日期/金额工具。

## 请求与错误总核对

统一RequestClient成功解包data（含null），非JSON只保留安全状态/中文，不把HTML直接展示；HTTP/信封一致性、fieldErrors、traceId和合法Retry-After保留。七种状态统一技术测试；并非每种状态都现场制造。NETWORK/TIMEOUT/CANCELLED/PROTOCOL分开，取消查询不展示业务错误；实际Body超时为确定性技术测试，不外推任意浏览器链路。

地址先校验：仅同源相对根和本空间API，拒绝绝对/协议相对/越界/秘密query，JSON/FormData互斥，multipart不写boundary。默认Cookie/no-store、15秒超时、redirect:error；写操作只获取当前空间CSRF，不自动重放。CSRF_INVALID使凭据过期，PERMISSION_DENIED触发身份重验且不混作CSRF；NETWORK/503不证明退出。当前退出只有成功data:null或契约已失效401才转登录，其余明确“退出未确认”。

错误owner：请求层只转换，Form定位字段，页面单次总错误，runtime/guard处理统一401。真实CSRF反例经回环代理仅移除平台logout Header，正式后端403、me200；[转发白名单](evidence/P06-03/csrf-forward.jsonl)、[首次呈现核对](evidence/P06-03/error-once-audit.json)及[最终视觉核对](evidence/P06-03/visual-audit-final.json)证明两个独立反例各手动一次logout，共两次请求，没有额外重放；每次仅一条未确认提示。FormData当前没有真实上传/下载接口，本轮不声明附件联调。

## 身份、路由、缓存与跨标签

两Cookie可并存且按/admin或/platform当前路由只解析相应空间。Query是me/权限唯一投影，Zustand只有布局偏好。登录后重新me；每空间过渡锁、CSRF与epoch独立；确定401按空间/代际合并，旧成功/旧401/旧CSRF不能覆盖新登录。身份/会话/范围变化推进本空间，取消旧请求、清旧Query并先交付新me，另一空间保全。范围key及旧响应/并发失效为既有受控技术测试；动态导入缓存探针不作为真实Query实例证明，没有假业务缓存页面。

路由/导航共用sessionPages，受限STAFF仅必要安全入口，returnTo/直接URL不能绕过；本人安全能力不依赖普通管理权限。权限成功重验后旧导航/操作立即去除，非法页面进入安全页或403。H2原生trusted visible focus及最小化恢复有效，本轮不重写其历史NOT_EXECUTED记录。

原生同源两标签补验：甲安全页有当前密码确认草稿；另一标签退出甲再登录乙；原生点击恢复第一标签，出现乙真实身份且四个密码字段空。另一标签当前退出→恢复标签转登录；另一标签logout-all200→恢复标签me401、旧安全页消失。正式改密后第二独立上下文旧会话401沿用H2。本轮较早整批改密脚本A标签等待超时仍保留，不能标整批PASS。

没有广播。恢复focus/visibility和下一保护路由重验之间有可见延迟，后台标签可以暂时展示上次身份；成功重验后旧身份不会继续展示，失败则进入受控故障页。后续每个请求由后端Cookie/数据库事实决定，写操作CSRF不匹配也不重放。没有发现既定恢复门禁之外必须即时通知的缺口，所以未加入BroadcastChannel，更不广播身份、权限、Token、密码或CSRF。

## 本轮真实浏览器和可访问性范围

专用tmpfs PostgreSQL/Redis、正式V1～V4、三个角色脚本、正式stdin初始化的两租户员工和平台技术账号；受限runtime、无测试profile或假Gateway。JAR复用P06-01同源正式产物（后端源码/摘要未变），不称本轮重新完整编译后端。Web5173通过/api同源到18090；末段18091回环代理仅对平台logout移除CSRF，其余透传；白名单日志无Body/Cookie/Token。初始化和故障数据准备不等于员工/角色管理功能验收。

Chrome154.0.8037.98/macOS。局部自动化证明入口/两登录390px、Tab顺序、label/aria错误关联、Enter/提交中禁用、双Cookie；该整批后续失败，不声明整批成功。原生CUA独立无痕窗口证明标签切换/退出/全撤销、Network真实连接失败/恢复、Query取消、受限/403/长中文通知和菜单恢复。实际连接失败只停止本轮Web：保持保护路径、不退出；恢复时Vite重连发生重载后原Cookie恢复，不能声称该例只由重试按钮恢复。暂停本轮正式后端后me pending，离开页面Network记录canceled(2.89s)，入口正常，CONT后端恢复；取消不证明服务端回滚。

已实现系统入口、两登录、STAFF/PLATFORM主壳、安全页、受限、403、NotFound、加载/故障分别结合H1/H2/本轮桌面及390px；Tab/焦点、label、文字定位、Enter、pending、长中文、恢复入口和菜单Escape有限定证据。没有Modal/Drawer，不能假称其关闭焦点已联调；P07届时必验。没有真实屏幕阅读器或完整WCAG评估，不称全部无障碍标准PASS。

未知前端异常边界另以浏览器临时技术故障测试：只在新标签内把现有/admin beforeLoad临时抛出非协议异常，真实SystemError呈现、390px可用；恢复原handler后点击重试进入正式me401/登录流程。这是RUNTIME_VERIFIED（前端技术故障），不是后端业务异常或Mock页面；随后整窗关闭，源码没有探针。冷动态模块断网是另外观察：冻结Router先一次整页reload，服务仍停时Chrome连接失败；不将浏览器错误页充当应用SystemError。证据及边界分别保存。

[原生逐场景结果](evidence/P06-03/browser-native-complete.json)；[失败/工具疑点审计](evidence/P06-03/browser-attempts-audit.json)。所有早期失败/续验/无代码变更历史保持原文件。

## 命令、退出码与复用

工作目录均为项目根；每项JSON保留命令、退出码、摘要、耗时和日志。正式环境证据见[environment](evidence/P06-03/environment.json)，只包含允许元数据。冻结pnpm原临时入口丢失曾启动失败，原日志保留；重新从官方注册表取得同版10.34.6临时工具并留SHA，未改全局配置/版本/锁。

| 实际命令 | 退出码 | 范围及证据 |
| --- | --- | --- |
| pnpm --filter @pet/admin-web typecheck | 0 | [最终记录](evidence/P06-03/typecheck-accepted.json) |
| pnpm --filter @pet/admin-web lint | 0 | [最终记录](evidence/P06-03/lint-accepted.json) |
| pnpm --filter @pet/admin-web test | 0 | [5文件127项通过，0跳过；含既有120](evidence/P06-03/test-accepted.json) |
| pnpm --filter @pet/admin-web build | 0 | [最终构建](evidence/P06-03/build-accepted.json)，500kB警告保留 |
| pnpm contracts:check | 0 | [10项后端导出/模型检查](evidence/P06-03/contracts-check.json)、schema及两份生成类型一致、契约类型检查；不是421全套 |
| pnpm check:repo | 0 | [最终仓库/小程序结构脚本](evidence/P06-03/repository-check-accepted.json)，不是微信GUI验收 |
| git diff --check | 0 | [最终diff检查](evidence/P06-03/diff-check.json) |
| 摘要、链接、结构、秘密与资源审计 | 0 | [最终审计](evidence/P06-03/final-audit.json)、[资源收尾](evidence/P06-03/cleanup.json)、[秘密清理及复扫](evidence/P06-03/secret-cleanup-audit.json) |

全部命令/工具尝试的记录见[命令索引](evidence/P06-03/command-index.json)，包括早期非0和NOT_EXECUTED；工具调用无进程退出码时保留null及toolStatus，不能伪造0。

H1/H2正式改密/旧密码拒绝/跨设备撤销/Redis503恢复/双域CSRF及原生focus复用，不写为本轮重跑。421完整后端、微信、多OS、远程CI、生产部署本轮NOT_EXECUTED，原因是无后端改动且不在本任务授权验收范围。

## 构建体积

P06-01入口711.10kB/gzip234.59；P06-02为714.55/235.61。本轮最终入口714.56kB/gzip235.62kB，相比H2增加0.01/0.01kB；布局94.09/29.58，Login3.60/1.79、SessionFailure61.01/22.83、CSS5.20/1.81，见[最终日志](evidence/P06-03/build-accepted.log)。现有路由块继续独立懒加载。500kB是minified chunk警告阈值，gzip与阈值不可混算；没有提高阈值/复杂拆包。源码没有明确无用导入可删除；bundle大小不等同于首屏耗时，未做性能测量。

## 结构、敏感信息与保全

实际源码只有RequestClient发fetch，业务统一runtime；生成类型只import，不手写DTO；Query/Mutation没有敏感输入长期变量；Form清输入/引用；Zustand不复制权限/身份、无persist；应用无凭据console或持久化。冻结Router内部有不含身份/秘密的模块失败reload标记，不能把依赖内部标记混成业务持久身份。源码检查和本轮证据精确秘密扫描分别留证，只保证扫描范围和已知输入。密码截图均空或仅在未保存过程中为掩码；保存截图不含密码值。不承诺JS物理擦除或未知第三方日志。

[最终文件表](evidence/P06-03/file-manifest.json)列出13项既有文件修改、两份新报告和本轮证据。4020项基线中4007项不变，13项变更全部在白名单；270项ui、后端、小程序、锁/依赖/CI、历史报告与证据原摘要保全，HEAD不变。相对文档链接和应用fetch/持久化/日志结构扫描通过。已知7项技术输入精确扫描无命中并移除；没有保留的角色口令/Token不能重扫。三个本轮进程组停止，5173/18090/18091关闭，两tmpfs容器消失，原三个exited容器状态未变。仅关闭本轮无痕窗口/IAB标签，普通窗口未发送关闭动作，不推断其标题始终不变。所有早期失败JSON不删除，不修改P06-01/02报告或旧证据。没有提交、推送或部署。

## 完成门禁

| 门禁 | 结果 | 依据与限定 |
| --- | --- | --- |
| G01 原P06完成条件 | PASS | 冻结P06/A06及25项矩阵逐项核对，无必选迁移 |
| G02 请求/错误协议 | PASS | 127技术测试+正式null/401/403/503历史及本轮连接/取消/单次CSRF退出 |
| G03 Cookie/CSRF/空间 | PASS | H1/H2双Cookie/CSRF正反例、本轮真实双域/不重放 |
| G04 路由/导航/刷新 | PASS | 共用元数据、H2真实原生focus及本轮受限/403 |
| G05 强制及本人安全 | PASS | H2完整两域主链；本轮受限本人入口/原生全部撤销 |
| G06 清旧缓存/草稿 | PASS | 原生甲→乙/退出/全撤销可见投影和草稿；范围缓存为受控测试 |
| G07 旧响应/并发失效 | PASS | 代际/401 single-flight确定性回归，未外推任意生产网络 |
| G08 跨标签 | PASS | 原生同源标签3场景；改密失效结合H2，明确无广播及可见延迟 |
| G09 窄屏/键盘 | PASS | 已实现页面的桌面/390px及限定键盘操作；最终页头完整，原视觉FAIL保留；无全套辅助技术结论 |
| G10 敏感信息 | PASS | 源码和公开证据限定扫描，7项已知输入无命中并清理；未知第三方日志不外推 |
| G11 必要检查 | PASS | 最终Web四检查、127测试、10契约检查、仓库/diff/审计退出0；原生逐场景补验通过 |
| G12 P07接入 | PASS | 总验收十项可执行接入规则，total安全范围、真实接口优先 |
| G13 既有成果/历史 | PASS | 4020摘要白名单、270ui及旧证据保全，专用资源收尾，HEAD不变 |
| G14 总验收范围一致 | PASS | 25项矩阵、原P06/A06和实际等级对应；生产与未来阶段限制未改写为PASS |

## 状态与下一建议

**P06-03 COMPLETE；P06 COMPLETE。** G01～G14全部PASS，原路线没有其他未完成P06必选条件；有限技术测试、历史联调与本轮原生补验按各自等级合并，没有把技术替身或失败批次改称真实验收。P07仍NOT_STARTED；建议 **P07-01：员工管理后端查询与授权契约**，以真实授权分页/详情接口与生成类型支撑企业列表，原P07的CRUD/组织/角色等完成条件保持，不自动执行。剩余真实限制完整列在[P06总验收](P06-ACCEPTANCE.md#未验证限制)。
