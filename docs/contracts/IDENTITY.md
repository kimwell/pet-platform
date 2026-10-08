# 身份字段与认证接口

冻结日期：2026-10-07，P01-02。本文拥有身份DTO及端点。Cookie/CSRF/Token名称、期限、载体冲突唯一在 [认证](../architecture/AUTHENTICATION.md)；授权范围语义见 [授权](../architecture/AUTHORIZATION.md)。下文原冻结清单包含后续计划；当前STAFF九个实际接口和PLATFORM六个接口以文末P05-02/P05-03/P05-04清单为准。

## 当前身份 CurrentIdentity

| 字段 | required / 类型 | PLATFORM | STAFF / CUSTOMER |
| --- | --- | --- | --- |
| principalId | 必填ID字符串 | 平台主体ID | 当前域主体ID；同UUID跨域也不等价 |
| principalType | 必填稳定枚举 | PLATFORM | STAFF或CUSTOMER |
| tenantId | 必填但可null | 必须null | 必须当前可信Tenant ID |
| displayName | 必填非空中文显示名字符串，≤100字符 | 有 | 有；不以OpenID作显示名 |
| sessionId | 必填UUID，非凭据 | 当前设备 | 当前设备 |
| permissionCodes | 必填string[]，可[] | 控制面权限 | 当前域权限 |
| dataScope | 必填nullable对象 | null | `{grants: ScopeGrant[]}`，不可null |
| authorizedStoreIds | 必填ID[] | [] | 当前授权门店上限，可[]，不等于所有操作范围 |
| authorizationVersion | 必填非负整数字符串 | 当前授权版本 | 当前授权版本 |
| expiresAt / idleTimeoutSeconds | 必填UTC时间 / 有界整数 | 绝对到期/闲置秒 | 同义，值来自会话策略 |

ScopeGrant：permissionCode必填，scopes必填非空数组。scope为判别union：`{type:"TENANT"}`、`{type:"STORES",storeIds:[...]}`、`{type:"SELF"}`。每权限唯一grant，scopes可STORES+SELF并集；TENANT存在时移除冗余scope。storeIds是本权限集合且为authorizedStoreIds子集；空STORES不授权，授权字段不允许客户端回传变更身份。本人归属映射由模块代码登记，不下发可执行字段表达式。

staff/customer主体都固定一个Tenant；不提供请求临时切租户。authorizedStoreIds为当前有效上限（含TENANT授权所覆盖的当前门店），可选Store的无门店租户为[]，TENANT仍允许该租户级资源。平台另有平台权限不伪造租户范围。

## 端点清单

下表prefix：平台 `/api/platform`、员工 `/api/admin`、客户 `/api/customer`。

| 方法与路径 | 身份/载体 | 输入→成功data | 阶段/调用端 |
| --- | --- | --- | --- |
| GET platform/admin `/auth/csrf` | 公共，预会话或对应Cookie | 无→csrfToken/expiresAt | P05；Web |
| POST platform/admin `/auth/login` | 公共COOKIE流程+CSRF | 平台loginName/password；员工tenantCode/loginName/password→对应域身份，Cookie由响应发 | P05；Web平台/员工 |
| POST `/api/admin/auth/token/login` | 公共，TOKEN登录，不接受Cookie | tenantCode/loginName/password→TokenLoginResult | P05/P09；员工小程序 |
| POST `/api/customer/auth/wechat/login` | 公共，CUSTOMER TOKEN建立 | tenantCode/code→TokenLoginResult | P09/P10；客户小程序 |
| GET各prefix `/auth/me` | 对应域身份，允许其载体 | 无→CurrentIdentity | P05；对应端 |
| POST各prefix `/auth/logout` | 当前身份；Cookie需CSRF | 无→null，当前设备撤销 | P05；对应端 |
| POST各prefix `/auth/logout-all` | 当前身份；Cookie需CSRF | 无→null，该身份所有设备撤销 | P05；对应端 |
| PUT platform/admin `/auth/password` | 当前身份；Cookie需CSRF | currentPassword/newPassword→null，全部会话撤销 | P05；Web/员工小程序 |
| POST `/api/customer/auth/phone` | CUSTOMER TOKEN | 独立phoneCode→CurrentIdentity | P09/P10；客户小程序 |
| GET `/api/admin/context/stores` | STAFF，当前授权 | 无→可见可用Store摘要ID/name[] | P05/P07/P09；Web员工/员工小程序 |

客户为微信认证，无密码字段；客户密码修改接口不适用、不生成空实现。员工账号密码重置由identity:user:reset-password管理用例，后续P07声明具体接口；不存在公开任意重置入口。客户phoneCode与登录code不能复用，手机号补充不是登录先决条件；需要手机号的业务显式返回补充要求，不冒充会话失效。

TokenLoginResult：`{identity:CurrentIdentity,token:{headerName:string,value:string,expiresAt:string}}`；headerName必须匹配对应域。value为**原始opaque Token**，客户端请求层加Bearer，不重复加前缀。Web登录只返回CurrentIdentity，不把Token或密码信息放JSON。

协议请求例：员工Token登录 `{"tenantCode":"example-tenant","loginName":"example-user","password":"<用户输入，不是预置密码>"}`。响应例仅表示字段：identity各字段按上表，token.value用`<仅运行时签发的Token>`，不是可用凭据，不作为Mock验收。

登录失败统一401 LOGIN_FAILED，“登录失败，请检查登录信息”，不区分租户/账号存在或密码错误。员工loginName规范化规则由账号模块唯一实现；密码不trim/不日志记录。账号密码最低12、最高128字符，允许Unicode，不强制格式组合、不保存明文；服务端使用JDK SecretKeyFactory/PBEKeySpec的PBKDF2WithHmacSHA256，600000次、每密码独立32字节SecureRandom盐、256位输出；记录algorithmVersion/iterations/salt/hash，MessageDigest.isEqual比较，升级成功登录时重新哈希。不会trim或Unicode归一化密码，禁止快速SHA/MD5和Hutool替代；P05验证UTF-8/Unicode一致性、性能与限流，参数只可经证据提高，不在生产静默降低。[OWASP依据](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)。登录默认IP最多60次/5分钟，tenantCode+规范化loginName摘要最多10次/15分钟；微信code登录IP最多20次/分钟。Redis原子计数，生产依赖失败不跳过；429遵循API，不以自动永久停用账号作为限流。

未登录me401不返回null身份；网络故障保留本地会话。小程序会话槽位及切换见 [小程序页面](../conventions/MINIPROGRAM-PAGES.md)，Web并发401见 [Web状态](../conventions/WEB-STATE.md)。

## P04-01 内部可信身份接入（2026-10-08）

上表CurrentIdentity仍是P05计划公开DTO，本轮不生成第二套身份JSON。内部CurrentPrincipalProvider返回Optional<CurrentPrincipal>，无身份为empty；默认生产Bean也是empty。CurrentPrincipal最小字段为principalType、principalId、tenantId、非凭据sessionId、非负authorizationVersion、permissionCodes、authorizedStoreIds及按permissionCode的DataScope。所有集合防御性复制，不含displayName/Token/客户资料/会话期限副本，类型不在OpenAPI注册。

PLATFORM必须tenantId=null、无租户grants/门店；STAFF/CUSTOMER必须非空可信tenant，CUSTOMER只SELF。权限范围必须与主体域/ID/租户相符，grant键必须已授操作权限，STORES是身份门店上限子集。SELF由业务代码映射归属，非createdBy通用规则。P05适配器必须在服务器验证端点身份域、载体/会话和权威安全/授权版本后提供事实；客户端tenantCode只作登录线索，tenantId/角色/权限/门店集不作为身份来源，平台不得隐式转租户。

本轮内部模型与拒绝/生命周期技术证据见 [P04-01](../testing/P04-01-VERIFICATION.md)，没有接入或伪造Sa-Token会话，不改变公开CurrentIdentity字段、错误枚举或三端类型。

## P05-02 正式STAFF接口清单（2026-10-08）

本轮只实施下表，其他计划端点未开放。登录字段由历史username细化为本轮要求的`loginName`，规范化仍由IdentityNames唯一实现；不保留username别名。错误正文使用既有ErrorCode中文注册表，LOGIN_FAILED实际为“登录失败，请检查登录信息”，无身份枚举。

| 方法 / 路径 | 认证来源及CSRF | 输入 | 成功data | 会话副作用 / 错误 |
| --- | --- | --- | --- | --- |
| GET /api/admin/auth/csrf | 匿名pre或WEB Cookie；拒绝Token Header | 无 | CsrfResult：csrfToken、expiresAt | 匿名预会话10min；已登录同设备稳定值；不续闲置；503依赖/401失效 |
| POST /api/admin/auth/login | pre或WEB Cookie；必须X-CSRF-Token及同源Origin/Referer | tenantCode、loginName、password | CurrentIdentity | 新WEB Cookie/CSRF，销毁pre/旧当前会话；不含Token；401 LOGIN_FAILED、403 CSRF_INVALID、422、429、409设备上限、503 |
| POST /api/admin/auth/token/login | 无Cookie/身份Header；无浏览器CSRF | 同上 | TokenLoginResult | 新MINIPROGRAM设备；仅此返回raw token；401、422、429、409、503 |
| GET /api/admin/auth/me | WEB Cookie或MINIPROGRAM X-Staff-Token二选一 | 无；tenantId线索不影响身份 | CurrentIdentity | 权威重载；不续闲置；401未登录/失效、400混用、503 |
| POST /api/admin/auth/logout | 当前设备；Cookie必须CSRF及来源，MINIPROGRAM Token不要求浏览器CSRF | 无Body | null | 服务端撤销当前设备；WEB清同属性Cookie/CSRF；401无效重复退出、403CSRF、503 |

全部统一success/data或error/traceId信封、X-Trace-Id和Cache-Control:no-store。CurrentIdentity严格只STAFF，保留原冻结公开字段；dataScope为每权限grants及TENANT/STORES/SELF判别范围，无密码哈希/内部Session。Web与小程序成功类型分别为SuccessCurrentIdentity和SuccessTokenLoginResult；正式模型由实际Controller/Java DTO导出，不手写前端身份类型。生产OpenAPI安全方案StaffCookie与StaffToken明确二选一，Cookie写入说明独立CSRF。载体格式、期限/状态/频控/故障与一致性以[认证当前实施](../architecture/AUTHENTICATION.md#p05-02-staff-实施2026-10-08)为准。

会话确认失效的WEB请求：SESSION_EXPIRED/REVOKED仍401，同时清当前sid/pre Cookie；浏览器随后重新GET csrf并登录。503不清Cookie、不作为失效处理，载体冲突也不清；不自动重放写请求。

## P05-03 员工凭据与会话安全接口（2026-10-08）

当前新增以下四个真实接口，合计九个STAFF路径。它们均只允许有效STAFF；WEB Cookie必须独立CSRF及同源Origin/Referer，MINIPROGRAM仅X-Staff-Token，不混Cookie。敏感密码只在JSON Body，不接受Query/路径密码，不记录提交值。成功统一data=null、Cache-Control:no-store、Pragma:no-cache；`X-Session-Cleanup: COMPLETE|PENDING`说明物理清理是否完成，PENDING仍代表数据库变更成功和旧会话逻辑失效，不可自动重放。

| HTTP方法与路径 | 操作授权与目标 | JSON请求 | 重新确认 / 成功会话影响 | 重复、失败及可观察状态 |
| --- | --- | --- | --- | --- |
| PUT /api/admin/auth/password | 有效当前员工本人；目标只取可信身份，不接受employeeId | currentPassword、newPassword | 当前密码；同事务改密码、清强制改密标志、递增安全/资源版本；撤销本人全部WEB/MINIPROGRAM设备，包括当前 | 旧会话重试401；确认失败403 SECURITY_CONFIRMATION_FAILED、规则/同密码422；DB失败503不宣称成功；查新密码登录及安全记录核对结果 |
| POST /api/admin/auth/logout-all | 有效当前员工本人 | currentPassword | 当前密码；不改密码/强制改密状态，递增安全/资源版本；撤销本人各渠道全部设备 | 旧会话重复401，不再次递增；重新登录后新授权请求是新的撤销操作 |
| PUT /api/admin/identity/users/{employeeId}/password | identity:user:reset-password；当前租户内独立目标管理策略 | version（目标资源版本十进制字符串）、currentPassword（操作者）、newPassword（目标临时密码） | 操作者当前密码；改目标密码、设置passwordChangeRequired、递增目标安全/资源版本，撤销目标全部设备；操作者保留 | 同version并发/重放409 VERSION_CONFLICT；无操作权限403、跨租户/范围外/不存在404、受保护账号403；不得改变角色/门店/tenant/其他字段 |
| POST /api/admin/identity/users/{employeeId}/revoke-sessions | 独立identity:user:revoke-sessions；同租户目标管理策略 | version、currentPassword（操作者） | 操作者当前密码；不改目标密码、角色、归属或强制改密状态；递增目标安全/资源版本，撤销目标全部设备 | 同旧version重复409；无权限403，范围外404；成功后目标凭正确密码可立即新登录 |

管理接口路径决定唯一目标，Body未知employeeId/tenantId/角色等400拒绝，不做Entity绑定。version为已有Employee.version，不能用securityVersion或authorizationVersion替代；目标版本来自合法的初始化/管理资料，完整员工读取/CRUD尚未实现，P07必须提供经过目标数据授权的资源版本。超过Long范围422，不截断。重置本人一律403，必须走本人改密；系统保留账号不允许管理员重置/撤销；持有保留tenant-admin角色的账号不允许管理员重置，不通过停用角色绕过保护。撤销tenant-admin会话要求操作者也为tenant-admin且持有TENANT范围的独立撤销权限，不需要“最后管理员”数量限制，密码仍可登录。

目标管理策略：TENANT范围允许本租户；STORES要求目标非空全部门店归属均在本操作范围内，多店目标不能因仅一个可见门店而被接管；SELF不能管理另一个员工。另校验目标有效权限与每权限范围均被操作者覆盖，操作者SELF不能用于覆盖另一个主体。查看员工权限不授予管理密码权限。新撤销权限只在PermissionCatalog声明，不加进ADMIN_PERMISSIONS、V1初始化或普通启动授权；既有角色需后续明确授权管理赋予，不能因角色叫管理员而放行。

管理员提供临时密码并通过经过认可的安全人工渠道交付，本API不返回或存储明文、不自动发短信/邮件、不设置固定默认密码。生产密码规则、PBKDF2、Unicode和原输入语义复用PasswordService；新密码与目标现密码相同422拒绝。临时密码登录仍通过生产密码验证；CurrentIdentity新增必填boolean `passwordChangeRequired`。该状态只允许me、WEB csrf、本人改密、当前退出、退出全部设备，其余路径403 PASSWORD_CHANGE_REQUIRED；异步任务也不可提交。本人改密成功清状态并撤销包括临时当前设备的全部会话，再用新密码登录。

本人成功后WEB清同名称/Path/安全属性的sid/pre Cookie，设备TokenSession删除时CSRF同时删除；Redis未清时旧CSRF也不能通过已失效会话认证。前端看到200即清CSRF内存、Query身份缓存并进入重新登录，不等下一次401，也不自动重放。小程序清STAFF Token/当前身份槽位、取消旧请求并忽略旧身份回调；CUSTOMER槽位不受影响。管理员操作成功仅刷新目标管理资料，不清操作者会话。PENDING提示清理待补偿，不可把503/网络失败当作已成功或确定回滚；不自动重试密码操作。

频控、权威版本、事务、竞争及补偿以[认证P05-03](../architecture/AUTHENTICATION.md#p05-03-凭据与撤销的当前实施2026-10-08)为准；角色保护以[授权P05-03](../architecture/AUTHORIZATION.md#p05-03-敏感员工管理策略2026-10-08)为准。

## P05-04 平台正式接口（2026-10-08）

当前PLATFORM六个路径全部使用Web Cookie，没有平台Token签发/微信入口。服务端路径决定认证空间，Header/Query的loginType无效且拒绝；浏览器同时带合法STAFF和PLATFORM Cookie时只读取端点对应Cookie。未知Body字段400拒绝，客户端tenantId不能改变身份域或创建TenantContext。平台登录名与初始化使用同一IdentityNames规则；原计划username明确落实为loginName。

| 方法 /api/platform/auth前缀 | 请求 | 成功data | 权限 / CSRF / 频控 / 会话副作用 |
| --- | --- | --- | --- |
| GET /csrf | 无 | CsrfResult | 公共；匿名10分钟独立pre；已登录返回当前绑定CSRF，不续闲置 |
| POST /login | loginName、password | PlatformCurrentIdentity | 公共；平台CSRF及固定同源来源；IP60/5分钟、账号摘要10/15分钟；轮换当前平台设备与CSRF，销毁pre，只Set-Cookie |
| GET /me | 无 | PlatformCurrentIdentity | platform:session:manage；每次权威重载；不续闲置，尝试补偿本人的清理意图 |
| POST /logout | 无 | null | platform:session:manage；平台CSRF及来源；只删除当前平台设备并清平台Cookie，STAFF保留 |
| PUT /password | currentPassword、newPassword | null | platform:credential:change；平台CSRF及来源、旧密码确认；敏感IP60/5分钟、主体/目标8/5分钟；数据库同事务改密/代际/记录/意图，全部旧平台设备失效 |
| POST /logout-all | currentPassword | null | platform:session:manage；同上敏感安全；不改密码，全部旧平台设备失效 |

敏感成功Header为X-Session-Cleanup: COMPLETE或PENDING；PENDING是已提交、旧会话逻辑失效而物理清理待补偿。200后清平台Cookie/CSRF缓存并重新登录，不清STAFF。DB/Redis故障503，错误凭据/不存在/停用统一401 LOGIN_FAILED，不泄露账号存在性；403表示CSRF/权限/重新确认，422密码规则/同密码，429附Retry-After，409设备上限。写入失败/网络中断不能推断一定回滚或自动重放。所有响应no-store、统一信封和trace。

PlatformCurrentIdentity为实际Java DTO和OpenAPI生成类型：principalType仅PLATFORM，tenantId、dataScope必填且仅null，authorizedStoreIds必填空数组；其余principalId/displayName/sessionId/permissionCodes/authorizationVersion/expiresAt/idleTimeoutSeconds沿用冻结语义。CurrentIdentity仍仅STAFF，tenantId为必填UUID字符串、dataScope为必填ScopeData；没有把STAFF放宽成可空object。可按principalType组成两种生成类型的判别union，不含密码、哈希、Token或完整Session。

权限代码固定三项；本轮没有多角色管理、平台账号列表/邀请/重置他人、租户CRUD/模拟登录/导出。初始化、安全记录、数据库函数与部署条件见[平台初始化](../development/PLATFORM-BOOTSTRAP.md)，实际证据见[P05-04](../testing/P05-04-VERIFICATION.md)。CUSTOMER模型/微信真实登录仍未实现，不从三端类型通过推导客户认证完成。

P05-04路径安全：API仅接受规范服务器路径；百分号编码/矩阵参数别名400 BAD_REQUEST，不作为免CSRF的登录入口。认证与MVC使用同一服务器路径，平台与员工均适用。
