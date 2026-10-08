# 认证、设备会话与传输安全

当前STAFF认证及敏感操作实施见本文P05-02/P05-03章节及[P05-03验证](../testing/P05-03-VERIFICATION.md)。旧阶段叙述保留为历史范围；本轮渠道/输入细化见当前章节。
冻结日期：2026-10-07，P01-02。本文拥有认证空间、Cookie/Token、CSRF、期限与撤销。接口及字段见 [身份契约](../contracts/IDENTITY.md)，权限见 [授权](AUTHORIZATION.md)。冻结时未实现正式认证；当前STAFF实施见文末P05-02/P05-03。

## 精确版本集成依据

Sa-Token 使用 [版本矩阵](../development/VERSION-MATRIX.md) 的 starter/Redis 组合。已读取官方 [多账号](https://sa-token.com/up/many-account.html)、[配置](https://sa-token.com/use/config.html) 和 [精确版本源码](../testing/evidence/P01-02/sa-token-source-record.json)。源码及临时编译探针确认 StpLogic(String)、setConfig、createLoginSession、createSaLoginParameter、setTokenValueToStorage、getLoginIdByToken、logoutByTokenValue、kickout 的真实 API；不猜测新版调用。

三类身份各一个 StpLogic：loginType=`platform/staff/customer`；tokenName=`pet:<environment>:platform/staff/customer`。实际键以 `<tokenName>:<loginType>:` 开始，后缀 token/session/token-session/last-active 等。辅助预会话CSRF键为 `pet:<environment>:auth:<staff|platform>:pre:<安全摘要>`，CUSTOMER尚未实现。environment 必填、启动校验，不同部署不共用前缀。

三个逻辑均关闭 `isReadBody/isReadHeader/isReadCookie/isWriteHeader`，tokenPrefix 不配置。项目适配器按端点声明严格解析载体，移除 Bearer 后以 `setTokenValueToStorage(rawToken)` 注入对应逻辑的**当前请求**存储，不写 Cookie/响应 Header；随后调用对应逻辑checkLogin并执行权威安全/授权查询，getLoginIdByToken不能单独代替期限/活跃检查。登录用 createLoginSession，响应适配器显式发送 Cookie/Token。禁止默认 StpUtil、Query/Body Token、框架默认载体优先顺序。P05 验证多逻辑注册、注解 loginType、拦截顺序与请求清理。

## 载体和 Cookie

| 身份域 | 生产 Web Cookie | Path | 小程序 Header |
| --- | --- | --- | --- |
| PLATFORM | `__Secure-pet_platform_sid` | `/api/platform/` | 不提供 |
| STAFF | `__Secure-pet_staff_sid` | `/api/admin/` | `X-Staff-Token: Bearer <opaqueToken>` |
| CUSTOMER | 预留 `__Secure-pet_customer_sid`，当前不开放客户 Web 登录 | `/api/customer/` | `X-Customer-Token: Bearer <opaqueToken>` |

生产：Domain 不设置，HttpOnly=true、Secure=true、SameSite=Lax；会话 Cookie 不设置 Max-Age/Expires，服务端期限仍约束。使用 __Secure 而非 __Host，因为 Path 分域。删除同名同 Path、Max-Age=0。Path 不代替后端认证隔离。[Cookie 属性依据](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Set-Cookie)。

local 默认 http://localhost，Cookie 名为 `pet_dev_platform_sid/pet_dev_staff_sid`、Secure=false，其他属性相同；客户 Cookie 不签发。Vite 同源代理 /api；HTTPS local 可使用生产属性。生产启动拒绝 dev 名称和 Secure=false。

Token Header 只接受准确 `Bearer`、一个空格、16～256 个 `[A-Za-z0-9_-]` opaque 字符。拒绝重复头、逗号拼接、控制字符、URL Token；不使用通用 Authorization 自动判断身份。

目标域 Cookie/Header 并存返回 400 `AUTH_CREDENTIAL_AMBIGUOUS`，不比较/选择。仅其他域 Header 返回 401 `AUTH_DOMAIN_MISMATCH`；多个身份 Header 也拒绝。其他域 Cookie 忽略、不回退。端点声明 PUBLIC/PLATFORM/STAFF/CUSTOMER 及允许 COOKIE/TOKEN；token 登录不接受 Cookie；当前CUSTOMER受保护端点仅允许TOKEN，预留Cookie不参与认证。公共接口不读取任意 StpLogic，微信回调仅验微信协议。

## 独立 CSRF

平台/员工 GET `<prefix>/auth/csrf` 返回 `{csrfToken,expiresAt}`，Cache-Control:no-store。匿名时建 10 分钟服务器预会话并设置 `__Secure-pet_<domain>_pre`（local=`pet_dev_<domain>_pre`），属性同对应会话 Cookie。已登录时绑定设备会话。随机值至少 256 bit，服务器会话保存随机值，校验摘要采用常量时间比较；前端只保存在内存，不放 URL/localStorage。

所有 Cookie POST/PUT/PATCH/DELETE，包括登录、退出、密码、multipart 上传、访问票据，携带 `X-CSRF-Token`。同时 Origin 必须精确等于配置的同源 origin；缺 Origin 时校验 Referer 的 scheme/host/port，二者皆缺拒绝，不根据 Host 动态放行。JSON 写检查 Content-Type，multipart 也检查来源/头。GET/HEAD 不业务写入，CSRF bootstrap 仅维护安全预会话，不用 GET 退出账号。

登录校验匿名预会话 CSRF；成功销毁预会话、创建新会话/CSRF，再 GET csrf 获取新值。退出/改密码撤销旧会话及 CSRF、删除 Cookie，随后重新取匿名凭据。有效会话内 GET csrf 返回同一值，避免并发标签页互相失效；会话轮换/撤销时更新。会话 CSRF 有效期不超过会话剩余期限，Redis 故障返回 503 `DEPENDENCY_UNAVAILABLE`，不可免校验。

CSRF 失败 403 `CSRF_INVALID`，不当成登录失效。前端可取新凭据，但不能静默重放写请求。Token 请求不携带 Cookie，无浏览器 CSRF；仍授权/校验/限流。Web 登录不返回 Token，不能靠混合载体绕过 CSRF。

## 设备、期限与失效

| 身份/设备 | 绝对期限 | 闲置期限 | 多设备 |
| --- | --- | --- | --- |
| PLATFORM/WEB | 8 小时 | 30 分钟 | 最多 5 个有效会话 |
| STAFF/WEB | 8 小时 | 30 分钟 | Web/小程序合计最多 5 个 |
| STAFF/MINIPROGRAM | 7 天 | 24 小时 | 同上，各设备独立 |
| CUSTOMER/MINIPROGRAM | 30 天 | 7 天 | 最多 5 个 |

SaLoginParameter 使用 setDeviceType(WEB/MINIPROGRAM)、setTimeout、setActiveTimeout、setIsShare(false)，isConcurrent=true。项目会话登记按账号分布式锁清理过期会话，达到上限返回 409 `SESSION_LIMIT_REACHED`，不把框架自动挤出作为策略。绝对期限不滑动；实际用户业务请求更新活跃度，me/CSRF获取/任务轮询及后台心跳不续期。闲置失效重新登录，不提供冻结恢复或 JWT refresh。

每设备独立 opaque token 与非凭据 UUID sessionId；同员工 Web/小程序不是同会话。logoutByTokenValue 只退当前设备并删除其 CSRF；logout-all 撤销该身份账号所有设备，一个身份退出不影响其他身份。

停用、密码重置/修改：DB 同事务更新 securityVersion 和撤销意图；提交后 kickout(loginId)。主动改密码也重新登录。每次受保护请求在授权前检查 DB 权威账号状态/securityVersion；Redis 延迟/删除失败不能放行旧版本。角色/门店/权限变化更新 authorizationVersion，立即停用旧权限快照、按当前权威值加载，保留合法会话并允许刷新 me。

401：AUTH_REQUIRED（未登录）、SESSION_EXPIRED（绝对/闲置失效）、SESSION_REVOKED（安全撤销）；已清理记录不能安全区分时用 SESSION_EXPIRED，不泄露账号存在性。网络故障、超时、503、取消请求都不代表会话失效。

## CORS、代理与秘密

默认同源，跨源 Web 关闭。小程序 HTTPS 请求不受浏览器 CORS 管理；允许开发 OPTIONS 时只声明 methods/headers，不建身份。跨源可选配置必须精确 origin 白名单、Allow-Credentials:true、Vary:Origin、明确 methods/headers（含 X-CSRF-Token/X-Trace-Id），禁止 *、null origin、后缀匹配、动态回显；仍校验 CSRF/来源。跨站 SameSite=None 必须 Secure，并单独验证浏览器第三方 Cookie 限制。

TLS 可在可信代理终止；代理删除用户 Forwarded/X-Forwarded-* 后重建，后端只允许可信代理网段，Host allowlist、publicOrigin 配置固定。Boot forwarded 处理仅在该网络约束下启用。[官方代理说明](https://docs.spring.io/spring-boot/4.0/how-to/webserver.html)。

仅小程序登录返回当前域 Token，Web 只 Set-Cookie。不得返回密码哈希、微信 session_key/AppSecret、支付密钥、CSRF 哈希、Redis 键。日志屏蔽 Cookie、Token、密码、手机号 code、票据 URL。临时内存探针不证明真实 Redis、HTTP、Cookie/CSRF，详见 [验证报告](../testing/P01-02-VERIFICATION.md)。

## P05-01 正式认证依赖（2026-10-08）

本轮已建立正式员工凭据、PasswordService与StaffAuthentication，完整说明见[身份初始化](../development/IDENTITY-BOOTSTRAP.md)，运行证据见[P05-01](../testing/P05-01-VERIFICATION.md)。算法沿用IDENTITY冻结，不新增依赖或另一认证框架；编码包括版本/迭代/独立随机盐，校验失败不输出编码，支持needsRehash但本轮不更新密码。

认证前仅调用两个固定SECURITY DEFINER函数，函数owner为非登录/非superuser/无BYPASSRLS的受限角色，FORCE RLS显式准许该角色必要读；PUBLIC EXECUTE撤销、固定search_path、明确限定表，无动态SQL。候选按有效租户编码+规范化登录名定位，内部对象不序列化、不返回Employee实体；tenantCode只作线索，不设GUC或建立可信范围。结构检查拒绝普通业务/Controller引用受限入口，只有未来身份认证适配器可使用公开契约。

密码匹配后重新读取当前状态与真实版本/角色范围，哈希期间版本变化则失败；无候选用随机运行时dummy编码执行比较。StaffIdentity不是CurrentPrincipal/会话，CurrentPrincipalProvider默认仍empty。后续P05-02必须接真实Sa-Token、载体解析/身份域、Cookie/CSRF、会话频控与统一LOGIN_FAILED，签发/每次请求复核权威状态版本。平台账号和客户微信身份本轮不建表，租户管理员仍STAFF。

Tenant/Employee停用、密码变化、角色/门店授权变化有权威字段/加载契约，但对应修改事务、撤销意图、会话撤销与敏感提交前校验尚未实现。异步执行前权威重验也未接入，不将字段存在或短期快照称为撤销已生效。本页上文所有HTTP接口仍是冻结计划。

## P05-02 STAFF 实施（2026-10-08）

本节为当前实现，上文P01/P04/P05-01“未实现”叙述是历史。Sa-Token Boot4 starter与Redis template沿用[版本矩阵](../development/VERSION-MATRIX.md)；实际解析、官方源码SHA、命令及门禁见[P05-02验证](../testing/P05-02-VERIFICATION.md)。只开放STAFF五个接口；PLATFORM/CUSTOMER有隔离StpLogic但没有登录入口或真实账号，不能使用STAFF凭据获得身份。

认证专属AuthenticationRedis继承官方SaTokenDaoForRedisTemplate；显式排除官方默认自动注册，仅安装此受限替代Bean，不作内存降级。官方1.46.0的servlet上下文Filter默认晚于本项目租户Filter，因此将其移动到Trace之后、STAFF认证之前，并只注册REQUEST；其finally清理官方ThreadLocal。顺序为Trace → Sa-Token servlet上下文 → STAFF载体识别/期限检查 → 受限staff_authorization权威查询 → CurrentPrincipal → TenantContextFilter的AUTHORITY_READ根范围 → 用例forPermission → ScopedTransaction同连接事务局部GUC → PostgreSQL RLS。受限查询不需要业务上下文，不关闭RLS、不建平台旁路。同步ERROR复用本请求可信快照；Servlet ASYNC不传播用户身份。

会话键沿用`pet:<environment>:staff:staff:*`，未来空间分别platform/customer；业务`pet:<environment>:v1:*`不混用。认证辅助键为`pet:<environment>:auth:staff:ip/account/pre/lock:<SHA-256摘要>`。独立DAO允许最长7天、128KiB固定JSON会话，未扩大业务Redis的24h/64KiB约束。线格式v1只接受SaSession固定投影、固定终端字段和字符串安全元数据；没有Java原生反序列化、类名/default typing、权限快照或任意对象。其他类型、未知格式、解析/Redis错误均503。Redis ACL、TLS、持久化和生产容量仍需部署验收。

会话保存可信Tenant/Employee ID、安全版本、非凭据sessionId、WEB/MINIPROGRAM、绝对到期、闲置期限和随机CSRF。角色/权限/门店/显示名每请求从正式数据库重载，绝不长期缓存授权。有效状态与Employee/Tenant securityVersion必须匹配；停用或版本变化返回401 SESSION_REVOKED，不依赖Redis删除成功。权限下降保留合法会话，当前me显示新授权；authorizationVersion保留数据库当前值。未来管理事务仍负责递增版本，当前每请求查询对角色停用/权限/关系/门店变动即使漏增授权版本也不复用旧授权。

Web生产`__Secure-pet_staff_sid/pre`，local HTTP为`pet_dev_staff_sid/pre`；同Path=/api/admin/、无Domain、HttpOnly、生产Secure、SameSite=Lax、会话Cookie无Max-Age/Expires。prod强制Secure；local显式false，HTTPS local可覆盖true。匿名GET csrf建立10分钟服务器预会话并返回256-bit随机凭据；CSRF采用[OWASP synchronizer token模式](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html)，摘要用MessageDigest.isEqual比较，不以可控Cookie自身作为信任根。Cookie写请求必须X-CSRF-Token及固定Origin；缺Origin必须同源Referer，两者都缺拒绝。登录销毁预会话，独立新Token/new sessionId/new CSRF，不将旧Cookie作为新Token；已登录重新登录撤销原当前设备。登录后GET csrf取新值；同会话多次获取稳定、不续闲置；退出删除对应服务端TokenSession与CSRF及同属性Cookie。CSRF缺失、错误、预会话过期403；会话到期401；不自动重放。当前只支持同源，未开放跨源CORS。

小程序POST token/login不接受STAFF会话Cookie、预会话Cookie或身份Header；成功仅该响应返回原始opaque Token，后续`X-Staff-Token: Bearer <原始值>`。Header Token必须匹配MINIPROGRAM设备，Cookie必须WEB；渠道不能切换或提升权限。Cookie/Header并存400 AUTH_CREDENTIAL_AMBIGUOUS，不比较或选择；多个/重复身份Header拒绝，其他身份或通用Authorization为401 AUTH_DOMAIN_MISMATCH（与Cookie并存仍400）。其他域Cookie忽略；Query/Body Token不认证。

Web绝对8h/闲置30min；小程序绝对7d/闲置24h；isConcurrent=true、isShare=false、autoRenew=false、dynamicActiveTimeout=true。me/CSRF/auth维护端点不续闲置，已识别的非auth业务请求才更新活跃时间，绝对期不滑动。相同员工跨渠道最多5个有效设备；按账号摘要分布式锁清理失效终端、到限409 SESSION_LIMIT_REACHED，禁止框架自动挤出。锁竞争429 Retry-After=1，30s租约失效或Redis失败503；不输出新凭据。退出只撤销当前设备，其他设备保留；过期/已退出重复退出401 SESSION_EXPIRED，无凭据401 AUTH_REQUIRED。logout-all/改密与完整设备管理尚未实施。

登录先422输入/规范化（密码保持原输入、Unicode码点沿用密码服务）→频控→受限候选→PBKDF2验证（缺候选使用随机运行时dummy哈希）→当前有效租户/员工/授权→创建会话。统一LOGIN_FAILED与既有中文错误注册表，不区分不存在/密码/停用；数据库/Redis错误503，不能当账号错误或删除会话。dummy仅减少明显差异，不承诺绝对恒定时间。tenantId/角色/权限等Body未知字段400，不作为认证输入。

频控为所有合法登录尝试的固定窗口：直接对端IP 60次/5min，规范化tenantCode+loginName摘要10次/15min；成功也计数且不清零，防成功流量绕过与并发清零竞争。Redis Lua一次原子递增并首次设TTL；阈值后429携带窗口剩余Retry-After，窗口自然恢复，不永久锁账号。没有可信租户时使用独立认证基础设施空间。server.forward-headers-strategy必须none，X-Forwarded-For/Forwarded不影响IP；当前未实现可信代理网段配置，生产通过固定公开Origin和网络约束部署，不能直接打开Boot转发处理。Redis故障关闭登录和认证，保留Cookie，不把503当401。

新请求在权威读SQL开始时看到已提交变化（READ COMMITTED）；已经验证并执行中的请求保持本次快照，不能撤回已完成操作，后续敏感写提交前重验仍需专门用例。当前真实会话来源的TenantTaskExecutor提交全部403禁止；旧30秒快照不证明撤销安全。纯内部技术边界仍保留，详见[异步约定](../conventions/ASYNC-EXECUTION.md)。

确认WEB Cookie会话过期/安全撤销时返回401并按同名/Path/安全属性清sid/pre Cookie，下一次可获取匿名CSRF并重新登录；不会在同一请求静默转成匿名成功。503数据库/Redis故障和载体冲突不清Cookie。恢复反例已由正式HTTP验证。

## P05-03 凭据与撤销的当前实施（2026-10-08）

接口及三端处理唯一见[IDENTITY P05-03](../contracts/IDENTITY.md#p05-03-员工凭据与会话安全接口2026-10-08)。本节替代P05-02中“改密/logout-all未实施”的当前边界，历史证据保持原样。只实现STAFF，不扩大平台/客户身份域。

复用Employee.securityVersion作为**安全失效代际**：密码修改/重置、本人退出全部会话、管理员撤销均递增；语义覆盖凭据改变和会话撤销，不机械新增credentialVersion/sessionGeneration字段。Employee.version同时递增用于管理员命令竞争；authorizationVersion不因单纯安全撤销变化。Tenant.securityVersion继续独立验证租户状态。每个会话保存签发时两个安全版本；每次身份建立从PostgreSQL读取有效员工、租户及版本，查询失败503，绝不按未变化放行。

`StaffSecurityOperations`在正式数据库事务中执行：可信STAFF根及当前tenant GUC → 租户FOR SHARE → 按UUID固定顺序锁操作者/目标员工 → 重载状态、版本及权限/目标管理策略 → 当前密码确认 → 用PasswordService原输入校验/拒绝同密码 → 仅更新密码和强制改密标志（如适用）→ 安全代际及资源版本+1 → 撤销意图及SUCCESS安全记录 → 提交前再次核对设备仍有效、当前安全/授权状态及操作者/目标权限与门店事实 → 提交。安全SQL是具体登记适配器，与既有JPA使用同一事务管理器/连接；不构造任意Entity或全局SQL通道。运行角色不能读取密码列，只能执行当前租户受限锁凭据函数；函数固定search_path、无动态SQL、PUBLIC EXECUTE撤销。未来授权/账号变更必须锁员工并同事务增版本，不能用无版本的管理写路径绕过串行化。

数据库提交是撤销线性化点。两次旧会话改密只有先提交者成功，另一请求事务内旧代际401；管理员同目标version的两个重置只一个成功，另一409；管理员重置和本人改密只有一个赢得旧状态，另一401或409。登录始终携带实际验证的不可变安全版本，创建前和创建后重载比对，绝不“旧密码验证后给新版本签发”；签发中发生变化则撤销未返回的设备并LOGIN_FAILED。签发后若撤销先完成，请求随后身份校验拒绝；撤销后重新验证正确凭据所建立的新代际会话允许存在。

提交完成后才清Redis。实际冻结Sa-Token的kickout(loginId)无设备过滤时覆盖WEB/MINIPROGRAM，但对稍后新会话也生效，不适合延迟补偿。因此通过该员工Account-Session最多5个终端索引，持既有账号锁，仅清`security < revoke_before`的本租户/本员工STAFF设备，调用官方logoutByTokenValue及精确设备补偿删除；不SCAN/KEYS整个Keyspace、不清其他身份域。官方退出在Token索引缺失时提前返回，补偿还清TokenSession、last-active及悬挂终端；当前索引检查完成后才记录COMPLETE。新代际设备不会被旧截止版本清理。签发也清已知旧代际终端，避免旧记录占满5设备。

数据库失败：密码/代际/资源版本/成功记录/撤销意图全部回滚，HTTP不宣称成功。数据库成功但Redis或完成标记失败：不回滚密码，200及X-Session-Cleanup=PENDING，旧会话由数据库立即拒绝；持久identity_session_cleanup保留截止代际，固定安全日志只含员工ID/traceId。恢复后目标员工合法me调用重试自己的未完成意图；后续对同目标安全操作也合并重试最大截止代际。重复物理删除与完成标记幂等，进程崩溃仍保留意图；没有定时平台/MQ/Outbox。目标长期不登录时记录可能保持PENDING，Redis设备按原TTL自然消失；不据此提前标记COMPLETE。TLS/HA/生产运维补偿与容量未验证。

敏感请求统一当前密码确认。四操作共用同一固定Redis Lua频控：直接对端IP最多60次/5分钟，可信tenant+操作者8次/5分钟，tenant+目标8次/5分钟；三维度一次原子计数，所有已进入用例的尝试（含成功）计数、不清零，失败429含Retry-After。辅助Key沿用pet:<env>:auth:staff:<kind>:<SHA256>，新增sip/actor/target，无密码/IP原文，不信X-Forwarded-For。Redis频控故障503关闭，不建竞争规则或永久停用账号。输入/CSRF在用例前拒绝，不逐条堆积安全表；日志与异常只返回固定中文提示，无rejectedValue/提交值。

安全操作记录和补偿表规则见[持久化P05-03](../conventions/PERSISTENCE.md#p05-03-安全事务和记录2026-10-08)，异步执行前权威重验及在途边界见[异步P05-03](../conventions/ASYNC-EXECUTION.md#p05-03-真实会话任务重验2026-10-08)。已经完成授权检查且执行中的普通请求/任务不承诺远程回滚；本轮四个敏感写有提交前重验。

账号索引锁复用P05-02的30秒租约；SET NX超时存在执行结果不确定性，finally仍以随机owner比较删除，不能删其他持有者。Redis完全不可达/进程退出时释放可能失败，锁自然到期前登录/清理可429/503，按Retry-After重试，不宣称立即恢复；不改变数据库权威失效与已提交密码。当前实现没有分布式租约续期/HA fencing保证，长进程暂停与Redis故障切换仍待相应生产验证。

## P05-04 平台独立认证（2026-10-08）

PLATFORM已接真实账号、独立初始化、六个控制面接口。两域共用SaIdentitySessions设备生命周期、AuthenticationRedis固定DAO和WebCookieSecurity来源/同步CSRF规则，业务状态/授权用例仍分别明确。STAFF旧会话数据保持原格式；PLATFORM不写tenant或tenantSecurity，无小程序Token签发。唯一SessionPrincipalProvider按/api/platform/或/api/admin/服务器路由安装对应身份，Trace→Sa servlet→域认证/PG重载→TenantFilter→MVC；平台不建立TenantContext。

平台Cookie、键、期限、频控及初始化/DB权限见[平台初始化](../development/PLATFORM-BOOTSTRAP.md)，实际接口见[IDENTITY](../contracts/IDENTITY.md#p05-04-平台正式接口2026-10-08)。同浏览器双域Cookie合法，本域才参与选择；本域重复/非法Header冲突拒绝，不允许客户端X-Login-Type/Query loginType选域。两域CSRF/pre/锁/频控/退出完全隔离，同UUID不互认互踢。Redis/DB故障503，不当密码错误、不免CSRF。

平台改密/全部退出复用DB权威安全代际、行锁事务、提交前状态/权限/设备复核和提交后按代际物理清理。旧密码验证不会升级到新安全代际，旧清理不删新会话；平台撤销不影响STAFF。PENDING由合法本人me/后续操作补偿，无定时补偿。实际运行与限制见[P05-04](../testing/P05-04-VERIFICATION.md)。

P05-04路径加固：服务器servletPath与原始URI必须一致（仅扣除服务器contextPath），API拒绝百分号编码/矩阵参数等别名400 BAD_REQUEST，避免MVC解码路由和认证/CSRF域判定不一致。两域共用requestPath，客户端不能通过编码平台或员工登录路径绕过过滤器。真实HTTP反例及修复历史见P05-04报告。
