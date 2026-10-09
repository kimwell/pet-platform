# B02 平台控制面与最小组织契约

日期：2026-10-09。正式类型由生产 Controller 生成至 [OpenAPI](../../packages/api-contracts/openapi/backend.openapi.json)，验收事实见 [B02](../testing/B02-ACCEPTANCE.md)。本文件拥有新增控制面和 Organization 语义；认证、Cookie/CSRF、统一响应、标量及分页沿用 [IDENTITY](IDENTITY.md)、[API](API.md)、[DATA-TYPES](DATA-TYPES.md)、[PAGINATION](PAGINATION.md)。

## 身份、模型和授权

PLATFORM 为独立无 tenantId 身份，只有本域正式 Web Cookie；不接受 STAFF/CUSTOMER Token、本域外 Cookie、客户端 tenantId 或角色名称。读取与每类写操作分别授权。Tenant/Store 仅管理元数据，控制面没有租户业务数据、模拟登录、通用切换租户、任意 SQL 或文件导出能力。Organization 是独立租户内部组织，不是 Store，也不自动影响员工权限范围。

平台沿用 platform_account/platform_permission 固定权限集合，不新增角色体系。原三项本人权限与受控缓存权限保留，新增19项控制面代码。新增能力由独立初始化身份显式执行 platform-management-upgrade，仅补首次 bootstrap 账号；迁移及普通启动不自动赋权。创建或配置权限必须是操作者当前权限子集，强制保留 platform:session:manage 和 platform:credential:change。权限目录显示 code/grantable；不可授项不靠前端约束。

租户普通投影：id/code/name/status/initialized/version/createdAt/updatedAt。平台账号普通投影：id/loginName/displayName/status/version/permissions/createdAt/updatedAt。Store：id/tenantId/code/name/status/version/createdAt/updatedAt。Organization 公开投影：id/code/name/status/version（数据库另保留创建/更新时间）。UUID 为字符串，资源 version 为十进制非负 Long 字符串，UTC 时间、ACTIVE/DISABLED 枚举。普通 DTO 不返回密码、散列、内部安全/授权版本或会话秘密。初始化密码/重置密码仅请求 writeOnly；不提供一次性密码回显。

## 平台接口

以下路径均以 /api/platform 开头，写请求必须通过本域 CSRF 和 Origin 检查。创建成功仍沿用真实 HTTP 200 的统一成功信封；失败使用真实状态和中文 error/traceId。

| 方法及路径 | 独立权限 | 输入 → data |
| --- | --- | --- |
| GET /tenants | platform:tenant:list | 六个列表参数 → PageResponseTenantView |
| GET /tenants/{id} | platform:tenant:detail | 无 → TenantView |
| POST /tenants | platform:tenant:create | code/name → ControlMutation |
| PUT /tenants/{id} | platform:tenant:update | version/name → ControlMutation |
| PUT /tenants/{id}/status | platform:tenant:enable 或 disable | version/status → ControlMutation |
| GET /accounts | platform:account:list | 六个列表参数 → PageResponseControlAccountView |
| GET /accounts/{id} | platform:account:detail | 无 → ControlAccountView |
| POST /accounts | platform:account:create 且 grant | loginName/displayName/initialPassword/permissions → ControlMutation |
| PUT /accounts/{id} | platform:account:update | version/displayName → ControlMutation |
| PUT /accounts/{id}/status | platform:account:enable 或 disable | version/status → ControlMutation |
| PUT /accounts/{id}/permissions | platform:account:grant | version/permissions → ControlMutation |
| PUT /accounts/{id}/password | platform:account:reset-password | version/newPassword → ControlMutation |
| POST /accounts/{id}/sessions/revoke | platform:account:revoke-sessions | version → ControlMutation |
| GET /permissions | platform:account:grant | 无 → ControlPermissionOption[] |
| GET /tenants/{tenantId}/stores | platform:store:control-list | 无 → ControlStoreView[] |
| POST /tenants/{tenantId}/stores | platform:store:control-create | code/name → ControlMutation |
| PUT /tenants/{tenantId}/stores/{id} | platform:store:control-update | version/name → ControlMutation |
| PUT /tenants/{tenantId}/stores/{id}/status | platform:store:control-status | version/status → ControlMutation |

ControlMutation 仅 id/version/sessionCleanupComplete。无初始化结果或秘密。租户/门店状态操作返回 sessionCleanupComplete=false：数据库身份代际已失效，Redis 老记录不宣称物理清理完成，依既有 TTL 结束。平台账号安全操作复用 P05 精确 cutoff 清理意图；失败仍以权威安全版本拒绝老会话，后续合法登录/安全操作补偿。

六参数为 keyword/status/page/pageSize/sortBy/sortOrder，缺省 page=1/pageSize=20/createdAt desc；pageSize 1～100，page/offset 沿用 int32 边界。total 为字符串。keyword 1～100 Unicode 码点，匹配规范化编码/登录名或名称，百分号和下划线按文字匹配。租户排序仅 code/name/status/createdAt，账号仅 loginName/displayName/status/createdAt，同向 id 稳定次序。未知/重复参数及未知 Body 字段400，非法排序400 SORT_INVALID，越界422。Store 为当前租户最小目录，无行业字段或业务报表。

code 使用既有 tenantCode 规则：1～32位小写 ASCII 字母数字/连字符、首位字母数字；loginName 复用既有规范化小写1～64位账号规则；name/displayName strip 后1～100 Unicode码点；密码复用 P05 的12～128码点、至多256 UTF-16单位，原样校验不 trim。标识创建后不可改。重复 code/loginName 为422及准确字段错误；权限未登记/重复、缺少两项本人权限为422 permissions；超出操作者可授集合为403。没有物理删除接口。

## 生命周期、并发与初始化

租户创建为 ACTIVE、initialized=false，不能声称立即可用。详情提供可执行既有 bootstrap 路径：独立 pet_bootstrap 登录身份，通过 Console 或受控 stdin 输入首位 STAFF 密码，按编码和同名待初始化租户原子建立员工/保留角色/基础授权并将 initialized=true。不要求门店；显式传正式 store 参数才建立真实门店。不能初始化 DISABLED、名称不符、已完成或未知半成品租户；绝不覆盖既有密码/关系。失败事务全回滚，待初始化记录保留；确认未提交后可重试，完成后重复拒绝。B01/Organization 新权限分别显式 identity-management-upgrade/organization-upgrade，幂等且只面向保留 tenant-admin。详见 [初始化](../development/IDENTITY-BOOTSTRAP.md)。

租户启停均在锁行后递增权威 security_version；STAFF/CUSTOMER 新登录核对 ACTIVE，旧会话每次服务端核对版本。重新启用需新登录，老撤销会话不能复活。受控异步任务执行前重新授权，即使停用后又启用，也拒绝旧快照。门店状态同样保守推进所属租户安全版本。其他租户及平台会话不受这项变化影响。

平台停用/启用/权限配置/重置密码/全会话撤销同事务推进 security_version；权限和状态另推进 authorization_version，精确平台域 Redis 清理。资料改名仅资源版本变化。本人停用、本人管理撤权/重置/撤销禁止，使用本人安全入口；不能移除最后一个 ACTIVE 且同时具有 session:manage、credential:change、account:grant、account:enable、account:reset-password 的有效管理入口（均带 platform: 前缀）。所有控制面写共用固定事务 advisory 锁，之后锁当前账号和目标、复核版本与授予上限，防止两个并发操作都移除最后入口。异步清理不撤销新版本会话。

版本冲突409 VERSION_CONFLICT，已处于相同状态或最后管理入口约束409 BUSINESS_STATE_CONFLICT；本人受保护操作403。无权限403，资源不存在404，身份无效401，字段422，依赖故障503安全拒绝且不伪装空列表。每个成功管理动作与最小无秘密管理事件同事务；通用审计查询/检索归B03。

## 独立 STAFF Organization

| 方法及路径（以 /api/admin 开头） | 独立 TENANT 权限 | 输入/输出 |
| --- | --- | --- |
| GET /identity/organizations | identity:organization:list | 六参数 → PageResponseOrganizationView |
| GET /identity/organizations/{id} | identity:organization:detail | → OrganizationView |
| POST /identity/organizations | identity:organization:create | code/name → MutationResult |
| PUT /identity/organizations/{id} | identity:organization:update | version/name → MutationResult |
| PUT /identity/organizations/{id}/status | identity:organization:enable 或 disable | version/status → MutationResult |

扁平最小模型仅本租户唯一 code、name、ACTIVE/DISABLED、资源版本和时间；不臆造部门/岗位/层级/员工组织授权。查询、写、版本和成功审计复用 B01 受控租户事务及 FORCE RLS，无上下文拒绝。客户端 tenantId 不授予身份，PLATFORM/CUSTOMER 不能进入 STAFF API。组织排序同租户 code/name/status/createdAt。Store 与 Organization 各自持久化，不借组织变动扩权。

## Web 与边界

/platform/tenants、/platform/tenants/$tenantId、/platform/accounts、/platform/accounts/$accountId 直接组合官方 Ant Design；Organization 位于 /admin/identity/organizations 及详情路由。列表 URL 六参数，详情返回白名单列表状态；total 安全转换、取消请求、真实身份/授权/会话代际 Query Key 及撤权清理沿用 B01/P07-02。401走正式会话失效，403/404/422/503/网络分别提示；503不自动退出。危险操作说明影响并确认；密码不进 URL、日志或持久化浏览器状态，提交结束清理；身份变化销毁旧页、缓存和草稿。

路线冻结的本批必选项为控制面租户/最小门店、平台账号与独立 Organization。邀请没有冻结模型、投递/接受契约或原路线必选任务；本批使用正式创建和安全密码输入，不以“邀请已完成”替代。套餐、订阅、计费、配额、品牌配置、冒用登录、行业门店业务和跨租户业务导出均不新增。生产角色部署、TLS/远程CI/多OS/容量与真实辅助技术边界保留。
