# 持久化与事务约定

P03-02 实施日期：2026-10-08。当前实现位于 `com.pet.platform.shared.persistence`；单 Module 和模块公开接口规则见 [模块边界](../architecture/MODULE-BOUNDARIES.md)。数据库是 PostgreSQL，正式结构由 [Flyway](DATABASE-MIGRATION.md) 管理。

`BaseEntity` 是 `@MappedSuperclass`，只有 `UUID id`、`Instant createdAt`、`Instant updatedAt`。首次 persist 时由 JDK `UUID.randomUUID()` 生成 UUID v4，数据库列为 uuid；没有外部 ID 框架。未持久化时 id 为 null，使 Spring Data 能正确识别新实体。没有主键或时间公共 setter，也没有依赖可变字段的 equals/hashCode；当前保持 Java 对象身份语义，跨会话需要显式比较非空 ID。

JPA Auditing 的 `AuditingEntityListener` 负责创建和修改时间。`Clock.systemUTC()` 是默认时间来源，可通过明确的 Clock Bean 替换；DateTimeProvider 截断到毫秒，数据库列使用 `timestamp(3) with time zone`，Hibernate JDBC 时区固定 UTC。创建时两个时间相同；实际 dirty update 时更新 updatedAt，重复设置相同值不触发时间变化。createdAt 为 `updatable=false` 且无公开 setter。SQL 不提供时间默认值，避免与应用审计争夺责任。

这些字段不是完整身份审计：本轮没有 createdBy/updatedBy、AuditorAware 或假操作者。基类不强制软删除、@Version、tenantId/storeId。实体按实际用例决定是否继承；需要乐观锁时由实体显式声明。原生 SQL、JPQL bulk update 会绕过实体监听器，不能假设时间审计自动执行；后续确需此类写入时由 owner 登记并维护时间/安全约束。

事务位于 Application Service 的公开用例方法，必须实际经过 Spring 代理；Controller 不持有事务，Repository 不编排完整用例。同步默认 `REQUIRED`，跨模块通过 owner 的公开 application 接口加入同一事务，不直接调用他人的 Repository。运行时异常默认回滚，受检异常需要用例显式决定 `rollbackFor`；不能 catch 后返回成功。必要时 flush 能让数据库约束在用例内失败，最终提交也可能失败。

`readOnly=true` 是事务优化/写入意图提示，不是权限、租户隔离或禁止一切写入的保证。OSIV 固定关闭；DTO/投影应在应用服务事务内完成，Controller 不返回 Entity 或 Spring Data Page。数据库事务不覆盖本地文件、微信 HTTP、消息发送等资源；远程请求和长 I/O 位于事务外，后续按所属能力使用意图、Outbox 或补偿。

分页只复用 [现有协议](../contracts/PAGINATION.md)。`JpaPageAdapter.toPageable(query, whitelist, rawParameters)` 将外部 page-1 转成内部页码，只通过 `SortWhitelist.resolve` 生成固定单层属性 Sort，显式 NULLS LAST，同向唯一字段由已有白名单逻辑补充一次。JPA offset 的 int 限制通过 long 比较检测，超出返回 422 RESULT_TOO_LARGE。不能自行把客户端字符串传给 PageRequest、JpaSort.unsafe 或 SQL。

应用服务可先 `Page.map(dtoMapper)`，再 `JpaPageAdapter.fromPage(dtoPage)` 得到现有 PageResponse；total 使用 `getTotalElements()` 的 long 保真字符串，不转 int 或以本页长度代替。实际列表返回超末页空集合，保留请求页码和真实总数。原生 SQL 的排序和 NULLS LAST 需要 owner 显式实现，不依赖 ORM 默认规则。

本轮不提供通用 BaseService/BaseRepository、无范围全局 findById 服务或生产演示接口。测试 Repository 的全局 CRUD 仅是技术夹具。当前尚未实现 TenantContext、ScopedPersistence、RLS 或无上下文拒绝；后续正式业务访问必须先经过 P04 的范围机制，本轮基类不能被称为多租户完成。

测试方法不加自动回滚事务：通过测试应用服务代理提交或抛异常，再由独立数据库查询核对提交/回滚，覆盖已 flush 单笔、多笔和唯一约束失败。Docker 必须可用，测试只连接动态独立 PostgreSQL 容器；详情与数量见 [P03-02](../testing/P03-02-VERIFICATION.md)。
