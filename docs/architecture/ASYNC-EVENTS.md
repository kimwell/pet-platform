# 异步事件、Outbox 与可选外部适配

冻结日期：2026-10-07，P01-02。本文拥有消息和 Outbox 语义；任务 HTTP/字段见 [异步任务](../contracts/ASYNC-TASKS.md)，开关与迁移见 [配置](CONFIGURATION.md)。

## RabbitMQ 事件

RabbitMQ 可选，默认关闭；关闭无连接、监听器或 broker 健康依赖，不让选项破坏基础启动。启用使用 Spring AMQP、JSON UTF-8、白名单 eventType，不接收 Java 序列化/任意类名。消息最大256KiB，大内容以受权附件/业务引用传递。

| 字段 | 契约 |
| --- | --- |
| schemaVersion | 正整数，初始1；不兼容升级新版本并提供明确消费者策略 |
| eventId / occurredAt | UUID事件ID / UTC时间点；同逻辑事件重试保持ID |
| eventType / producer | 注册稳定代码 / 注册模块代码 |
| tenantId | 租户事件必填字符串；平台事件允许null但必须平台 eventType，不能null绕过 |
| traceId | API规定的有效关联标识；不是权限 |
| actor | SYSTEM或身份域/主体ID/授权版本/执行目的，无Token或Cookie |
| payload | eventType具体schema；未知字段/超限拒绝 |

broker仅私有网络、独立应用vhost/受限生产者与消费者凭据，队列绑定限制eventType/producer，外部回调不能直接声明SYSTEM事件；消息中的tenantId/actor只在可信来源及handler授权校验后使用，不把JSON字段视为身份。

至少一次投递，无“恰好一次”承诺。durable queue、persistent消息、publisher confirm+mandatory return；收到 confirm ACK 且无路由退回才认为发布成功。连接中断/确认不确定会重发，消费者必须幂等。[RabbitMQ官方确认语义](https://www.rabbitmq.com/docs/confirms)。

消费去重键 `(consumerCode,tenantId,eventId)`，平台单独命名空间；幂等记录与业务变更同 DB 事务提交，事务成功后才 ack，失败不记录完成。外部渠道使用 eventId/业务幂等键或投递台账；不能靠内存 Set 去重。消息 trace/context 严格校验，handler 基于当前授权/系统目的建立上下文，finally清理。

暂时错误按1秒、10秒、60秒、300秒、900秒五次延迟重试；用固定TTL重试队列+DLX实现延迟，不依赖额外broker插件；登记 attempt，超限到 DLQ；协议/授权/永久业务错误直接 DLQ，禁止无限 requeue。DLQ 重放须显式操作权限、原因及审计，保持 eventId 幂等，不能修改租户后重放。定期观察积压、最老事件、重复、死信；不自动丢弃。

## Outbox

业务事务写 outbox：eventId、tenantId、eventType、schemaVersion、payload、traceId、actor、status、attempts、nextAttemptAt、leaseOwner、leaseUntil、fencingVersion、publishedAt。不是事务提交后才写日志来假装可靠。

领取短事务以 PostgreSQL `FOR UPDATE SKIP LOCKED`（[官方SELECT语法](https://www.postgresql.org/docs/17/sql-select.html)） 批量最多100条可发送记录，转 IN_FLIGHT，租约60秒并增加 fencingVersion；发布 I/O在事务外，超过30秒需续租。结果更新必须匹配 leaseOwner+fencingVersion，旧worker不得覆盖新领取。超时恢复后重发，同 eventId；崩溃窗口允许重复。

确认ACK且无return → PUBLISHED；NACK/退回/超时 → RETRY及nextAttemptAt，指数退避上限15分钟加抖动，20次后 DEAD并报警/人工重放。broker关闭时消息能力不接新消息用例，历史Outbox保留，不能转换成假成功。无broker的本地任务调度可按已安装handler处理，不冒充RabbitMQ投递。

技术调度器仅有限系统角色/登记函数领取Outbox和任务记录，无通用业务表查询权；取出后按tenant/actor/purpose重新建立范围执行。Outbox保留：PUBLISHED至少30天后按批清理，DEAD保留到明确处理；消费者幂等记录至少90天且长于可重放窗口（默认30天）。超窗口事件拒绝自动重放。

## 异步任务与微信适配

任务调度同样持久化领取/租约/超时恢复；业务handler独立定义导入校验、事务分片、导出字段及权限，不存在通用“任意表导出”接口。任务执行/结果访问按当前权限再验，协议见任务文档。

WxJava小程序/支付模块初始化按需选择、运行开关默认关闭。微信登录 code 与手机号补充 code 分开；session_key仅服务端短期处理。平台入口不能冒充客户微信身份。

支付仅适配边界：商户/应用与Tenant映射可信配置；验签、证书序列/轮换、时间/重复通知、金额/币种精确校验、幂等；微信成功响应仅在校验和持久化接收成功后发送。回调原始字节不能先被通用JSON处理改变；错误遵守微信协议。秘密不入模板/日志。业务模块定义支付结果如何处理，本模板不预建订单状态机；真实配置/联调P10执行。

## P04-03 当前进程内能力与后续责任

现在实现显式TenantTaskExecutor，限短期进程内工作；不可变快照来自当前可信操作范围，有限队列、超龄拒绝、无调用线程事务继承，实际工作线程finally清理。合法调用、提交时机、Future/取消/停止、MDC和期限见[ASYNC-EXECUTION](../conventions/ASYNC-EXECUTION.md)。本节只补当前实现，不改变上文RabbitMQ/Outbox/持久化用户任务冻结语义。

没有真实权限重校验，本轮不声称处理账号停用和权限撤销，也不提供可靠投递。即使在事务完成后提交，进程崩溃/拒绝仍可能丢任务；不能把afterCommit叫可靠消息。本Snapshot不是可序列化授权凭证，不适用于MQ、持久化队列或长期任务；Servlet ASYNC仍排除。

P05提供权威主体状态/授权版本/范围查询及正式数据owner；P10持久化任务/分片/handler根据可信来源和当前权限与提交范围交集重建，可靠性由Outbox/领取/重试/幂等保证。Redis资源端口不提供通用限流/Lua/分布式锁、Token DAO或消息去重业务。阶段结果见[P04-03](../testing/P04-03-VERIFICATION.md)。
