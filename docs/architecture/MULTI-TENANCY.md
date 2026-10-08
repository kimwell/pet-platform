# 租户、门店与持久化隔离

冻结日期：2026-10-07，P01-02。本文拥有数据范围执行和运行入口上下文；本轮未实现 JPA/RLS。

Tenant 是经营主体/客户组织；Store 为可选经营门店；Organization 为内部组织。后两者归 Tenant，不能互相替代。单租户仍有真实 tenantId 并走相同机制。身份固定一个 Tenant；tenantCode 仅用于登录/入口找候选，校验凭据与关系后才建可信 tenantId。业务 DTO 不接收 tenantId，未知字段拒绝。storeId 只表达意图，必须属于当前 Tenant、状态有效且在本操作范围。跨实体使用同租户引用查询与复合外键。

不可变 TenantContext：tenantId、principalType/principalId、sessionId 或 taskId、authorizationVersion、当前 permissionCode 有效范围、traceId、purpose；不从 HTTP 自报头构造。平台默认 PlatformContext，tenantId=null 不授权业务全量。

会话验真得到服务端保存的主体/租户后，先建立只允许身份owner最小安全查询的AUTHORITY_READ上下文，读取本主体账号状态/安全版本/角色范围；该上下文不能调用业务Repository。授权计算完成后生成含具体permissionCode范围的BUSINESS上下文。二者均设置同一可信tenant的RLS，但持久化入口目的白名单不同，避免“必须先授权才能读取授权关系”的循环依赖；HTTP输入不能选择purpose。

## 选定 JPA 隔离方向

**受限持久化入口 + PostgreSQL RLS**。RLS 强制租户底线；ScopedPersistence 统一操作/门店/本人谓词与写入检查。Hibernate Filter 不是底线；RLS 不自行证明门店/本人授权。

| 防线 | 责任 | 局限 |
| --- | --- | --- |
| application | 操作权限、数据范围、关联、业务状态 | 不能防遗漏的 SQL |
| ScopedPersistence | 查询/count/exists/更新/删除组合 tenant + 当前权限范围；无上下文拒绝 | 仍需 DB 防护与真实测试 |
| RLS | 每租户表 ENABLE/FORCE，USING/WITH CHECK 限租户；ORM/native/bulk 均受限 | 不代替 Store/SELF 规则 |
| 复合约束 | `(tenant_id,id)` 唯一，关联用 `(tenant_id,foreign_id)` 外键 | 不代替关联授权 |
| 架构/迁移检查 | 禁止无范围入口，检查表分类/策略/角色 | 不代替运行行为 |

tenant_id NOT NULL 且不可变；store 可空但使用复合外键。全局表仅平台目录/权限声明等显式白名单，不因缺 tenant_id 自动全局。

运行 DB 角色非 owner、SUPERUSER、BYPASSRLS，不继承 owner，不可 SET ROLE 升权、DDL/TRUNCATE；迁移凭据独立。下列是拟定技术策略，item 是技术占位而非业务表：

```sql
-- 同一事务连接，首次访问租户数据前；$1来自可信上下文
SELECT set_config('pet.tenant_id', $1, true);
ALTER TABLE item ENABLE ROW LEVEL SECURITY;
ALTER TABLE item FORCE ROW LEVEL SECURITY;
CREATE POLICY item_tenant ON item TO pet_runtime
  USING (tenant_id = nullif(current_setting('pet.tenant_id', true), '')::uuid)
  WITH CHECK (tenant_id = nullif(current_setting('pet.tenant_id', true), '')::uuid);
```

无值时 DB 行不可见/写拒绝；应用另外明确拒绝无上下文，不能将空结果当合法查询。只有 TenantExecution 可设置 GUC，普通 native SQL 不可自行设置。RLS 依赖可信服务端与参数化 SQL；恶意持有 DB 凭据的代码能改 GUC，故必须结合结构检查，不声称覆盖被攻陷应用。

依据 [PostgreSQL 17 行安全](https://www.postgresql.org/docs/17/ddl-rowsecurity.html)、[策略](https://www.postgresql.org/docs/17/sql-createpolicy.html)、[事务局部配置](https://www.postgresql.org/docs/17/functions-admin.html)。唯一/外键校验可能透露冲突，异常不得输出其他租户值，约束错误不作为授权判断。

## 查询、写入、SQL

application 只能调用本模块 scoped Repository，选择性暴露有范围方法，不直接公开 JpaRepository 全量 API。共享 repository factory/base implementation 统一上下文断言/谓词，P03/P04 按实际版本验证。禁止无范围 findAll/findById/deleteById/getReferenceById 或 save detached Entity。

查询/count/exists 同时加租户和本 permissionCode 的 TENANT/STORES/SELF 条件，总数也受限。更新/删除在同范围加载 managed Entity 后修改，带 @Version；创建 tenant 从上下文赋值，store/owner 按业务校验，禁止请求改 tenant/owner 逃逸。

默认禁止 bulk update/delete；确需时登记固定命令，参数化 SQL/JPQL 带统一范围谓词、影响行数校验、version 和持久化上下文失效处理。用户批量操作逐项事务，见 [API](../contracts/API.md)，不可退化为仅 id IN。native SQL 仅登记 infrastructure adapter，经 ScopedPersistence、同连接事务及 RLS；不动态拼表名/排序。EntityManager/JdbcTemplate/nativeQuery 按允许类清单检查。

先开启事务，再在 JPA 实际使用的同一连接 set_config(is_local=true)，包含只读与 REQUIRES_NEW；不能在另一 DataSource 设置。REQUIRED 不变 tenant/扩大范围。关闭 OSIV、租户实体二级缓存；禁止事务外懒加载、连接池长期 SET tenant、运行账号执行 Flyway。提交/回滚后局部设置消失，必须测连接复用。

## 上下文入口及非 DB 资源

| 入口/资源 | 建立、执行、清理 |
| --- | --- |
| HTTP | 正确域认证、当前授权后建立；finally 清线程变量，异步 servlet 显式重绑 |
| 登录查找 | 独立 AuthLookupPurpose，仅登录服务可调最小候选目录/凭据查询；限定函数/视图、不能列员工，不能创建业务 TenantContext；tenantCode 仍不可信 |
| 用户任务 | 保存提交主体/租户/权限快照，执行及结果访问按当前权限重验；每分片绑定，finally 清理 |
| 系统任务 | 登记 SYSTEM purpose/允许表/明确租户，逐租户事务；不空 tenant 全量查业务 |
| 消息 | 来源/schema/租户/handler purpose 校验；用户来源重验授权，系统来源仅固定服务能力；finally 清理 |
| 缓存 | environment/域/tenant/resource/范围摘要或 principal/授权版本进入 key，不跨范围共享值 |
| 附件/票据/导出 | 同租户业务策略/任务策略校验，不根据目录或 ID 授权 |
| Outbox | tenant 来自可信事务；调度器仅领取技术记录，无泛化业务读取权 |

登录查找选定SECURITY DEFINER限定函数（不是普通无范围Repository）：固定search_path、所有对象显式schema、禁止动态SQL，参数仅候选tenantCode和规范化username/已验证微信映射键，单条返回内部认证所需的主体ID/凭据摘要/状态/安全版本，不返回业务列表。函数owner为专用NOLOGIN认证查找角色，仅有认证表所需读取及RLS例外，运行角色不继承它，只获指定函数EXECUTE，PUBLIC权限撤销；创建客户映射只能经验证微信claims的专用注册用例与受限认证目的，不能取得员工权限。函数不能作为通用跨tenant查询口。调度领取函数同理由独立NOLOGIN角色限定访问Outbox/任务技术表，禁止访问业务表；二者需在迁移/结构清单明确登记和真PG反例验证。

不使用 InheritableThreadLocal 偷传；只传不可变最小执行描述，不传 Cookie/Token/EntityManager。正常/异常结束都清理。登录查找的具体限定 SQL 与 RLS 兼容性 P04/P05 必测，不扩展为业务绕过。

P04 真 PostgreSQL/Testcontainers 验证跨租户查询/count、JPA/native/bulk 写、Store/SELF、关联、无上下文、运行角色、连接复用、回滚/REQUIRES_NEW。迁移扫描检查全部租户表策略/FORCE/复合键；结构扫描检查未登记持久化，详见 [验收矩阵](../testing/ACCEPTANCE-MATRIX.md)。

## P04-01 已实现的上下文与执行边界（2026-10-08）

本轮实现 `shared.security.CurrentPrincipalProvider/CurrentPrincipal`、`shared.tenancy.TenantContext/TenantContextHolder/TenantExecutionScope/TenantScopeGuard`、DataScope/ScopeGrant、StoreScopeGuard/StoreOwnershipReader、TenantContextFilter 与 TrustedTenantExecutor。它们都是内部类型，不改变公开DTO/OpenAPI。尚无真实认证、正式门店事实源、ScopedPersistence、RLS、Redis或异步传播，不能称为多租户数据隔离完成。证据见 [P04-01](../testing/P04-01-VERIFICATION.md)。

CurrentPrincipal只保存主体域/ID、可信租户、非凭据sessionId、authorizationVersion、操作权限与按权限范围、门店上限。PLATFORM必须无tenant/grants/门店；STAFF/CUSTOMER必须有tenant，CUSTOMER仅SELF能力。无身份用Optional.empty表达，不能用null tenant表示全量权限。默认Provider返回空，P05由对应身份owner的可信Sa-Token适配器替换；公共端点应返回空，不从请求头/Query/Body建立身份，tenantCode仍只作候选线索。平台控制面用PlatformScopeGuard独立入口，没有转为租户身份的接口。

HTTP入口依次为TraceFilter、未来可信认证适配器、TenantContextFilter、MVC。REQUEST从Provider取最小事实，根上下文purpose=AUTHORITY_READ，没有业务permission/range；这只预留身份owner的安全查询边界，当前没有实现授权数据库查询。应用用例通过 `try (var scope = TenantExecutionScope.forPermission(permissionCode))` 选择服务端已授权的本权限范围，业务访问必须 `TenantScopeGuard.requirePermission(permissionCode)`；不能把读取Holder等同业务授权。缺上下文/身份边界不能执行业务，统一404 RESOURCE_NOT_FOUND；缺具体操作权限403 PERMISSION_DENIED。

范围只允许从当前合法边界建立，底层replace/frame不可公开。业务调用不能直接打开任意principal或tenant。同权限同租户可嵌套；forPermission在业务内保持当前收窄范围，narrow只接受同主体同租户子集，不允许扩大、跨权限或跨租户；内层关闭恢复外层。LIFO顺序错误拒绝且保持当前内层；可以按正确顺序补关。跨线程关闭拒绝，不修改任一线程。正常重复关闭幂等。最外层结束清理；HTTP和可信同步Executor的根边界发现未关内层时先恢复本次管理状态再报告内部错误，异常也不遗留上下文。没有InheritableThreadLocal，线程池/子线程不继承。

同步后台只提供TrustedTenantExecutor：从注入的可信Provider取身份后执行明确permission，不接收tenantId或任意Principal。默认Provider为空时401 AUTH_REQUIRED；平台身份不能进入。业务已在范围中应使用嵌套scope，不重新打开后台根。SYSTEM任务的登记、允许能力、权威重新授权、审计及描述传递由后续P04-03/P05/P10接入，当前没有系统超权入口。

MDC仅管理tenantId、operatorId、经StoreScopeGuard事实核验后显式选中的storeId。授权集合不是当前门店，未选择时无storeId。close恢复这三个键，不调用MDC.clear，不影响traceId或其他组件字段。不记录Token、完整身份、客户资料或门店授权集合，内部身份/上下文toString也不输出内容。

REQUEST/同步ERROR已覆盖。ERROR从私有请求属性复用服务器身份快照、不再次认证；一般再分发重建身份根边界，嵌套同步ERROR保持当前收窄范围。Filter异常由现有HandlerExceptionResolver与GlobalExceptionHandler输出既定JSON和trace；已提交响应沿用容器/流边界。ASYNC不注册/不自动绑定身份，公共异步链仍可继续；受限租户操作若没有显式可信范围必须拒绝。现有trace也仅支持同步REQUEST/ERROR，本轮不声称支持完整异步Servlet、任务或消息传播。

P04-02必须继续实现受控JPA/关联/真实PostgreSQL越权测试；P04-03处理Redis与异步边界及阶段验收。原RLS/角色/连接复用/SQL和所有阶段门禁保持不变。

P04-01异步边界加固：REQUEST启动Servlet异步后移除同步ERROR使用的服务端身份快照；后续ASYNC/异步ERROR不自动重绑身份，公共处理链继续运行。此反例仅验证拒绝隐式传播，不代表已实现或验证完整Servlet异步协议。
