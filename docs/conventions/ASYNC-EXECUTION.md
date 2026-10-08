# 进程内异步执行约定

实施日期：2026-10-08，P04-03。只支持显式注入 `shared.tenancy.TenantTaskExecutor` 的短生命周期进程内任务。没有用户任务 HTTP/表/调度器、RabbitMQ、Outbox、Servlet ASYNC 身份传播；持久化任务仍遵循 [ASYNC-TASKS](../contracts/ASYNC-TASKS.md)，不能用本机制替代当前权限重验和可靠投递。真实测试见 [P04-03](../testing/P04-03-VERIFICATION.md)。

## 调用与范围

调用者已经在可信 BUSINESS 的当前 permissionCode 范围，且调用方应用事务已经完成，才可提交：

```java
// 先由Spring代理应用服务完成提交，再提交不依赖未提交数据的工作。
var future = tenantTaskExecutor.submit(() -> {
    // 工作线程已建立捕获的操作范围；再调用代理方法开启自己的事务。
    return applicationService.queryResult();
});
var result = future.get(); // 任务失败通过ExecutionException观察。
```

`submit(Callable<T>)` 不接受 TenantContext、tenantId、CurrentPrincipal 或序列化快照。不可公开构造的内部 Snapshot 从当前可信范围捕获，只保存租户、主体域/ID、当前 permission/range、必要门店上限/明确门店、既有授权版本元数据与 trace；taskId 替代调用方 sessionId。所有集合不可变，快照不带完整 grants、Token、密码、Request、Session、可变实体、EntityManager、连接或事务资源。

Callable 自己也必须只捕获所需不可变技术输入/资源 ID，不能捕获可变 Entity、HTTP Request、会话或数据库句柄。结构检查能拒绝直接底层调用，但不能分析任意闭包对象图；这是模块接入和审查责任。工作线程只可保持/收窄当前 permission，不能借原身份另一个 grant、扩大捕获范围或 new 可信入口/provider 来伪造快照。

仅 shared 的无身份技术工作可用 `submitUnscoped`；从租户范围调用会拒绝，普通业务字节码调用也拒绝。该入口不建立租户身份，租户 Guard 默认拒绝。它用于无身份技术检查，不是匿名访问业务或 SYSTEM 超权入口。

## 时效与授权撤销

默认快照有效期 30 秒，配置可收紧，硬上限 60 秒；使用进程内单调时钟，从提交捕获开始计时，排队时间也计入。开始执行前检查，过期任务不运行 action、Future 返回拒绝异常；运行期间每次当前范围读取/Guard、嵌套权限选择及受控 DB beforeCommit 继续检查，action 返回前再检查。嵌套提交继承剩余期限，不能续期扩大有效窗口。

这不是自动终止 CPU/外部 I/O 的超时调度；无检查点的计算/外部调用不会被其他线程强制停止。模块须控制工作时长和 I/O 超时，并使用协作检查点；已完成的独立事务不能因后续过期或取消逆转。排除长期排队、持久化任务、跨进程消息以及需要保证执行时账号仍有效的安全敏感写入。本轮快照反映捕获时授权，**没有**权威账号停用/权限撤销重校验；已有版本字段不证明仍有效。

P05 提供权威主体状态、security/authorizationVersion、门店和角色授权查询；敏感用例必须在接入真实身份时重验。P10 持久化任务/每分片/消息 handler 使用当前授权与提交范围交集、可信来源/schema/purpose；不能把本 Snapshot 序列化后当 MQ 授权凭证。未来支持撤销是新验收责任，本阶段没有以快照过期替代它。

## 事务、RLS 与提交时机

`submit` 明确拒绝调用线程 active transaction 或 active synchronization。当前推荐在代理用例返回后提交；不提供 afterCommit helper，也不承诺已有事务的 afterCommit 回调可以直接调用（该时点同步资源可能仍绑定，应在事务完整完成后提交）。这种保守限制拒绝依赖未提交数据的用法。

工作线程的顺序是：检查无残留 → 建立新的范围/trace → 调用代理 application 方法开启新事务 → 沿用 P04-02 ScopedTransaction 在实际 JPA 同连接绑定局部 GUC → 受控查询/写 → 提交或回滚 → 实际线程 finally 清理范围。没有复制 transaction ThreadLocal/EntityManager/连接/GUC，也没有第二套 RLS 设置。只读事务同样通过当前策略；新事务/提交时 scope 必须一致且未过期。

提交后触发的进程内任务仍可能因拒绝、进程崩溃或停止而丢失；业务提交成功不代表任务可靠投递。不要用 afterCommit 或本线程池描述“可靠消息”。需要可靠通知/任务时等待 P10 Outbox 的事务意图、领取、重试和幂等。DB 与 Redis 也不组成分布式事务。

## 有限资源、失败、取消与停止

默认 2 个固定工作线程、32 个有界队列位置；配置线程1～16、队列1～1024。AbortPolicy：满队列/已停止抛 RejectedExecutionException，不使用 CallerRunsPolicy，不落回调用方事务线程。未进入工作线程的拒绝不安装上下文、不改变调用线程 MDC/trace。

| 生命周期 | 行为 |
| --- | --- |
| 正常/任务异常 | Future 可观察结果/ExecutionException；scope/trace由实际线程 finally 清理，后续同线程A/B/无身份任务不继承 |
| 未关嵌套范围 | 根边界清理后抛生命周期错误，不报告成功 |
| 排队取消 | Future立即CANCELLED，移除队列项释放容量；action不运行、身份不安装 |
| 运行中cancel(true) | 请求interrupt，Future可能已终止但action仍运行；只在实际action退出时由工作线程清理，不能从取消者线程清理 |
| cancel(false) | 不请求运行中断；运行任务仍须自行结束，Future不再返回其结果 |
| 执行异常 | 非取消Future记录异常；固定日志记录taskId及异常类型，无异常原文/堆栈。取消后的Future无法再承载晚到的执行异常，日志仍使失败可观察 |
| stopNow | 取消排队Future、请求运行线程中断；不强行清理运行任务的上下文 |
| close/应用停止 | 停止接新任务，等待shutdown-wait（默认5s，硬上限30s）；超时stopNow，再等同一时长。仍未退出抛明确错误，运行线程保留其生命周期责任 |

取消不能撤销已提交 DB/外部副作用，需模块定义安全检查点及幂等；这不是未来用户任务状态机。不要依赖 Future.isDone/isCancelled 推断线程已退出，[JDK Future 契约](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/Future.html)及测试均体现此边界。

进入任务前和退出后检查租户、trace、管理MDC键及事务资源必须干净；发现残留拒绝/记录固定错误，Future失败，废弃该工作线程，不将残留身份当合法外层。线程由实际线程自身退出，后续任务用新线程。普通任务正常异常不需要人工clear。只复制允许的 traceId，不复制任意 MDC；tenantId/operatorId/storeId来自执行范围，traceId通过TraceScope恢复，调用方字段不变。没有全局MDC.clear，不安装InheritableThreadLocal，不包装第三方池/公共ForkJoinPool，不开启 @EnableAsync 或自动 Servlet 重绑。

## 新模块接入

注入唯一登记的执行器，只在可信当前权限、事务完整完成后提交；输入最小且不可变；工作端代理用例新事务；资源层仍执行本次TENANT/STORES/SELF/门店和关联检查；检查Future/异常，定义拒绝/取消/停止/已提交副作用的业务处理；需要当前授权时先接P05权威能力，可靠性需求接P10。真实PostgreSQL/Redis范围及异常反例必须随模块补充，不能以基础测试通过推导全部未来业务安全。

## P05-02 真实会话的明确限制（2026-10-08）

当前选择**禁止真实会话授权快照**，尚未实现执行前撤销重验。生产STAFF Provider实现SessionPrincipalProvider标记；TenantContextFilter/TrustedTenantExecutor创建带会话来源标记的身份根，forPermission/narrow/门店选择均保持标记，captureTaskDeadline在入队前403 PERMISSION_DENIED。标记不来自请求，不向业务开放安装接口；因此真实用户任务不可能以30秒旧快照入队后继续执行，停用/退出无需等待30秒。

此限制只针对新增真实会话来源；现有测试与明确内部技术Provider边界保持，submitUnscoped仍不授租户业务权限。没有系统任务伪装员工会话，也没有给所有任务加Token。以后用户任务需先实现原sessionId重验、当前安全状态/授权与捕获范围交集、当前权限扩大不得扩大任务授权，然后再建立范围/新事务；不得仅删除本限制。真实HTTP提交反例及原执行器回归见[P05-02](../testing/P05-02-VERIFICATION.md)。

## P05-03 真实会话任务重验（2026-10-08）

本节替代P05-02真实会话全部禁止的限制，内部技术边界和有限线程/队列/期限/清理机制保留。唯一执行器经shared.security.TaskAuthority端口接入StaffTaskAuthority；未装配可信端口的真实会话任务仍403拒绝，不仅删除旧限制。

入队从当前服务器会话捕获私有Proof：真实源sessionId、tenant/employee引用、安全代际与租户安全代际，不带Token、CSRF、密码、完整Request或全部grants，不接受客户端Proof。工作上下文taskId仍是独立UUID。执行前顺序为单调时效 → 新trace边界 → 正式数据库当前员工/租户状态及两个安全版本 → 账号有限终端索引核对原sessionId/绝对及闲置期限（不续期）→ 当前操作权限 → 与捕获的类型/门店范围及门店上限交集 → 新业务上下文/独立事务。版本不符、设备退出/到期或强制改密401，权限撤销403，DB/Redis故障503；任何反例都不运行action。

当前授权扩大不能扩大已捕获范围；类型交集为空保守拒绝，不用另一个权限grant替代原permission。明确门店不再允许时拒绝。会话来源标记在工作边界保留，真实任务再次嵌套入队没有HTTP源证明，当前403拒绝；不能把工作范围降为技术Provider以续期或绕过撤销。内部技术任务的原嵌套剩余时效保持。

真实测试用生产登录获得会话、可控Latch占住工作线程、排队后改密/全撤销/当前退出/撤权限/数据库权限故障，再释放线程，验证拒绝及后续线程MDC/事务/上下文清理。扩大权限测试验证STORES捕获仍保留原范围。测试挂钩仅在src/test的端口装饰器，无随机sleep或生产故障开关。

本机制检查**之后开始执行的排队任务**。已经执行并完成授权重验的任务仍承担自己的事务、资源授权与协作退出责任；不承诺远程回滚，不从其他线程清它的上下文。普通进程内任务仍不是可靠投递、长任务状态机、MQ授权证明或P10替代。
