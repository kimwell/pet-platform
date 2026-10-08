# 权限与授权执行

当前STAFF认证及敏感操作实施见本文P05-02/P05-03章节及[P05-03验证](../testing/P05-03-VERIFICATION.md)。旧阶段叙述保留为历史范围；本轮渠道/输入细化见当前章节。
冻结日期：2026-10-07，P01-02。本文拥有权限注册、范围组合、授权顺序；字段见 [身份契约](../contracts/IDENTITY.md)，隔离执行见 [多租户](MULTI-TENANCY.md)。

代码固定 `模块:资源:动作`，正则 `^[a-z][a-z0-9-]*:[a-z][a-z0-9-]*:[a-z][a-z0-9-]*$`；例如 identity:user:list/create/update/disable/export 分别独立。代码声明权限注册表：身份域、中文名、动作及支持范围。DB 仅保存角色、主体和授权关系及范围参数，不提供任意权限脚本或隐含 * 全权。

接口逐个声明权限；StpInterface/项目端口按明确 loginType 获取权限，禁止默认 StpUtil 跨域。PLATFORM 只访问控制面；STAFF 访问当前 Tenant；CUSTOMER 只有代码声明的本人能力，不继承员工角色。无隐含管理员业务全权。

顺序：认证/安全状态 → 操作权限 → 本权限数据范围 → 关联完整性 → 业务状态 → 敏感写提交前安全版本。缺权限 403 PERMISSION_DENIED；具权限但资源在范围外统一 404 RESOURCE_NOT_FOUND；合法资源状态不允许 409 BUSINESS_STATE_CONFLICT。操作权限、数据范围和状态分别实现。

同 permissionCode 多角色范围取并集：TENANT 包含当前租户全部范围；否则 STORES 集合与 SELF 行规则可并存。不同权限不能借用最大范围；list:TENANT 不让 update 自动 TENANT。门店集合与身份门店上限相交；空 STORES 没有行，null store 租户级行不因此放行。SELF 归属字段由模块登记，使用 principalType+principalId；不能统一用 createdBy，没有归属规则则禁止 SELF。

前端字段仅控制路由/按钮呈现，后端最终复核。可返回 allowedActions 提示业务状态，但不作为权限凭据；中文禁用理由，不在无权响应泄露资源存在性。

角色/门店/账号关系变更与 authorizationVersion 同事务提交；每次授权以权威当前版本为准。前端 me 变化取消旧请求、清理缓存、重建导航。合法会话权限下降返回 403，保留未保存输入，不自动重发写请求。

平台跨租户业务访问默认关闭，无空 tenantId 绕过。若后续需要，单独设计明确目标租户、具体权限、理由、范围和审计的用例，仍建受限 TenantContext、受 DB 策略约束；当前接口没有该能力。

管理者不能给他人授予超出自己可委派的权限、租户、门店。敏感授予/撤销审计，真实测试覆盖同 ID 跨身份、撤销、list/update 范围差异、门店交集、本人与状态判断。

## P04-01 范围模型与门店校验（2026-10-08）

沿用冻结名称TENANT/STORES/SELF，DataScope绑定tenantId及principalType/principalId并防御性复制集合；STORES允许空，含义是没有门店权限，绝不代表全部。SELF仅表达行归属要求，不自动转换为createdBy，也不能单独授权整个门店；实际SELF谓词与业务owner映射归P04-02及对应业务任务。

ScopeGrant.mergeForPermission显式要求同permissionCode、同主体、同租户；同权限角色按冻结并集规则合并，TENANT归一化为唯一类型，否则STORES+SELF可并存，门店集与身份上限相交。不同权限不能合并或借用最大范围。CurrentPrincipal验证权限/范围/门店上限一致；它不是公开身份DTO，角色授权与权威版本查询仍待P05。当前模型可表达授权结果，不是完整角色/权限管理。

StoreScopeGuard只接受目标storeId，通过StoreOwnershipReader获取内部归属事实；shared不依赖platform/store实现，也不接受调用者自称所属租户的Store对象。调用前必须建立本操作的业务范围，并由应用用例检查requirePermission。TENANT也不是平台特权，门店仍必须属于当前租户且位于身份门店上限。STORES再检查本权限storeIds。SELF与空STORES都不授权整店操作。

| 事实与当前范围 | 行为 |
| --- | --- |
| 门店不存在/属于其他租户 | 404 RESOURCE_NOT_FOUND，统一“资源不存在或不可访问” |
| 本租户但不在本权限范围或身份门店上限 | 同一404与文案，不泄露存在性 |
| 本租户且STORES含目标门店、身份上限允许 | 通过；可显式openStore绑定当前门店/MDC |
| TENANT、本租户且身份上限允许 | 通过；仍核对真实归属 |
| 仅SELF或STORES空集合 | 404；不会自动推断本人经营门店 |
| 生产尚无StoreOwnershipReader事实实现 | 默认503 DEPENDENCY_UNAVAILABLE，绝不放行 |

openStore只绑定经过上述检查的当前门店；同门店嵌套可恢复，不允许已选择门店的内层静默换店。当前无正式Store表/状态规则，未来platform/store实现必须提供权威归属并按用例检查有效状态，不能把当前查询端口当Store CRUD已完成。

模型、Guard与技术请求链结果见 [P04-01](../testing/P04-01-VERIFICATION.md)，操作范围不等于数据库行隔离、真实角色撤销或业务验收。

## P04-02 资源策略与写入授权（2026-10-08）

本轮已将TENANT/STORES/SELF进入真实SQL，由ResourceAccessPolicy按资源静态声明支持组合：tenantOnly仅TENANT，stores支持TENANT/STORES，owned支持TENANT/SELF，storesAndOwned同时支持三者及STORES+SELF并集。未声明组合整体拒绝，即使其中一个分支有映射也不擅自忽略未知分支。TENANT门店查询仍检查身份门店上限；租户级无store字段不会被STORES放行。空STORES对已声明门店模型是空结果；SELF使用principalType+ownerId，员工/客户同UUID不会互认。具体模型必须明确owner业务意义及不可变性。

ScopedPersistence实例固定本用例permissionCode，访问时requirePermission核对，避免将list范围用于update。租户与资源条件在所有列表/count/exists/单条/ID集合以及固定批量DML上强制AND；单条未找到和范围外均TenantAccessDeniedException（既定404）。无可信BUSINESS或只有AUTHORITY_READ不能使用该能力。缺操作权限仍403；缺正式门店事实源仍沿用P04-01的503，不提供生产默认事实。

新增来自可信上下文，门店归属/授权经Guard；更新从范围内加载managed实体，仅模块固定业务方法改允许字段，不接收请求Entity。批次先锁定并核对完整去重目标，含不存在/跨租户/未授权门店即统一拒绝，写入语句再次带范围、核对实际影响行数。SELF读写、主体域冲突、门店上限、事务期间范围改变和独立数据库结果见[P04-02](../testing/P04-02-VERIFICATION.md)。该测试不实现真实认证、权限管理、权威撤销或提交前实时版本重验；P05及对应模块仍需承担这些责任。

## P05-01 正式角色授权加载（2026-10-08）

正式Employee/Role/EmployeeRole/RolePermission/EmployeeStore由identity拥有，RolePermission保存permission_code与scope_type的关联。PermissionCatalog注册9项基础管理代码（员工、角色、门店），初始化使用显式TENANT清单，无*或未来行业权限；准确清单及支持组合见[身份初始化](../development/IDENTITY-BOOTSTRAP.md#权限与认证接入契约)。PLATFORM权限及账号留待后续，不把STAFF租户管理员当平台身份。

加载只采用ACTIVE租户/员工/角色，身份门店上限来自明确EmployeeStore中ACTIVE Store；STORES按该上限取交集，TENANT仍不能跳过门店授权上限。相同权限用既有ScopeGrant合并；不同权限分别保留，测试list:TENANT与update:STORES+SELF/停用角色反例。未注册代码或不支持范围失败关闭；SELF还须具体资源登记行归属。没有管理API、默认未来门店授权或授予权限范围旁路。

Employee.authorization_version是实际持久化字段，初始化0；所有未来角色/权限/关系与门店有效状态变更须同事务更新受影响员工。加载当前值已实现，真实会话重验、撤销、敏感写提交前复核与异步权限交集仍未实现。当前正式Store Provider读取当前租户ACTIVE事实后由既有Guard校验操作范围；404/503语义保持原契约。

## P05-02 当前请求授权（2026-10-08）

STAFF真实会话先验证期限、设备渠道与可信主体，再通过P05-01窄范围函数读取当前租户/员工、角色、每权限范围及有效门店。CurrentPrincipal来自当前查询，TenantContextFilter继续建立AUTHORITY_READ根，业务仍forPermission/Guard/ScopedTransaction/RLS。客户或平台Token不借员工权限。每请求重载策略不依赖授权版本字段独自保证撤销；角色停用、权限/角色关系/门店关系及门店状态变化在提交后的新权威查询中生效，securityVersion/Tenant securityVersion变化拒绝旧会话。

已在执行的请求保留当次授权快照；本轮没有管理写API，未实现未来敏感写提交前重验，不能声称撤回已提交操作。未来修改服务仍须按冻结事务责任递增安全/授权版本并记录撤销意图。真实会话异步快照禁止提交，内部技术任务保持原边界。[P05-02验证](../testing/P05-02-VERIFICATION.md)覆盖正式HTTP/PG/Redis，完整CRUD与审计留待后续。

## P05-03 敏感员工管理策略（2026-10-08）

本人改密/全部退出使用经过验证的STAFF本人能力，不接受目标ID；currentPassword确认当前账号，成功含当前设备全部失效。管理员重置复用既有identity:user:reset-password；管理员撤销声明独立identity:user:revoke-sessions，支持TENANT/STORES，不靠角色名称放行。代码目录声明新权限，既有ADMIN_PERMISSIONS和V1初始化仍为原九项，不在启动时扩大角色授权。

操作权限和目标管理是独立条件：跨租户/不存在/超范围404；STORES须覆盖目标所有门店关联且目标不能无门店，多店任一范围外就拒绝；SELF不能管理另一个员工。目标有效权限及每权限范围不能超出操作者可覆盖的能力，不能把能查看员工等同能接管账号。全部检查在行锁事务重载当前事实后执行，并在提交前重验操作者当前状态、安全/授权版本及权限/门店集合。

system_reserved是真实员工受保护标志，无HTTP修改接口，管理员重置/撤销均拒绝。tenant-admin是已有初始化保留角色的明确代码，任何该角色关联（包括暂时停用角色）使账号受到重置保护，不比较角色中文名称或所谓角色级别。管理员本人不允许走管理重置/撤销，分别走本人改密/全退出。tenant-admin目标的密码重置一律禁止；会话撤销只允许同样持保留角色且具有TENANT撤销权限的操作者。撤销不改密码，最后管理员仍能正确凭据登录，不引入无意义数量门禁。未建设遗失管理员密码的离线恢复/审批流程。

管理员命令必须带目标Employee.version，旧资源版本409；不自动覆盖或重放。重置临时密码后仅开放本人改密/身份/CSRF/退出最小路径，管理权限即使仍在me中也不能用于受限之外路径；任务提交也拒绝。具体接口、输送及客户端状态处理以[IDENTITY](../contracts/IDENTITY.md#p05-03-员工凭据与会话安全接口2026-10-08)为准。

## P05-04 控制面权限（2026-10-08）

identity的PlatformPermissions只声明platform:session:manage、platform:credential:change、platform:redis:operate，分别对应本人会话、本人凭据及P04受限平台Redis。PlatformScopeGuard验证真实PLATFORM和明确代码，不能只isLogin或比较角色名；没有平台角色管理/通配符/租户业务权限。每请求从pet_control.authorization重载有效账号与权限，未登记代码失败关闭。敏感事务行锁后再查当前权限，授权变动不沿用请求旧快照。

平台tenantId=null不是全租户访问；不建立TenantContext，租户持久化和任务入口默认拒绝，STAFF也不能调用平台接口/Redis能力。没有跨租户管理、impersonate/runAsTenant或管理员重置其他平台账号。当前范围与验证见[平台初始化](../development/PLATFORM-BOOTSTRAP.md)、[P05-04](../testing/P05-04-VERIFICATION.md)。

## P05-05 客户授权当前实施（2026-10-08）

CUSTOMER只拥有customer:session:manage本人会话能力，DataScope的principalType=CUSTOMER、principalId=customerId、tenantId来自已验证微信绑定；范围仅SELF，门店上限空，授权版本固定0（本轮没有可变客户角色授权）。不把客户ID映射成employeeId，不授STAFF角色/TENANT/门店管理能力。相同UUID分别属于三域仍不同身份，Header及服务端路由严格隔离；平台Cookie/员工Cookie不回退为客户认证。

登录入口知道有效tenantCode只意味着服务端允许在该租户独立建立微信客户，不能获取员工或其他客户权限；共享AppID同OpenID跨租户独立主体。入口当前显式允许公开注册，没有虚构邀请/成员策略，也没有把tenantCode当强身份认证。后续有邀请要求必须实现明确策略。

客户每请求从正式PG重验客户/租户/绑定状态和安全代际，SELF后续资源仍须登记正确owner+主体域且经ScopedPersistence/RLS；本轮仅身份/会话，没有客户业务资源CRUD。CUSTOMER任务入口提交前403拒绝，不生成可排队证明。完整员工/角色/权限管理API仍未实现，阶段归属及未完成项逐项见[P05总验收](../testing/P05-ACCEPTANCE.md)。
