# P03-01 统一响应、异常、字段错误与分页协议验证

日期：2026-10-08（Asia/Shanghai）。**P03-01 COMPLETE；G01～G15全部PASS；P03整体 IN_PROGRESS。** 本报告是本次公共协议实现和限定运行证据，不是数据库、认证、租户、三端业务或生产验收。

## 1. 前置事实与授权

本任务按用户明确P03-01授权直接在 `/Users/kimwell/work/pet-platform` 执行；该授权替代AGENTS中的P02-01旧阶段限制，技术/目录/变更规则仍适用。未建worktree/子项目、未移动ui、未使用其他治理框架、未提交/推送/发布/部署、未自动执行下一任务。

修改前已读取 AGENTS、README、TECHNICAL-BASELINE、MODULE-BOUNDARIES、CORE-CONTRACTS、API、PAGINATION、DATA-TYPES、BACKEND、VERSION-MATRIX、ROADMAP、DECISION-LOG、P02-02-VERIFICATION、ACCEPTANCE-MATRIX。P02最新第13节实际结论为 P02-02/P02 COMPLETE；微信构建/模拟器通过，基础真机只由用户反馈支持，其他边界保持历史原样，本轮未重跑或改动微信。Git main尚无提交，所有既有成果原本未跟踪；因此使用修改前SHA清单核对既有文件，不能用Git差异假装所有未跟踪文件都属本轮。

原后端只有 Application、EnvironmentSettings、环境配置、ApplicationTest；无响应、异常、trace、分页或业务Controller。新依赖仅有实际DTO/MVC用途的validation starter，由冻结Boot BOM管理，未覆盖版本或新增数据库启动依赖。实际解析、工具链与唯一版本事实见 [矩阵](../development/VERSION-MATRIX.md)、[依赖树](evidence/P03-01/backend-dependency-tree.txt)、[工具执行](evidence/P03-01/toolchain.json)。[Spring MVC验证](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html)、[框架错误响应](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)、[Boot JSON](https://docs.spring.io/spring-boot/4.0/reference/features/json.html) 为官方依据；实际冻结版本行为以本地编译/测试证据为准，未升级框架。

## 2. 实现与文件

生产基础包固定 `com.pet.platform`，一个Maven Module、Wrapper和启动类不变。当前源码具有实际协议行为，不含行业业务或伪适配。

| 新增/修改 | 具体文件与作用 |
| --- | --- |
| shared.api响应 | [ApiResponse](../../apps/backend/src/main/java/com/pet/platform/shared/api/ApiResponse.java)、[ApiError](../../apps/backend/src/main/java/com/pet/platform/shared/api/ApiError.java)、[FieldErrorDetail](../../apps/backend/src/main/java/com/pet/platform/shared/api/FieldErrorDetail.java)、[ErrorCode](../../apps/backend/src/main/java/com/pet/platform/shared/api/ErrorCode.java)：封闭成功/失败分支、字段错误与稳定映射 |
| shared.api分页 | [PageQuery](../../apps/backend/src/main/java/com/pet/platform/shared/api/PageQuery.java)、[PageResponse](../../apps/backend/src/main/java/com/pet/platform/shared/api/PageResponse.java)、[SortRule](../../apps/backend/src/main/java/com/pet/platform/shared/api/SortRule.java)、[SortWhitelist](../../apps/backend/src/main/java/com/pet/platform/shared/api/SortWhitelist.java)：原始标量、边界、保真total、受控稳定排序 |
| shared.exception | [BusinessException](../../apps/backend/src/main/java/com/pet/platform/shared/exception/BusinessException.java)、[ResourceNotFoundException](../../apps/backend/src/main/java/com/pet/platform/shared/exception/ResourceNotFoundException.java)、[PermissionDeniedException](../../apps/backend/src/main/java/com/pet/platform/shared/exception/PermissionDeniedException.java)、[TenantAccessDeniedException](../../apps/backend/src/main/java/com/pet/platform/shared/exception/TenantAccessDeniedException.java)、[ConflictException](../../apps/backend/src/main/java/com/pet/platform/shared/exception/ConflictException.java)、[GlobalExceptionHandler](../../apps/backend/src/main/java/com/pet/platform/shared/exception/GlobalExceptionHandler.java)、[ValidationErrors](../../apps/backend/src/main/java/com/pet/platform/shared/exception/ValidationErrors.java) |
| trace/框架兜底 | [TraceContext](../../apps/backend/src/main/java/com/pet/platform/shared/observability/TraceContext.java)、[TraceFilter](../../apps/backend/src/main/java/com/pet/platform/shared/observability/TraceFilter.java)、[ApiConfiguration](../../apps/backend/src/main/java/com/pet/platform/shared/config/ApiConfiguration.java)、[ProtocolErrorController](../../apps/backend/src/main/java/com/pet/platform/shared/api/ProtocolErrorController.java)：同步REQUEST/ERROR、严格JSON、框架/error安全JSON |
| 构建 | 修改 [pom.xml](../../apps/backend/pom.xml)，新增BOM管理validation starter，无手工版本覆盖 |
| 测试 | 新增 [ApiProtocolTest](../../apps/backend/src/test/java/com/pet/platform/protocol/ApiProtocolTest.java)、[ApiProtocolIT](../../apps/backend/src/test/java/com/pet/platform/protocol/ApiProtocolIT.java)、[ProtocolFixtures](../../apps/backend/src/test/java/com/pet/platform/protocol/ProtocolFixtures.java)；修改 [ApplicationTest](../../apps/backend/src/test/java/com/pet/platform/ApplicationTest.java)，核对未导入测试配置时协议夹具不存在 |
| 文档 | 本报告；API、PAGINATION、BACKEND、VERSION-MATRIX、ROADMAP、DECISION-LOG、ACCEPTANCE-MATRIX、PROJECT-STRUCTURE、README，均只更新当前实现/状态和相应事实；P01/P02历史报告保留 |
| 证据 | [P03-01目录](evidence/P03-01/) 内的命令记录、日志、依赖树、JUnit报告、SHA/范围审计与启动记录；完整变更清单见 [最终审计](evidence/P03-01/final-audit.json) |

没有全局ResponseBodyAdvice。Controller显式 `ApiResponse.success(dto/null)`，创建/任务可包在原生ResponseEntity中。禁止业务Controller返回Entity/Spring Data Page/异常对象。ProtocolErrorController只接管框架/error，不是生产演示或业务端点；`/__protocol/**` 仅在测试源码，由测试配置导入，不进入生产JAR。

## 3. 响应与HTTP结果

以下是协议示例，固定trace只用于说明；可复用类型已实现，示例不是生产业务接口。

```json
{"success":true,"data":null,"traceId":"0123456789abcdef0123456789abcdef"}
```

```json
{"success":false,"error":{"code":"VALIDATION_FAILED","message":"请检查填写内容","fieldErrors":[{"field":"items[0].name","code":"REQUIRED","message":"请输入名称"}]},"traceId":"0123456789abcdef0123456789abcdef"}
```

成功必有data，即使null；无error。失败必有error，无伪data；无字段错误时省略fieldErrors，不返回null/空数组。构造器不允许矛盾分支。字段路径、局部代码与公开提示详见 [API实际用法](../contracts/API.md#p03-01-已实现用法与边界2026-10-08)。同字段不同代码保留；同路径/代码去重；按field/code/message字典序稳定选择并排列。对象/跨字段约束只提供顶层校验提示，不造虚拟字段。模板只取安全静态中文，禁止插值后的输入；不含rejectedValue。

| HTTP | 已实现代码/行为 |
| --- | --- |
| 200/201/202 | 显式成功工厂，状态由Controller/ResponseEntity负责；200对象/null、201与Location已运行；没有实际任务接收功能 |
| 400 | BAD_REQUEST：JSON语法/字段类型、未知/重复属性、尾随JSON、缺参/转换；PAGINATION_INVALID、SORT_INVALID、AUTH_CREDENTIAL_AMBIGUOUS显式映射 |
| 401 | AUTH_REQUIRED、SESSION_EXPIRED、SESSION_REVOKED、AUTH_DOMAIN_MISMATCH、LOGIN_FAILED；仅协议映射，不是认证实现 |
| 403 | PERMISSION_DENIED、CSRF_INVALID；仅协议映射 |
| 404 | RESOURCE_NOT_FOUND；租户目标不可访问与不存在同中文提示“资源不存在或不可访问” |
| 405/406/413/415 | METHOD_NOT_ALLOWED/NOT_ACCEPTABLE/PAYLOAD_TOO_LARGE/UNSUPPORTED_MEDIA_TYPE；保留框架状态与Allow等头；405/406/415实际框架链通过，413本轮只验证显式注册映射，未执行真实上传超限 |
| 409 | BUSINESS_STATE_CONFLICT、DUPLICATE_RESOURCE、VERSION_CONFLICT、SESSION_LIMIT_REACHED、IDEMPOTENCY_CONFLICT |
| 422 | VALIDATION_FAILED（Body/嵌套/方法输入）、DATE_RANGE_INVALID、AMOUNT_INVALID、RESULT_TOO_LARGE；JSON格式/type错误不混用422 |
| 429 | RATE_LIMITED；正整数Retry-After，测试值12秒；没有实际限流器 |
| 500 | 未分类异常/IllegalArgumentException/方法返回值约束失败：INTERNAL_ERROR，固定“系统暂时无法处理请求，请稍后重试” |
| 503 | DEPENDENCY_UNAVAILABLE、CAPABILITY_DISABLED；仅映射，没有外部依赖故障集成 |
| 其他框架状态 | 保留原状态（实测410）；未登记4xx为REQUEST_REJECTED，其他5xx为安全INTERNAL_ERROR，不使用原reason/message |

异常响应统一JSON/no-store，复制只读框架头，不损坏Allow。未知异常日志只写trace/异常类型，不输出message/cause/堆栈/请求值；最终运行日志未包含技术敏感标记或SQL。协议测试对响应断言无原输入、SQL、内部类名、堆栈、rejectedValue。框架/error在容器sendError后保留状态和原trace；直接访问无错误属性则404。

## 4. trace、分页、排序与例外

`X-Trace-Id`只接受单个32位小写十六进制。缺失、大小写/长度/字符非法、换行、多Header或逗号多值均重新生成，不拒绝业务，也不记录原值。响应头/体、TraceContext、MDC一致。finally恢复此前trace和本次MDC键，其他组件MDC保留；无旧值则移除。覆盖同步REQUEST与同步ERROR（含嵌套错误）分发。异步Servlet/线程池/任务/消息/跨服务传播未实现，不声称完整观测或分布式追踪。

page缺失1、pageSize缺失20；page范围1～2147483647，pageSize范围1～100。只接受十进制数字，可有前导零，响应规范化；空串/空白/正负号/小数/指数/重复/超界均400，无静默裁切。long安全offset最大214748364600，不做int乘法。数据库若无法承载深分页，未来适配须422 RESULT_TOO_LARGE，不由当前参数合法推定查询可执行。

排序严格独立`sortBy`/`sortOrder`，公开→内部固定白名单；默认单列主排序加唯一字段。典型默认createdAt DESC,id DESC；name asc→displayName ASC,id ASC；只传name使用默认方向；直接选id不重复补id。删除两个排序参数恢复默认。仅sortOrder、空白/空值、大写方向、未知/内部属性、实体路径、SQL/函数、分隔符、多字段、重复标量均400 SORT_INVALID。唯一补充与主方向相同，nullable字段未来适配必须NULLS LAST。

PageResponse只有items/page/pageSize/total；items不可null，空[]；total规范非负字符串，long工厂最大值保真，不按items.size造计数。空技术页保留请求最大页码；不代表真实数据库超末页/count已验收。无Spring Data、PageableFactory、JPA/数据库配置/Repository或查询。本轮没有占位持久化适配。

文件成功为原始bytes，Content-Type/Disposition和trace头保留，MockMvc与真实HTTP通过。未自动包装工具格式/微信ACK/流；本轮未安装这些适配器，其实际链未验证。已提交响应、流/Range、附件生命周期归后续任务，本轮不会尝试重写已经写出的字节。

## 5. 实际命令、退出码与证据

所有命令由本轮 [run-check.py](evidence/P03-01/run-check.py) 保存独立cwd/argv/真实退出码/开始结束时间/工具目录/日志摘要。Java使用已核对的冻结Temurin任务目录，未改变全局配置；临时目录若以后被清理，需按矩阵准备相同分发，可通过P03_JAVA_HOME指定另一个已核对的工具位置。

| cwd | 命令/检查 | 实际退出码与结果 | 证据 |
| --- | --- | --- | --- |
| apps/backend | ./mvnw dependency:tree -DoutputFile=../../docs/testing/evidence/P03-01/backend-dependency-tree.txt | 0，RESOLVED；BOM中的JSON/校验实际解析 | [记录](evidence/P03-01/dependencies.json) |
| apps/backend | ./mvnw -DskipTests compile | 0，首次生产源码编译COMPILED | [记录](evidence/P03-01/compile-first.json) |
| apps/backend | ./mvnw clean verify 首次 | 1，真实FAIL：测试中文正则转义错误，已修复 | [原始记录](evidence/P03-01/verify-first.json) |
| apps/backend | ./mvnw clean verify 第二次 | 1，真实FAIL：测试配置中夹具重复注册，已改为仅导入测试Controller | [原始记录](evidence/P03-01/verify-second.json) |
| apps/backend | ./mvnw clean verify 第三次 | 1，真实FAIL：框架只读HttpHeaders被修改导致校验处理回退；数字→字符串仍被接受 | [原始记录](evidence/P03-01/verify-third.json)、[日志](evidence/P03-01/verify-third.log) |
| apps/backend | ./mvnw clean verify 第四次 | 1，真实FAIL：Jackson3枚举特性归EnumFeature，非旧DeserializationFeature；按本地API修复 | [原始记录](evidence/P03-01/verify-fourth.json) |
| apps/backend | ./mvnw clean verify 第五次 | 0，PASS；修复后的协议/真实HTTP链通过 | [记录](evidence/P03-01/verify-fifth.json) |
| apps/backend | ./mvnw clean verify 最终 | 0，PASS；32单元/MVC+2随机端口IT，0失败/错误/跳过 | [最终记录](evidence/P03-01/verify-final.json)、[完整日志](evidence/P03-01/verify-final.log)、[测试汇总](evidence/P03-01/test-results.json) |
| apps/backend | ./mvnw --version | 0，冻结工具实际可用，RESOLVED | [记录](evidence/P03-01/toolchain.json) |
| 根目录 | python3 docs/testing/evidence/P03-01/runtime-audit.py | 0，产物检查与local真实启动/HTTP/停止通过 | [记录](evidence/P03-01/runtime-audit.json)、[产物清单/SHA](evidence/P03-01/artifact-audit.json)、[真实HTTP](evidence/P03-01/local-runtime.json)、[启动日志](evidence/P03-01/local-startup.log) |
| 根目录 | python3 docs/testing/evidence/P03-01/final-audit.py 首次 | 1，脚本先检查自己尚未写出的结果链接；已调整为结果写出后核对，非协议运行失败 | [原始记录](evidence/P03-01/audit-first.json)、[日志](evidence/P03-01/audit-first.log) |
| 根目录 | python3 docs/testing/evidence/P03-01/final-audit.py 最终 | 0，文档链接/范围/保全/产物与日志一致性PASS；不替代运行 | [最终审计](evidence/P03-01/final-audit.json)、[命令](evidence/P03-01/audit-final.json) |

修复事实依据：框架异常头先复制再写；Jackson文本类型明确禁止Integer/Float/Boolean转换；枚举按当前API启用EnumFeature。未更换体系、降版本、放宽契约或跳过失败。失败日志原样保留。

JUnit逐项原报告见 [MVC](evidence/P03-01/TEST-com.pet.platform.protocol.ApiProtocolTest.xml)、[HTTP IT](evidence/P03-01/TEST-com.pet.platform.protocol.ApiProtocolIT.xml)、[工程回归](evidence/P03-01/TEST-com.pet.platform.ApplicationTest.xml)。MockMvc使用真实Boot WebApplicationContext、实际JSON/校验/Advice与Filter，未mock服务或异常处理；IT使用真实Tomcat随机端口与JDK HttpClient，包含sendError→ERROR再分发。数值、名称和敏感标记均为技术夹具，不是业务数据/凭据。

生产JAR实查24个项目class，无测试Controller/`__protocol`常量/测试配置，无新增数据库、认证、缓存或消息依赖。local产物（随机回环端口）真实启动，`/`、`/__protocol/success`和无错误属性`/error`均404标准JSON，后者即使Accept:text/html也不会暴露框架HTML。正常SIGTERM后进程退出143、观察到优雅关闭，端口关闭；记录真实退出码，不将信号终止写成exit0。未访问或操作既有infra服务。

## 6. G01～G15门禁

| 门禁 | 结果/等级 | 依据 |
| --- | --- | --- |
| G01 冻结前置事实 | PASS / DOCUMENTED | 第1节读取与P02最新结论，修改前 [SHA](evidence/P03-01/baseline-files.json)；实现遵守权威API/分页/类型 |
| G02 响应结构 | PASS / COMPILED / RUNTIME_VERIFIED | exact信封键、对象/null、失败无data、fieldErrors可选；explicitSuccessNullAndResponseEntityKeepExactShape |
| G03 HTTP映射 | PASS / RUNTIME_VERIFIED（协议） | 所有注册代码循环实测；业务专用类、429头、普通IllegalArgumentException/框架410；不是认证或限流实现 |
| G04 嵌套数组字段 | PASS / RUNTIME_VERIFIED | name/profile.name/items[0].name、多代码、稳定顺序、Body与MVC方法嵌套；对象级错误无虚拟字段 |
| G05 中文安全 | PASS / RUNTIME_VERIFIED | JSON语法/type/未知字段与未知异常均中文，响应无内部/SQL/敏感原值，最终异常日志脱敏；非第三方全量日志保证 |
| G06 trace一致 | PASS / RUNTIME_VERIFIED | header/body/TraceContext/MDC；MockMvc与真实HTTP、404/500/sendError再分发 |
| G07 入站限制与清理 | PASS / RUNTIME_VERIFIED | 缺失/非法/换行/重复/合并多值生成；finally移除或恢复owned键，保留其他MDC；异步不在范围 |
| G08 分页边界 | PASS / RUNTIME_VERIFIED（参数/响应） | 默认/最大/非法/空/重复/超限、long offset、空页与total大整数；无DB验收 |
| G09 白名单稳定排序 | PASS / RUNTIME_VERIFIED（规则） | 默认/缺方向/合法映射/同向唯一补充不重复；未知路径/函数/分隔符/多值拒绝；NULLS LAST待PG适配 |
| G10 文件例外 | PASS / RUNTIME_VERIFIED | MockMvc与真HTTP原bytes/头，不重复包装；完整附件与流/Range未执行 |
| G11 关键HTTP | PASS / RUNTIME_VERIFIED | 400/422/404/405+Allow/406/415/500，以及随机端口ERROR；无生产测试端点 |
| G12 clean verify | PASS / COMPILED / RUNTIME_VERIFIED | 最终exit0，34 tests、0失败/错误/跳过；失败历史保留 |
| G13 测试隔离 | PASS / COMPILED / RUNTIME_VERIFIED | 测试配置导入；ApplicationTest未导入时404，生产JAR无夹具，local该测试URL404 |
| G14 范围保全 | PASS / DOCUMENTED / RESOLVED / RUNTIME_VERIFIED（local启动） | 单Module/固定包、无业务/DB/身份实现；产物依赖与SHA保全；前端/小程序/ui/历史未改 |
| G15 文档一致 | PASS / DOCUMENTED | 使用/例外/版本/状态/下一边界与实际源码测试一致，链接与保全审计exit0 |

DOCUMENTED为规则/使用说明，RESOLVED为实际解析，COMPILED为生产/测试编译与打包；本报告RUNTIME_VERIFIED只指表内明确的本机DTO/MVC/HTTP/local场景，等级不互相替代。范围保全和文档审计不是业务运行证明。

## 7. 未执行范围、问题与下一任务

| 项目 | 状态与原因/owner |
| --- | --- |
| PostgreSQL/JPA/Flyway/真实count、NULLS LAST、超末页/并发插删、深分页适配 | NOT_EXECUTED / NOT_VERIFIED，本轮授权明确排除，P03-02实现 |
| 身份/租户/权限/CSRF/Redis/真实限流/异步传播 | NOT_EXECUTED / NOT_VERIFIED，仅错误代码和同步trace协议，P04/P05/P10 |
| 完整UUID/时间/金额/null/PATCH/version序列化、实际OpenAPI与三端类型/CI生成、结构防绕过 | NOT_EXECUTED / NOT_VERIFIED，P03剩余任务；本轮仅严格基础JSON及total |
| 前端/微信/完整真机/生产联调、真实文件/Range/微信回调/Actuator/OpenAPI工具接口 | NOT_EXECUTED / NOT_VERIFIED，源码未动或能力未接入，不能以文件bytes夹具扩大结论 |
| 远程CI、Windows/Linux、部署/发布 | NOT_EXECUTED / NOT_VERIFIED，没有本轮授权与相应环境；不以macOS本机结果替代 |

当前无未解决的P03-01完成阻塞。真实观察保留：Mockito显式agent造成JVM类共享提示；故意405请求出现框架PageNotFound warning；故意内部错误产生脱敏ERROR日志。这些是技术反例/运行提示，不冒充“日志全无告警”。框架只读头与Jackson转换/枚举API问题已实际修复并回归，原失败未删除。

**下一合法任务：P03-02 PostgreSQL/JPA/Flyway 持久化基础（NOT_STARTED）。** 与 [路线图](../development/ROADMAP.md) 原P03迁移/事务目标一致，应同时落地受控分页适配并使用真实PostgreSQL验证；本轮只报告。P03仍IN_PROGRESS，须完成持久化、完整标量、OpenAPI/生成与结构验收后才可整体COMPLETE。
