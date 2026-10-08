# P06-02 Web 权限呈现、强制改密与本人会话安全

日期：2026-10-08；最新状态 **P06-02 COMPLETE、P06 IN_PROGRESS**。用户明确授权原生窗口focus续验与门禁复核；真实focus缺项已补齐，G01～G14 PASS。不使用 Product Delivery OS，不自动下一任务，不提交、推送或部署。前轮NOT_EXECUTED、工具失败及PARTIAL记录保留为历史；本轮证据见下方“原生窗口focus续验”。P06-01 COMPLETE、原77项测试已保留。

## 编码前实际接口与页面行为对应表

已核对生成 api.d.ts、StaffSecurityController、PlatformAuthenticationController、PasswordService、域认证过滤器和 P06-01 AuthService/SessionRuntime/beforeLoad。安全页面没有既定精确路径，本轮采用各空间 `/security`，路由与导航共用元数据。

| 空间 / 页面 | 实际接口 | 请求 / 事实 / 页面行为 |
| --- | --- | --- |
| STAFF /admin、/admin/security | GET /api/admin/auth/me | CurrentIdentity.passwordChangeRequired 必填 boolean；permissionCodes 与 dataScope.grants 逐权限保留 |
| STAFF /admin/security 改密 | PUT /api/admin/auth/password | ChangePasswordInput：currentPassword/newPassword；本人能力，无普通管理权限要求 |
| STAFF /admin/security 退出全部 | POST /api/admin/auth/logout-all | ConfirmationInput：currentPassword；本人能力，包含 WEB/小程序当前及其他设备 |
| PLATFORM /platform、/platform/security | GET /api/platform/auth/me | PlatformCurrentIdentity；没有强制改密字段；platform:session:manage |
| PLATFORM /platform/security 改密 | PUT /api/platform/auth/password | 同生成 ChangePasswordInput；platform:credential:change、当前密码确认 |
| PLATFORM /platform/security 退出全部 | POST /api/platform/auth/logout-all | 同生成 ConfirmationInput；platform:session:manage、当前密码确认 |
| 两域必要身份及退出 | 各 GET auth/csrf、POST auth/logout | CSRF 内存分域；当前退出沿用 P06-01 未确认语义 |

STAFF 受限会话后端只允许 me/csrf/password/logout/logout-all；其他路径 403 PASSWORD_CHANGE_REQUIRED，前端优先转 /admin/security，不采用 returnTo 绕过。PLATFORM 不伪造该状态。

两类安全写 200/data:null 与 X-Session-Cleanup=COMPLETE/PENDING 均表示 DB 已提交、所有旧会话逻辑失效；服务端清本域 sid/pre Cookie；前端清当前 Query/CSRF/旧请求并返回本域登录。PENDING 不自动重放、不暴露内部 Redis 操作步骤。密码服务要求 12～128 Unicode 码点、最多256 UTF-16单位、合法代理对，无组合要求，不 trim/归一化；拒绝与原密码相同。确认密码仅为 Form 字段。403 SECURITY_CONFIRMATION_FAILED/CSRF_INVALID/PERMISSION_DENIED 分开，409/422/429/503 使用实际标准信封；网络或依赖失败无法证明提交回滚。

## 权限、路由与敏感表单的实际行为

`sessionPages` 共用元数据拥有空间、标题、本人会话/权限条件与受限访问条件；导航和 beforeLoad 使用同一规则。只登记四个实际保护页面，不加员工/角色/租户或设备列表。`hasPermission/usePermission/PermissionBoundary` 按空间精确代码默认拒绝；没有角色名推断、通配符扩权或持久化权限。Query保留后端各权限grant的范围关联，不取跨权限最大范围；前端呈现不替代后端授权。

每次进入保护路由重验me；手动重试/刷新、Query可见性机制与当前保护空间的window focus监听是实现的重验触发点，不对普通渲染发me。明确权限/受限错误使本空间me重验。授权版本、权限/范围、门店、主体/会话及STAFF受限标记改变时，先清旧作用域，再把新me投影交给新代际，然后通知路由/观察者，避免中间空事实。刷新失败为可重试身份状态，暂停敏感表单，不能视为已经退出；没有Zustand权限镜像。原生focus实际触发已在本轮续验确认，详见后文。

安全写沿用空间过渡锁和取消机制，白名单构造请求，不能自动重试。官方AntD Form/Input.Password拥有草稿，密码原样提交，确认密码仅前端使用。同步提交锁、pending禁用、current-password/new-password、中文label和键盘提交已验证。普通成功me重验不覆盖未保存输入；成功/失败/退出/卸载清密码。Mutation variables始终undefined、retry=false/gcTime=0，并在完成/卸载reset。局部字段和请求对象的密码引用也清理；这不承诺JavaScript内存物理擦除。422只映射精确currentPassword/newPassword，原型或未知嵌套路径只显示总错误，不动态写属性。

成功先清本域Query/CSRF/旧请求，统一通知“密码已修改，请重新登录”或“当前账号的全部设备已退出，请重新登录”；不自动重登或填新密码。后续旧401不能覆盖成功文案，旧代际不能清新登录。另一空间的事实、CSRF、Cookie和布局状态不误清。PENDING仍是逻辑撤销成功，前端不要求用户操作Redis。

403 CSRF_INVALID仅使本域CSRF过期，不重放原写；权限403、当前密码确认403、PASSWORD_CHANGE_REQUIRED分别处理。409要求重验，422白名单，429使用Retry-After；网络/超时/协议/取消/503不证明写入已回滚，不提示安全操作成功。结果未确认由runtime单一通知，提供身份重验，禁止直接重复提交；当前退出保留P06-01“退出未确认”语义。

## 正式后端与真实浏览器证据

工作目录均为项目根目录。使用独立tmpfs `pet-p06-02-pg`/`pet-p06-02-redis`、冻结镜像、三个正式角色脚本、V1～V4独立迁移和正式 `backend-identity.sh bootstrap/platform-bootstrap --password-stdin`。生产JAR复用P06-01产物；后端源码未改，runtime非superuser/非BYPASSRLS，local配置、微信关闭、无测试profile/Gateway，readiness实际200 UP。初始化与命令/退出码见[环境](evidence/P06-02/environment-before-final.json)、[后续隔离环境](evidence/P06-02/environment-before-focus.json)、[focus补验环境](evidence/P06-02/environment.json)。未修改日常账号或数据库。

正式初始化的tenant-admin受重置保护且不能由本人管理员重置自己，因此强制标记在**本轮隔离库**准备：设置 `password_change_required=true`，增加security_version/resource version；普通权限减少也仅在隔离库准备并增加authorization_version。随后全部登录、me、拒绝、改密及撤销走正式后端；不声称管理员UI或正式reset接口完成了准备，不弱化既有重置保护。

浏览器为Google Chrome 154.0.8037.98的隔离上下文，后续最终页面通过CUA应用内浏览器补验。应用构建使用冻结Node；早期独立浏览器辅助进程使用bundled Node 24.19.0，后续辅助进程使用冻结Node 24.21.0，不更改工程依赖。证据分段汇总，**不是每个早期完整脚本均退出成功**；各次失败保留，不能把一个PASS段解释成整批成功。

| 真实验收项 | 实际结果与证据 |
| --- | --- |
| STAFF正常改密、旧密码拒绝、新密码登录 | PASS；[主链路](evidence/P06-02/browser-results-main.json)及[最终CUA](evidence/P06-02/browser-results-cua.json)，首尾空格参与密码，未trim |
| PLATFORM相同链路 | PASS；主链路及最终CUA，最终版本再次实际修改、旧密码拒绝、新密码明确登录 |
| 同浏览器两身份不互清 | PASS；两套Cookie/CSRF共存，员工改密/退出后平台仍200，反向也成立 |
| 第二独立浏览器上下文旧会话失效 | PASS；主链路中两域改密/全撤销后另一上下文各旧me401；不伪造设备数量 |
| 正式受限状态登录与直接路径/returnTo | PASS；[受限补验](evidence/P06-02/browser-results-resume.json)、最终CUA直接/admin回/admin/security，登录返回目标不能绕过、无循环，改密后正常导航恢复 |
| 后端受限普通请求拒绝 | PASS；受限过滤器真实403 PASSWORD_CHANGE_REQUIRED。探针普通路径未实现CRUD，403来自正式过滤器，不称管理页面/API已开发 |
| 错误当前密码、确认不一致、重复提交 | PASS；真实403确认错误/字段清空，确认不一致不提交，重复Enter仅一次PUT，主链路有请求计数 |
| 字段错误与CSRF/Cookie | PASS；[最终Chrome补验](evidence/P06-02/browser-results-final.json)/[HTTP白名单](evidence/P06-02/http-observations-final.json)：同密码422只有总错误，直接DTO短密码422含newPassword明细；缺失或另一空间CSRF均403，sid为HttpOnly/独立Path/SameSite=Lax |
| 实际权限减少与无管理权限本人能力 | PASS；Staff普通权限0仍可改密/全退出；平台移除credential权限隐藏改密，移除session权限403页；最终CUA未清员工空间 |
| 真实Redis不可用及恢复 | PASS；暂停本轮Redis后“请求结果未确认”，身份失败未视为退出，恢复后只重验仍有效身份、不宣称全退出。敏感写无自动重放次数断言另外由受控测试证明 |
| 普通me刷新保留草稿 | PASS；最终CUA刷新后没有重填新密码，仅填写当前/确认密码即正式改密成功、新密码可登录；受保护值等值DOM探针本身NOT_VERIFIED，不用其false结果伪造PASS |
| 桌面、390px、键盘、控制台 | PASS；最终1280px与390px空密码截图，scrollWidth≤390，Chrome label/autocomplete和Tab到新密码、键盘提交通过。CUA最终warn/error日志为空；Chrome预期401/403/422资源错误另记，不能当作零网络反例 |
| 原生窗口重新聚焦 | **PASS（续验）**；[实际事件](evidence/P06-02/resume-native-focus-2026-10-08/native-events.json)、[续验结果](evidence/P06-02/resume-native-focus-2026-10-08/browser-results.json)。前轮[原生focus边界](evidence/P06-02/native-focus-boundary.json)中的NOT_EXECUTED及工具拒绝保留为历史；未将bringToFront或手动重验改称focus通过 |

最终无凭据截图：

- [员工桌面](evidence/P06-02/staff-security-1280-cua.png)、[员工390px](evidence/P06-02/staff-security-390-cua.png)
- [平台桌面](evidence/P06-02/platform-security-1280-cua.png)、[平台390px](evidence/P06-02/platform-security-390-cua.png)
- [受限390px](evidence/P06-02/staff-restricted-390-cua.png)、[Redis未确认](evidence/P06-02/redis-uncertain-cua.png)、[权限403](evidence/P06-02/platform-permission-denied-cua.png)、[全退出成功](evidence/P06-02/logout-all-success-cua.png)

没有HAR、录像或Body记录；请求观察只method/path/status、CSRF存在布尔值、Cookie名称/属性，不含值。截图密码为空或失败诊断明确遮罩。两轮[凭据扫描](evidence/P06-02/secret-scan.json)/[focus补验扫描](evidence/P06-02/secret-scan-focus.json)退出0；历史临时凭据未保留，无法重新精确扫描其值，不能承诺扫遍所有历史秘密。技术输入均已清除，当前账号随tmpfs停止消失；[最终收尾](evidence/P06-02/cleanup-final.json)记录本轮进程终止、5173/18086关闭、专用容器结束、原三容器仍exited，既有卷未改。

## 自动化与最终命令

5个文件120项通过，原77项保留、新增43项。新增确定性覆盖空间隔离/默认拒绝、导航与直接守卫、受限无循环/returnTo、自身操作、密码Unicode/不trim/确认、422原型路径白名单、409/429/503/网络/取消语义、无自动重放/变量清理、两域缓存与旧401、新事实原子交付及刷新失败区别未登录。替身只用于技术状态与竞态；不称这些为真实外部服务或生产竞争证明。密码Form的实际输入/提交/草稿由真实浏览器补充。

冻结工具通过进程PATH/JAVA_HOME选择，没有修改全局配置。以下工作目录均 `/Users/kimwell/work/pet-platform`；每项JSON包含命令、退出码、时长及输出摘要，log保留完整技术输出。

| 命令 | 退出码 / 结果 | 证据 |
| --- | --- | --- |
| pnpm --filter @pet/admin-web typecheck | 0 / COMPILED | [typecheck](evidence/P06-02/typecheck-final.json) |
| pnpm --filter @pet/admin-web lint | 0 | [lint](evidence/P06-02/lint-final.json) |
| pnpm --filter @pet/admin-web test | 0 / 5文件120项 | [test](evidence/P06-02/test-final.json) |
| pnpm --filter @pet/admin-web build | 0 / COMPILED，体积警告保留 | [build](evidence/P06-02/build-final.json) |
| pnpm contracts:check | 0 / 10项导出/模型检查及类型一致 | [contracts](evidence/P06-02/contracts-check.json) |
| pnpm check:repo | 0 | [repo](evidence/P06-02/repo-final.json) |
| git diff --check及保护摘要/文档链接审计 | 0 | [audit](evidence/P06-02/final-audit.json) |

入口JS **714.55 kB / gzip235.61 kB**，P06-01为711.10/234.59，分别增加3.45/1.02 kB。最终布局93.86/29.47、登录3.57/1.77、错误状态61.01/22.83 kB；实际产物见build日志。没有提高500kB阈值、依赖变化或无关拆包。后端未改，不无理由重跑完整421项；本轮10项契约检查不替代421项回归。

## 变更、失败与保全

16个Web文件涉及路由/布局、AuthService/LoginPage、SessionRuntime/失效/返回目标、权限能力和本人安全页/校验/测试，逐项见[变更清单](evidence/P06-02/changed-files.json)。文档为本报告、WEB-STATE、WEB-PAGES、LOCAL-DEVELOPMENT、ROADMAP、DECISION-LOG、ACCEPTANCE-MATRIX及README；其余仅本轮证据。ui、后端源码、生成类型/公开契约、小程序、infra、冻结依赖/锁、AGENTS和P06-01证据按初始SHA256比较，保全结果见final-audit，不修改无关历史成果。

保留初期类型unknown/unused、eslint mixed-export/hooks依赖、测试重复消费Response及错误签名等失败，修正后最终检查通过。浏览器按钮文字空格、fetch.status属性、实际422文案和过早URL/导航count造成的失败也保留。登录根据正式me直接选安全页，权限变化改为新投影先交付后通知；实际手动刷新与权限减少复验通过。早期动态导入探针不足以证明活跃Query实例，不能据其判断产品刷新失败；所谓“另一实例”只为当时未证实假设，最终不作事实结论。旧的staff-security-390.png等加载态截图不是最终布局证据；使用上述最终CUA截图。

前轮原生focus失败没有通过伪造事件或检查源代码改称真实PASS；本轮用新增真实证据闭合G11。生产HTTPS/代理/跨源、Redis ACL/HA部署、远程CI、多OS、实时跨标签推送撤权、真实设备名单和全部未来业务均未验证/未实现，不纳入本轮PASS。PLATFORM无强制改密机制是实际既定契约，本轮没有模拟或扩展后端权限体系。

最终审计首轮因自身final-audit.json尚未写出而报一个缺失链接，[首轮结果](evidence/P06-02/audit-initial-self-link.json)保留；报告生成后复验退出0，1773个保护文件摘要无变化。本地文档链接全部有效，最新数量以final-audit为准。

## 原生窗口focus续验（2026-10-08 22:35～23:01）

本轮没有修改Web、后端、公开契约或依赖。新建tmpfs `pet-p06-02-focus-pg` / `pet-p06-02-focus-redis`，正式角色、V1～V4迁移、bootstrap/platform-bootstrap均退出0，生产JAR受限runtime readiness 200 UP；[环境与命令](evidence/P06-02/resume-native-focus-2026-10-08/environment.json)。使用CUA原生Google Chrome新建无痕窗口，与用户已有窗口隔离；同一无痕上下文正式登录STAFF/PLATFORM。授权减少仅通过隔离测试库准备并递增authorization_version，不伪称管理UI操作。

| 原生场景 | 事件、请求和页面结果 |
| --- | --- |
| 最小化后撤销平台改密权限，再通过Window菜单恢复 | 隐藏期间me保持STAFF=6/PLATFORM=12；22:52:36.310收到trusted focus，22:52:36.927恢复visible，平台me增至14且200、STAFF仍6。改密表单隐藏，退出全部设备保留；该场景包含visibilitychange，不单独据此证明可见时focus监听 |
| 通过Window菜单切至已有空白窗口，在隔离库撤销平台session:manage，再切回 | 22:57:21.159可信blur、22:58:12.238可信focus，visibility始终visible，两事件间没有visibilitychange。平台me在离开期间保持15，返回后16且403；员工仍6。当前/platform/security进入统一“当前账号没有访问权限”，旧表单/导航不再展示 |
| 同上下文直接/admin/security | 无重新登录，员工me200，修改密码与退出全部设备表单仍可见；平台Cookie或授权变化没有清员工会话 |

[事件白名单](evidence/P06-02/resume-native-focus-2026-10-08/native-events.json)仅包含实际事件类型、isTrusted、时间、visibility及hasFocus；临时控制台观察器只监听，没有dispatch合成事件、修改业务状态或触发me。窗口切换由原生Chrome菜单makeKeyAndOrderFront完成；AX Raise、点击当前标签、访达键盘目标未提供可证明focus，继续记NOT_VERIFIED/OBSERVED，不算PASS。[HTTP观察](evidence/P06-02/resume-native-focus-2026-10-08/http.json)只透传并记录白名单，不存Body/Cookie值/密码。原生工具调用无shell退出码，结果记TOOL_COMPLETED/exitCode:null。

空密码证据：[恢复窗口后](evidence/P06-02/resume-native-focus-2026-10-08/platform-after-native-restore.png)、[可见窗口focus后的403页面](evidence/P06-02/resume-native-focus-2026-10-08/platform-after-window-focus.png)、[员工空间保全](evidence/P06-02/resume-native-focus-2026-10-08/staff-after-platform-revoke.png)。控制台只有匿名身份检查预期401和权限撤销预期403资源错误，没有新增JavaScript运行异常。该续验不重新宣称390px、多OS或生产网络验收，沿用前轮布局证据。

本轮7个仍可读取技术输入精确扫描3447文件无命中，退出0；[扫描边界](evidence/P06-02/resume-native-focus-2026-10-08/secret-scan.json)。[收尾](evidence/P06-02/resume-native-focus-2026-10-08/cleanup.json)及[UI恢复](evidence/P06-02/resume-native-focus-2026-10-08/ui-cleanup.json)：仅停止本轮三进程组、两tmpfs容器，5173/18088/18089关闭，技术秘密文件清除，原三容器仍exited；观察器移除、辅助访达及无痕窗口关闭，用户原窗口保留。未修改日常账号密码或数据库。续验保护摘要/链接与仓库检查见[最终审计](evidence/P06-02/resume-native-focus-2026-10-08/final-audit.json)、[check:repo](evidence/P06-02/resume-native-focus-2026-10-08/repo-check.json)。无代码变化，不无理由重复六项构建检查或完整后端421项；前轮六命令退出0、120项Web/10项契约检查继续有效。

## G01～G14与最新任务状态

| 门禁 | 结果 | 依据 |
| --- | --- | --- |
| G01 实际契约/权限/受限状态 | PASS | 编码前映射、正式源码/生成类型及真实接口 |
| G02 分空间权限/default deny | PASS | 受控测试、真实权限下降及零管理权限本人链路 |
| G03 路由/导航/直接访问一致 | PASS | 共用元数据、路由测试与最终直接/admin限制 |
| G04 强制改密真实无循环 | PASS | 正式受限标记、后端403、最终改密/新登录 |
| G05 本人改密真实后端 | PASS | 两域真实PUT及旧密码拒绝/新密码登录 |
| G06 本人全会话撤销 | PASS | 两域POST、独立上下文旧me401与最终代码补验 |
| G07 密码无持久化/日志泄漏 | PASS（应用范围） | undefined变量/reset/引用清理、空截图、白名单日志与精确扫描；不承诺物理擦除 |
| G08 错误/CSRF/未确认 | PASS | 真实403/422/Redis故障，确定性错误及不重放测试 |
| G09 旧请求与失效竞争 | PASS（技术竞态） | 原77项及新增旧401/成功通知/原子投影测试；不外推生产竞争 |
| G10 双身份空间保全 | PASS | 真实两域/多上下文相互保留、范围测试 |
| G11 真实浏览器完整验收 | **PASS** | 前轮安全/布局/错误流程；续验真实可信focus、当前空间权限收缩/403及STAFF保全 |
| G12 类型/静态/测试/构建 | PASS | 六项实际命令退出0；120项Web/10项契约检查 |
| G13 无关成果保全 | PASS | 两轮初始摘要比对、资源及原窗口恢复；续验1814项保护文件不变 |
| G14 文档与事实一致 | PASS | 更新当前状态，保留历史失败/未执行及生产限制；未关闭P06整体 |

前轮状态（历史）：**P06-02 IN_PROGRESS；P06 IN_PROGRESS**，G11 PARTIAL；当时下一任务为原生窗口focus续验，未进入综合验收。旧证据没有改写。本轮实际补齐该缺项，复核G01～G14 PASS，因此最新状态 **P06-02 COMPLETE；P06 IN_PROGRESS**。

按实际路线，下一建议为 **P06 Web阶段综合验收（建议拆为P06-03）**：综合核对既定A06-01/A06-02及阶段完成条件，再判断P06是否可关闭；该拆分尚未授权执行。没有自动进入P06综合验收/P07或其他任务，没有提交、推送、发布或部署。
