# P05-03 员工凭据变更、全会话撤销与敏感操作安全闭环

日期：2026-10-08。项目根目录 `/Users/kimwell/work/pet-platform`；后端 `com.pet.platform`。用户明确授权本任务，技术版本/根目录保全规则继续适用，不使用 Product Delivery OS，不自动下一任务，不提交、推送或部署。

**P05-03 COMPLETE；G01～G15 PASS。P05整体仍 IN_PROGRESS。** 完整后端328项0失败/错误/跳过；291项原用例保留，一项异步限制按本轮重验方案替换，新增36项。历史失败及未执行边界保留。

## 1. 实际前置与实现依据

已读取 AGENTS、README、AUTHENTICATION/AUTHORIZATION/MULTI-TENANCY/CONFIGURATION、IDENTITY/API/OPENAPI-GENERATION、PERSISTENCE/REDIS/ASYNC-EXECUTION、ROADMAP/DECISION-LOG/VERSION-MATRIX、P05-01/02验证及ACCEPTANCE-MATRIX，核对实际源码和正式V1。P05-02确实实现五条STAFF路径、Sa-Token 1.46.0真实Redis会话、服务端PBKDF2、独立Cookie/CSRF和小程序Token、登录频控、每请求状态/版本/授权重载；其真实身份异步采用禁止提交方案，未实现凭据写和全会话撤销，本轮据此扩展，没有推断前置已含这些能力。

复用PasswordService、AuthenticationJdbc/StaffAuthentication、StaffHttpAuthentication、SaStaffSessions/AuthenticationRedis、TenantExecutionScope/TaskExecutor。员工已有security_version、authorization_version及资源version；租户已有security_version。不建第二套凭据版本/撤销计数：security_version明确为**凭据与会话共同失效代际**，密码变化和退出全部均递增，事件类型区分原因；authorization_version继续用于授权事实，资源version用于管理员请求竞争检查。正式audit模块/表尚未建立，P08仍NOT_STARTED。

依赖、POM、根锁及VERSION-MATRIX未变。冻结工具输出见[java](evidence/P05-03/java-version.log)、[node](evidence/P05-03/node-version.log)、[pnpm](evidence/P05-03/pnpm-version.log)。Sa-Token实际源码的全部登录终端kickout/设备logout语义见[来源与摘要](evidence/P05-03/sa-cleanup-source.json)。未使用不带代际过滤的loginId全量kickout：延迟清理会误伤新会话；复用最多五个终端索引逐个执行官方logout，补齐缺失Token映射时的孤立设备/活跃记录清理，不扫描Redis键空间。

等级：策略与契约 DOCUMENTED；冻结依赖 RESOLVED；后端和三端类型 COMPILED；真实PG/Redis/HTTP场景按最终测试 RUNTIME_VERIFIED。未执行的生产/设备范围单列，等级不能互相替代。

## 2. 实际接口、权限与客户端行为

四项只接受有效STAFF身份，WEB Cookie和MINIPROGRAM Header二选一，载体/渠道边界保持。所有密码字段仅JSON Body、writeOnly，安全toString；路径/Query密码参数拒绝。Web写请求必须绑定当前会话的X-CSRF-Token及同源Origin或Referer，小程序按真实Token渠道认证，不使用浏览器CSRF。目标/角色/租户等未知Body字段400，不绑定Entity。

| 方法与路径 | 操作权限及目标 | 请求Body | 成功与重复语义 |
| --- | --- | --- | --- |
| PUT `/api/admin/auth/password` | 本人，从可信身份取employeeId | currentPassword、newPassword | 200 data=null，全部设备含当前失效；旧会话重复401，新密码重新登录 |
| POST `/api/admin/auth/logout-all` | 本人，从可信身份取employeeId | currentPassword | 200 data=null，密码不变，全部设备含当前失效；旧会话重复401 |
| PUT `/api/admin/identity/users/{employeeId}/password` | `identity:user:reset-password`及独立目标管理策略 | version（十进制string）、currentPassword（操作者）、newPassword（目标临时密码） | 200，目标全失效/必须改密，操作者保留；旧version重放409 |
| POST `/api/admin/identity/users/{employeeId}/revoke-sessions` | **独立** `identity:user:revoke-sessions`及独立目标管理策略 | version、currentPassword（操作者） | 200，目标全失效，密码/强制改密状态不变；旧version重放409 |

新撤销代码仅在PermissionCatalog.DECLARED中声明TENANT/STORES，没有加入ADMIN_PERMISSIONS、V1/V2授权数据或启动补授，原初始化九权限不变。具有管理员角色不自动具有此权限。version使用已有Employee.version，不是安全代际；合法资源版本由初始化/管理资料提供，完整员工读取/管理API待P07，不在本轮顺手实现。

目标必须同租户；TENANT或目标全部门店归属被STORES覆盖，空归属/仅部分可见不授管理权，SELF不管理另一个员工。另要求目标全部当前有效权限和每权限范围被操作者覆盖；操作者SELF不用于覆盖其他主体。不存在/跨租户/范围外404，不读取对方资料；无操作权限/保护规则403。

管理员禁止重置本人、system_reserved账号、持有保留tenant-admin角色的账号（角色停用也不绕过）；本人必须走旧密码改密。撤销system_reserved也拒绝；撤销其他tenant-admin要求操作者同为tenant-admin并有TENANT范围撤销权限。不比较角色名称高低，不新建最后管理员数量限制；撤销不改密码，被撤销管理员仍可重新登录，不丢恢复能力。员工停用仅通过原权威状态验证，未新增停用API。

管理员显式提供符合原PasswordService规则的临时密码，响应/查询均不回显，无固定默认密码；交付责任为既有安全人工渠道，不使用URL/日志/普通查询或自接短信/邮件。登录后CurrentIdentity.passwordChangeRequired=true，只允许me、csrf、本人改密、logout、logout-all；即使原权限很高也不能管理或执行真实身份任务。本人使用临时密码确认并改为不同新密码后清标记、全会话再次失效，重新登录才可正常使用。

本人改密原规则：12～128 Unicode码点及最多256 UTF-16单位，不trim、不改大小写、不静默截断；拒绝与当前密码相同，沿用PBKDF2 600000及随机盐。当前密码验证同时完成本次重新确认，不发行二次证明。

全部响应no-store/no-cache；密码/哈希/Token不回显。成功响应 `X-Session-Cleanup: COMPLETE|PENDING`，均表示数据库变更已提交；只有COMPLETE表示物理清理也已完成。本人成功Web按原属性清sid/pre Cookie，设备CSRF随会话销毁；小程序须立即清STAFF Token/会话状态并重新登录。管理员成功保留操作者会话；目标客户端下次401清自己的槽位。PENDING不得误认为操作失败而自动重复变更。503/连接中断可能无法确认提交，按契约重新登录/核对版本，禁止盲重试；无新增幂等键服务。

## 3. 权威代际、原子事务与失败一致性

追加正式[V2](../../apps/backend/src/main/resources/db/migration/V2__staff_credential_security.sql)，V1字节/checksum保持。新增password_change_required/system_reserved，两张必要表identity_security_event和identity_session_cleanup；FORCE RLS、同租户约束，runtime仅必要列写和事件INSERT，不能直接SELECT密码、修改租户/保留标记、查询/删除事件。窄范围凭据锁函数只用可信事务GUC；函数owner不能登录/旁路，search_path固定，PUBLIC不可执行。StaffSecurityJdbc唯一新增固定SQL登记，没有泛化原生SQL权限。

安全用例开独立PG事务，在同一连接设置局部tenant GUC，锁租户及按UUID顺序锁操作者/目标；当前状态、真实设备、当前授权/目标策略、原密码及资源version均检查。密码/强制改密、security_version+1、资源version+1、SUCCESS事件及清理意图在同事务提交。提交前重载操作者授权/门店/授权版本与安全状态，管理员再校验目标保护/权限/范围。数据库变更或成功记录失败整体回滚，503，不宣称成功；密码和版本不部分变化。

事务返回后读取目标最大未完成cutoff、按STAFF员工索引删除 `session.securityVersion < cutoff` 的WEB/MINIPROGRAM所有设备，再独立事务标完成。整个数据库事务不包含Redis清理。清理失败200/PENDING，旧会话每请求PG重验立即401，即使物理TokenSession残留也无身份。查询故障503，不当作版本相同放行，不回滚到旧密码。

持久化意图用于跨进程失败后最小补偿，避免只有内存重试标识；目标合法me或后续安全操作从可信租户范围触发重试，不向匿名请求开放GUC/处理入口。重复同cutoff清理安全，不删除cutoff及更新代际的新会话；新撤销产生更高cutoff另行处理。完成状态写失败仍PENDING可再做。没有调度器/MQ/Outbox、自动定时清理或通用平台；未再登录的员工可保留PENDING及旧物理记录到自然TTL，不能报告已物理完成。运维只读复核和责任见[本地开发](../development/LOCAL-DEVELOPMENT.md#p05-03-员工安全操作与补偿2026-10-08)。

账号索引仍复用30秒随机owner租约。真实Redis暂停揭示SET NX超时可能在恢复后执行，本轮把owner比较释放放入覆盖获取失败的finally，避免可恢复的悬挂锁且不删他人锁；完全不可达/进程崩溃仍依赖TTL，恢复初期可能429/503，应按Retry-After重试。没有宣称Redis HA/fencing或任意进程暂停下的分布式锁保证。

## 4. 并发线性化与异步边界

| 竞争 | 策略与允许结果 |
| --- | --- |
| 两个旧会话同时本人改密 | 员工行锁串行，先提交者200，后者旧代际401；不得两个旧密码覆盖成功 |
| 管理员重置与本人改密 | 同一员工行锁；重置先完成则本人401，本人先完成则旧资源version重置409 |
| 两个管理员同version重置/撤销 | 仅一个提交，另一个409；失败不改变密码/版本/成功记录 |
| 登录与改密/退出全部 | 签发前后都检查**原始实际校验的**凭据代际，不给旧密码升级到新代际；受控屏障下旧尝试401、新正确登录成功 |
| 已签发会话与随后撤销 | 数据库提交为撤销线性化点，此后新的权威检查拒绝旧代际；早于提交签发也将失效 |
| 提交后的新正确登录与旧清理 | 新会话携带新代际，旧cutoff只删较小值，账号终端锁正常竞争串行；延迟补偿不能全量踢出新登录 |
| 重复本人全部退出 | 当前会话已经失效，重复401；重新登录后再次明确请求属于新操作 |

登录保持P05-02不可变AuthenticationCandidate→StaffIdentity事实，账号索引锁内检查签发前/签发后数据库版本；若后检查失败撤销刚签发设备并返回登录失败。提交可能在最终检查后发生：该会话随后作为旧代际失效，不承诺登录响应到达客户端时仍有效。未来授权管理写事务须遵守员工锁与授权版本更新协议；本轮没有开发关系写CRUD，提交前重读防止已提交授权撤销沿用请求旧事实，不声称能阻止持数据库凭据的任意恶意SQL。

P05-02真实身份任务禁止方案已由真实重验替换。TaskAuthority shared端口、identity私有Claim仅存tenant/employee/非凭据sessionId及捕获代际，无Token/CSRF；只可由当前HTTP可信身份捕获。执行开始前查PG状态/安全代际、实际Redis设备仍在且未过期、当前权限与捕获上限交集、门店上限与当前选择；依赖失败拒绝执行，权限扩大不能扩大原任务。任务与事务/上下文/MDC仍使用原线程生命周期，执行后清理，不从其他线程强清上下文。无HTTP证明的真实来源嵌套任务不入队；纯内部任务原边界保留。

承诺变更提交后的新请求、之后开始授权的排队任务拒绝旧会话。已经执行并完成授权检查的请求/任务不承诺远程强制回滚，不撤销已提交业务结果；本轮无新业务任务API或RabbitMQ。

## 5. 安全操作记录与频控

最小正式记录含id、operation、tenant_id、operator_id、target_employee_id、result、数据库毫秒时间、traceId，仅四种操作及固定结果代码，不含密码/哈希/Token/完整Request。SUCCESS与变更同事务；已经通过敏感频控且进入用例的业务失败在回滚后独立事务记失败，不随业务回滚消失。事件表runtime只追加，不开放审计查询页面。

缺身份/CSRF/输入字段错误及429在进入用例前拒绝，不无界写事件；依赖导致失败记录也无法持久化时仅固定中文警告、操作类型/traceId，不含异常正文/提交值，不伪称已记录成功。成功记录不可写则整项变更回滚。P08负责整合该真实表及保留/归档/查询权限策略，不能丢弃既有记录；本轮无完整audit模块/无限失败消息队列。

沿用认证专属命名空间和原子Lua，新增固定sip/actor/target摘要种类，同一5分钟窗口直接对端IP60、tenant+操作者8、tenant+目标8，四种操作共用，成功也计数不清零；三维一起原子递增，429含Retry-After。密码不进Key；转发Header不能伪造可信来源，现有配置仍forward-headers-strategy=none。基础设施故障503失败关闭，不回退内存或静默放行。

## 6. 实际测试与命令证据

测试使用正式V1/V2、冻结真实PostgreSQL 17.11和Redis 8.2.10容器、受限运行身份、生产PasswordService/Provider/Sa-Token/HTTP。租户和首管理员由真实IdentityBootstrap/BootstrapJdbc初始化；由于本轮无员工CRUD，次要员工通过正式表+正式PasswordService作明确技术初始化，全部会话通过真实登录获得，无Mock身份/页面/内存数据库。不得把这些临时技术账号当生产业务账号。

新增34项StaffCredentialSecurityIT、1项V1→V2升级测试、1项双生产JAR JVM改密测试；旧真实身份异步禁止用例按本轮新行为改为执行前重验成功用例，保留原测试数量并明确该语义替换。其他既有用例逐项回归，正式升级不清库/修复checksum，空库同样运行正式迁移。并发竞争使用CyclicBarrier/CountDownLatch和受控签发关卡；测试故障代理、屏障和提交后Redis暂停只在src/test，生产无故障开关。

最终 `./mvnw clean verify` 退出0：132单元+196集成=328，0失败/错误/跳过；其中34项本轮安全用例、1项正式升级、1项双JVM改密新测试。原292项中291项逐项保留，真实身份异步禁止用例明确改为重验用例，没有未解释缺失。[XML汇总与用例差异](evidence/P05-03/test-summary.json)、[最终XML/观察与JAR子进程日志](evidence/P05-03/verify-final-reports/)、[双JVM结果](evidence/P05-03/verify-final-reports/p05-03-two-instances.txt)保留。每条runner元数据记录工作目录、argv、起止UTC、退出码、日志与摘要，完整清单见[命令索引](evidence/P05-03/COMMAND-INDEX.json)。

| 工作目录 | 实际命令 | 当前退出码与证据 |
| --- | --- | --- |
| apps/backend | `./mvnw clean verify` | 0；328项通过，[最终元数据](evidence/P05-03/verify-second.json)、[日志](evidence/P05-03/verify-second.log)；首轮1保留[verify-first](evidence/P05-03/verify-first.json) |
| 根目录 | `pnpm contracts:generate` | 0（修正schema后）；[元数据](evidence/P05-03/contracts-generate-second.json) |
| 根目录 | `pnpm contracts:check` | 0；[元数据](evidence/P05-03/contracts-check.json) |
| 根目录 | `pnpm contracts:typecheck` | 0；[元数据](evidence/P05-03/contracts-typecheck.json) |
| 根目录 | `pnpm --filter @pet/admin-web typecheck` | 0；[元数据](evidence/P05-03/web-typecheck.json) |
| 根目录 | `pnpm --filter @pet/wechat-miniprogram typecheck` | 0；[元数据](evidence/P05-03/miniprogram-typecheck.json) |
| 根目录 | `pnpm check:repo` | 0；[元数据](evidence/P05-03/repo-check.json) |
| 根目录 | `node docs/testing/evidence/P05-03/contracts-final-snapshot.mjs` | 0；[元数据](evidence/P05-03/contracts-final-snapshot.json)，最终构建产物与五份生成文件一致 |
| 根目录 | `python3 docs/testing/evidence/P05-03/final-audit.py` | 0；[元数据](evidence/P05-03/final-audit-command.json)、[结果](evidence/P05-03/final-audit.json) |
| 根目录 | `docker ps --filter label=org.testcontainers=true --format '{{.ID}} {{.Image}}'` | 0；无运行中测试容器，[元数据](evidence/P05-03/container-cleanup-check.json) |

公开生产OpenAPI九条路径，四项writeOnly输入、version string、CurrentIdentity.passwordChangeRequired boolean、准确null信封及string清理Header；三端纯类型和少量type-only断言，无页面/请求实现。最终构建真实导出已在不clean JAR情况下再次比对五份生成产物，[快照结果](evidence/P05-03/contracts-final-snapshot-result.json)全部字节/SHA一致；完整旧客户端兼容分析不属于本轮生成一致性结论。

| 用户测试要求 | 覆盖与可观察证据 |
| --- | --- |
| 1～5 真实旧密码/错误/规则/相同/新旧登录 | HTTP码、原PasswordService验证DB实际哈希、新旧登录；Unicode/空格/大小写/不截断 |
| 6～7 所有渠道/不影响他人他租户 | 当前及其他WEB/mini后续401，实际TokenSession/终端/活跃键清理，其他员工/租户me仍200 |
| 8～10 操作/租户/门店/敏感账号 | 无操作403、跨租户/未知/范围外404、全部门店和权限覆盖、本人/保留/tenant-admin保护 |
| 11 临时密码强制修改 | flag真实DB和me，授权管理仍403，本人改密200后flag清除、再次登录 |
| 12～13 CSRF/重新确认 | 缺错CSRF/Origin403不变，错误当前密码403+独立失败记录 |
| 14 DB完整回滚 | 测试代理在版本/意图后故障及真实REVOKE事件INSERT，密码/版本/意图/成功事件均回滚 |
| 15～16 Redis失败/新会话保护 | 测试代理确定性失败和提交后真实pause Redis，200/PENDING、旧401、恢复补偿；旧cutoff不删新设备、孤立映射清理 |
| 17～19 并发/登录竞争/撤销 | 行锁三组竞争、签发前真实数据库读取受控屏障、唯一成功/冲突/旧登录失败；双真实生产JAR JVM |
| 20 排队执行重验 | 撤销/改密/当前设备退出/权限删除/权威查询不可用后不运行；扩权不扩大捕获STORES |
| 21 真实无秘密记录 | 正式表固定字段/traceId/结果，事件与DB事务状态及脱敏观察JSON，包与日志特征扫描 |
| 22 并发频控 | 24并发仅8通过，429/Retry-After，不永久锁账号；真实暂停Redis失败关闭 |
| 23 请求/线程清理 | 每项测试结束context空；复用任务线程上下文/MDC清空，原同步/ERROR/GUC连接复用回归 |

## 7. G01～G15

| 门禁 | 当前结果 | 证据范围 |
| --- | --- | --- |
| G01 P05-02能力及契约 | PASS | 五接口/实际版本/异步禁止历史均确认，不推断 |
| G02 本人真实密码校验 | PASS | 正式PasswordService/真实登录/DB不变反例 |
| G03 管理重置操作/目标授权 | PASS | 独立权限、全部门店/目标授权和账号保护 |
| G04 重新确认/频控 | PASS | 四用例当前密码与原子三维窗口、24并发/故障 |
| G05 密码与依据原子 | PASS | 同事务密码/代际/资源version/成功事件/意图 |
| G06 所有目标设备渠道 | PASS | 本人/目标WEB和mini、他人他租户保留 |
| G07 Redis清理失败仍失效 | PASS | 实际容器暂停和PENDING/旧401/补偿 |
| G08 登录/改密/撤销竞争 | PASS | 可控关卡/行锁、未来代际不误删、双JVM |
| G09 CSRF/渠道边界 | PASS | 旧认证反例回归，新增敏感写正反例 |
| G10 排队任务重验 | PASS | PG代际/权限交集/真实设备/查询失败关闭 |
| G11 记录无秘密 | PASS | 成功事务绑定、失败独立事务、固定字段/产物扫描 |
| G12 原测试/新增集成 | PASS | 完整clean verify及语义替换逐项记录 |
| G13 OpenAPI/三端类型 | PASS | generate/check/三个typecheck=0 |
| G14 生产包隔离 | PASS | 无测试类/固定凭据/故障开关、V1/V2字节匹配 |
| G15 文档一致 | PASS | 契约、执行/补偿/授权/前端边界、链接/保全 |

[生产产物审计](evidence/P05-03/artifact-audit.json)：最终JAR包括七个正式JPA实体、V1/V2与实际STAFF Provider及七个Sa-Token 1.46.0模块；正式迁移与源码字节一致，无测试类/资源/测试路由/故障代理、固定编码密码或未解决编译stub。日志/最终XML/观察JSON无凭据特征命中；1478项既有ui/infra/CI、历史报告证据、V1、规则/锁/POM/版本事实源保全通过。git diff --check=0，文档链接通过，无运行中Testcontainers；没有删除用户容器/卷。

## 8. 失败历史、未实现与未验证

保留compile-first=1（注解同名及Parameters不适用于类，修正注解/位置）；smoke-first和security-it-first=1（新增固定SQL适配未登记，精确白名单修正）；security-it-second=1（增量构建含不可见protected clearLastActive调用的未解决编译stub，HTTP500；诊断仅记录异常类型/帧、不提交值，改用公开splicingKey+受控delete并clean重编）；security-it-third=0（当时32安全+1升级及14结构通过）。contracts-generate=1（清理Header未指定string导致错误object枚举），schema-diagnostic确认真实schema后修正，contracts-generate-second=0及check=0。以上失败不改写为PASS。

verify-first=1：132单元/196集成已运行，旧IdentityFoundationIT的27项因V2外键新增表未列入测试TRUNCATE在@BeforeEach失败；明确增加两表、更新正式迁移数/受限函数数，未改生产外键或用CASCADE抹平。新增实际Redis暂停反例在数据库200/PENDING和旧401之后，新登录429，揭示获取锁超时结果不确定且获取失败未释放；补owner比较finally释放，修正后verify-second=0，328项通过；真实Redis暂停反例成功变更/PENDING、旧会话拒绝、恢复新密码登录及me补偿均通过。首轮XML/观察JSON保留[verify-first-reports](evidence/P05-03/verify-first-reports/)。

未实现：完整员工/角色CRUD及停用API、Web/小程序页面与请求层、短信邮件找回/自动密码交付、微信客户凭据、平台管理员体系、RabbitMQ/Outbox/通用审批、审计查询页面/完整保留平台、定时补偿工作进程。P08/P07等后续责任不在本轮自动执行。

NOT_EXECUTED / NOT_VERIFIED：用户现有库升级/真实管理账号操作、生产TLS浏览器/可信代理/Redis ACL/HA/Cluster或跨区域故障切换、任意长进程暂停的分布式锁fencing、生产PBKDF2负载容量、远程CI/部署、Windows/Linux、完整微信真机身份流程。P05-02历史浏览器证据保持，本轮敏感接口验证为真实HTTP和两个生产JAR实例，没有新增浏览器页面或真实设备验收。

## 9. 文件、状态与下一合法任务

生产新增StaffSecurityController/Operations/Store/Jdbc、StaffTaskAuthority/TaskAuthority、正式V2；修改实际会话、频控、强制改密和当前身份、异步执行、权限清单/启动验证及员工映射。新增两组真实集成测试、扩展双JVM和原断言；修改OpenAPI导出/结构/生产包测试、三端schema/纯类型/断言。完整新增修改文件见[文件清单](evidence/P05-03/file-manifest.json)，不修改ui/、V1、冻结依赖/版本或根锁，不清理用户数据库/卷。

文档更新AUTHENTICATION、AUTHORIZATION、IDENTITY、ASYNC-EXECUTION、LOCAL-DEVELOPMENT、ROADMAP、DECISION-LOG、ACCEPTANCE-MATRIX，并补PERSISTENCE/REDIS/CONFIGURATION/MULTI-TENANCY/MODULE-BOUNDARIES/OPENAPI-GENERATION、迁移目录说明和README。历史任务报告/证据保留。

P05-03 COMPLETE，仅关闭本任务；**P05整体仍IN_PROGRESS**。按[实际路线](../development/ROADMAP.md#p05-03-当前执行2026-10-08)剩余平台身份owner/初始化/认证控制面要求，下一建议拆分为 **P05-04：平台管理员身份基础、独立初始化与控制面认证接入**（NOT_STARTED，需明确授权）。这是依据既有阶段目标提出的任务名称，不伪称已有冻结实现方案；客户微信认证按后续P05/P09/P10另行安排。只报告、不执行。
