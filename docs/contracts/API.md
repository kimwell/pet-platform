# API、错误与写入协议

冻结日期：2026-10-07，P01-02。本文是 HTTP分区、响应、错误、追踪、写入和批量行为的权威定义。P03-01 已落地公共响应、异常与 trace；P07-01正式员工读取见[员工契约](EMPLOYEE-MANAGEMENT.md)，原冻结示例只说明协议，不提供生产演示接口。分页和标量分别见 [分页](PAGINATION.md)、[数据类型](DATA-TYPES.md)。

## 分区与接口索引

| 分区 | 身份/用途 | 三端关系与详细接口owner |
| --- | --- | --- |
| /api/platform/** | PLATFORM 控制面 | Web平台入口；[身份](IDENTITY.md)，不自动访问租户业务 |
| /api/admin/** | STAFF租户操作 | 员工Web和小程序复用同权限/范围/DTO；传输载体与设备会话不同 |
| /api/customer/** | CUSTOMER本人能力 | 客户小程序，不复用员工认证 |
| /api/integrations/wechat/** | 微信登录外部适配/回调 | 微信回调验签，无其他域Cookie兜底 |
| /api/integrations/attachments/access/{ticket} | 受限附件票据兑换 | 专用凭据例外，见[附件](ATTACHMENTS.md) |

员工小程序复用 /api/admin，因为身份域、操作权限、数据范围和资源归属完全相同；不得复制一个权限宽松的mobile API。基础接口清单分别在 [身份](IDENTITY.md)、[附件](ATTACHMENTS.md)、[任务](ASYNC-TASKS.md) 定义；不在此复制参数形成竞争事实源。租户/门店/员工/角色 CRUD 在后续实施按 owner schema登记，本轮不伪造完整业务接口。

P07-01正式员工列表/详情读取位于`/api/admin/identity/users`及其ID子资源，独立list/detail权限与多对多范围、字段、错误及排序唯一见[员工管理读取](EMPLOYEE-MANAGEMENT.md)。历史“尚未实现业务接口”为此前阶段边界；本轮没有新增创建/修改/停用或完整CRUD。

## 响应与状态

JSON用UTF-8；成功体必有 success=true、data（可null）、traceId；失败体必有 success=false、error、traceId，失败不同时放data。

```json
{"success":true,"data":null,"traceId":"0123456789abcdef0123456789abcdef"}
```

```json
{"success":false,"error":{"code":"VALIDATION_FAILED","message":"请检查填写内容","fieldErrors":[{"field":"items[0].name","code":"REQUIRED","message":"请输入名称"}]},"traceId":"0123456789abcdef0123456789abcdef"}
```

| HTTP | 场景 | 稳定错误代码/规则 |
| --- | --- | --- |
| 200 | 查询、修改、退出/删除成功 | 无数据data:null；不使用204丢失统一追踪体 |
| 201 | 资源创建 | data含资源ID；可有同源Location |
| 202 | 持久化任务已接收 | data含taskId/status；不是已完成 |
| 400 | JSON/参数语法、未知字段、非法分页/排序、混合认证 | BAD_REQUEST、PAGINATION_INVALID、SORT_INVALID、AUTH_CREDENTIAL_AMBIGUOUS |
| 401 | 未认证/过期/撤销/身份域错误/登录失败 | AUTH_REQUIRED、SESSION_EXPIRED、SESSION_REVOKED、AUTH_DOMAIN_MISMATCH、LOGIN_FAILED |
| 403 | 已认证但无操作权限、CSRF无效 | PERMISSION_DENIED、CSRF_INVALID |
| 404 | 不存在或已具操作权限但目标不可访问 | RESOURCE_NOT_FOUND；同文案“资源不存在或不可访问” |
| 409 | 状态/唯一/版本/重复操作冲突 | BUSINESS_STATE_CONFLICT、DUPLICATE_RESOURCE、VERSION_CONFLICT、SESSION_LIMIT_REACHED、IDEMPOTENCY_CONFLICT |
| 422 | 语法合法但字段/组合约束不满足 | VALIDATION_FAILED、DATE_RANGE_INVALID、AMOUNT_INVALID |
| 429 | 限流 | RATE_LIMITED；Retry-After秒数，不输出内部配额 |
| 500 | 未知内部异常 | INTERNAL_ERROR；“系统暂时无法处理请求，请稍后重试” |
| 503 | 基础依赖不可用/能力关闭 | DEPENDENCY_UNAVAILABLE、CAPABILITY_DISABLED；不伪装401 |

代码采用大写下划线，公共错误注册在shared；模块新增 `<MODULE>_<REASON>` 并登记OpenAPI枚举/HTTP映射，不能把任意异常名当业务代码。字段code为稳定局部代码，例如 REQUIRED/INVALID_FORMAT/OUT_OF_RANGE；中文message可改、客户端分支看code。

fieldErrors为可选非空数组；没有字段错误时省略，禁止null。每元素field/code/message必填。field是请求DTO路径：`name`、`address.city`、`items[0].name`、`storeIds[2]`；零基数组索引、无 `$` 前缀/点数字路径，属性名不包含点/括号。跨字段错误用顶层message，不造虚拟字段。后端拒绝未知输入字段；前端映射不了的路径留在表单总错误，不能静默丢失。

## Trace与秘密

请求/响应Header为 `X-Trace-Id`。入站只允许**单个32位小写十六进制**，否则忽略并服务端生成新值（不拒绝合法业务）；禁止换行、多个值、超长及直接原值日志。响应JSON同traceId，失败也携带；外部代理trace不作授权。敏感响应Cache-Control:no-store。

不返回堆栈、SQL、密码、凭据、物理路径、内部类名；异常日志按trace关联但脱敏，未知错误用统一中文提示。

## PUT、PATCH与并发

PUT 全量替换所有可编辑字段：必填全部存在，可空字段明确null；遗漏422，不改只读ID/tenant/审计字段。PATCH 使用 `application/merge-patch+json`：缺失不变，null仅清空schema标记nullable字段，空字符串不等于null，数组整体替换；禁止JSON Patch操作数组。DTO记录“是否出现”，不能用普通null反序列化混淆缺失/清空。

版本控制资源的PUT/PATCH/DELETE携带当前version（非负十进制字符串）；PATCH也必须version，DELETE用查询version。后端 `@Version` 乐观锁，旧值409 VERSION_CONFLICT；无version422。版本由服务端递增，响应返回新值，冲突响应不泄露范围外最新资源。前端保留草稿并允许显式重新加载/比较，不能自动覆盖重试。

协议例子：`PATCH /api/admin/identity/users/<id>`，body `{"version":"3","displayName":"示例员工","remark":null}`；remark为该接口声明的nullable字段才可清空。这只是未来员工接口形状，不是已实现API。

写入默认不自动重试。确需防重复的接口明确要求 Idempotency-Key（UUID），同身份/租户/操作原子领取并保存请求摘要和结果24小时；同key并发只执行一次、处理中409，同key不同body409，重放需重新授权。客户端按钮防重复不能保证服务端幂等。

## 批量部分失败

模块显式注册 `POST <resource>/batch-actions`（不是任意资源通用接口），最多100个唯一ID，items含id/version，action为白名单。缺整体权限/输入非法整批拒绝；合法批次每项独立事务、每项重新范围/版本检查。外层200只表示批处理协议成功：data含 succeededIds、failures（id/code/message）、requestedCount/succeededCount/failedCount，三计数一致。不回显不存在/不可见项的其他字段，统一RESOURCE_NOT_FOUND；401/依赖中断用整体错误，未执行项标 NOT_EXECUTED。客户端只移除成功项选择，保留失败项，不把外层200显示“全部成功”。

## 协议例外

附件下载/媒体流成功为字节，支持200/206、Range错误416；写出前错误尽可能用标准JSON。微信回调按官方签名/ACK格式，不能套success信封。未来SSE等流必须专门schema，当前不提供。健康检查/受控OpenAPI导出按工具格式例外，不含业务数据或绕过授权。

## P03-01 已实现用法与边界（2026-10-08）

实际类型位于 `com.pet.platform.shared.api` / `shared.exception`。`ApiResponse<T>` 是封闭的成功/失败分支，构造器不对外开放；Controller 显式返回 `ApiResponse.success(dto)` 或 `ApiResponse.success(null)`。创建/任务接口可返回 `ResponseEntity.status(201/202).body(ApiResponse.success(dto))`，状态、Location 等由接口负责。工厂从当前 `TraceContext.currentId()` 取值；无请求上下文时拒绝构造，不能在后台线程自行生成另一个 trace 冒充同一请求。

未注册 `ResponseBodyAdvice`，不会自动包装已经构造的信封、`ResponseEntity`、字节/流、微信 ACK、健康或 OpenAPI 格式。`GlobalExceptionHandler` 继承框架异常基类，覆盖两种 MVC 校验异常并保留其余框架状态/响应头；复制只读头后再增加 JSON 与 `Cache-Control: no-store`。Controller 局部异常处理优先，未来微信回调/工具专用错误应由所属适配器显式实现；本轮未接入微信、Actuator、OpenAPI，未验证其实际调用。文件写出前可用标准 JSON，写出后的异常不能改写已提交响应，文件生命周期/Range 验收仍归后续任务。

`BusinessException(ErrorCode)` 使用注册的中文默认提示；需要明确公开的用例信息可用 `BusinessException(code, publicMessage)`，禁止把底层异常 `getMessage()` 传入。字段错误可用 `BusinessException(code, fieldErrors)`；429 使用 `BusinessException.rateLimited(秒数)`，保证正整数 `Retry-After`。`ResourceNotFoundException` 与 `TenantAccessDeniedException` 均为 404 同文案；`PermissionDeniedException` 为 403；`ConflictException` 只接受已登记 409 代码。上述身份/租户映射不提供认证、授权或隔离实现。

补充框架稳定代码：405 `METHOD_NOT_ALLOWED`（保留 Allow）、406 `NOT_ACCEPTABLE`、413 `PAYLOAD_TOO_LARGE`、415 `UNSUPPORTED_MEDIA_TYPE`。其他未登记框架 4xx 保留原状态并用 `REQUEST_REJECTED`；其他 5xx 保留原状态并用 `INTERNAL_ERROR`，503 使用已冻结代码。不是把所有框架异常变成 500；普通未分类异常（包括未经明确业务分类的 `IllegalArgumentException`）才是 500。容器 `/error` 由 `ProtocolErrorController` 安全兜底，保留合法错误状态，不读取异常原文；直接访问且无错误属性为 404，没有新增业务接口。

请求 JSON 拒绝未知属性、重复属性、尾随第二个 JSON、类型不符与标量隐式转换（例如数字不能转字符串、字符串不能转布尔、浮点不能转整数）。合法 JSON 的 Body Bean Validation / MVC 方法输入约束为 422；JSON 格式/类型、缺参/转换错误为 400。方法返回值约束失败属于服务器错误，为 500。方法验证采用 MVC 内建机制，Controller 不加类级 `@Validated`，避免产生另一种 AOP 校验路径；application 方法校验失败作为内部异常，不自动映射客户端 422。

字段规则沿用上文路径，实际测试覆盖 `name`、`profile.name`、`items[0].name`。同字段不同错误代码保留；相同路径/代码去重；以 field、code、message 字典序确定唯一项和稳定顺序。路径不符合 DTO 规范的校验项只体现在顶层校验失败，不回显动态 map key。对象/跨字段错误使用顶层“请检查填写内容”，不造虚拟字段，没有字段项就省略 fieldErrors。局部代码：NotNull/NotBlank/NotEmpty→REQUIRED；范围/大小/精度→OUT_OF_RANGE；Pattern/Email→INVALID_FORMAT；其他约束→INVALID_VALUE。默认提示固定中文；Body 字段可使用约束声明中的静态中文模板（无插值/换行、长度受限），MVC 方法参数使用固定中文，不读取 rejectedValue 或插值后的输入。

`TraceFilter` 在 REQUEST 与同步 ERROR 分发运行，错误分发复用请求内生成的 ID。响应头、信封、`TraceContext` 和 MDC `traceId` 一致；请求结束恢复此前的 trace 上下文和该 MDC 键，不清空其他组件 MDC。无输入、非法长度/大小写/字符、重复 Header、逗号多值均生成新值；不记录非法输入。异步 Servlet 分发、后台任务/线程池、消息和跨服务传播未实现；不是完整分布式追踪。异常日志仅记录 trace 与异常类型，不打印原始 message/cause/堆栈/请求体。容器或未来第三方组件的日志策略需对应阶段单独验证。

真实处理链与生产包检查见 [P03-01 验证](../testing/P03-01-VERIFICATION.md)。协议夹具只位于测试源码，由测试配置导入；不存在可调用的生产示例接口。PUT/PATCH/version/幂等/批量业务语义仍是冻结设计，未由公共错误类型实现。

P05-05新增微信错误注册：401 WECHAT_CODE_INVALID；503 WECHAT_RESULT_UNCERTAIN/WECHAT_UPSTREAM_ERROR/WECHAT_RESPONSE_INVALID/WECHAT_CONFIGURATION_MISSING，均固定中文安全消息，无原始响应或秘密。实际客户四路径及身份判别唯一见[IDENTITY](IDENTITY.md#p05-05-客户正式接口2026-10-08)，没有改动ID/total字符串或时间/trace/信封协议。
