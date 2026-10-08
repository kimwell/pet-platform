# P05-05 客户身份、微信认证后端与身份阶段验收

**最新结论（2026-10-08 20:08 +08:00）：G12 PASS / RUNTIME_VERIFIED；P05-05 COMPLETE；P05 COMPLETE。** 真实微信四步及第二个新鲜code的客户复用通过，见第11节和[P05综合验收](P05-ACCEPTANCE.md#2026-10-08-真实微信通过后的阶段验收)。下一建议为P06首项，只提出、不执行。

以下第1～10节保留此前实现轮与缺秘密准备轮的历史事实；其中BLOCKED、NOT_EXECUTED和“下一任务”是当时结论，不覆盖本次真实执行结果。

日期：2026-10-08；根目录 `/Users/kimwell/work/pet-platform`；基础包 `com.pet.platform`。

**P05-05 BLOCKED；P05 IN_PROGRESS。** 独立后端实现、421项自动化回归及类型检查通过；G12的真实微信code交换/客户会话/me/退出尚未执行，不能用Gateway测试替身关闭任务。下一合法任务是本任务的真实微信续验，不进入P06、P09、P10。

2026-10-08续验最新结果：用户确认暂时无法提供AppSecret；已完成安全文件、专用入口、隔离正式迁移/初始化/生产后端与工具预检，四步仍NOT_EXECUTED。当前状态不变，详见第10节；此前BLOCKED与所有失败/未执行证据保留。

## 1. 实际前置与边界

已核对AGENTS、README、AUTHENTICATION/AUTHORIZATION/MULTI-TENANCY/MODULE-BOUNDARIES/CONFIGURATION、IDENTITY/API/OPENAPI-GENERATION、VERSION-MATRIX/ROADMAP/DECISION-LOG、P05-01～04报告和ACCEPTANCE-MATRIX，以及实际生产Provider、Sa/Redis、密码与撤销、SELF、RLS/受限函数、异步重验、安全记录、初始化/迁移代码。当前明确任务覆盖AGENTS历史P02范围限制；技术、保全及证据规则继续适用，不读或更新其他治理工作流状态。

前置P05-04实际通过记录为`verify-second`及其XML，共366项；其保留的`verify-final`是失败历史，不能作为成功证据。P05-03原328项及P05-04全部366项以(classname,testcase name)逐项与本轮XML比较，无缺失，见[测试汇总](evidence/P05-05/test-summary.json)。只更新实体/迁移数量与测试表清理范围，不删除旧测试或放宽其认证断言。

用户本轮明确归属：P05是客户主体、微信登录后端、会话和隔离；P09是页面、会话恢复、隐私授权及真机完整业务；P10是订阅消息、支付、RabbitMQ、Outbox。没有完整客户资料CRUD、手机号、员工OpenID绑定、UnionID/手机号自动合并、注销流程、平台租户页面。未提交、推送、发布、部署或迁移日常/生产数据库。

原路线的角色权限要求与管理API归属已回溯冻结提交`60481ce`：P05要求角色权限、安全会话与三域隔离；P07明确员工/组织/角色页面及真实创建/修改/停用基础API；原IDENTITY将管理重置具体接口安排P07，P05-03已提前完成该敏感安全子集。员工、角色和权限完整管理API仍未实现，继续是P07必选交付，不因本轮三域接入标为完成，也不临时挪走原P05必选项。见[原始条目](evidence/P05-05/original-stage-boundary.json)及[P05总验收](P05-ACCEPTANCE.md)。

## 2. 正式模型、并发与RLS

新增`customeridentity.domain.CustomerSubject`、`WechatBinding`及正式JPA映射；表为`public.customer_subject`、`public.customer_wechat_binding`。客户含UUID、tenant_id、ACTIVE/DISABLED、security_version、资源version、created_at/updated_at；绑定含UUID、tenant_id/customer_id、AppID、OpenID、状态和审计字段。OpenID仅在AppID范围内使用；唯一键`customer_wechat_identity_unique(tenant_id,app_id,open_id)`，另有同租户客户/绑定唯一关联和复合外键`(tenant_id,customer_id)`。当前一个客户只支持一条微信绑定，没有解绑/重绑或资料管理API。

同AppID/OpenID在不同租户创建独立客户。UnionID本轮无业务用途，不持久化；有无UnionID都不改变定位键，不自动合并。无登录code、AppSecret、session_key列，不返回或记录这些值；session_key只存在于SDK临时解析结果，不进入业务身份对象、应用Token或Redis。

只追加V4，V1～V3字节与HEAD保持一致。客户与绑定在受限函数短事务内原子创建；8线程同时首次注册仅一个客户/绑定，无孤立客户。仅准确识别上述绑定唯一约束的23505冲突后回滚该子事务并重读，其他约束/数据库异常不吞掉；测试插入故障确认客户回滚。HTTP并发会受原设备索引锁影响返回429，重新获取code可恢复；未承诺每个并发请求都立即成功。停用主体或绑定保留原映射、拒绝登录，不删除重建；会话签发失败不删除已合法客户，下次新code可复用。

认证前不能建立CustomerPrincipal或任意TenantContext。受限`pet_customer`五个固定函数负责有效租户解析、注册、当前身份投影、最小安全事件、版本撤销。专用NOLOGIN `pet_customer_auth_owner`非SUPERUSER、无BYPASSRLS；runtime不继承它，无建schema、表DML、OpenID直接读取或角色升权权限。函数固定`search_path=pg_catalog,pg_temp`，显式对象名、无动态SQL、撤销PUBLIC执行，仅runtime获EXECUTE；角色和表强制RLS在真实PostgreSQL中验证。租户FOR SHARE锁需要最小UPDATE(status)授权，专用UPDATE RLS为USING(true)/WITH CHECK(false)，不能实际修改租户。源SQL见[角色](../../infra/database/provision-customer-roles.sql)、[V4](../../apps/backend/src/main/resources/db/migration/V4__customer_wechat_identity.sql)。

运行role非owner/非超级/无BYPASSRLS验证：无上下文读零行，跨租户不可读，不能读取OpenID、DDL/TRUNCATE或SET ROLE，复合FK拒绝跨租户关联。新角色须由明确管理员先预配置再由既有独立迁移入口执行；普通应用不使用数据库超级账号。

## 3. 可信入口与微信Gateway

`WechatProperties`默认关闭且应用/入口为空；server entryId→application配置→明确tenantCodes白名单，tenantCode只用于查询有效租户。AppID、安全secretProperty引用、版本、100～10000ms超时、每分钟1～20次交换上限由服务端决定，最多16应用/64入口。只读取指定`PET_WECHAT_[A-Z0-9_]+_SECRET`安全属性，不读取无关秘密；客户端不能传AppSecret、任意URL或选择自由配置。

共享AppID入口允许用户在白名单有效租户建立新客户，知道tenantCode不是强身份认证，不授予员工、门店管理或其他客户权限。没有虚构邀请/成员限制；若未来业务需要，须增加明确入口策略。

生产Gateway使用矩阵冻结WxJava小程序4.8.0。业务仅接收appId/openId/可选unionId，不依赖SDK DTO。核对[官方4.8.0用户实现](https://raw.githubusercontent.com/binarywang/WxJava/v4.8.0/weixin-java-miniapp/src/main/java/cn/binarywang/wx/miniapp/api/impl/WxMaUserServiceImpl.java)及[基类](https://raw.githubusercontent.com/binarywang/WxJava/v4.8.0/weixin-java-miniapp/src/main/java/cn/binarywang/wx/miniapp/api/impl/BaseWxMaServiceImpl.java)：普通get具有access_token、重试与原响应日志路径，因此仅登录客户端覆写get，调用SDK官方SimpleGetRequestExecutor一次，URL固定微信jscode2session，Apache关闭自动重试和重定向、连接/读取/池等待超时受限。SDK/Apache原始日志级别强制OFF；无“微信故障模拟成功”。配置客户端缓存按配置身份/版本/AppID/秘密摘要/超时隔离，未使用全局可变配置或switchover；配置变更/轮换需要重启并提高版本。

| 情况 | HTTP / 安全错误 | 行为 |
| --- | --- | --- |
| 无效/已用code（40029/40163） | 401 WECHAT_CODE_INVALID | 不建客户/会话，获取新code |
| 网络或超时，结果不确定 | 503 WECHAT_RESULT_UNCERTAIN | 不重试旧code，不伪造成功 |
| 微信其他错误 | 503 WECHAT_UPSTREAM_ERROR | 固定中文提示，不透传原响应 |
| 非JSON/缺OpenID/应用不符 | 503 WECHAT_RESPONSE_INVALID | 拒绝认证 |
| 启用入口缺安全配置 | 503 WECHAT_CONFIGURATION_MISSING | 不调用微信 |
| 全局关闭 | 503 CAPABILITY_DISABLED | 无连接要求 |
| 无效/停用租户入口或客户/绑定 | 401 LOGIN_FAILED | 不泄露主体存在性 |

外部微信请求在数据库事务外。交换结果仅在CustomerHttpAuthentication认证服务→受限identity store的内部链路流转；结构检查禁止Controller或普通业务直接调用Gateway、构造已认证凭据或注册客户。数据库函数是数据库runtime的固定受限能力，应用模块通过准确类/方法规则限制，非任意runAsTenant入口。

WxJava新增后实际选择commons-io2.14.0，导致既有Testcontainers/commons-compress容器文件复制NoSuchMethodError；[冲突证据](evidence/P05-05/dependency-conflict.log)。最小修复显式固定**原基线实际解析的2.20.0**，未替换框架或覆盖Boot BOM版本。版本矩阵记载原因，最终[依赖树](evidence/P05-05/backend-dependency-tree.txt)确认WxJava4.8.0、commons-io2.20.0、Boot4.0.8、Sa1.46.0、Testcontainers2.0.5。

## 4. 正式接口、CUSTOMER会话和授权

| 方法与路径 | 输入/成功行为 |
| --- | --- |
| POST /api/customer/auth/wechat/login | tenantCode、entryId、code；返回CustomerTokenLoginResult，仅此接口签发Token |
| GET /api/customer/auth/me | 当前CustomerCurrentIdentity，真实数据库与会话投影 |
| POST /api/customer/auth/logout | 当前设备退出，data=null |
| POST /api/customer/auth/logout-all | 当前客户全设备安全代际撤销，data=null；X-Session-Cleanup=COMPLETE/PENDING |

只使用`X-Customer-Token: Bearer <raw token>`；30天绝对、7天闲置、最多5设备、独立设备，不签STAFF/PLATFORM/客户Cookie。认证维护/me不续闲置；退出全部无需客户密码。复用SaIdentitySessions/AuthenticationRedis唯一现有框架，customer独立Sa空间、`pet:<env>:customer:customer:*`及辅助`pet:<env>:auth:customer:*`；仅客户认证键TTL允许30天，其他域/业务上限不变。

每请求验证Sa期限/设备→有效租户、客户、绑定→客户与租户安全版本；签发前后也重验。DB/Redis故障503关闭，不能当作401密码错误或跳过检查。数据库全撤销原子增代际与LOGOUT_ALL记录，即使物理清理失败旧Token仍拒绝；PENDING由下一合法新登录清旧代际或原TTL清除，不新增调度/Outbox。并发全退出只成功增一次，延迟清理不删新代际。无分布式DB/Redis原子提交承诺；成功安全记录失败不返回Token并尝试撤销未返回设备，依赖持续故障可能保留受限期约束的悬挂设备。未来状态停用/恢复必须同事务增security_version，以防恢复旧Token；本轮无状态CRUD。

唯一生产CurrentPrincipalProvider接CUSTOMER，customerId不冒充employeeId；`DataScope(tenantId,CUSTOMER,customerId,SELF)`只授予`customer:session:manage`，无员工角色、门店管理或TENANT范围。严格CustomerCurrentIdentity不暴露OpenID、UnionID、session_key或passwordChangeRequired；STAFF/PLATFORM/CUSTOMER三种生成模型组成判别union，不放宽为全字段可空。

真实HTTP验证三类主体同UUID、不同域Token/两Web Cookie/命名空间仍独立；跨租户客户主体/会话不混用，客户不能访问员工或平台，反向凭据不被客户接口接受。客户明确在StaffTaskAuthority.capture入口403拒绝，未入队；员工既有捕获范围上限与执行前重验回归通过，不扩大Servlet ASYNC或沿用员工假设。

## 5. 频控与安全记录

MVC输入校验前Redis原子限制来源60次/分钟；交换同一来源跨所有应用合计20次/分钟，另按配置上限计数；未知字段400、非法格式422，异常输入不能无限消耗Gateway。所有已进入交换的成功/失败均计数，429含Retry-After；多实例共享Redis，不使用内存计数或信任X-Forwarded-For。Key用来源/配置摘要，无明文code/OpenID/手机号。Redis失败503，外部故障无自动重试。

`pet_customer.security_event`只记录tenant_id、CUSTOMER、已知customer_id或null、结果代码、时间、traceId；未知主体不伪造ID，成功/失败/LOGOUT_ALL字段受限。运行日志不打印code、AppSecret、Token、session_key/OpenID或原微信响应；测试安全事件和产物扫描见下节。本轮日志中的Redis暂停、权限故障和旧测试容器关闭后连接拒绝是故障/生命周期证据，不是实际业务环境故障通过。

## 6. 自动化、命令和证据

所有命令元数据记录cwd/argv/退出码/起止时间及日志，[COMMAND-INDEX](evidence/P05-05/COMMAND-INDEX.json)。工具仅在任务进程设定路径：Maven3.9.16、Temurin21.0.12.1、Node24.21.0、pnpm10.34.6，见各version日志；没有修改全局配置。

| 工作目录 | 命令 | 退出码 / 结果 | 证据 |
| --- | --- | --- | --- |
| apps/backend | ./mvnw clean verify | 0；146单元+275集成=421；失败/错误/跳过全0 | [元数据](evidence/P05-05/verify-final.json)、[日志](evidence/P05-05/verify-final.log)、[XML](evidence/P05-05/verify-final-reports/) |
| 根目录 | pnpm contracts:generate | 0；10导出用例+类型编译 | [记录](evidence/P05-05/contracts-generate-second.json) |
| 根目录 | pnpm contracts:check | 0；实际导出与生成产物一致 | [记录](evidence/P05-05/contracts-check.json) |
| 根目录 | pnpm contracts:typecheck | 0 | [记录](evidence/P05-05/api-typecheck.json) |
| 根目录 | pnpm --filter @pet/admin-web typecheck | 0 | [记录](evidence/P05-05/web-typecheck.json) |
| 根目录 | pnpm --filter @pet/wechat-miniprogram typecheck | 0 | [记录](evidence/P05-05/miniprogram-typecheck.json) |
| 根目录 | pnpm check:repo | 0 | [记录](evidence/P05-05/repo-check.json) |
| apps/backend | ./mvnw dependency:tree -DoutputFile=<证据绝对路径> | 0；RESOLVED | [记录](evidence/P05-05/dependencies-final.json) |
| 根目录 | node docs/testing/evidence/P05-05/check-final-contracts.mjs | 0；最终verify导出再生成逐字一致，不覆盖产物 | [记录](evidence/P05-05/final-contracts.json)、[结果](evidence/P05-05/final-contract-snapshot.json) |
| 根目录 | python3 docs/testing/evidence/P05-05/collect-final.py | 0；366/328逐项保留、JAR/迁移/锁/ui检查 | [记录](evidence/P05-05/collect-final.json)、[产物](evidence/P05-05/artifact-audit.json) |
| 根目录 | python3 docs/testing/evidence/P05-05/final-audit.py | 0；链接、日志和差异保全检查 | [记录](evidence/P05-05/final-audit.json) |

新增55项：客户HTTP/正式会话39、迁移1、WxJava Gateway8、配置5、架构规则1、生产产物1。外部Gateway替身及会话故障注入仅src/test；SDK单元测试只替换HTTP边界，不访问真实微信。其余身份流程是正式SQL、真实PostgreSQL、Redis、Sa、生产HTTP过滤器和Provider，不用H2/内存会话验收。所有18项用户要求覆盖在[用例映射](evidence/P05-05/test-coverage.json)，逐用例DB观察在[客户快照](evidence/P05-05/verify-final-reports/p05-05-observations/)。测试技术账号/密码/OpenID不代表真实用户或微信成功。

失败历史完整保留在evidence/P05-05：compile-first为增量target残留，clean编译恢复；smoke-first为精确架构复用名单；customer-first/second为commons-io兼容问题；customer-third为target清理失败，将生成目录保存在明确临时目录；customer-fourth为Header导入遗漏；units-first纠正SDK线程变量默认值及清理部分编译残留；focused-fifth为FOR SHARE缺UPDATE RLS、Tomcat在过滤器前拒绝编码斜杠（只验真实400）；customer-sixth把脱离Servlet的平台verify探针改为真实CSRF/login/me；verify-first保留419项失败XML，修复旧夹具显式清理新增关联表与同UUID平台安全记录外键；contracts-generate首次Header schema缺string type造成类型生成失败，补准确schema后再生成成功；final-audit-first在结果文件生成前检查自身链接且用例映射名称写错，生成后修正实际名称再审计。所有失败元数据exit1不改写，修复后最终clean verify/契约检查exit0。

## 7. 真实微信验证与外部限制

[预检](evidence/P05-05/real-wechat-preflight.json)exit0只表示检查程序完成；[真实结果](evidence/P05-05/real-wechat-result.json)明确**NOT_EXECUTED**：本轮未载入`PET_WECHAT_LOCAL_SECRET`、本地指定AppID及入口租户配置，未获取新鲜code或发起真实微信请求。已知`wx9bcab67d52e2ee04`只可显式用于该本地验证，未写入通用默认或生产JAR；私有AppID不能推断AppSecret已具备。当前DevTools登录状态和微信网络均NOT_VERIFIED；P02历史工具成功不证明本轮code交换。没有读取其他秘密或要求在聊天粘贴凭据。

| 真实步骤 | 本轮等级 |
| --- | --- |
| 新鲜code→微信交换 | NOT_EXECUTED |
| 真实客户会话建立 | NOT_EXECUTED |
| 该真实会话me | NOT_EXECUTED |
| 该会话退出后失效 | NOT_EXECUTED |
| P09页面/隐私/真机完整业务 | NOT_EXECUTED，归P09 |
| P10通知/支付/AMQP/Outbox | NOT_EXECUTED，归P10 |
| 生产角色/秘密/TLS/代理/Redis ACL与部署 | NOT_EXECUTED / NOT_VERIFIED |
| 远程CI、Windows/Linux | NOT_EXECUTED / NOT_VERIFIED |

G12是本任务真实微信验证门禁，不能降为部署限制或用本地Gateway替身代替。续验只在安全外部配置、已登录开发工具和新鲜code具备时进行；探针放明确临时测试目录，不进入正式小程序。记录四步结果、trace/非秘密主体信息，不保存code/Token/session_key/AppSecret。

## 8. 产物与文件

生产JAR SHA256：`783dce595d9c45fe275db083093b625b9fdfe670debdfb56f030f6bf1ed0581f`。两项ProductionArtifactIT通过；[独立扫描](evidence/P05-05/artifact-audit.json)无测试class/resource、测试Gateway/固定技术凭据/开发工具探针/本地指定AppID；生产V1～V4与源码匹配。SDK依赖合法含协议字段名不等于保存秘密；本轮真实AppSecret未配置，扫描不声称外部日志平台或环境秘密管理已验证。AGENTS、pnpm-lock、V1～V3和ui保持原成果。

完整新增/修改文件见[changed-files.json](evidence/P05-05/changed-files.json)。新增customeridentity实体、内部认证/配置/Gateway、四接口DTO/Controller、V4/role、客户39IT/迁移/SDK配置测试和本报告/总验收；修改唯一会话引擎/Redis/域路由、运行权限和异步客户拒绝、错误/schema、必要旧测试夹具、纯生成类型与架构/契约/版本/本地开发/路线/验收文档。未改正式Web/小程序页面或请求调用。

## 9. G01～G15

| 门禁 | 结果 / 等级 | 事实 |
| --- | --- | --- |
| G01 前置及阶段边界 | PASS / DOCUMENTED | 实际代码、366/328报告及原路线核对；P05/P09/P10明确 |
| G02 模型与绑定 | PASS / RUNTIME_VERIFIED | 正式实体/V4、唯一/复合FK/无秘密列 |
| G03 服务端入口配置 | PASS / RUNTIME_VERIFIED | 白名单、有效租户、配置身份/版本、安全引用；技术配置反例 |
| G04 冻结WxJava | PASS / RESOLVED、COMPILED | 4.8.0官方API、一次执行、缓存隔离；非真实微信结论 |
| G05 首次/并发 | PASS / RUNTIME_VERIFIED | 原子创建、准确冲突重读、无孤立、故障恢复 |
| G06 CUSTOMER会话 | PASS / RUNTIME_VERIFIED | 真实Sa/Redis/HTTP，30天/7天/设备、退出与全撤销 |
| G07 三域/跨租户 | PASS / RUNTIME_VERIFIED | 同UUID真实三域会话与不同tenant/app/open隔离 |
| G08 SELF与授权 | PASS / RUNTIME_VERIFIED | CUSTOMER主体ID，仅SELF，不授员工/TENANT权限 |
| G09 状态/异步 | PASS / RUNTIME_VERIFIED | 每次DB重验，版本失效；客户403未入队，员工旧重验回归 |
| G10 错误/频控/记录 | PASS / RUNTIME_VERIFIED | 一次消费/安全映射、Redis分布式限制、最小事件 |
| G11 PG/Redis | PASS / RUNTIME_VERIFIED | 正式migration、非超级无BYPASSRLS、实际故障关闭 |
| G12 真实微信 | BLOCKED / NOT_EXECUTED | 续验已准备本机入口与隔离正式后端；用户暂时无法提供AppSecret，安全文件为空，未获取code或交换；四步尚无真实证据，历史缺配置结论见第7节 |
| G13 回归/类型 | PASS / COMPILED、RUNTIME_VERIFIED | 421全通过，原366/328逐项保留，五项生成/类型检查通过 |
| G14 生产产物 | PASS / COMPILED、扫描 | JAR排除测试替身/凭据/探针；迁移与锁/ui保全 |
| G15 P05剩余和文档 | PASS / DOCUMENTED | P05 IN_PROGRESS；真实续验阻塞，P07管理API未交付 |

**下一合法任务：P05-05续验——安全加载指定微信入口配置，使用新鲜code验证真实交换、会话、me、退出失效，补齐G12后重新综合P05验收。** 这里只报告下一任务，没有自动执行其他阶段。

## 10. 2026-10-08 真实微信续验：准备完成，AppSecret仍缺少

本轮明确只补真实微信与阶段验收。用户在安全配置提示后确认“暂时无法提供AppSecret，完成准备并保留BLOCKED”。因此未获取或消费code，未请求微信，不把匿名协议检查算作真实登录。原第7节及`real-wechat-result.json`为前轮历史，未覆盖；本轮另存[真实结果](evidence/P05-05/resume-2026-10-08/real-wechat-result.json)、[隔离准备及逐命令元数据](evidence/P05-05/resume-2026-10-08/isolation-preparation.json)、[工具白名单预检](evidence/P05-05/resume-2026-10-08/tool-preflight.json)。起止时间以其中Asia/Shanghai（+08:00）记录为准。

安全属性实际为`PET_WECHAT_LOCAL_SECRET`。本轮创建忽略的`.local-data/p05-05-wechat/secrets/PET_WECHAT_LOCAL_SECRET`：文件存在、可读、为空，0600；其父目录与专用目录0700；`git check-ignore`退出0。只检查元数据，不输出或读取其内容。用户以后自行填写值，不在聊天提供。私有`application-local.yaml`只存本地公开AppID/入口和secret-property引用，通过Spring标准`configtree:`加载秘密文件，无参数秘密、公共默认或客户端AppSecret；具体安全示例见[本地开发续验准备](../development/LOCAL-DEVELOPMENT.md#p05-05-真实续验安全准备2026-10-08)。已有私有小程序文件含AppID而不承担AppSecret存储。

| 准备项 | 本轮结果与边界 |
| --- | --- |
| 冻结工具/产物 | Temurin21.0.12.1+1、PG17.11/Redis8.2.10既定digest；生产JAR SHA仍为第8节值，V1～V4源码与JAR逐字匹配 |
| 专用数据库/Redis | 两个仅回环、临时数据容器；不复用日常库/卷，正式角色脚本均退出0；真实Redis认证PING=PONG |
| 正式独立迁移 | `scripts/backend-identity.sh migrate`退出0；history V1～V4全部success=true，无repair/clean/baseline或测试迁移 |
| 正式验收租户初始化 | bootstrap经受控stdin退出0；tenantCode=`p05-05-wechat-acceptance`且ACTIVE；无Store，初始化的员工仅临时技术身份，不用于客户验收 |
| 运行角色/RLS | `p05_probe_runtime`非superuser/无BYPASSRLS/无OpenID列SELECT权限，无上下文客户计数0；不关闭隔离 |
| 服务端入口 | entryId=`p05-05-acceptance`→`local-mini`→仅上述tenantCode；私有配置AppID与工具和指定值一致 |
| 正式后端 | 既有生产JAR、local profile、受限DB身份、关闭启动迁移；readiness=200 UP，匿名`GET /api/customer/auth/me`=401 AUTH_REQUIRED且trace一致 |
| Gateway装配依据 | JAR含唯一正式WxJava4.8.0组件、无测试Gateway/替身、无test profile或替换配置；真实exchange仍NOT_EXECUTED |
| DevTools | 当前界面识别AppID、Stable2.02.2608080、基础库3.17.2，已登录工程界面/应用信息可见；没有重新执行wx.login，当前其开发权限仍NOT_VERIFIED |
| CLI | help退出0；islogin退出246，确认为服务端口关闭，不解释为账号无权限；未开启服务端口或更改登录/安全设置 |
| 临时探针 | 忽略的专用probe目录、无依赖和秘密，不进入正式源码/产物；未导入、未自动登录、未记录敏感网络 |
| 客户/绑定 | 准备前后均0/0；真实客户创建和已有客户复用均NOT_EXECUTED |

准备脚本为[prepare-isolated.py](evidence/P05-05/resume-2026-10-08/prepare-isolated.py)，根目录执行退出0，只用于缺AppSecret时的准备，不是完整真实微信验收器。每项命令的cwd/argv/时间/退出码/白名单摘要均在隔离JSON；后端以SIGTERM停止，退出143记录为信号终止，不伪写退出0。两个本轮容器停止退出0；临时凭据输入文件移除，原有容器状态相同，已有进程和数据库卷未停止/删除。临时验收库随本轮专用资源结束，下一真实续验重新按正式路径初始化，不能把本轮临时tenant UUID用于日常库。

| 真实四步 | HTTP / trace / 本轮等级 |
| --- | --- |
| 新鲜code→真实WxJava交换→登录 | 无HTTP/trace，NOT_EXECUTED（缺AppSecret） |
| PG客户/绑定与Redis CUSTOMER会话 | 无真实会话，NOT_EXECUTED |
| 带实际X-Customer-Token的me/SELF | 无HTTP/trace，NOT_EXECUTED；匿名401仅准备检查 |
| 当前退出→同旧Token再次me失效 | 无HTTP/trace，NOT_EXECUTED |

没有后端代码、依赖或公开契约变更，无需重生成类型或重跑已通过421项故障回归；只做本轮准备、仓库/文档及证据保全检查，结果见[收尾检查](evidence/P05-05/resume-2026-10-08/final-checks.json)。证据采集仅固定字段，不写code/Token/session_key/OpenID/UnionID/凭据头、原始SDK响应或配置内容；未存原始工具信息树/截图。

**G12 BLOCKED / NOT_EXECUTED；P05-05 BLOCKED；P05 IN_PROGRESS。** 其他门禁维持既有通过证据，本轮不冒充重新运行。P05已重核：尚缺真实四步，不能关闭阶段；完整员工、组织、角色权限管理仍P07必选。P09完整交互/真机、P10消息支付、生产部署/远程CI/多OS限制独立保留。

具体人工事项仅为在上述安全文件填写该AppID的AppSecret；取得新鲜code由下一次续验现场完成，若工具要求扫码则使用有对应开发权限的微信。仅在选用automation时再按实际能力处理“设置→安全设置→服务端口”，记录原状态并结束后恢复；当前不开端口。下一合法任务仍是P05-05真实续验，四步通过后再判断P05和提出P06首项，不自动进入其他阶段或提交/推送/部署。

## 11. 2026-10-08 真实微信续验通过与阶段关闭

用户随后提供了指定本地应用的配置，并明确允许临时开启工具服务端口、完成后恢复关闭。秘密仅存既定Git忽略文件，存在/可读/非空，权限0600、父目录0700；后端经私有Spring配置树读取`PET_WECHAT_LOCAL_SECRET`。没有输出内容、秘密命令参数、客户端秘密或公共默认AppID。因为凭据已在聊天中出现，建议用户在微信管理后台轮换，再自行更新该安全文件、提高私有配置版本并重启；不在聊天提供新值，本轮没有代为轮换。

正式生产JAR仍是第8节SHA256，使用local profile和唯一`WxJavaMiniProgramGateway`（WxJava4.8.0），没有测试Gateway/test profile/替身。重新建立专用临时PostgreSQL17.11、Redis8.2.10，执行正式角色脚本、V1～V4 migration和受控bootstrap；受限runtime非SUPERUSER、无BYPASSRLS、不拥有业务表，未关闭RLS。entryId=`p05-05-acceptance`，tenantCode=`p05-05-wechat-acceptance`，仅该ACTIVE验收租户在入口白名单内。首次真实登录实际创建一名验收客户及一条绑定；不改日常业务租户。构建、迁移、命令与资源恢复见[正式环境执行记录](evidence/P05-05/resume-2026-10-08/real-run-second/isolation-preparation.json)。

工具为Stable2.02.2608080，运行时基础库3.17.2；独立探针的`wx.getAccountInfoSync()`实际AppID与`wx9bcab67d52e2ee04`一致，当前工具登录/开发权限由两次成功`wx.login`及真实交换确认。使用临时目录中官方`miniprogram-automator@0.12.1`和本机回环自动化接口，未加入生产依赖/根锁；只信任指定探针，未开启全局默认信任或获取工具登录票据。探针在忽略目录，不修改正式业务页面，无自动登录或控制台code输出。code取得后立即由Node验证进程向本地正式接口提交；Token仅保存在进程内存，未用DevTools网络面板传输Token，不订阅console或保存原始协议/微信响应。

首轮真实执行停在TOOL_LAUNCH、微信尝试数0：[失败结果](evidence/P05-05/resume-2026-10-08/real-run/real-wechat-result.json)。原因是隔离子进程未继承HOME，官方CLI无法定位工具用户资料；仅继承既有HOME/USER/TMPDIR后，`islogin`与指定探针`auto`退出0，见[工具恢复记录](evidence/P05-05/resume-2026-10-08/automation-recovery.json)。没有改全局环境或认证校验，没有把工具启动失败归为账号无权限；没有获取或重试消费过的code。首次失败和准备轮证据保留。

真实链路执行时间为2026-10-08 **20:04:52.920～20:04:56.424 +08:00**（JSON记录UTC）。独立执行器退出0；仅两次新鲜code、两次正式微信交换。字段白名单结果见[真实四步与复用](evidence/P05-05/resume-2026-10-08/real-run-second/real-wechat-result.json)。

| 步骤 | HTTP / 非敏感断言 | traceId |
| --- | --- | --- |
| POST /api/customer/auth/wechat/login | 200；真实WxJava交换，CUSTOMER、验收租户、SELF；无STAFF/PLATFORM权限 | d96058409811551be03f2f19c6d2b351 |
| PostgreSQL与Redis会话 | 非HTTP检查；客户/绑定1/1，租户/客户/AppID/ACTIVE关联正确；真实Redis CUSTOMER/MINIPROGRAM会话，主体/租户和TTL符合冻结策略 | 对应上行登录trace |
| GET /api/customer/auth/me | 200；`X-Customer-Token: Bearer <进程内实际值>`，同客户/同租户，SELF，授权门店0 | 12d24c1adccecb2ea73eaec2dd04a8ff |
| POST /api/customer/auth/logout | 200，data=null | 9ce2758cda397ec21feeb13a2b8ee0a1 |
| 同旧Token再次GET me | 401 SESSION_EXPIRED；对应Redis token-session不存在 | 3575dfda57c37f5adf9ebb5c126a2cc4 |
| 第二个新鲜code登录 | 200；同客户UUID；客户/绑定仍1/1，无重复绑定 | 24e3181dde9e9de3ec934ebe71eb2b9b |
| 第二会话清理退出 | 200，data=null | 9007c69e1672d312c0adc006d44a8608 |

所有业务响应trace头与信封一致、Cache-Control含no-store且无Set-Cookie。数据库观察只读计数/关联状态，未SELECT OpenID/UnionID；Redis值仅在进程内核对，不采集Token键或会话原文。记录没有AppSecret、code、Token、session_key、OpenID/UnionID、Cookie、凭据头或原始敏感配置/响应。环境记录里的顶层`steps`是启动前初始化字段，未被执行器更新，不能用作最终四步状态；最终状态以独立`real-wechat-result.json`及[阶段复核](evidence/P05-05/resume-2026-10-08/real-run-second/stage-review.json)为准，原始执行快照保留。

正式后端SIGTERM退出143，两专用临时容器停止退出0并结束临时数据；技术凭据输入目录已移除，原有三容器状态不变，原用户进程/卷未停止或删除。第二会话已退出，探针窗口已关闭。工具服务端口恢复关闭、46904/9420不再监听；登录票据/全局信任/插件端口均关闭，回到原工程窗口，见[恢复证据](evidence/P05-05/resume-2026-10-08/real-run-second/tool-restoration.json)。私有秘密文件保留供用户安全轮换；无Token/code临时文件需要清理。

本轮只调整私有配置、验证工具环境和证据/五份文档，没有后端、依赖、公开契约、正式小程序代码修改。历史421项及生成/漂移/三端类型证据维持，不无理由重跑；本轮脚本语法、仓库结构、链接/保全、秘密无泄漏与产物SHA复核见[收尾审计](evidence/P05-05/resume-2026-10-08/real-run-second/final-audit.json)。

| 门禁复核 | 当前结论 |
| --- | --- |
| G01～G11、G13～G14 | PASS，沿用既定通过证据；本轮正式配置、迁移、生产包与真实运行没有发现新增问题 |
| G12 真实微信 | **PASS / RUNTIME_VERIFIED**，上述四步全部真实通过 |
| G15 阶段与文档 | PASS / DOCUMENTED，五份文档已追加证据；P05既定条件综合复核见总验收 |
| P05-05 / P05 | **COMPLETE / COMPLETE**，不覆盖此前BLOCKED/NOT_EXECUTED |

完整员工/组织/角色权限管理API仍P07必选且未实现；P09完整交互/隐私/真机、P10消息支付、生产部署/远程CI/多OS限制独立保留。下一建议为**P06-01：Web应用壳、请求与同源会话基础**，依据现有P06与A06-01/02范围提出，尚未开始；未提交、推送或部署。
