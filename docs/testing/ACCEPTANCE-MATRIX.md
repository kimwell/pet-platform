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
| A04-01 | tenancy / P04 | 同ID/跨tenant查询/count/exists/关联；无上下文拒绝 | 真PG/Testcontainers/JPA，两个租户技术数据 | [隔离](../architecture/MULTI-TENANCY.md) |
| A04-02 | persistence / P04 | 创建/更新/删除/bulk/native跨tenant；运行角色不能绕RLS/DDL/TRUNCATE | 真PG运行角色与owner分离，SQL/JPA反例、迁移策略扫描 | 同上 |
| A04-03 | tenancy / P04 | store属于他tenant/无授权store、SELF归属、不同权限不同范围 | 真PG读写/总数；不是前端按钮检查 | [授权](../architecture/AUTHORIZATION.md) |
| A04-04 | persistence / P04 | 连接复用/提交回滚/REQUIRES_NEW/事务外加载，不能旧tenant泄漏 | 真连接池JPA事务，SQL GUC与线程清理记录 | [隔离](../architecture/MULTI-TENANCY.md) |
| A05-01 | 三身份 / P05 | 同主体ID跨域拒绝；Cookie/Header混合与错误头；公共端点不继承身份 | 真Redis与HTTP，空间/键隔离及客户端请求 | [认证](../architecture/AUTHENTICATION.md) |
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
| A10-04 | wechat / P10 | 微信code/phone分离、支付验签/重复/金额/证书轮换、不引订单状态机 | 启用时有合法外部配置的微信环境；缺配置NOT_EXECUTED | [异步](../architecture/ASYNC-EVENTS.md)、[身份](../contracts/IDENTITY.md) |
| A11-01 | 模板 / P11 | 新目录name/package/标识替换完整、可选组合、生成前冲突拒绝 | 全新临时生成项目、三端构建/真实infra；原工程SHA不变 | [范围](../product/SCAFFOLD-SCOPE.md) |
| A12-01 | 验收 / P12 | 干净环境全新项目，全部所选能力正反例、备份恢复、版本窗口复核 | 全新生成的真实三端/PG/Redis/所选外部能力；发布另需任务授权 | [路线](../development/ROADMAP.md) |

架构冻结只是DOCUMENTED；依赖解析为RESOLVED，编译/类型为COMPILED，只有明确运行场景才能RUNTIME_VERIFIED。测试夹具不是业务验收账号/数据。无法执行记录NOT_EXECUTED及具体原因/影响，禁止替换为PASS。


2026-10-08微信准备轮历史补充（当时BLOCKED）：沿用A02-03与路线“三端可独立构建”门禁，不改验收范围。实际AppID和目标工具已准备，typecheck/lint/静态结构exit0；账号尚未登录，开发权限NOT_VERIFIED，工具npm、源码编译、模拟器、预览和真机NOT_EXECUTED。P02-02保持BLOCKED/P02 IN_PROGRESS；A09-01的完整真机/后端/身份验收仍归P09。首轮及P01/P02-01所有失败和未执行证据保留，不以离线组件输出、工具安装或配置文档检查转换为真实微信编译PASS。


2026-10-08登录后关闭：A02-03按既有门禁通过，P02-02/P02 COMPLETE，P03 NOT_STARTED。GUI真实npm构建与工具编译日志all done、模拟器TDesign渲染/交互和0运行错误已留证；公共AppID恢复为空，实际绑定保留在忽略私有文件，复开验证通过。真机基础预览依据用户原文“已打开且按钮正常”记为限定PASS（用户反馈），没有独立截图/控制台；不等同A09-01完整业务验收。CLI服务端口关闭、内部preload警告及代码质量建议如实保留，未通过变更门禁或关闭编译检查完成任务。


2026-10-08 P03-01关闭：公共信封/状态/字段错误/trace/分页排序输入与空页响应已实现。G01～G15均PASS，详见[P03-01报告](P03-01-VERIFICATION.md)。A03-01仅公共协议场景 RUNTIME_VERIFIED；A03-02的真实数据库部分、A03-03完整标量/DB往返、A03-04实际OpenAPI/三端生成、A03-05完整结构与持久化防绕过仍 NOT_EXECUTED / NOT_VERIFIED。P03整体 IN_PROGRESS，不能用34项技术测试或文档审计推导业务、认证、租户、数据库、前端完成。

2026-10-08 P03-02：已接入真实 PostgreSQL/JPA/Flyway、UUID/时间审计、应用服务事务、分页适配和数据库健康。62项测试包括17项专门 PostgreSQL IT，完整门禁与进程反例/产物隔离见 [P03-02](P03-02-VERIFICATION.md)。A03-02 在本轮技术数据库分页场景 RUNTIME_VERIFIED；A03-03仅 UUID/Instant 数据库存储和毫秒精度通过，完整JSON标量/DST/金额/version仍未执行。A03-04实际OpenAPI/三端生成、A03-05完整模块结构与持久化防绕过、A04全部范围/RLS/角色权限仍 NOT_EXECUTED / NOT_VERIFIED。没有正式业务模型或业务验收；P03保持 IN_PROGRESS，远程CI与多OS不由本机结果替代。

2026-10-08 P03-03：G01～G15 PASS，79项后端测试无失败/错误/跳过；生产/测试OpenAPI严格分离，生成产物一致性反例、Web/小程序纯类型消费、字节码违规与产物隔离已执行。P03-03/P03整体COMPLETE依据原路线的协议与生成一致、真实PostgreSQL迁移/事务可重复条件，三项报告合并核对，不修改历史失败。生产独立迁移运行、运行/迁移角色权限仍NOT_EXECUTED（原A04-02及部署边界）；远程CI、多OS仍NOT_VERIFIED。完整破坏性分析、租户/身份/业务筛选/完整微信验收不在当前证据范围。下一P04需后续授权，不自动执行。详见 [P03-03](P03-03-VERIFICATION.md)。

2026-10-08 P04-01：新增A04-00细化上下文/范围/生命周期技术门禁，原A04-01～04及P04原完成条件保持不变。真实认证、正式Store事实源、JPA/RLS/数据库越权、Redis/异步仍NOT_EXECUTED/NOT_VERIFIED，不由上下文或结构测试推导多租户隔离完成。最新数量/命令/退出码/生产产物及G01～G15见 [P04-01](P04-01-VERIFICATION.md)。P04整体IN_PROGRESS，下一P04-02不自动执行。
