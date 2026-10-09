# B02 平台控制面管理集中验收

日期：2026-10-09。项目：`/Users/kimwell/work/pet-platform`。当前 **B02 COMPLETE：实现、两条正式页面/接口主链、真实 CUSTOMER 生命周期及集中检查全部通过**。**P07 IN_PROGRESS；B03 NOT_STARTED。** 本报告拥有本批验收结论；不以历史数量、客户技术会话、源码或文档检查替代真实主链。

## 范围核对与原批次

读取实际代码、[B01主报告](B01-ACCEPTANCE.md)、[路线](../development/ROADMAP.md)、身份/初始化/P05认证安全、B01授权、Web请求/Query、生产角色与生成契约。B01 COMPLETE，历史469后端/254 Web；B02此前NOT_STARTED且具准入条件。当前完整回归另行执行，共490后端/266 Web。

| 原任务与批次 | 本轮实际状态 |
| --- | --- |
| B01 / P07员工、角色授权及P07-01/02/03 | 原COMPLETE保留，469个测试方法本轮全部执行，无方法丢失 |
| B02 / 原platform Tenant、最小Store治理 | 租户列表/筛选分页排序/详情/创建/改名/启停及可执行初始化衔接；正式门店目录/创建/改名/启停，PLATFORM独立元数据入口 |
| B02 / P05-04平台身份管理扩展 | 固定权限集合、账号列表/详情/创建/改名/启停/授权、重置密码/全平台会话撤销，复用安全版本和补偿清理 |
| B02 / 原P07 Organization及页面 | 冻结并实现独立租户扁平组织：id/tenantId/code/name/status/资源版本/时间，公开必要投影；正式列表/详情/创建/改名/启停，TENANT权限与FORCE RLS |
| B02集中验收 | COMPLETE，两条正式页面/接口主链、真实微信 CUSTOMER 生命周期、集中检查及资源收尾通过 |
| P07整体 | IN_PROGRESS，原A07-02批量部分失败和A07-01删除末页回退仍归B03且未验 |
| B03～B05 | NOT_STARTED；不自动推进 |

原路线与冻结模型没有邀请作为必选任务、邀请投递/接受模型或契约。本批提供正式创建与安全密码输入，不虚构邀请能力；P05历史“账号管理/邀请未实现”是当时范围，不删除已冻结任务。未新增套餐、订阅、计费、配额、品牌、模拟登录、通用切换租户、行业门店业务或跨租户业务导出。

## 实现、接口、权限与状态

23个新增正式操作、16个新增路径，当前公共OpenAPI共46路径/57操作。完整路径/方法/请求响应/字段/排序/状态/并发/初始化与失效事实源为 [CONTROL-MANAGEMENT](../contracts/CONTROL-MANAGEMENT.md)。Web/小程序类型从生产Controller生成，无手写平行DTO。

PLATFORM只接受本域正式Cookie/CSRF身份，STAFF/CUSTOMER及同UUID其他空间拒绝；客户端tenantId不能建立可信上下文。列表、详情、创建、资料、启用、停用、授权、重置与撤销分别授权；只读不授写。平台账号权限为操作者当前可授集合子集，保持两项本人安全权限，不引入平台角色体系或重建STAFF角色。

控制面经 ControlManagement → 两个明确 application端口 → ControlAccountJdbc/ControlTenantJdbc → 固定数据库函数。新增NOLOGIN、NOSUPERUSER、NOBYPASSRLS、非表owner的pet_control_manager_owner，不授runtime成员/表读写。只对元数据/平台账号安全/固定事件开放精确入口，不读任意租户业务。结构规则仅注册具体类型及调用者，任意服务/其他Jdbc反例测试通过，不作包级豁免。

原V1～V6保全，新增V7/V8；[独立角色脚本](../../infra/database/provision-control-management-roles.sql)必须在正式迁移前显式配置。runtime启动继续JPA validate及权限/FORCE RLS检查。部署条件与显式升级分别见 [平台初始化](../development/PLATFORM-BOOTSTRAP.md)、[租户初始化](../development/IDENTITY-BOOTSTRAP.md)、[持久化](../conventions/PERSISTENCE.md)、[迁移](../conventions/DATABASE-MIGRATION.md)。

租户创建ACTIVE、initialized=false，不自动创建员工/假门店。bootstrap按同编码/当前名称补齐首位STAFF、保留角色和初始授权，原子完成initialized；不要求Store。停用、同名不符、已初始化或未知半成品拒绝，失败全部回滚，确认未提交后可重试，完成后重复不覆盖。B01/组织管理权限独立显式升级，幂等；普通迁移/启动不自动赋权。页面明确“待初始化”，既有受控CLI本轮真实执行成功。

租户启停均推进权威安全版本，旧STAFF/CUSTOMER会话与受控异步快照执行时重验，重新启用不复活旧会话。门店状态保守推进所属租户安全版本。平台权限/启停/重置/会话撤销在事务中推进对应安全与授权版本并保存精准清理意图；Redis失败不放行旧身份，补偿不误删新代际/STAFF。

本人危险管理操作禁止，沿用本人安全入口；最后一个ACTIVE且同时具有本人会话/改密、account:grant/enable/reset-password的治理入口必须保留。固定事务advisory锁、当前操作者/目标行锁、版本及提交前重验保护并发，真实PG并发测试通过。普通详情不返回散列、内部安全版本或秘密；成功事件与目标状态同事务，不物理删除账号/租户。

## 本轮真实主链与反例

正式生产JAR local profile、同源Vite、受限runtime、冻结PostgreSQL/Redis临时容器；正式bootstrap和页面创建账号，不使用测试身份解析器、Mock网关、测试API或SQL业务数据准备。SQL仅用于角色预配置、故障注入与只读观察，不能冒充创建/初始化成功。

链路A（完整通过）：

1. 平台正式页面登录、页面创建b02-acceptance，详情明确待初始化。
2. 生产JAR现有bootstrap通过安全stdin建立首位STAFF并衔接同一UUID；两项显式升级成功，不创建假门店。
3. 正式页面编辑资料、建立正式最小Store；STAFF登录使用B01员工列表；创建/编辑/停用独立Organization，390px与Enter确认通过；随后实时wx.login经正式WxJava建立同租户CUSTOMER，登录/me均200。
4. 正式页面停用租户，旧STAFF me401；对照租户B及PLATFORM仍200。
5. 停用期间新STAFF经正式登录页面401；真实CUSTOMER旧me401、新wx.login认证401。
6. 正式页面重新启用，旧STAFF/CUSTOMER仍401；新STAFF登录及已有管理模块有效，真实客户新wx.login认证200且仍为同一主体。STAFF退出后PLATFORM仍有效，再独立退出平台，客户新会话独立退出。

续验在全新独立数据库重新通过正式页面创建/CLI初始化，证据：[创建与初始化](evidence/B02/completion/browser-stage1.json)、[合并主链15组场景](evidence/B02/completion/browser-stage2.json)、[真实CUSTOMER十步HTTP记录](evidence/B02/completion/customer-real.json)，三个运行均退出0。第一阶段 [STAFF生命周期](evidence/B02/browser-staff-lifecycle.json)保留。

链路B：正式页面创建b02-reader及当前可授只读权限→新账号正式登录→可读租户、无创建/停用按钮、正式写403→权限配置撤销detail→旧身份401及旧数据清理→新身份直达详情403→正式页面重置密码/全平台会话撤销/停用→旧平台身份失效，最终账号DISABLED。STAFF和操作者不被误清。

续验[最终数据库](evidence/B02/completion/final-database.json)核对租户ACTIVE/已初始化、恰好1个员工/1个门店、一个正式微信客户主体；reader DISABLED、首位平台管理员ACTIVE，平台清理待办0，runtime无高权限。只读观察不读取密码或内部安全版本。第一阶段额外治理账号的创建/撤权/草稿清理及停用见原证据，其[只读执行结果](evidence/B02/final-database-accepted.log)保留。续验快照独立存放；第一阶段原始快照因脚本路径错误被覆盖，问题和原摘要见 [路径纠正](evidence/B02/completion/evidence-path-correction.json)，不把续验状态冒充历史快照。

第一阶段9组账号场景已通过，但等待客户工具时合并运行退出1，原失败在 [原始账号运行](evidence/B02/browser-stage2-first-failure.json)保留。续验同一完整脚本重新执行账号全部操作及租户/CUSTOMER生命周期，15组场景全部通过且退出0，不能将浏览器场景与JUnit数量相加。

第一阶段补充正式页面验证（续验47个源码/生成产物与该阶段一致）：

- URL筛选/分页/排序与详情返回，真实重复编码422字段错误，真实并发API改名触发409且禁盲目重试，关闭后重读权威资料；最小门店资料编辑及启停通过，记录在 [运行及中途脚本失败](evidence/B02/browser-supplement-fourth-failure.json)。
- 真PG执行权限故障503，页面错误不伪装空列表/不误退出，恢复后列表正常；真Redis暂停503及恢复；真实浏览器断网及恢复；正式404、非法状态422、未知tenantId参数400；390px无页面横向溢出及键盘Enter筛选，[故障补验](evidence/B02/browser-supplement.json)完整退出0。
- 第二治理账号由正式页面创建并配置操作者可授权限→撤销当前操作者编辑权限→旧写401、页面/敏感草稿销毁→新身份无编辑入口→恢复仍需新登录，表单只含权威资料→停用额外治理账号，保留管理入口，[操作者草稿清理](evidence/B02/browser-draft-authority.json)退出0。

[20项真实PG/Redis/HTTP控制面IT](evidence/B02/backend-test-results.json)另外覆盖：同UUID跨域、STAFF/CUSTOMER控制面拒绝、读取拒写、授予上限、重复/排序分页/参数、并发版本冲突、最后管理员互相停用并发、本人保护、初始化审计/关系失败回滚重试/未知半成品、租户B隔离、技术客户旧会话不复活、排队受控异步停用后重启拒执行、数据库和Redis安全拒绝恢复、平台安全精确失效及RLS直接表/owner/私有函数拒绝。该客户会话由测试代码登记，**只作技术夹具，不等于真实微信认证/业务验收**；测试探针不进生产JAR或公共契约。

## 真实CUSTOMER门禁关闭

使用既有私有真实微信入口配置、冻结开发者工具2.02.2608080和SDK3.17.2、固定automator0.12.1；真实wx.login实时code→现有WxJava→正式customer登录200，未使用测试网关/测试身份/SQL客户主体。客户访问PLATFORM和STAFF正式接口分别401 AUTH_DOMAIN_MISMATCH；租户停用旧客户401 SESSION_REVOKED、新认证401 LOGIN_FAILED；重新启用旧客户仍401、新认证200、主体UUID不变、me200、logout200。HTTP只记录方法、路径、真实状态、错误码和traceId，code/Token/秘密不进入日志、URL、截图或报告。

第一阶段服务端口关闭的三次启动失败和NOT_EXECUTED原始证据保留：[第二次](evidence/B02/customer-real-second-failure.json)、[第三次](evidence/B02/customer-real-third-failure.json)、[第一阶段最后结果](evidence/B02/customer-real.json)。用户要求继续验收后，本次实际读取安全设置发现服务端口已开启46904，使用现有端口，没有修改安全设置、令牌、登录票据或全局信任配置；[设置基线](evidence/B02/completion/devtools-initial.json)。原先单次确认待回复与未执行结论已由本次真实执行关闭，不改写历史失败。仅打开自己的临时项目，完成后关闭探针并保留原项目。

使用 [miniprogram-development技能](/Users/kimwell/.agents/skills/miniprogram-development/SKILL.md)准备与执行真实工具探针。工具依赖仅放忽略目录，安装使用pnpm --ignore-workspace --ignore-scripts，未改工程依赖和锁文件；完成后与本轮凭据/探针全部清理。

## 集中命令与实际数量

准确工作目录/参数/退出码/输出日志及耗时逐条见续验 [commands.jsonl](evidence/B02/completion/commands.jsonl) 和第一阶段 [命令记录](evidence/B02/commands.jsonl)，续验运行环境/正式身份/迁移/生产JAR启动见 [runtime-setup](evidence/B02/completion/runtime-setup.json)。长运行启动仅STARTED/readiness200，不伪造进程退出码。所有数量均本轮实际执行；历史469/254只是基线。

| 收尾检查 | 工作目录 | 本次续验实际结果 |
| --- | --- | --- |
| `./mvnw --batch-mode verify` | apps/backend | [串行退出0](evidence/B02/completion/backend-verify-serial.log)，147单元+343集成=490，失败/错误/跳过0，原469+新增21 |
| `pnpm check:web` | 项目根→admin-web | [退出0](evidence/B02/completion/web-check.log)，typecheck/lint/test，9文件266项，原254+12 |
| `pnpm build:web` | 项目根→admin-web | [退出0](evidence/B02/completion/web-build.log)，主入口728.75kB；500kB警告保留，不调整阈值 |
| `pnpm contracts:generate` | 项目根 | [退出0](evidence/B02/completion/contracts-generate.log)，10项导出检查及共享类型通过 |
| `pnpm contracts:check` | 项目根 | [退出0](evidence/B02/completion/contracts-check.log)，10项及产物一致性通过 |
| `pnpm contracts:typecheck` | 项目根→api-contracts | [退出0](evidence/B02/completion/contracts-typecheck.log)；generate/check另行再次执行 |
| `pnpm check:miniprogram` | 项目根→wechat-miniprogram | [退出0](evidence/B02/completion/miniprogram-check.log)，类型和lint；不等于真机验收 |
| `pnpm check:repo` | 项目根 | [退出0](evidence/B02/completion/repo-closure.log) |
| `git diff --check` | 项目根 | [退出0](evidence/B02/completion/diff-check-closure.log) |

完整XML逐类/逐方法结果在契约清理target前保存为 [续验测试清单](evidence/B02/completion/backend-test-results.json)。本次完整verify第一次490项/1错误不计PASS；串行第二次490项全部通过，不累计两次数量。生成/检查各10项为重复门禁，不与490相加。第一阶段490/266实际执行日志仍保留，不与续验重复计数。

第一阶段Maven完整回归与契约导出顺序执行，XML结果在契约清理target前保存在 [测试清单](evidence/B02/backend-test-results.json)。[B01方法与保全核对](evidence/B02/preservation.json)确认469方法无缺失、272个UI及97个B01报告/证据摘要不变；V1～V5与Git、V6与B01清单一致，冻结锁/版本/依赖未修改。续验 [保全核对](evidence/B02/completion/preservation.json)直接比对B01全部469方法、97个证据/报告、270个Git已跟踪UI文件和第一阶段47个源码/生成产物，方法无缺失/文件无变化；[当前生成产物](evidence/B02/completion/production-contract.json)一致且无测试路径/散列/内部安全版本。[生产产物](evidence/B02/production-artifact.json)核对无测试身份/探针/夹具、写入密码writeOnly、普通DTO安全。

## 历史失败与修复

- 编译首次出现PlatformPermissions同名导入歧义，改明确所属类型，后续通过。
- 契约首轮使用全局pnpm10.33失败，改为既有隔离冻结10.34.6；第二轮错误改了customer.resolve_tenant返回类型，恢复原TABLE签名及最小initialized列权限，第三轮及最终通过。
- 目标IT首轮客户函数initialized列权限不足和PUT反例误用GET期望404，修正权限和实际方法后18项通过；后续增加异步/半成品2项。
- 全量首轮新组织FK使老夹具TRUNCATE不完整、公开路径数量尚用30，并与同步编辑未发布V7出现checksum冲突；最小扩展夹具清理/预期路径，之后禁止边跑Maven边改迁移，保留 [首轮](evidence/B02/backend-full-first-results.json)。
- 全量第二轮6条初始化失败，定位为PL/pgSQL返回tenant_id与未限定列同名，显式表别名修复；[第二轮](evidence/B02/backend-full-second-results.json)保留。20项目标复验及完整490通过。本轮无卷环境重建并重新执行正式迁移/页面创建，不repair既有数据库；[环境重建](evidence/B02/runtime-rebuild.json)。
- 页面脚本首次精确“登录/取消”未匹配Ant Design二字按钮自动空格；补验等待关闭弹窗时遇多个退场动画；不存在UUID初次使用非v4导致实际400而非预期404。仅修验收脚本选择/等待/规范UUID，原失败日志和场景保留，补验退出0，不伪装产品修复。
- 最终数据库核对初次误把全部清理历史记录数当未完成数，断言失败保留；按既有completed=false规则更正，7条历史记录已完成，实际待办0。首次只读查询误用客户表名也报错，改按V4正式customer_subject，仅观察未写数据。
- 清理初次将Maven运行期间快照中的临时容器也要求永远保留，断言失败；按准确所有权核对两个本轮命名容器、88个本轮日志ID和既有命名基础设施，保留4个临时容器变化及未直接清理的事实，不声称整个Docker快照不变。
- 第一阶段平台账号9组功能全部成功后，客户工具未就绪导致运行退出1；续验重新执行完整两条主链及真实客户认证，退出0，原失败保留。
- 续验首次环境迁移因契约检查清理了生产JAR而退出2，未执行迁移；先清理本次两容器，实际verify产出生产JAR后重建正式环境成功，[准备失败](evidence/B02/completion/runtime-setup-missing-jar.json)保留。
- 续验浏览器首次账号创建入口超时，失败截图停留列表；增加等待正式账号列表首行后完整重跑通过，只改验收脚本，保留 [首次失败](evidence/B02/completion/browser-stage2-initial-failure.json)。
- 续验数据库脚本克隆时有一处具体输出路径未切换，覆盖了本批第一阶段同名JSON快照；已修输出路径并将真实续验快照归入completion目录。原快照完整内容无法恢复，不伪造；第一阶段原执行日志、页面/故障证据和快照摘要保留，B01及源码不受影响。
- 续验第一次完整verify执行490项，生产JAR文档策略请求出现HttpTimeoutException，0失败/1错误、退出1，[结果](evidence/B02/completion/backend-first-results.json)与 [日志](evidence/B02/completion/backend-verify.log)保留。首次运行与真实环境启动并行，随后先清理真实环境再串行复验，490项0失败/错误/跳过、退出0；未改产品源码、未放宽现有超时、不隐藏失败。并行负载是可能原因，不能把重跑通过当作确定根因证据。

## 文件、资源与未验证边界

本批集中在控制面Controller/application/Jdbc、V7/V8和独立角色/升级命令、B01管理聚合的Organization扩展、精确结构/迁移/产物回归、20项控制面IT、Web三个独立页面/API与12项技术测试、必要路由/草稿/样式、生成产物、权威契约/路线/初始化/迁移文档与本报告。B01此前未提交成果保留，不把它们全部当作B02新增。[文件清单](evidence/B02/file-manifest.json)和[最终核对](evidence/B02/final-audit.json)在最终记录中汇总。

本轮专用PG/Redis无持久卷，使用独立受限运行/初始化/迁移账号，秘密仅在忽略目录、受控stdin及内存；截图无技术凭据。续验数据库快照与第一阶段只读执行日志分别留存。续验[资源清理](evidence/B02/completion/cleanup.json)确认两容器、四服务端口及9421/9422无监听，25个本轮日志容器均已删除，忽略目录3402个工具/探针/凭据/会话文件已清理。第一阶段[清理](evidence/B02/cleanup.json)与88个容器核对保留；串行回归/契约临时容器另作最终核对。既有UI、卷、命名基础设施、冻结依赖和B01证据保全。开发者工具原服务端口由用户保持开启，本次从未更改该配置，不把既有服务作为本轮资源关闭。不提交、推送、发布或部署，不读取/引入Product Delivery OS。

DOCUMENTED：范围/模型/契约/部署要求；RESOLVED：冻结工具链/依赖；COMPILED：完整后端/Web/三端检查；RUNTIME_VERIFIED：正式PLATFORM/STAFF页面、真实wx.login/WxJava客户认证、真实PG/Redis、安全反例；NOT_EXECUTED：B03及后续、生产角色部署/TLS/远程CI/多OS/真机/真实辅助技术/容量与灾备。等级不互相代替。五张续验截图已目视检查，[秘密扫描](evidence/B02/completion/secret-scan.json)10种真实秘密值扫描5090文件，匹配0；只记录数量，不记录值。

**B02 COMPLETE；P07 IN_PROGRESS；B03准入条件满足但NOT_STARTED。** B02全部实现、正式主链、真实客户门禁及集中检查已通过，无本批功能或验收剩余项。P07原A07-02批量部分失败与A07-01删除末页回退仍归B03，须按原条件另行完成，不能由B01/B02完成推断P07完成。本轮不自动进入B03，不提交、推送、发布或部署。
