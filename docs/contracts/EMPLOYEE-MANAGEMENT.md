# 员工管理读取契约

日期：2026-10-09；P07-01。实际公开类型来自生产 Controller / `EmployeeView` 和 [生成契约](../../packages/api-contracts/openapi/backend.openapi.json)，运行证据见 [验证报告](../testing/P07-01-VERIFICATION.md)。本文件拥有本轮员工读取语义；公共信封/错误、标量、分页分别沿用 [API](API.md)、[DATA-TYPES](DATA-TYPES.md)、[PAGINATION](PAGINATION.md)。

## 接口及操作权限

| 方法 / 路径 | 操作权限 | 成功 data |
| --- | --- | --- |
| GET `/api/admin/identity/users` | `identity:user:list`（查看员工） | `PageResponseEmployeeView`：items/page/pageSize/total |
| GET `/api/admin/identity/users/{employeeId}` | `identity:user:detail`（查看员工详情） | `EmployeeView` |

仅 STAFF 正式会话；Web 同源员工 Cookie 或员工小程序 `X-Staff-Token: Bearer <opaque>` 二选一，沿用现有认证/载体冲突及强制改密规则。CUSTOMER/PLATFORM 不得调用，没有平台跨租户入口。错误域Header返回401 AUTH_DOMAIN_MISMATCH；将CUSTOMER opaque值放入STAFF Header在STAFF空间无法解析，返回401 SESSION_EXPIRED；仅携PLATFORM Cookie的STAFF请求仍无身份，返回401 AUTH_REQUIRED，不读取或回退其Cookie。GET 无状态写入、不要求浏览器 CSRF；本人安全接口仍独立，无读取权限不阻断本人改密/退出。

能读取不等于能编辑、重置密码、撤销会话或分配权限。本轮不增加写操作；既有敏感安全接口仍依其独立权限与完整目标策略。列表与详情不互借权限或数据范围。

## 列表请求

全部是可省略的单值 Query 参数。未知参数或重复标量拒绝，空字符串不是省略。详情不接受 Query 参数。没有 tenantId、日期条件、`sort` 别名或未实现筛选。

| 字段 | 默认 / 校验 / 语义 |
| --- | --- |
| keyword | 缺失不筛选；1～100 Unicode 码点，不能全空白，不trim或Unicode归一化；匹配 loginName **或** displayName 的大小写不敏感字面量包含。Java Locale.ROOT 规范化输入，PG lower 比较；`%`、`_`、`\`转义为字面量，使用绑定参数，不拼 SQL。特殊语言大小写由数据库排序规则决定，不承诺全文/拼音匹配。 |
| status | 缺失包含 ACTIVE 与 DISABLED；仅接受实际模型枚举两值，大小写精确；空或未知400。目标停用不隐藏读取记录，操作者停用由认证拒绝。 |
| storeId | 小写标准 UUID v4；Store 事实源检查本租户、ACTIVE、身份有效门店上限及**本操作**范围。不存在/跨租户/停用/未授权均404。仅SELF不能授权门店筛选；TENANT也不能绕身份门店上限。筛选只是与既有安全谓词AND，不能扩大权限。 |
| page | 缺失1，1～2147483647十进制整数；空、重复、负数、小数、越界400 PAGINATION_INVALID。 |
| pageSize | 缺失20，1～100，不截断；错误400 PAGINATION_INVALID。 |
| sortBy | 缺失createdAt；白名单 id/loginName/displayName/status/createdAt/updatedAt，均对应同名受控内部属性。 |
| sortOrder | 缺失desc；仅asc/desc。有sortOrder必须同时有sortBy；重复/未知/空排序400 SORT_INVALID。 |

每种排序同方向补 `id` 唯一稳定条件，主字段已经是id时不重复补；沿用显式 NULLS LAST。当前公开字段都非空，但不会改为数据库默认空值行为。page-1仅内部使用，offset以long计算；超出JPA int offset能力422 RESULT_TOO_LARGE。

空查询/超末页200：items=[]，请求page/pageSize保留，total为实际范围内数量的规范非负十进制**字符串**。前端后续Table任务须按既有安全数值规则适配，不在本轮转number。每次列表items与count位于同一REPEATABLE_READ只读事务快照；不同HTTP翻页间不保证并发插入/删除的快照一致。

## Employee 范围映射

Employee为租户级主体，没有单一storeId。门店关系是 EmployeeStore，多门店不能复制员工行或扩大响应资料。

| 当前读取权限范围 | 可见目标 |
| --- | --- |
| TENANT | tenantId等于当前可信租户的全部员工，包括无门店/多门店/停用目标。未指定storeId不要求目标或操作者具门店关联。 |
| SELF | 同租户且当前主体域STAFF、Employee.id等于当前principalId。不是createdBy；目标无门店仍可读本人。 |
| STORES | 同租户且目标至少一个EmployeeStore关联的storeId位于**当前权限**storeIds；该集合已与操作者当前ACTIVE门店授权上限相交。任一交集允许员工基础读取；多门店目标不会因此暴露其其他门店资料。 |
| STORES为空或操作者无有效门店 | STORES分支无行；无门店目标也不可见。 |
| 同一权限STORES+SELF | 两分支并集，再与tenant AND；无门店操作者仍可从SELF读取本人。 |
| 同一权限含TENANT | 既有ScopeGrant归一化为TENANT；仍只当前租户。 |
| 不同权限 / 多角色 | 同权限多角色范围并集；list:TENANT不会使detail或reset-password扩大。 |

目标状态不改变上述读取范围；有效身份/角色/门店事实每请求重载。停用操作者不能登录/使用旧会话；停用角色或撤销权限的新请求403，撤销门店后的STORES列表缩小或清空。已经开始的读取沿用当次可信授权快照，不能声称撤回已完成/在途请求。

## 响应白名单及展示边界

列表items和详情均为完全相同的 `EmployeeView`，六字段必填、非null：

| 字段 | 类型 / 展示边界 |
| --- | --- |
| id | 小写UUID v4字符串；仅资源引用，不授访问权 |
| loginName | 正式规范化账号字符串 |
| displayName | 正式员工显示名称字符串 |
| status | ACTIVE / DISABLED字面量枚举；中文标签由后续页面映射 |
| createdAt / updatedAt | UTC时间点字符串，固定三位毫秒与Z |

本轮不返回门店摘要/完整门店、角色授权、权限全集、tenantId副本、密码哈希、securityVersion/authorizationVersion、强制改密/系统保留内部值、Token/OpenID/会话/秘密配置或canEdit/canResetPassword。没有字段层关联泄漏，也不能通过详情绕过列表字段策略；详情使用独立权限可能有不同的**行**范围，这是明确授权而非字段旁路。

尚未提供管理资源version或写入能力字段；后续写任务需要重新冻结相应DTO/version协议，不能假设本次读取已给出安全管理能力。当前时间字段不接受日期筛选、不解释经营自然日。

## 错误行为

| HTTP / code | 场景 |
| --- | --- |
| 400 BAD_REQUEST | 非法状态/ID、未知或重复业务筛选、详情Query；公共认证载体格式另沿用既有错误 |
| 400 PAGINATION_INVALID / SORT_INVALID | 原分页/排序协议输入不合法 |
| 401 AUTH_REQUIRED / SESSION_EXPIRED / SESSION_REVOKED / AUTH_DOMAIN_MISMATCH | 无身份、期限/安全代际失效、客户/平台跨域 |
| 403 PERMISSION_DENIED / PASSWORD_CHANGE_REQUIRED | 缺当前读取权限；现有强制改密只允许本人最小安全路径 |
| 404 RESOURCE_NOT_FOUND | 不存在、跨租户或**具有当前操作权限但目标范围外**；相同“资源不存在或不可访问”，不回显目标资料 |
| 422 VALIDATION_FAILED / RESULT_TOO_LARGE | keyword空/全空白/超长；分页offset过深 |
| 503 DEPENDENCY_UNAVAILABLE | DB或正式认证Redis依赖失败；不当作401，不清合法会话 |

所有响应使用既有信封/trace，Cache-Control:no-store；不回显SQL或请求秘密。

## 持久化和数据库边界

`EmployeeDirectory`选择当前操作范围，再用显式TransactionTemplate覆盖完整只读事务，提交前校验完成后关闭scope。`EmployeeQueries`仅identity.infrastructure，两个受控读取实例分别固定list/detail权限。shared不引用identity类型；模块以静态类和关系属性注册 `relatedStoresAndSelf(EmployeeStore, employeeId, STAFF)`。查询始终：tenant AND 当前权限范围 AND 业务筛选。

STORES和storeId过滤使用与根tenant/id关联的EXISTS，没有根join、DISTINCT补救、全量内存过滤或分页后裁剪。列表/count/详情都经同一ResourceAccessPolicy；count只统计员工根行。DTO构造投影只SELECT六个公开列，不加载Employee Entity，不读password_hash；详情1条、列表2条Hibernate查询，指定storeId再加1条事实查询，没有关联N+1。

V1～V4不改写。V5只加默认分页索引与显式权限升级函数/最小授权政策，原Employee与EmployeeStore FORCE RLS仍按事务局部pet.tenant_id限租户。RLS负责租户底线，不计算读取操作权限、SELF/STORES或字段白名单；这些由当前应用策略承担。受限运行登录角色实测，无超级用户作为RLS证据；生产角色部署/生产TLS/代理仍未验证。

## 新详情权限的显式补充

PermissionCatalog只声明新 `identity:user:detail`（TENANT/STORES/SELF），原ADMIN_PERMISSIONS与V1九项初始化清单不变。普通启动、迁移、首次bootstrap不会自动授予新权限。已有和新建的保留管理员有列表权限，使用详情前需显式升级。

在正式V5迁移及生产包检查完成后，使用既有独立PET_BOOTSTRAP数据库配置（不得用runtime或把秘密放命令参数）：

```sh
scripts/backend-identity.sh employee-read-upgrade --tenant-id <目标租户UUID-v4>
```

命令只对指定租户ACTIVE、代码固定tenant-admin的保留角色，将其**已经存在的list范围**按原值补为detail范围；不接受员工ID/角色代码/权限代码/范围参数，不授写权限，不处理普通角色或其他租户，不输出员工资料/秘密。重复调用0变化；缺保留角色固定安全失败退出2。成功新增时同事务递增关联员工authorizationVersion、资源version与updatedAt；新请求从正式权威加载读取新权限。并发命令通过同租户事务advisory lock和唯一约束只新增一次。没有list授权时无补充。

函数SECURITY DEFINER owner沿用独立NOLOGIN `pet_bootstrap_owner`，固定search_path、显式schema和RLS租户条件，PUBLIC/runtime无EXECUTE，runtime无owner成员或SET ROLE。这是受限离线能力，不是任意员工读取或完整角色管理API；普通角色的授权修改留后续任务。

## P07-01 后续页面依赖（历史）

P07-02可复用生成 `paths` 中listEmployees/getEmployee及 `EmployeeView`；URL筛选分页排序使用上述字段和错误，Query key须含P06完整身份范围。两读取权限分开呈现；不能凭角色名称或list权限显示详情。门店事实/选择器尚无本轮新增公开目录API，页面不得凭任意UUID生成扩权筛选。创建/编辑/停用、组织/角色授权、批量/导入导出、客户资料、Web Table均未在本轮实现。

## P07-02 Web 列表接入（2026-10-09）

STAFF页面 `/admin/identity/users` 已接正式listEmployees生成类型和真实列表接口，权限identity:user:list，与导航和直接路由共同判断；六字段中文展示，不放详情链接，不返回或补造角色/门店/管理能力。本人安全入口独立。

本页只提交keyword/status/page/pageSize/sortBy/sortOrder。后端storeId能力仍按上文保持，但本页没有可信公开门店选择事实，URL中的storeId作为本页不支持参数移除并提示；不新增roleId/日期或sort别名。关键词提交空输入省略；非空按后端字面量保持，不trim/改大小写。非法/重复已知URL参数恢复整组默认并提示，未知移除，所有canonical用replace；不将后端400/422本身改成成功。

total协议与生成string完全未改；BigInt无损检查后仅安全值给官方Pagination，超MAX_SAFE保留精确总数和分页限制；页数/int32/offset边界及最多一次末页replace纠正见[列表规范](../conventions/WEB-LIST-PAGES.md)。Query安全范围来自当前正式me逐权限grants及既有身份代际，前端不承担最终授权。详情独立detail权限、完整详情页、返回列表状态、写入/批量/授权修改均尚未实现；当前验证边界见[P07-02](../testing/P07-02-VERIFICATION.md)。

## P07-03 Web 详情接入（2026-10-09）

STAFF页面 `/admin/identity/users/$employeeId` 已接现有正式getEmployee/EmployeeView与独立identity:user:detail，只有detail可合法直达；list不授detail，detail不授list，PLATFORM不借STAFF路由。UUID v4校验失败不请求；后端详情仍不接受Query，页面returnTo仅供路由使用，不发送给接口。六字段及非null/时间/枚举契约、公开接口、行范围和默认角色授权均未改。

列表仅按当前detail权限显示明确入口；列表可见而详情范围外统一“员工不存在或不可访问”。目标独立Query包含完整正式身份/授权/代际，无列表DTO占位；重验清理和错误处理见[详情规范](../conventions/WEB-DETAIL-PAGES.md)。返回仅当前员工列表及原六参数白名单，复用P07-02规范化；没有list权限返回当前身份。详情刷新/原生历史保留已提交URL，不承诺草稿/滚动位置。正式浏览器与未执行门禁见[P07-03验证](../testing/P07-03-VERIFICATION.md)。没有任何写入、角色/门店关系或管理能力字段。

## B01 员工与授权管理（2026-10-09）

以上P07读取及页面历史契约保留。B01在既有六字段读取之外提供单独管理投影，不改变EmployeeView或旧列表/详情范围。公开类型全部从生产Controller生成。

| 方法 / 路径（前缀 /api/admin） | 独立权限 | 范围与行为 |
| --- | --- | --- |
| GET /identity/users/{id}/management | identity:user:detail | 独立详情范围；关联资料要求TENANT或全部目标门店覆盖，SELF仅本人；version、roleIds/storeIds、protectedAccount、passwordChangeRequired，不含凭据 |
| POST /identity/users | identity:user:create | TENANT；loginName规范化并唯一，displayName；人工临时密码经既有PasswordService，ACTIVE/空关联/强制改密 |
| PUT /identity/users/{id} | identity:user:update | 独立目标范围及全部门店覆盖；仅displayName，不改变账号 |
| PUT /identity/users/{id}/status | identity:user:enable 或 disable | ACTIVE/DISABLED；版本防重放，不物理删除；禁止本人及受保护管理员；安全代际失效 |
| PUT /identity/users/{id}/roles | identity:user:roles | 全量角色ID替换（0～100、不重复），同租户ACTIVE非保留角色，逐权限授予上限，撤销旧身份 |
| PUT /identity/users/{id}/stores | identity:user:stores | 全量门店ID替换（0～100、不重复），同租户ACTIVE/本操作范围/身份门店上限，重新检查全部角色权限，撤销旧身份 |
| GET /identity/roles | identity:role:list | TENANT，page/pageSize，字符串total，稳定code/id排序 |
| GET /identity/roles/{id} | identity:role:detail | TENANT，角色摘要/version、逐权限scopeType；不借list权限 |
| POST /identity/roles | identity:role:create | TENANT，编码唯一/创建后固定，名称、ACTIVE/空权限 |
| PUT /identity/roles/{id} | identity:role:update | TENANT，名称/ACTIVE或DISABLED；全体关联员工失效 |
| PUT /identity/roles/{id}/grants | identity:role:grant | TENANT，完整替换permissionCode/scopeType集合；同权限并集、不同权限独立；全体关联员工失效 |
| GET /identity/permissions | identity:role:detail | 正式目录与当前可授予范围，只供表单；不授写入权限 |
| GET /platform/stores/options | platform:store:list | page/pageSize；当前可信租户ACTIVE、本操作范围与身份门店上限交集；仅id/code/name |
| PUT /identity/users/{id}/password、POST /identity/users/{id}/revoke-sessions | 既有reset-password / revoke-sessions | 直接复用P05正式安全服务、操作者当前密码确认/version/频控/强制改密/可补偿撤销，不另建凭据体系 |

普通读取不授写权限，写权限不授读取；Web入口同时要求对应独立读取以取得正式版本/选择事实。管理关联读取比基础详情更严格，多店交集不能暴露其他门店授权ID。无权限403；具有权限但目标或关联跨租户/不存在/范围外统一404；保护账号/授予上限403；version不一致409 VERSION_CONFLICT；字段校验422及准确fieldErrors；未知tenantId/未知Body或Query字段400；依赖503不当成身份401。列表及详情沿用P07原状态返回与列表returnTo。

version为资源非负long十进制字符串，写输入必须提供当前版本；不以updatedAt、安全或授权代际替代。创建账号和名称沿用IdentityNames；不新增部门、岗位、联系方式或Organization持久化模型。角色保留tenant-admin不可创建/修改/停用/授予；持有该角色（含停用角色）或systemReserved账号不允许授权和状态变更，防止失去最后管理路径。本人授权修改拒绝；本人资料允许独立SELF更新。

授予上限按每个permissionCode检查：操作者TENANT可授该权限支持的范围；STORES只能授STORES且覆盖被授权员工全部门店；操作者SELF不授另一主体SELF；缺某权限不能借其他管理/列表权限。角色修改还检查所有关联员工的全部既有角色与门店，停用角色不能藏高权限后再启用。无关联角色的STORES授权未来分配时仍检查员工实际门店。关联撤销也要求完整管理目标策略，不能接管操作者无权管理的高权限员工。

V6只追加正式DML和强制RLS的管理事件，不改V1～V5，不在迁移或普通启动扩大角色权限。唯一显式补充为 `scripts/backend-identity.sh identity-management-upgrade --tenant-id <UUID-v4>`，使用既有独立PET_BOOTSTRAP环境；将保留管理员既有list/update/disable/role-list/role-update/reset范围对应补充到detail/roles/stores/enable/role-detail/create/grant/revoke-sessions，角色范围只TENANT、revoke只TENANT/STORES。同事务authorizationVersion递增，重跑0变化，runtime/PUBLIC不能EXECUTE。不接受任意角色/权限/范围参数。

所有管理写事务先锁租户、使用同租户advisory lock及UUID顺序员工锁，锁内重验正式会话/安全和授权版本；目标检查使用原受控JPA投影，固定登记SQL写关联，所有SQL含可信tenant条件并受RLS/复合FK。为避免与P05固定员工锁顺序形成死锁，管理写串行锁定该租户员工行；这是管理模块的保守并发策略，未来容量优化须保留原安全测试。资料/角色/关联、版本、撤销意图和管理事件全部原子提交；任何失败完整回滚。授权/状态变更同时递增authorizationVersion与既有securityVersion，成功后P05清理旧会话；Redis清理失败返回sessionCleanupComplete=false，数据库逻辑失效仍成立。

Web使用官方Ant Design Form/Table/Modal/Select及现有fetch/Query/Router。密码只存在短期表单与请求中，关闭/提交完成即清理，无Token/localStorage持久化。身份/权限代际变化通过原SessionRuntime取消请求、清缓存、重新挂载清草稿。防重复提交使用同步提交锁与pending禁用；不自动重试写入。409或网络/503结果不确定时锁定提交，显式重新读取版本、核对状态后才允许再次保存；保留非敏感草稿。创建结果不确定须回列表核对唯一账号/编码。角色和员工成功后失效相关身份范围内Query。门店及角色选择从正式分页接口读取全部页面，不以任意UUID造选项。
