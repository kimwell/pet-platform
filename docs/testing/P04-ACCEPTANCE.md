# P04 多租户与数据范围总体验收

日期：2026-10-08（Asia/Shanghai）。**P04-01、P04-02、P04-03及P04整体 COMPLETE，限定后端工程基础能力。** 未执行P05，没有提交、推送、发布或部署。结论依据原路线“单/多租户共用机制、无上下文拒绝、越权查询和写入被阻止”，并综合三项任务的实际实现/真实资源反例；没有删除原门禁，也没有用认证/业务未实现来冒充它们已完成。

当前完整命令、数量、逐项回归、G01～G15及产物/资源审计以[P04-03](P04-03-VERIFICATION.md)及其证据为准；P04-01/P04-02原历史日志/失败/限制保持原样。

| 验收面 | 当前实现和真实证据 | 阶段边界 |
| --- | --- | --- |
| 可信身份入口 | Provider默认empty，PLATFORM独立，STAFF/CUSTOMER须可信tenant；内部根安装，不读HTTP自报tenant；[P04-01](P04-01-VERIFICATION.md) | 正式Sa-Token会话/账号状态/授权查询未实现，P05负责 |
| 默认拒绝/嵌套 | AUTHORITY_READ不能业务持久化；BUSINESS按permission选择，narrow只同主体子集；LIFO/同线程关闭，根泄漏兜底清理 | 业务不能新建未登记可信入口或task snapshot |
| TENANT/STORES/SELF | 同权限范围并集/门店上限，空STORES无行；SELF以主体域+owner ID；SQL与写入反例 | 未来模型必须声明自己的归属，不推断createdBy |
| 门店归属与授权 | StoreOwnershipReader/Guard检查tenant、本权限范围和身份门店上限；缺正式事实503；测试有真实安全表/内部技术事实 | 正式Store数据/状态源未建，不能作为门店业务验收 |
| HTTP生命周期 | REQUEST/同步ERROR、正常/错误/嵌套/线程复用与MDC；真实Tomcat请求链 | Servlet ASYNC/异步ERROR不自动继承，完整Servlet异步排除 |
| JPA安全谓词 | ScopedPersistence强制tenant AND scope AND business，OR/count/exists/ID详情/分页同范围；[P04-02](P04-02-VERIFICATION.md) | 不提供裸Repository、任意Specification替代安全条件、未登记native/JPQL |
| 新增/更新/删除 | 可信归属、managed当前范围、version、固定字段与范围DML；伪造/detached/跨tenant/门店/owner拒绝且独立PG核对无写 | 所有未来模块仍需自身查询/写入测试 |
| 原子批次 | 1～100输入、去重、全目标锁/范围校验、实际影响数、version/audit/clear、异常整批回滚 | 内部原子集合不是公开batch-actions逐项成功协议 |
| 公开逐项批处理边界 | 原API契约未改，文档明确逐项事务/响应/version/幂等责任 | 没有提供公开批处理API，P07对应业务实现另验 |
| 跨实体约束 | 五个技术表非空tenant、复合unique/FK、RESTRICT；绕过应用跨tenant关联PG23503 | 仅技术迁移，正式业务SQL仍需添加并验收 |
| RLS与角色 | ENABLE/FORCE、USING/WITH CHECK；runtime非owner/SUPERUSER/BYPASSRLS/CREATEROLE/CREATEDB/INHERIT且无owner成员关系；native/bulk/DDL/TRUNCATE/升权真实反例 | 容器角色配置不是生产角色权限；[部署前清单](../conventions/PERSISTENCE.md#p04-03-异步事务与生产rls前置)待实际部署验证 |
| Redis命名空间 | 环境/租户/门店/平台独立、无歧义UTF-8编码、受控Key签发及操作范围复核；真实CRUD/TTL/隔离/故障 | 原始资源命中仍查本次授权；权限过滤结果禁止缓存，无Token DAO/业务缓存/Cluster多Key支持 |
| 异步范围与事务 | 只当前权限的不可伪造快照；有限显式池；调用方事务结束后提交，工作端新事务沿用ScopedTransaction；A/B/空STORES/SELF真PG隔离 | 短期快照不是执行时当前授权或MQ凭证，P05/P10补重验/可靠性 |
| 线程/连接复用 | 相同worker A/B/无身份、并发、异常/漏关/超龄/取消/停止；单连接池A/B、flush/过期提交回滚、局部GUC事务外EMPTY | 取消Future不是实际停止，运行线程finally清理；不强清仍运行的上下文 |
| 结构与生产产物 | 本项目ASM字段/泛型/注解/调用/方法句柄反例；裸DB/Redis/执行器/Key伪造/测试引用拒绝；全部测试class/resources/JAR隔离 | 不证明任意反射、恶意凭据代码、闭包对象图或缓存内容语义 |

A04-00～04仍按原目标验证；新增Redis/异步及角色核对作为A04-05～07补充，没有降低或替代原范围。单租户仍使用非空真实tenantId，同一机制没有SINGLE模式旁路；技术A/B并列证明统一隔离，但未提供租户管理产品功能。

## 真实限制与归属

| 未实现/未验证事项 | 当前标记 | 后续责任 |
| --- | --- | --- |
| 正式Tenant/Store/平台/员工/客户/角色/组织/凭据数据与初始化 | NOT_IMPLEMENTED | P05-01先建正式数据基础/迁移/安全初始化及owner关系 |
| Sa-Token认证、三域会话、Cookie/CSRF、真实授权撤销/提交前权威版本重验 | NOT_IMPLEMENTED / NOT_VERIFIED | P05；不能用测试Provider完成登录验收 |
| 登录候选限定函数及AUTHORITY_READ正式数据查询 | NOT_IMPLEMENTED | P05在正式身份SQL上实现并实测RLS/受限运行角色兼容，不能作为P04已完成能力 |
| 正式Store归属/有效状态源 | NOT_IMPLEMENTED | P05数据owner接入StoreOwnershipReader，后续经营规则由对应业务定义 |
| 停用/权限撤销后的异步重新授权 | NOT_IMPLEMENTED | P05提供权威查询；P10执行前/每分片取当前与提交范围交集 |
| RabbitMQ/Outbox/持久化任务/可靠投递 | NOT_IMPLEMENTED / NOT_EXECUTED | P10；不序列化本进程Snapshot，不把afterCommit当可靠消息 |
| 生产运行/迁移角色、最小权限、TLS/ACL/容量、独立迁移任务 | NOT_VERIFIED / NOT_EXECUTED | 部署前按PERSISTENCE/配置清单实测，当前没有生产操作 |
| 附件/导出/公开逐项批处理/所有未来模块的业务授权 | NOT_IMPLEMENTED / NOT_VERIFIED | P07/P08及每个owner接入用例；基础测试不是业务验收 |
| Servlet ASYNC、长期队列、跨进程/Cluster多Key | UNSUPPORTED / NOT_VERIFIED | 当前明确排除，需要独立设计和验收，不自动扩展 |
| 远程CI、Windows/Linux、生产部署/端到端真实账号验收 | NOT_EXECUTED / NOT_VERIFIED | 相应后续阶段；本机macOS不代替 |

原工程阶段没有要求在P04执行正式认证/业务/生产部署；这些限制保持既有owner，未临时删门禁。生产DB凭据被恶意代码掌握后修改GUC，或恶意代码绕过Redis命名空间，超出应用受控接口与静态规则证明范围。

## 下一合法任务

**P05-01：正式身份数据基础、初始化路径与认证接入依赖。** 沿用冻结平台/员工/客户三域、Tenant/可选Store/Organization区分及角色关系，建立实际数据owner、正式Flyway/复合约束/RLS与受限角色验收；设计并实现可重复/可审计、没有默认账号和明文密码的安全初始化路径，连接StoreOwnershipReader和权威账号/版本/授权查询，落实后续登录候选查找函数与认证依赖。

该任务先解决真实数据和初始化，不用测试身份/技术表/内存会话通过登录验收。随后P05认证任务才能基于正式数据验证Sa-Token Redis/域隔离/Cookie/CSRF/会话失效。此处只定义下一合法任务，**P05仍NOT_STARTED，未自动执行**。
