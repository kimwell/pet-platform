# 认证、设备会话与传输安全

当前实施状态见本文P05-01章节及[P05-01验证](../testing/P05-01-VERIFICATION.md)。旧阶段“尚未实现”描述保留为历史范围；冻结安全契约不变。
冻结日期：2026-10-07，P01-02。本文拥有认证空间、Cookie/Token、CSRF、期限与撤销。接口及字段见 [身份契约](../contracts/IDENTITY.md)，权限见 [授权](AUTHORIZATION.md)。当前未实现正式认证。

## 精确版本集成依据

Sa-Token 使用 [版本矩阵](../development/VERSION-MATRIX.md) 的 starter/Redis 组合。已读取官方 [多账号](https://sa-token.com/up/many-account.html)、[配置](https://sa-token.com/use/config.html) 和 [精确版本源码](../testing/evidence/P01-02/sa-token-source-record.json)。源码及临时编译探针确认 StpLogic(String)、setConfig、createLoginSession、createSaLoginParameter、setTokenValueToStorage、getLoginIdByToken、logoutByTokenValue、kickout 的真实 API；不猜测新版调用。

三类身份各一个 StpLogic：loginType=`platform/staff/customer`；tokenName=`pet:<environment>:platform/staff/customer`。实际键以 `<tokenName>:<loginType>:` 开始，后缀 token/session/token-session/last-active 等。辅助 CSRF 键为 `pet:<environment>:csrf:<domain>:`。environment 必填、启动校验，不同部署不共用前缀。

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
