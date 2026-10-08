# 权限与授权执行

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
