# 持久化与事务约定

P03-02 实施日期：2026-10-08。当前实现位于 `com.pet.platform.shared.persistence`；单 Module 和模块公开接口规则见 [模块边界](../architecture/MODULE-BOUNDARIES.md)。数据库是 PostgreSQL，正式结构由 [Flyway](DATABASE-MIGRATION.md) 管理。

`BaseEntity` 是 `@MappedSuperclass`，只有 `UUID id`、`Instant createdAt`、`Instant updatedAt`。首次 persist 时由 JDK `UUID.randomUUID()` 生成 UUID v4，数据库列为 uuid；没有外部 ID 框架。未持久化时 id 为 null，使 Spring Data 能正确识别新实体。没有主键或时间公共 setter，也没有依赖可变字段的 equals/hashCode；当前保持 Java 对象身份语义，跨会话需要显式比较非空 ID。

JPA Auditing 的 `AuditingEntityListener` 负责创建和修改时间。`Clock.systemUTC()` 是默认时间来源，可通过明确的 Clock Bean 替换；DateTimeProvider 截断到毫秒，数据库列使用 `timestamp(3) with time zone`，Hibernate JDBC 时区固定 UTC。创建时两个时间相同；实际 dirty update 时更新 updatedAt，重复设置相同值不触发时间变化。createdAt 为 `updatable=false` 且无公开 setter。SQL 不提供时间默认值，避免与应用审计争夺责任。

这些字段不是完整身份审计：本轮没有 createdBy/updatedBy、AuditorAware 或假操作者。基类不强制软删除、@Version、tenantId/storeId。实体按实际用例决定是否继承；需要乐观锁时由实体显式声明。原生 SQL、JPQL bulk update 会绕过实体监听器，不能假设时间审计自动执行；后续确需此类写入时由 owner 登记并维护时间/安全约束。

事务位于 Application Service 的公开用例方法，必须实际经过 Spring 代理；Controller 不持有事务，Repository 不编排完整用例。同步默认 `REQUIRED`，跨模块通过 owner 的公开 application 接口加入同一事务，不直接调用他人的 Repository。运行时异常默认回滚，受检异常需要用例显式决定 `rollbackFor`；不能 catch 后返回成功。必要时 flush 能让数据库约束在用例内失败，最终提交也可能失败。

`readOnly=true` 是事务优化/写入意图提示，不是权限、租户隔离或禁止一切写入的保证。OSIV 固定关闭；DTO/投影应在应用服务事务内完成，Controller 不返回 Entity 或 Spring Data Page。数据库事务不覆盖本地文件、微信 HTTP、消息发送等资源；远程请求和长 I/O 位于事务外，后续按所属能力使用意图、Outbox 或补偿。

分页只复用 [现有协议](../contracts/PAGINATION.md)。`JpaPageAdapter.toPageable(query, whitelist, rawParameters)` 将外部 page-1 转成内部页码，只通过 `SortWhitelist.resolve` 生成固定单层属性 Sort，显式 NULLS LAST，同向唯一字段由已有白名单逻辑补充一次。JPA offset 的 int 限制通过 long 比较检测，超出返回 422 RESULT_TOO_LARGE。不能自行把客户端字符串传给 PageRequest、JpaSort.unsafe 或 SQL。

应用服务可先 `Page.map(dtoMapper)`，再 `JpaPageAdapter.fromPage(dtoPage)` 得到现有 PageResponse；total 使用 `getTotalElements()` 的 long 保真字符串，不转 int 或以本页长度代替。实际列表返回超末页空集合，保留请求页码和真实总数。原生 SQL 的排序和 NULLS LAST 需要 owner 显式实现，不依赖 ORM 默认规则。

本轮不提供通用 BaseService/BaseRepository、无范围全局 findById 服务或生产演示接口。测试 Repository 的全局 CRUD 仅是技术夹具。P03-02当时尚未实现TenantContext或范围机制；P04-01现已实现上下文/Guard及无上下文拒绝，但ScopedPersistence、RLS和数据库隔离仍待P04-02。正式业务访问必须经过完整P04范围机制，BaseEntity不能被称为多租户完成。

测试方法不加自动回滚事务：通过测试应用服务代理提交或抛异常，再由独立数据库查询核对提交/回滚，覆盖已 flush 单笔、多笔和唯一约束失败。Docker 必须可用，测试只连接动态独立 PostgreSQL 容器；详情与数量见 [P03-02](../testing/P03-02-VERIFICATION.md)。

## P04-02 当前受控持久化能力（2026-10-08）

本轮增加5个shared.persistence类型，无新依赖、通用业务CRUD Service或Spring Data全量Repository。实体基础、事务范围及RLS责任见[隔离](../architecture/MULTI-TENANCY.md)。继承TenantScopedEntity的业务构造显式initializeTenant；StoreScopedEntity调用initializeStore(storeId,guard)。没有租户/门店公共setter；只读JPA getter保留可代理性。创建时不得接收客户端Entity、tenantId或任意detached对象。owner由具体模型明确记录主体域+ID，普通更新不得迁移。

模块适配器继承 `ScopedPersistence<T>`，构造参数包括EntityManager、固定permissionCode、实体类型、ResourceAccessPolicy、可批量更新属性白名单、Clock及StoreScopeGuard。底层句柄是private，方法protected final；业务只暴露固定用例，不透传Supplier/Consumer/Specification、属性名称或Entity输入给HTTP。单个能力实例只能服务其登记权限；需要不同动作权限时分别构造相应能力，不用调用者选择任意permission。

下列是真实src/test安全夹具中的合法适配器方法节选，不是生产演示接口；完整代码见 `apps/backend/src/test/java/com/pet/testing/tenantpersistence/SafetyRepositories.java`：

```java
UUID create(String code) {
    return insertNew(() -> new SafetyParent(code, "初值")).getId();
}
void rename(UUID id, String name) {
    update(id, entity -> entity.rename(name));
}
void remove(UUID id) { delete(id); }
Page<SafetyParent> list(BusinessCondition<SafetyParent> condition, Pageable page) {
    return page(condition, page);
}
int renameAll(Collection<UUID> ids, String value) {
    return updateBatch(ids, "displayName", value); // 属性固定在代码中
}
```

应用服务公开方法使用@Transactional并实际经过Spring代理，例如测试service.create/rename/remove/renameAll。查询方法使用readOnly事务，在事务内映射DTO。固定查询条件示例（模块适配器内）：

```java
var condition = (BusinessCondition<SafetyParent>) (root, cb) ->
    cb.or(cb.equal(root.get("code"), "shared-code"),
          cb.equal(root.get("displayName"), "初值"));
var pageable = JpaPageAdapter.toPageable(pageQuery, STATIC_SORTS, rawParameters);
return page(condition, pageable); // 基类强制tenant AND scope AND (整个OR)
```

业务条件必须是静态且无副作用的Criteria谓词，不能来自客户端可执行表达式。排序只通过现有JpaPageAdapter+模块静态SortWhitelist；基类还拒绝无界分页和任意属性路径。page/count/exists/findIds使用相同ResourceAccessPolicy；count为真实数据库计数，total沿用字符串。不承诺READ_COMMITTED下列表与count两条语句的并发快照一致性。

单条require(id)在有范围SQL内定位，失败404；exists对范围外是false，不先无范围加载。更新只改变模块模型允许的业务字段并显式检查归属/加载执行范围，再flush；使用具体模型@Version。普通tenant/store迁移没有接口。删除委托保留范围的固定CriteriaDelete，不使用裸deleteById。没有getReferenceById/lazy reference普通入口；当前关联以明确引用ID和目标模型受控query处理。

### 内部原子集合命令

本基础能力updateBatch/deleteBatch是**内部原子集合操作**：输入1～100个非空ID（按提交条数限制），重复ID去重；空/超限/含null属于应用输入校验错误，不直接冒充公开HTTP错误。范围内以PESSIMISTIC_WRITE锁定全部去重目标；任何不存在/不可见目标均统一404且不报告哪个ID，随后同一事务的Criteria DML再次强制安全条件。成功数是数据库实际影响行数，不是请求条数。异常影响行数会标记rollbackOnly并抛错；错误不得catch后当作成功。批量更新仅固定白名单业务列，同时更新UTC毫秒updatedAt及递增显式long @Version；批量删除由数据库FK约束限制。bulk绕过实体回调，不依赖这些回调保证安全。

DML前flush，成功后clear整个当前持久化上下文，禁止继续复用先前managed引用；用例在批次后应结束或重新经受控入口加载。新模块需要批次时必须明确该副作用、模型long version和字段白名单，并验证约束/影响行数/失败回滚。未提供任意复杂bulk转换或原生SQL扩展。

冻结[API批量部分失败](../contracts/API.md#批量部分失败)仍是公开batch-actions的逐项独立事务协议；本轮没有实现这个HTTP用例或改写其响应。未来实现应逐项调用经范围和version检查的单条用例，不能把内部原子集合偷偷当作公开逐项成功行为，也不能反向弱化内部原子保证。技术批次测试不代替公开version/幂等契约验收。

### 关联与新模块接入清单

父表与子表均有tenant_id，目标建立UNIQUE(tenant_id,id)，引用列以FOREIGN KEY(tenant_id,parent_id) REFERENCES父表(tenant_id,id) ON DELETE RESTRICT；门店外键同理。具体模型先经目标受控查询确认关联授权/tenant一致，再保存ID。RLS/FK不证明门店或owner关联授权，不启用危险级联删除。当前测试采用标量引用列，避免隐式JPA导航；新join/懒加载/投影须独立验证被引用资源策略且只能在事务内完成。

新模块接入必须完成：

1. 定义资源类型、明确支持的范围与owner业务含义；未声明组合拒绝，principalType参与跨身份owner判定。
2. 实体业务构造从可信上下文初始化，DTO仅白名单业务字段；owner/tenant/store归属不经普通更新修改，具体模型显式@Version。
3. infrastructure构造固定权限受控能力，application使用本模块固定方法与实际代理事务，api仅DTO；先收窄范围再开启事务。
4. 条件和排序静态登记，所有单条/列表/count/exists/ID集合及变更使用强制范围；DTO关联必须经目标自己的受控策略。
5. 正式Flyway增加非空归属、复合唯一/FK、删除约束、ENABLE/FORCE RLS和运行角色USING/WITH CHECK；不得照搬技术表名。
6. 确需内部批次则明确1～100提交条数、去重、原子拒绝、long version、审计/clear、真实影响数和回滚；公开逐项协议另行实现。
7. 真PG验证跨租户/门店/owner/无上下文/OR/count、伪造/关联/批次/事务/连接复用及当前模块新增SQL；跑结构违规和生产产物隔离。
8. 上线前单独验证实际迁移/运行角色非owner/SUPERUSER/BYPASSRLS且无DDL/TRUNCATE/升权，部署权限不能用本地容器代替。

禁止EntityManager.find/merge/getReference、JdbcTemplate/JDBC、裸JpaRepository/save/deleteById/deleteAllInBatch、native Query和字符串JPQL作为普通业务入口。当前底层白名单仅ScopedPersistence/ScopedTransaction。结构规则不是任意未来SQL安全证明；数据库凭据被恶意代码掌握后更改GUC仍在防御边界之外。

完整验证见[P04-02](../testing/P04-02-VERIFICATION.md)。生产表与角色配置、真实认证/实时权威版本重验、正式Store事实源、Redis、异步/消息/附件/导出隔离、所有未来业务和生产部署仍未实现或未验证。

## P04-03 异步事务与生产RLS前置

工作线程只建立捕获的最小执行范围，不继承调用方transaction ThreadLocal、EntityManager或连接。TenantTaskExecutor拒绝在active transaction/synchronization中提交；代理应用服务返回后再提交，工作端通过代理开启新事务，沿用唯一ScopedTransaction在同一JPA连接set_config(local)。提交时仍需完整当前范围一致且未过快照支持期。没有第二套RLS入口，也不把提交后进程内工作当可靠消息。调用方法、短时授权和取消边界见[ASYNC-EXECUTION](ASYNC-EXECUTION.md)。

P04-02实际运行角色为非owner/SUPERUSER/BYPASSRLS/CREATEROLE/CREATEDB/INHERIT，只授显式DML；迁移owner独立，五个技术租户表ENABLE/FORCE及USING/WITH CHECK。P04-03重新核对目录权限，并以单连接池验证异步A/B事务、跨行读写拒绝、flush回滚与超龄beforeCommit回滚后GUC为空。应用范围谓词、数据库RLS、容器角色配置与生产权限是四类证据，不能互相替代；生产仍NOT_VERIFIED。

部署前必须逐项检查（本轮不执行生产操作）：

1. 运行账号非表owner、SUPERUSER、BYPASSRLS、CREATEROLE/CREATEDB，无可继承/SET ROLE到owner或特权成员关系；迁移凭据不交给运行进程。
2. schema无CREATE、无DDL/TRUNCATE/危险函数授权；只授所需表DML及登记最小函数/序列权限，PUBLIC默认权限与后续对象default privileges都检查。
3. 每个正式租户表非空归属、复合唯一/FK、ENABLE/FORCE RLS、运行角色USING/WITH CHECK；全局表需明确白名单，不能凭无tenant列成为全局。
4. 使用真实运行账号验证无GUC默认拒绝/无可见行、跨tenant native/bulk/WITH CHECK及DDL/TRUNCATE/升权反例；验证连接提交/回滚/REQUIRES_NEW/线程复用清理。
5. prod启动只validate，独立迁移任务先升级；实际部署网络、凭据、备份与最小权限留证，不能用Testcontainers owner或本地Compose管理员替代。

正式身份/Store数据、权限撤销重验以及所有未来查询/附件/导出/消息仍按owner另行接入，不把本轮安全夹具当正式数据基础。[P04总验收](../testing/P04-ACCEPTANCE.md)保持这些限制。
