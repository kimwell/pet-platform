# 身份字段与认证接口

冻结日期：2026-10-07，P01-02。本文拥有身份DTO及端点。Cookie/CSRF/Token名称、期限、载体冲突唯一在 [认证](../architecture/AUTHENTICATION.md)；授权范围语义见 [授权](../architecture/AUTHORIZATION.md)。以下均为计划接口/协议示例。

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
| POST platform/admin `/auth/login` | 公共COOKIE流程+CSRF | username/password；staff另tenantCode→CurrentIdentity，Cookie由响应发 | P05；Web平台/员工 |
| POST `/api/admin/auth/token/login` | 公共，TOKEN登录，不接受Cookie | tenantCode/username/password→TokenLoginResult | P05/P09；员工小程序 |
| POST `/api/customer/auth/wechat/login` | 公共，CUSTOMER TOKEN建立 | tenantCode/code→TokenLoginResult | P09/P10；客户小程序 |
| GET各prefix `/auth/me` | 对应域身份，允许其载体 | 无→CurrentIdentity | P05；对应端 |
| POST各prefix `/auth/logout` | 当前身份；Cookie需CSRF | 无→null，当前设备撤销 | P05；对应端 |
| POST各prefix `/auth/logout-all` | 当前身份；Cookie需CSRF | 无→null，该身份所有设备撤销 | P05；对应端 |
| PUT platform/admin `/auth/password` | 当前身份；Cookie需CSRF | currentPassword/newPassword→null，全部会话撤销 | P05；Web/员工小程序 |
| POST `/api/customer/auth/phone` | CUSTOMER TOKEN | 独立phoneCode→CurrentIdentity | P09/P10；客户小程序 |
| GET `/api/admin/context/stores` | STAFF，当前授权 | 无→可见可用Store摘要ID/name[] | P05/P07/P09；Web员工/员工小程序 |

客户为微信认证，无密码字段；客户密码修改接口不适用、不生成空实现。员工账号密码重置由identity:user:reset-password管理用例，后续P07声明具体接口；不存在公开任意重置入口。客户phoneCode与登录code不能复用，手机号补充不是登录先决条件；需要手机号的业务显式返回补充要求，不冒充会话失效。

TokenLoginResult：`{identity:CurrentIdentity,token:{headerName:string,value:string,expiresAt:string}}`；headerName必须匹配对应域。value为**原始opaque Token**，客户端请求层加Bearer，不重复加前缀。Web登录只返回CurrentIdentity，不把Token或密码信息放JSON。

协议请求例：员工Token登录 `{"tenantCode":"示例入口","username":"示例账号","password":"<用户输入，不是预置密码>"}`。响应例仅表示字段：identity各字段按上表，token.value用`<仅运行时签发的Token>`，不是可用凭据，不作为Mock验收。

登录失败统一401 LOGIN_FAILED，“账号或登录信息不正确”，不区分租户/账号存在或密码错误。username规范化规则由账号模块唯一实现；密码不trim/不日志记录。账号密码最低12、最高128字符，允许Unicode，不强制格式组合、不保存明文；服务端使用JDK SecretKeyFactory/PBEKeySpec的PBKDF2WithHmacSHA256，600000次、每密码独立32字节SecureRandom盐、256位输出；记录algorithmVersion/iterations/salt/hash，MessageDigest.isEqual比较，升级成功登录时重新哈希。不会trim或Unicode归一化密码，禁止快速SHA/MD5和Hutool替代；P05验证UTF-8/Unicode一致性、性能与限流，参数只可经证据提高，不在生产静默降低。[OWASP依据](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)。登录默认IP最多60次/5分钟，tenantCode+规范化username摘要最多10次/15分钟；微信code登录IP最多20次/分钟。Redis原子计数，生产依赖失败不跳过；429遵循API，不以自动永久停用账号作为限流。

未登录me401不返回null身份；网络故障保留本地会话。小程序会话槽位及切换见 [小程序页面](../conventions/MINIPROGRAM-PAGES.md)，Web并发401见 [Web状态](../conventions/WEB-STATE.md)。

## P04-01 内部可信身份接入（2026-10-08）

上表CurrentIdentity仍是P05计划公开DTO，本轮不生成第二套身份JSON。内部CurrentPrincipalProvider返回Optional<CurrentPrincipal>，无身份为empty；默认生产Bean也是empty。CurrentPrincipal最小字段为principalType、principalId、tenantId、非凭据sessionId、非负authorizationVersion、permissionCodes、authorizedStoreIds及按permissionCode的DataScope。所有集合防御性复制，不含displayName/Token/客户资料/会话期限副本，类型不在OpenAPI注册。

PLATFORM必须tenantId=null、无租户grants/门店；STAFF/CUSTOMER必须非空可信tenant，CUSTOMER只SELF。权限范围必须与主体域/ID/租户相符，grant键必须已授操作权限，STORES是身份门店上限子集。SELF由业务代码映射归属，非createdBy通用规则。P05适配器必须在服务器验证端点身份域、载体/会话和权威安全/授权版本后提供事实；客户端tenantCode只作登录线索，tenantId/角色/权限/门店集不作为身份来源，平台不得隐式转租户。

本轮内部模型与拒绝/生命周期技术证据见 [P04-01](../testing/P04-01-VERIFICATION.md)，没有接入或伪造Sa-Token会话，不改变公开CurrentIdentity字段、错误枚举或三端类型。
