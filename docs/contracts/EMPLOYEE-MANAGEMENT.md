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
