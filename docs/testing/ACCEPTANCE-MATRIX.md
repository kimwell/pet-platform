# 后续实施与真实验收矩阵

冻结日期：2026-10-07，P01-02。本表拥有契约到P02～P12的实施/验收映射。P02-01 已执行 A02-01 的后端/Web/小程序静态检查及离线组件构建、A02-02 的三项容器健康/基础认证连接与后端默认关闭外部能力启动，结果见 [P02-01](P02-01-VERIFICATION.md)；P02-01当时目标微信工具编译、远程CI未执行的历史记录保留。P02-02 实际运行与配置结果见 [本轮报告](P02-02-VERIFICATION.md)：后端/Web/infra 已 RUNTIME_VERIFIED；登录后已补足目标微信工具npm/源码编译/模拟器与预览权限，P02-02/P02 COMPLETE。基础真机预览有用户反馈，完整P09验收仍未执行。P03-01公共HTTP协议已有[本轮证据](P03-01-VERIFICATION.md)；其余业务场景仍 **NOT_EXECUTED / NOT_VERIFIED**。已有P01临时探针不替代本表。每项证据必须保存cwd/命令/退出码/日志摘要/输入技术夹具说明/产物SHA及环境。

| 编号 | owner / 阶段 | 验收目标与必测反例 | 真实环境与证据 | 权威规则 |
| --- | --- | --- | --- | --- |
| A02-01 | 工程 / P02-01 | 根目录三端壳、唯一Module/Wrapper、pnpm单锁、固定package；无行业业务/旧成果覆盖 | 干净工具缓存Wrapper、frozen install、三端构建、文件清单 | [结构](../architecture/PROJECT-STRUCTURE.md) |
| A02-02 | infra / P02-01 | 固定镜像/卷，关闭RabbitMQ/微信不连接、不要求凭据 | PostgreSQL/Redis实际启动、本地默认后端启动日志 | [配置](../architecture/CONFIGURATION.md) |
| A02-03 | 工程 / P02-02 | 三端启动/停止、配置错误、开发代理、Web 构建/预览、RabbitMQ 停止和卷保留 | 后端/Web/infra PASS；G01～G16及退出码见 [报告](P02-02-VERIFICATION.md)；实际私有AppID/开发预览权限、干净工具npm、源码编译和模拟器入口PASS；基础真机预览用户反馈通过；P02-02 COMPLETE；[登录后证据](P02-02-VERIFICATION.md#13-2026-10-08-登录后续验与p02关闭)。旧NOT_EXECUTED/FAIL保留 | [既有路线](../development/ROADMAP.md) 的三端可独立构建条件；不改变 P01 冻结规则 |
| A03-01 | shared.api / P03 | 状态码/信封/fieldErrors/trace、未知异常、下载例外；非法JSON/未知字段/trace注入 | P03-01 PASS（限定协议）；JUnit/MockMvc与随机端口真实HTTP JSON、中文/脱敏/容器ERROR；[报告](P03-01-VERIFICATION.md)。工具/微信/附件业务验收未执行 | [API](../contracts/API.md) |
| A03-02 | shared / P03 | page缺失/非法/超限/超末页，sort白名单/稳定唯一，total字符串 | P03-01仅参数/白名单/total字符串/空响应PASS；P03-02 已验证真 PostgreSQL 不同页、超末页、NULLS LAST、实际count/条件组合与稳定唯一；跨页并发快照不作保证，详见 [报告](P03-02-VERIFICATION.md)；[报告](P03-01-VERIFICATION.md) | [分页](../contracts/PAGINATION.md) |
| A03-03 | shared / P03 | UUID/金额/UTC/日期/null/缺失/version，DST自然日、精度超限 | P03-03真实HTTP、schema、PG标量往返与JDK DST技术反例通过；业务自然日查询/并发version应用未实现，[报告](P03-03-VERIFICATION.md) | [类型](../contracts/DATA-TYPES.md) |
| A03-04 | API / P03 | 实际后端OpenAPI导出、生成/复制、破坏性差异使CI失败 | P03-03临时重新生成/比对、真实total string→number差异exit1、三端tsc通过；CI命令已接入，远程未执行。完整破坏性分类/同步接受变更后的旧端兼容未实现，[报告](P03-03-VERIFICATION.md) | [生成](../contracts/OPENAPI-GENERATION.md) |
| A03-05 | 各模块 / P03 | 结构依赖、shared禁止具体模块、跨Repository拒绝 | P03-03编译字节码/泛型/注解/继承规则与违规夹具、JAR隔离通过；范围/SQL注册表按既有A04实施，不用结构检查替代越权读写，[报告](P03-03-VERIFICATION.md) | [模块](../architecture/MODULE-BOUNDARIES.md) |
| A04-00 | tenancy / P04-01 | 可信身份/范围/默认拒绝、Header/Query/Body防伪造；嵌套/LIFO/异常/同线程连续A/B/匿名、门店归属/授权、MDC/REQUEST/ERROR、生产测试夹具隔离 | P04-01 PASS：114项0失败/错误/跳过，原79项全部回归；模型与真实MVC链、同一单线程Executor及真Tomcat技术测试；[本轮报告](P04-01-VERIFICATION.md)。不替代下列真PG数据库验收 | [隔离](../architecture/MULTI-TENANCY.md)、[授权](../architecture/AUTHORIZATION.md) |
| A04-01 | tenancy / P04 | 同ID/跨tenant查询/count/exists/关联；无上下文拒绝 | P04-02 RUNTIME_VERIFIED：独立PG两个租户、强制范围、OR/total/exists/关联/无上下文；[报告](P04-02-VERIFICATION.md) | [隔离](../architecture/MULTI-TENANCY.md) |
| A04-02 | persistence / P04 | 创建/更新/删除/bulk/native跨tenant；运行角色不能绕RLS/DDL/TRUNCATE | P04-02 RUNTIME_VERIFIED：测试runtime/owner分离、FORCE RLS/WITH CHECK、DDL/TRUNCATE/升权拒绝、混合批次及真实影响数回滚；生产角色NOT_VERIFIED；[报告](P04-02-VERIFICATION.md) | 同上 |
| A04-03 | tenancy / P04 | store属于他tenant/无授权store、SELF归属、不同权限不同范围 | P04-02 RUNTIME_VERIFIED：STORES/空集/门店上限、SELF主体域+ownerID读写、跨权限拒绝；正式授权数据仍待P05；[报告](P04-02-VERIFICATION.md) | [授权](../architecture/AUTHORIZATION.md) |
| A04-04 | persistence / P04 | 连接复用/提交回滚/REQUIRES_NEW/事务外加载，不能旧tenant泄漏 | P04-02 RUNTIME_VERIFIED：池复用/局部GUC、REQUIRES_NEW、flush失败回滚、旧范围实体/commit前范围改变拒绝；当前关联只存ID，不开放事务外懒加载；[报告](P04-02-VERIFICATION.md) | [隔离](../architecture/MULTI-TENANCY.md) |
| A04-05 | redis / P04-03 | tenant/store/platform命名空间、无上下文/门店/跨范围拒绝、编码/TTL/实际CRUD/故障、授权结果缓存限制 | 独立认证Redis实际读写/TTL/故障与健康HTTP；raw命中重验，过滤结果禁止；[P04-03](P04-03-VERIFICATION.md) | [REDIS](../conventions/REDIS.md) |
| A04-06 | tenancy / P04-03 | 不可伪造快照、同线程A/B/无身份、异常/MDC、拒绝/取消/停止/期限、异步新事务/连接复用 | 确定性Latch/屏障/时钟及受限PG；不继承第三方线程变量；[P04-03](P04-03-VERIFICATION.md) | [ASYNC-EXECUTION](../conventions/ASYNC-EXECUTION.md) |
| A04-07 | persistence / P04-03 | 实际RLS运行角色非owner/SUPERUSER/BYPASSRLS、FORCE/迁移关系、阶段总核对 | 容器目录/受限角色及原native/bulk/DDL/TRUNCATE/升权反例回归；生产NOT_VERIFIED；[P04总验收](P04-ACCEPTANCE.md) | [PERSISTENCE](../conventions/PERSISTENCE.md) |
| A05-09 | CUSTOMER / P05-05 | 正式客户/绑定、tenant+app+open唯一/复合FK、可信入口、WxJava单次交换、CUSTOMER Token/SELF、三域/并发/状态/故障、频控/记录、产物与真实微信 | 本地正式PG/Redis/Sa/HTTP及真实微信四步 RUNTIME_VERIFIED；登录200、me200、退出200/旧Token401，客户复用且绑定1/1；G12 PASS、P05-05/P05 COMPLETE；原421项及366/328保全；[真实验证](P05-05-VERIFICATION.md#11-2026-10-08-真实微信续验通过与阶段关闭)、[总验收](P05-ACCEPTANCE.md#2026-10-08-真实微信通过后的阶段验收) | [身份](../contracts/IDENTITY.md)、[认证](../architecture/AUTHENTICATION.md)、[配置](../architecture/CONFIGURATION.md) |
| A05-08 | PLATFORM security / P05-04 | 独立账号/首次初始化、PLATFORM空间与Cookie/CSRF/频控、控制面权限/租户拒绝、本人改密/撤销/记录、故障竞争、契约与产物 | RUNTIME_VERIFIED：正式PG/Redis、生产JAR初始化、真实浏览器、366项0失败/错误/跳过；[P05-04](P05-04-VERIFICATION.md)，部署/客户认证限制保留 | [平台初始化](../development/PLATFORM-BOOTSTRAP.md)、[身份](../contracts/IDENTITY.md)、[认证](../architecture/AUTHENTICATION.md) |
| A05-07 | STAFF security / P05-03 | 本人改密/退出全部、管理重置/撤销；重新确认/频控、独立操作和目标授权；DB原子版本/记录、Redis失败补偿与新代际保护、登录/并发、强制改密、排队重验 | RUNTIME_VERIFIED：328项0失败/错误/跳过；正式V1→V2、实际Redis暂停/补偿、确定性竞争及双JVM通过，G01～G15 PASS；291项原用例保留、一项异步限制明确替换；[P05-03](P05-03-VERIFICATION.md) | [身份](../contracts/IDENTITY.md)、[认证](../architecture/AUTHENTICATION.md)、[异步](../conventions/ASYNC-EXECUTION.md) |
| A05-06 | STAFF security / P05-02 | 正式初始化员工→真实登录/当前身份；Cookie/CSRF/Token设备隔离；期限、并发频控、状态授权变化、DB/Redis故障、RLS与线程清理；P05-02历史真实身份异步禁止（P05-03已替换为重验）、双JVM共享会话 | 真PostgreSQL/Redis/Testcontainers、正式生产Provider与HTTP、实际IAB浏览器；292项0失败/错误/跳过；G01～G15见[P05-02](P05-02-VERIFICATION.md) | [认证](../architecture/AUTHENTICATION.md)、[身份](../contracts/IDENTITY.md)、[异步](../conventions/ASYNC-EXECUTION.md) |
| A05-00 | identity/platform / P05-01 | 正式七表/首迁移/JPA validate、复合关联/RLS、密码/受限查询、Store事实、显式初始化/回滚/重跑/并发/生产包 | 正式PostgreSQL/Testcontainers与实际生产JAR命令 RUNTIME_VERIFIED；258项0失败/错误/跳过，原222逐项回归；[P05-01](P05-01-VERIFICATION.md)。未操作真实开发/生产库，不能代替后续登录/会话验收 | [初始化](../development/IDENTITY-BOOTSTRAP.md)、[认证](../architecture/AUTHENTICATION.md)、[持久化](../conventions/PERSISTENCE.md) |
| A05-01 | 三身份 / P05 | 同主体ID跨域拒绝；Cookie/Header混合与错误头；公共端点不继承身份 | P05-05真实三域同UUID/HTTP/Sa/Redis和双Cookie隔离通过；本轮真实微信CUSTOMER/SELF无STAFF/PLATFORM权限，见[P05总验收](P05-ACCEPTANCE.md)；历史客户入口未执行结论在下方保留 | [认证](../architecture/AUTHENTICATION.md) |
| A05-02 | security / P05 | Cookie属性/local-prod/代理；登录、退出、上传/写CSRF正反例；CORS错误origin | 浏览器+真后端/代理，Cookie/Origin/CSRF证据 | 同上 |
| A05-03 | identity / P05 | 绝对/闲置/设备上限、当前退出vs全部撤销、停用/改密、Redis删除故障 | 真Redis两设备/跨实例、DB版本、故障注入；不依赖Mock内存 | 同上 |
| A05-04 | authorization / P05/P07 | 角色/门店撤销立即对新请求生效，敏感旧事务回滚，平台不默认业务全权 | 真PG/Redis/HTTP并发，授权版本及审计 | [授权](../architecture/AUTHORIZATION.md) |
| A05-05 | credentials / P05 | PBKDF2版本/盐/参数/Unicode、密码不日志、登录枚举/限流 | JDK已冻结版本、真实HTTP、性能/并发限流；技术密码夹具 | [身份](../contracts/IDENTITY.md) |
| A06-01 | Web / P06 | fetch Cookie/CSRF/signal/非JSON/网络与401区分、并发401一次处理 | 真实后端+浏览器并发/断网/取消；旧epoch响应拒绝 | [Web状态](../conventions/WEB-STATE.md) |
| A06-02 | Web / P06 | Query key含身份/范围、mutation精确失效、身份变化不残留数据 | 真账号切换/权限变化、请求与缓存观察 | 同上 |
| A07-01 | Web / P07 | URL筛选恢复、草稿分离、空态vs失败、刷新保留、末页删除回退 | 真员工基础API/浏览器；可访问性/键盘验证 | [Web页面](../conventions/WEB-PAGES.md) |
| A07-02 | identity/Web / P07 | 字段错误、重复提交、未保存、Modal清理、版本冲突保留草稿、批量部分失败 | 真PG/HTTP/浏览器并发；成功失败计数核对 | [API](../contracts/API.md)、[页面](../conventions/WEB-PAGES.md) |
| A08-01 | attachment / P08 | 伪MIME/扩展/大小/内容、穿越/符号链接/并发替换 | 真磁盘+PG，恶意技术文件夹具与清理 | [存储](../architecture/FILE-STORAGE.md) |
| A08-02 | attachment / P08 | 临时/绑定/解绑/删除授权，跨tenant/store/self、一个附件双绑定 | 真PG事务、文件、HTTP；不通过目录ID直接授权 | [附件](../contracts/ATTACHMENTS.md) |
| A08-03 | attachment / P08 | I/O与DB失败、孤儿/残片/清理幂等、备份恢复一致 | 真磁盘/PG故障注入和一致备份对账 | [存储](../architecture/FILE-STORAGE.md) |
| A08-04 | attachment/audit / P08 | 票据过期/撤销/Range/秘密日志，审计成功同事务、失败可追踪 | 真HTTP/Redis/文件，完整日志脱敏扫描 | 同上、[后端](../conventions/BACKEND.md) |
| A09-01 | mini / P09 | 目标基础库/DevTools、类型/组件构建、两槽位/public不带Token | 官方工具+真机+真后端，截图/请求/产物；无假的运行PASS | [小程序](../conventions/MINIPROGRAM-PAGES.md) |
| A09-02 | mini / P09 | 切域取消/旧回调、登录合法返回、分享扫码参数、权限/网络区分 | 真机并发/离页/断网/跨域同ID反例 | 同上 |
| A09-03 | mini / P09 | 下拉/触底互斥、失败不推进、刷新竞态；受保护图/视频/download | 真机+HTTP，header、本地临时文件、票据Range/过期重取 | 同上、[附件](../contracts/ATTACHMENTS.md) |
| A10-01 | messaging / P10 | 启停/安装清单、旧库升级/关闭保留数据/Flyway checksum | 真PG迁移组合；禁止关闭时删表或移历史迁移 | [配置](../architecture/CONFIGURATION.md) |
| A10-02 | Outbox/AMQP / P10 | 并发领取/租约/fence/超时/确认不确定重复、至少一次、去重/死信 | 真RabbitMQ/Testcontainers+PG；断连/崩溃故障注入 | [异步](../architecture/ASYNC-EVENTS.md) |
| A10-03 | tasks / P10 | 当前权限与提交范围交集、租户传播/清理、取消竞态、结果过期/下载 | 真PG/Redis/文件、两个租户任务/线程复用 | [任务](../contracts/ASYNC-TASKS.md) |
| A10-04 | wechat / P10 | 订阅消息、支付验签/重复/金额/证书轮换、不引订单状态机；登录code与phoneCode分离 | 登录后端按本轮明确授权归A05-09/P05，页面/真机归P09；P10通知支付启用时需合法外部配置；当前NOT_EXECUTED | [异步](../architecture/ASYNC-EVENTS.md)、[身份](../contracts/IDENTITY.md) |
| A11-01 | 模板 / P11 | 新目录name/package/标识替换完整、可选组合、生成前冲突拒绝 | 全新临时生成项目、三端构建/真实infra；原工程SHA不变 | [范围](../product/SCAFFOLD-SCOPE.md) |
| A12-01 | 验收 / P12 | 干净环境全新项目，全部所选能力正反例、备份恢复、版本窗口复核 | 全新生成的真实三端/PG/Redis/所选外部能力；发布另需任务授权 | [路线](../development/ROADMAP.md) |

架构冻结只是DOCUMENTED；依赖解析为RESOLVED，编译/类型为COMPILED，只有明确运行场景才能RUNTIME_VERIFIED。测试夹具不是业务验收账号/数据。无法执行记录NOT_EXECUTED及具体原因/影响，禁止替换为PASS。


2026-10-08微信准备轮历史补充（当时BLOCKED）：沿用A02-03与路线“三端可独立构建”门禁，不改验收范围。实际AppID和目标工具已准备，typecheck/lint/静态结构exit0；账号尚未登录，开发权限NOT_VERIFIED，工具npm、源码编译、模拟器、预览和真机NOT_EXECUTED。P02-02保持BLOCKED/P02 IN_PROGRESS；A09-01的完整真机/后端/身份验收仍归P09。首轮及P01/P02-01所有失败和未执行证据保留，不以离线组件输出、工具安装或配置文档检查转换为真实微信编译PASS。


2026-10-08登录后关闭：A02-03按既有门禁通过，P02-02/P02 COMPLETE，P03 NOT_STARTED。GUI真实npm构建与工具编译日志all done、模拟器TDesign渲染/交互和0运行错误已留证；公共AppID恢复为空，实际绑定保留在忽略私有文件，复开验证通过。真机基础预览依据用户原文“已打开且按钮正常”记为限定PASS（用户反馈），没有独立截图/控制台；不等同A09-01完整业务验收。CLI服务端口关闭、内部preload警告及代码质量建议如实保留，未通过变更门禁或关闭编译检查完成任务。


2026-10-08 P03-01关闭：公共信封/状态/字段错误/trace/分页排序输入与空页响应已实现。G01～G15均PASS，详见[P03-01报告](P03-01-VERIFICATION.md)。A03-01仅公共协议场景 RUNTIME_VERIFIED；A03-02的真实数据库部分、A03-03完整标量/DB往返、A03-04实际OpenAPI/三端生成、A03-05完整结构与持久化防绕过仍 NOT_EXECUTED / NOT_VERIFIED。P03整体 IN_PROGRESS，不能用34项技术测试或文档审计推导业务、认证、租户、数据库、前端完成。

2026-10-08 P03-02：已接入真实 PostgreSQL/JPA/Flyway、UUID/时间审计、应用服务事务、分页适配和数据库健康。62项测试包括17项专门 PostgreSQL IT，完整门禁与进程反例/产物隔离见 [P03-02](P03-02-VERIFICATION.md)。A03-02 在本轮技术数据库分页场景 RUNTIME_VERIFIED；A03-03仅 UUID/Instant 数据库存储和毫秒精度通过，完整JSON标量/DST/金额/version仍未执行。A03-04实际OpenAPI/三端生成、A03-05完整模块结构与持久化防绕过、A04全部范围/RLS/角色权限仍 NOT_EXECUTED / NOT_VERIFIED。没有正式业务模型或业务验收；P03保持 IN_PROGRESS，远程CI与多OS不由本机结果替代。

2026-10-08 P03-03：G01～G15 PASS，79项后端测试无失败/错误/跳过；生产/测试OpenAPI严格分离，生成产物一致性反例、Web/小程序纯类型消费、字节码违规与产物隔离已执行。P03-03/P03整体COMPLETE依据原路线的协议与生成一致、真实PostgreSQL迁移/事务可重复条件，三项报告合并核对，不修改历史失败。生产独立迁移运行、运行/迁移角色权限仍NOT_EXECUTED（原A04-02及部署边界）；远程CI、多OS仍NOT_VERIFIED。完整破坏性分析、租户/身份/业务筛选/完整微信验收不在当前证据范围。下一P04需后续授权，不自动执行。详见 [P03-03](P03-03-VERIFICATION.md)。

2026-10-08 P04-01：新增A04-00细化上下文/范围/生命周期技术门禁，原A04-01～04及P04原完成条件保持不变。真实认证、正式Store事实源、JPA/RLS/数据库越权、Redis/异步仍NOT_EXECUTED/NOT_VERIFIED，不由上下文或结构测试推导多租户隔离完成。最新数量/命令/退出码/生产产物及G01～G15见 [P04-01](P04-01-VERIFICATION.md)。P04整体IN_PROGRESS，下一P04-02不自动执行。

2026-10-08 P04-02：受控JPA与资源策略、归属不可由客户端决定、复合关联、RLS同连接执行、内部原子批次及结构违规反例已实施，P04-02 COMPLETE（167项0失败/错误/跳过，原114项全部回归），最终命令/XML/数据库快照/JAR隔离与G01～G15见[P04-02](P04-02-VERIFICATION.md)。A04-01～04的本轮技术模型证据使用独立PostgreSQL与受限角色；正式生产角色/独立迁移、认证/权威撤销、所有未来业务、Redis/异步、跨OS/远程CI/部署仍未验证。P04整体IN_PROGRESS；下一合法P04-03只报告、不自动执行，公开HTTP逐项批处理协议未改变且尚未实现。


2026-10-08 P04-03及P04关闭：A04-00～04原目标和反例保留，新增A04-05～07补Redis/异步/角色与总体验收，最终命令/数量与零跳过以[P04-03](P04-03-VERIFICATION.md)为准。真实认证、正式Store、权威撤销重验、MQ/Outbox、生产角色部署和所有未来业务仍未实现或未验证；技术夹具不是业务账号，基础阶段完成不等于产品上线。下一合法P05-01先正式身份数据基础/初始化/认证依赖，P05仍NOT_STARTED，不自动执行。

2026-10-08 P05-01：正式身份数据基础/初始化/认证依赖已完成，A05-00限定正式SQL/角色/函数/密码/Store/原子性和生产命令技术验收；258项（原222+新增36）0失败/错误/跳过，G01～G15及命令证据见[P05-01](P05-01-VERIFICATION.md)。A05-05密码存储/Unicode/编码反例已验证，真实HTTP枚举/频控、生产性能仍未验证。A05-01～04真实三域会话/载体/Cookie/CSRF/撤销条件未改变且未执行；P05整体IN_PROGRESS。平台/客户/Organization及管理CRUD、异步权限撤销重验未实现，生产部署和真实管理员初始化NOT_EXECUTED/NOT_VERIFIED。下一合法P05-02只建议、不自动执行。

2026-10-08 P05-02：A05-06及本轮G01～G15 PASS；132单元+160集成=292项，原258项逐项保留，真实员工由正式初始化服务建立并使用唯一生产会话Provider。A05-01已验证STAFF与预留空间/载体隔离，其他域正式登录未执行；A05-02同源本地浏览器链路和生产Cookie属性/配置反例通过，生产TLS/代理/上传/跨源仍未验证；A05-03当前设备/跨实例/期限/状态/凭据版本/Redis故障通过，全设备撤销API留下一任务；A05-04新请求读取当前授权通过，敏感旧事务提交前检查/平台审计仍未执行；A05-05真实HTTP安全失败和并发限流通过，生产负载性能未验证。异步选择真实身份任务403禁用，未实现撤销重验；不把30秒技术快照作为安全证据。P05-02 COMPLETE，P05整体IN_PROGRESS，P05-03建议范围见[路线](../development/ROADMAP.md#p05-02-当前结果及后续范围2026-10-08)，不自动执行。详见[P05-02报告](P05-02-VERIFICATION.md)。

2026-10-08 P05-04：新增A05-08限定平台身份与控制面认证。366项（原P05-03的328逐项保留+新增38）0失败/错误/跳过；独立正式初始化/五表/V3、真实Sa/Redis/PG、同UUID/双Cookie/跨域CSRF/租户拒绝、本人改密/全撤销、实际Redis清理故障及确定性竞争、当前平台权限/受限Redis入口通过，真实浏览器在平台Cookie Path内且登录态有效时验证HttpOnly可见性。G01～G15 PASS，P05-04 COMPLETE，P05整体IN_PROGRESS；客户正式登录未执行，生产TLS/代理/跨源/数据库与Redis部署/真实管理员初始化、远程CI及多OS仍NOT_EXECUTED/NOT_VERIFIED。完整证据和失败历史见[P05-04](P05-04-VERIFICATION.md)，下一P05-05仅建议、不执行。


2026-10-08 P05-05当前结论：新增A05-09，正式客户/微信绑定、服务端入口/安全秘密引用、WxJava4.8.0一次交换、四接口/CUSTOMER Sa会话、SELF/同UUID三域/跨租户、并发及依赖故障/频控/事件在真实PG/Redis及测试Gateway边界通过。最终421项0失败/错误/跳过，原366/328逐项保留；五项生成/类型检查与JAR隔离通过。真实微信安全配置未载入、新鲜code未获取，交换/会话/me/退出全NOT_EXECUTED，G12 BLOCKED，其余门禁通过；P05-05 BLOCKED/P05 IN_PROGRESS。P09页面/隐私/真机、P10消息支付、生产/远程CI/多OS未执行；完整员工角色权限管理API仍P07必选且未实现，不能将三类登录齐备当整个身份阶段COMPLETE。下一仅P05-05真实续验，不自动执行；详见[P05-05](P05-05-VERIFICATION.md)、[P05总验收](P05-ACCEPTANCE.md)。历史失败/NOT_EXECUTED与P05-01～04证据原样保留。

2026-10-08 P05-05真实续验追加：用户确认暂时无法提供AppSecret。实际秘密属性和0600/0700忽略文件、专用白名单入口已准备；冻结PG/Redis、正式角色/V1～V4迁移/租户初始化、既有生产JAR受限运行及readiness=200 UP实际通过，匿名me=401 AUTH_REQUIRED仅为准备检查。客户及绑定0/0；未获取code、未交换、未签真实CUSTOMER会话、未验证真实me/退出/再次复用。当前工具AppID/版本匹配，wx.login开发权限仍NOT_VERIFIED；CLI246为服务端口关闭。A05-09真实四步仍NOT_EXECUTED、G12 BLOCKED、P05-05 BLOCKED/P05 IN_PROGRESS，其他门禁维持历史证据。仅本轮资源停止，既有卷/进程保全；无认证代码/依赖/公开契约修改、无全套421项重跑。证据见[本轮准备](evidence/P05-05/resume-2026-10-08/isolation-preparation.json)、[本轮四步结果](evidence/P05-05/resume-2026-10-08/real-wechat-result.json)、[报告第10节](P05-05-VERIFICATION.md#10-2026-10-08-真实微信续验准备完成appsecret仍缺少)。P07/P09/P10及生产/远程CI限制不变，下一仅P05-05续验。

2026-10-08 20:04真实微信最终续验：指定本地安全配置、实际AppID/工具基础库匹配，正式WxJava4.8.0生产Gateway交换通过；登录200、PG客户/绑定1/1及Redis CUSTOMER/MINIPROGRAM会话、me200同租户/SELF、logout200后旧Token401 SESSION_EXPIRED/旧Redis会话不存在。第二个新鲜code200复用同客户，计数仍1/1并退出。A05-09真实四步 RUNTIME_VERIFIED、G12 PASS；综合P05全部原条件后P05-05/P05 COMPLETE。原准备BLOCKED/NOT_EXECUTED、首轮HOME导致的自动化失败（微信尝试0）均保留。仅本轮资源已恢复，包括临时服务端口关闭，原进程/卷保全；无认证/依赖/公开契约改动，不重跑421项。详见[实际四步](evidence/P05-05/resume-2026-10-08/real-run-second/real-wechat-result.json)、[综合复核](P05-ACCEPTANCE.md#2026-10-08-真实微信通过后的阶段验收)。P07完整员工/组织/角色权限管理仍必选未实现，P09/P10/生产/远程CI/多OS仍独立未执行；下一P06-01仅建议、不自动执行。

## P06-01 本轮验收追加（2026-10-08）

| 项 | 当前结论 / 证据边界 |
| --- | --- |
| A06-01 请求/会话 | RUNTIME_VERIFIED：正式PG/Redis/生产JAR+浏览器Cookie/独立CSRF、真实双域登录/恢复/当前退出、仅访问当前空间、503未确认退出与恢复、错误密码/防重复、NotFound；请求错误/取消/超时/非JSON/旧epoch/并发401为77项受控技术测试，不能称真实网络竞态全部重现 |
| A06-02 Query作用域 | 真实双身份并存及员工退出后平台仍有效RUNTIME_VERIFIED；Query身份唯一投影、范围key/旧响应/授权版本清理以受控测试COMPILED/RUNTIME_VERIFIED（技术场景）。实际业务列表/管理权限变化、后续mutation关联失效尚未实现/NOT_VERIFIED，留P06-02/P07 |
| P06-01 G01～G14 | PASS，逐项依据见[P06-01报告](P06-01-VERIFICATION.md)；typecheck/lint/test/build/contracts检查退出0，77项前端受控测试、10项契约导出检查；421项为原后端完整基线，本轮没有重跑 |
| 保全/边界 | 506项后端/类型/小程序/ui/根依赖保护摘要无变化；原P05未提交修改保留。入口chunk警告保留；生产/TLS/跨源/远程CI/多OS/下载/实时推送/全部未来业务NOT_EXECUTED或NOT_VERIFIED |

P06-01 COMPLETE，P06整体IN_PROGRESS，下一建议P06-02 NOT_STARTED，只报告不执行。真实验证与受控替身测试按报告分开，不用文档检查替代联调。

## P06-02 前轮验收追加（历史，2026-10-08）

| 项 | 当前证据与边界 |
| --- | --- |
| A06-01 请求/会话与敏感表单 | 两域真实改密、旧密码拒绝/新密码登录、第二独立浏览器上下文旧会话失效、全设备退出、Cookie/跨域CSRF反例、Redis未确认与恢复RUNTIME_VERIFIED；自动重放/旧401/取消/超时为受控技术测试 |
| A06-02 权限/范围与Query | Query唯一身份事实、元数据共用/受限导航、真实权限下降、STAFF零管理权限本人操作及双空间保全RUNTIME_VERIFIED；未来业务资源范围/关联缓存仍未实现，不能外推 |
| 本轮自动化/命令 | 5文件120项Web、10项契约导出检查及六项实际命令退出0；421项为既有后端完整基线，本轮未重跑 |
| G01～G10/G12～G14 | PASS；具体等级、命令、保全摘要和限制见[P06-02](P06-02-VERIFICATION.md) |
| G11 | PARTIAL；安全流程/实际字段错误/权限/1280px及390px/键盘通过，原生窗口focus补验NOT_EXECUTED，不能将bringToFront或手动刷新等价为原生focus |

P06-02 IN_PROGRESS、P06 IN_PROGRESS。下一合法任务仅P06-02原生focus续验与门禁复核，不自动进入下一任务。用户日常账号未改，本轮资源/临时输入已结束，旧成果及依赖/锁/ui/后端/小程序/契约保全。历史失败/未执行原样保留，生产/远程CI/多OS/实时推送及未来业务仍未验证。

## P06-02 原生focus续验与门禁复核（2026-10-08）

| 项 | 最新结果及证据边界 |
| --- | --- |
| A06-01/A06-02 原生focus刷新 | RUNTIME_VERIFIED；原生Chrome Window菜单切换产生isTrusted=true、visible focus，事件间无visibilitychange；平台me由15增至16且403，STAFF仍6，当前页旧敏感表单/导航消失。最小化恢复平台改密权限隐藏另有证据 |
| 双空间保全 | 同一无痕上下文进入/admin/security，员工me200、本人安全表单有效，无重新登录；非跨标签主动推送证明 |
| G01～G14 | PASS；G11缺项由本轮真实事件/HTTP/空密码截图补齐，其余沿用既有证据并复核；[报告](P06-02-VERIFICATION.md) |
| 检查/保全 | 无源码/公开契约/依赖变化，原5文件120项Web及六命令退出0继续有效；新增check:repo、摘要/链接审计、已知技术输入扫描退出0。仅本轮资源恢复，原用户窗口及三容器保全 |

最新P06-02 COMPLETE、P06 IN_PROGRESS。前轮PARTIAL/NOT_EXECUTED与工具失败作为历史保留；不把手动刷新或源码存在等价为focus通过。下一建议P06 Web阶段综合验收（建议拆为P06-03，NOT_STARTED），不自动执行。生产TLS/跨源、远程CI、多OS、实时推送与未来业务边界不变。
