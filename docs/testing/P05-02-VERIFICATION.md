# P05-02 Sa-Token真实认证、会话与当前身份验证

日期：2026-10-08。根目录：`/Users/kimwell/work/pet-platform`；基础包：`com.pet.platform`。用户明确授权本任务，覆盖AGENTS的历史P02范围；未使用Product Delivery OS、未执行下一任务、未提交/推送/部署。开始时工作区干净，既有ui、历史迁移/证据和用户数据库卷保留。

**P05-02 COMPLETE；P05整体IN_PROGRESS。** G01～G15通过，后端最终292项（132单元+160集成）零失败/错误/跳过，原P05-01的258项逐项保留。当前STAFF能力完成不代表平台/客户认证、全部凭据管理、安全管理CRUD或生产交付完成。

## 1. 前置事实与接入版本

实际读取AGENTS、README、认证/授权/多租户/配置/模块边界、身份/API/生成契约、Redis/异步约定、版本矩阵、身份初始化、路线/决策、P05-01验证与验收矩阵，并检查对应生产实现。P05-01已完成正式Tenant/Store/Employee/Role及三关系、正式V1迁移、受限运行/迁移/初始化角色、受限认证前查询、PBKDF2密码服务、显式初始化和正式Store事实源；[原报告](P05-01-VERIFICATION.md)及失败历史未覆盖。原限制包括无真实登录/会话Provider、无凭据变更/撤销链路、无异步权威重验、无平台/客户账号/管理API/生产部署，本轮逐项处理适用范围，没有仅凭下一任务推定前置通过。

接入`sa-token-spring-boot4-starter`与`sa-token-redis-template`，实际均为**1.46.0**，唯一冻结版本事实源仍为[VERSION-MATRIX](../development/VERSION-MATRIX.md)。传递core/Jakarta Servlet/Boot WebMVC common/Jackson 3模块同版；实际[依赖树](evidence/P05-02/backend-dependency-tree.txt)及[解析命令](evidence/P05-02/dependencies.json)留证，未覆盖Boot BOM或扩大业务Redis限制。

核对[官方配置](https://sa-token.com/use/config.html)、[多认证空间](https://sa-token.com/up/many-account.html)、[Redis集成](https://sa-token.com/up/integ-redis.html)及冻结版本Maven发布的真实源码，URL/SHA见[源码清单](evidence/P05-02/sa-token-sources.json)。使用实际`StpLogic`、`createLoginSession`、`SaLoginParameter`动态闲置期限、`setTokenValueToStorage`、`checkLogin`、`logoutByTokenValue`、`SaSession/SaTerminalInfo`和官方`SaTokenDaoForRedisTemplate`；未根据其他版本编造API。官方servlet上下文Filter调整到认证Filter之前，自动读Body/Header/Cookie与写Header均关闭。

等级：版本与策略DOCUMENTED；依赖RESOLVED；代码/三端类型COMPILED；下述实际场景RUNTIME_VERIFIED；未执行范围单独列出，不互相替代。

## 2. 正式接口与响应

本轮正式清单沿用[IDENTITY](../contracts/IDENTITY.md#p05-02-正式staff接口清单2026-10-08)。员工输入为`tenantCode/loginName/password`，loginName复用IdentityNames规范化，密码保持原Unicode输入、不trim。历史username字段细化为loginName，无别名；Body可信tenantId/角色/权限/管理员标志等未知字段400拒绝。

| 方法/路径 | 认证及CSRF | 成功data / 副作用 |
| --- | --- | --- |
| GET `/api/admin/auth/csrf` | 匿名pre或WEB Cookie；拒绝Token Header | CsrfResult；匿名创建10分钟预会话，已登录取当前设备CSRF；不续闲置 |
| POST `/api/admin/auth/login` | pre或WEB Cookie；X-CSRF-Token及固定Origin/同源Referer | CurrentIdentity；创建新WEB会话并轮换CSRF，销毁pre/旧当前设备；JSON不含Token |
| POST `/api/admin/auth/token/login` | 无Cookie/身份Header；无浏览器CSRF | TokenLoginResult；独立MINIPROGRAM设备；仅该响应返回必要原始Token |
| GET `/api/admin/auth/me` | WEB Cookie或MINIPROGRAM Header二选一 | 当前正式授权CurrentIdentity；不续闲置，无凭据401 |
| POST `/api/admin/auth/logout` | 当前设备；WEB需CSRF及来源，MINIPROGRAM无需浏览器CSRF | null；撤销服务端当前会话，WEB按原属性清Cookie/CSRF，其他设备保留 |

全部统一success/data或error/traceId、X-Trace-Id及中文错误；认证响应Cache-Control:no-store、Pragma:no-cache。当前身份只STAFF，公开字段为主体/租户/非凭据sessionId/显示名/当前权限/每权限数据范围/门店上限/授权版本/期限，无密码哈希、内部Session或计算过程。生产OpenAPI五路径，测试路径不进入公共包；成功类型分别SuccessCurrentIdentity、SuccessTokenLoginResult、SuccessCsrfResult、SuccessVoid。Cookie/Header方案为OR，CSRF独立说明，见[OpenAPI审计](evidence/P05-02/openapi-audit.json)。

401区分LOGIN_FAILED、AUTH_REQUIRED、SESSION_EXPIRED/REVOKED、AUTH_DOMAIN_MISMATCH；不存在租户/账号、密码错、停用统一LOGIN_FAILED“登录失败，请检查登录信息”，不枚举。400输入语法/未知字段/载体冲突，403 CSRF或已认证权限拒绝，422字段约束，429频控/账号锁竞争，409五设备上限，503认证DB/Redis故障；未把基础设施故障包装成密码错误。

## 3. Cookie、CSRF、Token及期限

生产Cookie `__Secure-pet_staff_sid`、预会话`__Secure-pet_staff_pre`：Path=/api/admin/、无Domain、HttpOnly、Secure、SameSite=Lax、会话Cookie不带Max-Age/Expires；本地HTTP显式`pet_dev_staff_sid/pre`与Secure=false。prod禁用Secure或开启未经支持的Boot forwarded处理均启动失败。退出同Path/安全属性、Max-Age=0清理。

CSRF为[OWASP synchronizer token模式](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html)：256-bit随机值绑定服务器Redis预会话/当前设备，Cookie只定位绑定记录，不以攻击者可控Cookie自行证明。提交X-CSRF-Token并精确匹配public-origin；缺Origin时必须同源Referer，两者都缺拒绝。预会话10分钟，失效/错误/缺失403；成功登录销毁预会话并生成独立新Token/sessionId/CSRF，旧凭据不能固定新会话。登录后重新GET csrf，退出删除当前TokenSession及CSRF；已认证会话到期401。当前同源策略无开放CORS，不允许凭据配任意来源。

员工小程序后续Header为`X-Staff-Token: Bearer <原始opaque值>`；不认证Web Cookie，Token设备必须MINIPROGRAM，Cookie设备必须WEB。Cookie/Header并存400 AUTH_CREDENTIAL_AMBIGUOUS，多个/重复身份Header拒绝；其他域Header或通用Authorization为401 AUTH_DOMAIN_MISMATCH，其他域Cookie忽略。URL/Body不提供Token认证。staff/customer/platform为独立StpLogic及独立Redis键；预留两个域无登录入口，STAFF Token在两域查询不产生主体。

Web绝对8小时/闲置30分钟，小程序绝对7天/闲置24小时；isShare=false、多设备独立、最多5有效设备，不自动挤出。账号锁清理失效终端并检查上限；竞争429/Retry-After=1，30秒锁租约异常503。绝对期限不滑动；me/CSRF/auth维护请求不续闲置，合法身份的非auth业务请求更新活跃时间。退出当前设备不影响其他设备；过期/已退出重复退出401 SESSION_EXPIRED，无凭据401 AUTH_REQUIRED。确认SESSION_EXPIRED/REVOKED时同属性清当前sid/pre Cookie，401不静默降成匿名；浏览器随后可GET匿名CSRF再登录。DB/Redis 503及载体冲突不清Cookie。跨JVM测试验证创建A、读取/退出B、A随后拒绝；短测试专用4秒绝对/2秒闲置配置验证闲置过期、业务活跃与绝对上限，生产不能覆盖冻结期限。

## 4. 当前身份、RLS与授权变化

顺序：TraceFilter → 官方Sa servlet上下文 → StaffAuthenticationFilter识别显式载体/校验会话 → P05-01窄范围`staff_authorization(tenant,employee)`验证有效状态/版本并加载当前合法授权 → CurrentPrincipalProvider → TenantContextFilter的AUTHORITY_READ根 → 业务forPermission/Guard → ScopedTransaction → 同连接RLS。

会话只保存服务器主体引用、设备/sessionId、安全版本、绝对/闲置期限和CSRF；正式数据库为状态/权限/范围/门店权威。复用受限函数解决身份与租户上下文循环，未关闭RLS、扩大数据库角色或建立平台旁路。无/未知/过期/撤销会话不建立上下文；公共接口无凭据保持无身份。客户端Header/Query tenantId不覆盖身份，测试正式Store读取同租户成功、跨租户404；异常后重复匿名请求及线程范围清理无泄漏。

**每请求权威重载**，不依赖长期权限快照或仅authorizationVersion：租户/员工停用及securityVersion/Tenant securityVersion不符401；角色停用、角色权限、员工角色/门店关系/范围改变，当前me及业务范围即时按数据库结果收窄。集成测试明确使用正式SQL数据变更（允许的测试变更），授权关系即使未增加authorizationVersion也不会沿用旧值；凭据securityVersion增加后旧会话拒绝。没有新增仅存储但无人更新的授权版本字段。

一致性边界为READ COMMITTED的本次权威SQL开始：变更提交后的新请求读到失效或新授权；已完成身份校验且执行中的请求保持本次快照，不能撤回完成操作。员工改密/全撤销与敏感写提交前重验用例留后续任务，不能以本轮每请求查询宣称它们已完成。

## 5. 异步与认证基础设施

选择本轮允许的**真实身份任务禁止**方案：SessionPrincipalProvider标记生产会话来源，TenantExecutionScope随嵌套/同步执行保留标记，在TenantTaskExecutor捕获快照/入队前403 PERMISSION_DENIED；真实HTTP验证不会提交。未实现异步撤销重验，也未将30秒快照期限作为停用处理。既有纯内部任务边界/空身份入口和原测试保留，不伪装员工；后续开放真实身份任务必须先重验会话/状态/当前权限，再与捕获上限取交集并建新事务。

认证Redis适配官方DAO，独立`pet:<env>:staff:staff:`及其他域空间，辅助`pet:<env>:auth:staff:<kind>:<64hex安全摘要>`。类型仅固定SessionWire、String数据与最多5终端，无Jackson多态/Java原生反序列化，不开放任意对象/SCAN/脚本；TTL最多7天、值最多128KiB，仅认证例外，普通业务Redis限制不变。原始Token只在必要服务端键/值及小程序一次登录响应存在，不进入日志。

最小频控采用合法登录尝试固定窗口：直接对端IP 60次/5分钟、规范化tenantCode+loginName摘要10次/15分钟。Lua原子递增及首次TTL，成功也计数不清零；阈值429携带剩余窗口Retry-After，自然恢复，无永久账号停用。密码不入Key，未认证时不伪造TenantContext。只接受直接对端地址，X-Forwarded-For/Forwarded无效；当前可信代理链未实现，server.forward-headers-strategy必须none。

缺账号使用运行时随机dummy PBKDF2哈希减少明显时间差，不声称绝对恒定时间。Redis不可用503关闭认证/频控，不绕过、不回退内存、不删Cookie；恢复后原有效会话可用。数据库故障503，不改为LOGIN_FAILED/401、不删除有效会话。SQL故障测试通过撤销受限函数EXECUTE权限再恢复；Redis故障暂停真实容器再恢复，准确区分测试注入方式。

## 6. 实际命令与结果

工具使用既有临时冻结JDK/Node/pnpm，不改全局配置；实际版本输出见[java](evidence/P05-02/java-version.log)、[node](evidence/P05-02/node-version.log)、[pnpm](evidence/P05-02/pnpm-version.log)。每条元数据记录cwd、argv、退出码、起止UTC及日志，封装runner只配置本进程工具路径、不输出环境秘密。

| 工作目录 | 实际命令 / 检查 | 退出码 / 结果 | 证据 |
| --- | --- | --- | --- |
| apps/backend | `./mvnw clean verify` | 0；132单元+160集成=292，0失败/错误/跳过 | [元数据](evidence/P05-02/verify-final.json)、[日志](evidence/P05-02/verify-final.log)、[XML汇总](evidence/P05-02/test-summary.json) |
| apps/backend | `./mvnw dependency:tree -DoutputFile=.../backend-dependency-tree.txt`（完整argv在证据） | 0；冻结Sa-Token及BOM解析 | [元数据](evidence/P05-02/dependencies.json)、[树](evidence/P05-02/backend-dependency-tree.txt) |
| 根目录 | `pnpm contracts:generate` | 0；10导出测试通过，实际生产模型生成 | [证据](evidence/P05-02/contracts-generate.json) |
| 根目录 | `pnpm contracts:check` | 0；两份schema/类型、小程序同步无漂移 | [证据](evidence/P05-02/contracts-check.json) |
| 根目录 | `node docs/testing/evidence/P05-02/contracts-final-snapshot.mjs` | 0；最终292项构建的真实导出重新生成比对五份产物，SHA/字节一致，不删除已验收JAR | [元数据](evidence/P05-02/contracts-final-snapshot.json)、[结果](evidence/P05-02/contracts-final-snapshot-result.json) |
| 根目录 | `pnpm contracts:typecheck` | 0；api-contracts tsc --noEmit | [证据](evidence/P05-02/contracts-typecheck.json) |
| 根目录 | `pnpm --filter @pet/admin-web typecheck` | 0 | [证据](evidence/P05-02/web-typecheck.json) |
| 根目录 | `pnpm --filter @pet/wechat-miniprogram typecheck` | 0 | [证据](evidence/P05-02/miniprogram-typecheck.json) |
| 根目录 | `pnpm check:repo` | 0；单锁/依赖/秘密特征/固定入口/小程序结构 | [证据](evidence/P05-02/repo-check.json) |
| apps/backend | `java -cp <真实Maven依赖+临时类> P05BrowserHarness`（完整argv在证据）+IAB实际点击 | 0；同源浏览器9项状态结果PASS | [元数据](evidence/P05-02/browser-harness.json)、[结果](evidence/P05-02/browser-result.json)、[临时测试工具源码](evidence/P05-02/browser-harness.java.txt)、[截图](evidence/P05-02/browser-cookie-csrf.jpg) |
| 根目录 | `python3 docs/testing/evidence/P05-02/final-audit.py` | 0；测试保留/JAR/文档/日志/范围审计 | [元数据](evidence/P05-02/final-audit-command.json)、[结果](evidence/P05-02/final-audit.json) |
| 根目录 | `docker ps --filter label=org.testcontainers=true --format ...` | 0；无运行中Testcontainers | [元数据](evidence/P05-02/container-cleanup-check.json)、[输出](evidence/P05-02/container-cleanup-check.log) |

最终报告XML及实际生产JAR子进程日志保留在[verify-final-reports](evidence/P05-02/verify-final-reports/)。原258项按classname+testcase name集合对比，缺失0、新增34项（31 STAFF HTTP/Redis/PG，3生产JAR共享会话/期限/安全配置）；测试账号由正式IdentityBootstrap/BootstrapJdbc建立，密码运行时随机技术夹具，未导入测试Provider作登录。生产JAR SHA、正式V1/生产Provider、同版Sa模块、无测试/临时页面结果见[产物审计](evidence/P05-02/artifact-audit.json)。

真实浏览器：匿名CSRF200 → 缺CSRF登录403 → 合法登录200且JSON无Token/HttpOnly不可见 → me200 → 旧CSRF退出403 → 当前CSRF退出200 → me401。临时页面只在独立测试容器进程，由临时工具注册，未加入正式工程/JAR；完成后进程/容器/临时类清理，未删除用户卷。生产Secure属性通过实际JAR HTTP响应及反配置启动验证，未冒充生产TLS浏览器验收。

## 7. 用例覆盖与门禁

| 用户要求的测试组 | 实际覆盖 |
| --- | --- |
| 1～4 正确/错误/不存在/停用登录 | 正式初始化、正式候选/密码/当前授权；错误不创建会话、统一响应 |
| 5～9 Cookie、无JSON Token、缺错过期CSRF、合法链路、固定防护 | local/prod属性、预会话到期、登录后轮换、独立Token/sessionId；实际浏览器 |
| 10～15 当前正式身份、客户端tenantId、退出/设备、冲突、跨域 | 正式9权限/门店/范围、正式Store RLS、Header/Query/Body反例、Web/mini隔离、预留域查询 |
| 16～19 期限、并发频控、Redis/DB故障 | 生产JAR短期限；24并发Lua仅10放行；转发头无法绕过；暂停Redis/撤销DB执行权限再恢复 |
| 20～22 权限/范围/密码版本/异步 | 当前关系重载、角色停用/员工角色/门店变化、安全版本拒绝；真实身份异步403禁止 |
| 23～24 线程与两实例 | 异常后匿名复用/范围清理；两个真实生产JAR JVM共用正式PG/Redis登录/退出 |

| 门禁 | 结果 | 证据范围 |
| --- | --- | --- |
| G01 正式数据及初始化前提 | PASS | P05-01原证据核对，正式路径建立技术验收员工 |
| G02 Sa-Token/Redis真实接入 | PASS | 真实DAO、固定序列化、跨JVM |
| G03 正式身份/密码登录 | PASS | 正式查询/PBKDF2/状态/授权，无测试Provider |
| G04 Web Cookie/CSRF/固定防护 | PASS | HTTP属性及真实浏览器正反链路；生产TLS仍未验证 |
| G05 小程序Token/渠道 | PASS | 服务端协议/独立设备/载体拒绝；未开发页面 |
| G06 当前身份/租户/RLS | PASS | 顺序/正式Store/受限角色，复用原范围事务 |
| G07 客户端不可覆盖 | PASS | tenantId Header/Query忽略、非法Body拒绝、授权只服务器 |
| G08 退出/期限/设备 | PASS | 当前退出、五设备、绝对/闲置、双JVM |
| G09 状态/授权变化 | PASS | 每请求重载及安全版本拒绝；一致性边界明确 |
| G10 异步撤销或限制 | PASS（限制方案） | 真实会话来源捕获/入队前403禁止；未声称重验已实现 |
| G11 频控/基础设施故障 | PASS | 原子Lua、受控来源、503失败关闭及恢复 |
| G12 PostgreSQL/Redis/HTTP | PASS | 真Testcontainers、正式迁移/角色/初始化、292零跳过 |
| G13 OpenAPI/三端类型 | PASS | 实际generate/check/三端tsc，五生产路径/准确安全方案 |
| G14 生产产物/秘密 | PASS | JAR无测试Provider/Controller/临时页面/固定技术凭据；日志不泄漏凭据 |
| G15 文档/行为一致 | PASS | 要求文档及额外模块/Redis/版本入口更新，局部链接/产物审计 |

## 8. 失败历史与验证限制

保留[compile-first](evidence/P05-02/compile-first.json)（注解名称/导入编译错误，修正后compile-second=0）、[first-regression](evidence/P05-02/first-regression.json)（生产路径旧零断言和结构自调用白名单，修正后通过）。[auth-it-first](evidence/P05-02/auth-it-first.json)退出0但指定skipTests导致测试跳过，明确**NOT_EXECUTED**，不作验收；[auth-it-executed](evidence/P05-02/auth-it-executed.json)发现CSRF返回Instant非毫秒导致既有序列化拒绝，改为毫秒后[auth-it-second](evidence/P05-02/auth-it-second.json)29项通过。首个浏览器准备进程未验收即停止143，历史[记录](evidence/P05-02/browser-harness-first.json)保留，后续两次真实链路通过；最终截图来自后一次。[full-regression-second](evidence/P05-02/full-regression-second.json)291项先通过，此前verify验证完整成功200 OpenAPI schema/额外结构收口（[保留记录](evidence/P05-02/before-cookie-recovery-verify-final.json)）。最终复核补过期/撤销Cookie的401清理与503保留，新增恢复登录反例，最终292项通过；此前291项记录保留，不覆盖失败。产物审计首稿检查了错误V1文件名，实际迁移文件未缺失；[首稿](evidence/P05-02/artifact-audit-first.json)保留，最终按真实V1名称及字节比较审计。

未实现：Web登录页/前端请求层/小程序页面、平台管理员或微信客户登录、完整用户/角色CRUD、员工改密/重置/全会话撤销API、真实身份异步撤销重验（已禁用）、敏感写提交前新权威检查、跨源及可信代理链。

NOT_EXECUTED / NOT_VERIFIED：生产TLS浏览器/代理/Redis ACL或HA/Cluster、真实生产库/真实管理员初始化/部署、远程CI、Windows/Linux、完整微信真机身份链路、生产PBKDF2/登录负载性能及跨版本旧客户端完整破坏性分析。HTTP属性与本地浏览器不替代这些证据；不把技术测试账户当生产业务验收账户。

## 9. 文件与后续

新增identity/api/authentication公开DTO/五接口，application/authentication的HTTP用例/SessionPort，infrastructure/session的Sa/Redis/Filter/装配，shared/security的会话来源标记，两组真实认证/生产JAR集成测试；修改pom/生产与local配置、TenantExecutionScope/Filter/TrustedExecutor和结构/导出/包测试。更新生产/测试OpenAPI、公共生成类型和小程序生成声明，没有修改页面/请求层。完整[文件清单](evidence/P05-02/file-manifest.json)区分生产/测试/生成/文档/证据。

更新AUTHENTICATION、AUTHORIZATION、CONFIGURATION、IDENTITY、OPENAPI-GENERATION、ASYNC-EXECUTION、LOCAL-DEVELOPMENT、ROADMAP、DECISION-LOG、ACCEPTANCE-MATRIX；同步README、VERSION-MATRIX、MODULE-BOUNDARIES及REDIS，历史报告保留。

后续前端：同源先GET csrf，内存保存并POST login时提交；登录后重取CSRF，写请求带Cookie/CSRF，401与503分开处理；小程序只在独立Token槽位保存登录返回值并添加对应Header，不混Cookie；两端直接消费生成类型。本轮不实施。

按[实际路线](../development/ROADMAP.md#p05-02-当前结果及后续范围2026-10-08)原P05剩余password/logout-all与敏感授权要求，下一建议 **P05-03：员工凭据变更、全会话撤销与敏感操作安全闭环**，NOT_STARTED，需后续授权；只报告，不执行。P05整体仍IN_PROGRESS。
