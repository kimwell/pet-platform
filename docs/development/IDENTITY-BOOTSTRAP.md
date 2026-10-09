# 正式身份数据与显式初始化

实施：P05-01，2026-10-08。入口为生产JAR中的独立命令，不启动普通Spring应用、HTTP、JPA或Redis。普通启动只迁移（local显式配置独立迁移凭据时）和validate，不创建管理员、不读取初始密码、不重置账号。独立迁移/bootstrap命令已在隔离PostgreSQL执行，角色SQL由测试配置连接完整执行；真实开发库和生产库均未初始化。

## 数据归属与正式结构

唯一正式迁移为 `apps/backend/src/main/resources/db/migration/V1__platform_identity_foundation.sql`。此前P03/P04生产目录只有说明，没有业务SQL；本次是首次正式建表。测试的旧状态仅为空Flyway history，不声称存在历史身份业务表升级。

| 模型 / owner | 表 | 业务唯一性 / 关联 |
| --- | --- | --- |
| Tenant / platform | platform_tenant | UUID主键；code全局唯一；控制面，不继承TenantScopedEntity |
| Store / platform | platform_store | tenant_id+code唯一，归属Tenant |
| Employee / identity | identity_employee | tenant_id+login_name唯一，归属Tenant；密码只有编码哈希 |
| Role / identity | identity_role | tenant_id+code唯一，归属Tenant |
| EmployeeRole / identity | identity_employee_role | tenant_id+employee_id+role_id唯一；两端复合FK |
| RolePermission / identity | identity_role_permission | tenant_id+role_id+permission_code+scope_type唯一；Role复合FK |
| EmployeeStore / identity | identity_employee_store | tenant_id+employee_id+store_id唯一；Employee/Store复合FK |

七表都有服务端UUID、created_at/updated_at（UTC毫秒）、非负bigint version，正式JPA映射显式@Version。本轮映射只读（@Immutable），没有Repository全量CRUD、隐式关联、级联或删除功能。Tenant/Store/Employee/Role状态仅ACTIVE/DISABLED；关系是否存在表达分配，角色和员工有效状态另外核对。六个租户表tenant_id非空，均有UNIQUE(tenant_id,id)，全部关联ON DELETE RESTRICT。不默认软删除，也不提供物理删除租户或账号。

Tenant安全版本、Employee安全/授权版本初始化为0，是实际数据库字段；没有伪造当前会话版本。未来租户停用递增Tenant.security_version；员工停用/密码变化递增Employee.security_version；角色/权限/员工角色/门店授权及门店有效状态变化须同事务递增受影响员工authorization_version。对应修改用例、撤销意图、提交前复核及会话撤销尚未实现，不能仅修改DB版本就声称旧会话已撤销。

租户编码和门店编码：1～32位ASCII，首位字母或数字，后续字母/数字/连字符。员工登录名：1～64位ASCII，首位字母或数字，后续允许点、下划线、连字符。唯一入口IdentityNames先strip首尾空白，再按Locale.ROOT转小写，数据库CHECK拒绝不规范值，普通varchar唯一性比较规范化值。同一租户不允许重复登录名；不同租户允许同名。名称strip首尾空白，1～100个字符。密码不执行strip、大小写转换或Unicode归一化。

## 前置角色与权限

数据库管理员先对**明确目标数据库**预配置角色。当前主机没有psql，本地Docker目标使用冻结PostgreSQL镜像内真实客户端；先由管理员明确IDENTITY_DATABASE_CONTAINER、IDENTITY_DATABASE_ADMIN、IDENTITY_DATABASE_NAME三项非敏感目标值：

```sh
docker exec -i "$IDENTITY_DATABASE_CONTAINER" \
  psql -X -v ON_ERROR_STOP=1 --single-transaction \
  --username "$IDENTITY_DATABASE_ADMIN" --dbname "$IDENTITY_DATABASE_NAME" \
  < infra/database/provision-identity-roles.sql
```

容器内连接由受控凭据配置（例如受限PGPASSFILE）或该目标的既定本地Socket认证提供；不要在参数/URI放密码。远程目标由数据库管理员使用现有PostgreSQL客户端执行同一SQL文件。该目标操作本轮未执行；测试中已通过独立容器管理员连接运行完整SQL，冻结镜像psql客户端另留版本证据。脚本真实存在，只创建无登录能力角色，不创建账号/凭据/租户。角色已存在时命令失败并回滚，应核对既有权限；不会自动修复或覆盖。由管理员配置三个独立登录身份并安全提供凭据，不把数据库管理员当应用账号。

| 能力角色 | 登录身份成员配置 / 使用 | 权限 |
| --- | --- | --- |
| pet_migrator | 独立迁移登录身份：INHERIT TRUE、SET TRUE | 数据库/schema CREATE、正式表owner、可SET ROLE到两个函数owner；不得交给运行进程 |
| pet_runtime | 独立应用登录身份：INHERIT TRUE、SET FALSE | 正式表必要SELECT，Employee禁止直接SELECT密码列；仅两个认证函数EXECUTE |
| pet_bootstrap | 独立初始化登录身份：INHERIT FALSE、SET TRUE | 只可执行bootstrap_tenant，不读表、不执行认证函数 |
| pet_auth_owner | NOLOGIN、非superuser、无BYPASSRLS | 两个认证函数owner，只读七个必要身份表；RLS显式仅允许该角色读取 |
| pet_bootstrap_owner | NOLOGIN、非superuser、无BYPASSRLS | 初始化函数owner，只SELECT Tenant、INSERT七表；无UPDATE/DELETE/TRUNCATE |

成员配置属于管理员工作；不得让应用登录身份拥有SUPERUSER/BYPASSRLS/CREATEROLE/CREATEDB、业务表ownership，或成为迁移/初始化/函数owner成员。所有七表ENABLE/FORCE RLS，运行角色依赖现有ScopedTransaction的事务局部pet.tenant_id；无上下文不可读。Tenant使用自身id匹配GUC，六表使用tenant_id。schema PUBLIC CREATE与函数PUBLIC EXECUTE撤销，函数固定search_path=pg_catalog,pg_temp，表名显式限定，无动态SQL。两个函数owner只在迁移期间拥有专用schema CREATE，结束后撤销；运行角色无永久旁路。

IdentityRuntimePermissions在正式实体装配时核对实际应用登录身份、成员关系、密码列权限、schema CREATE、表owner、FORCE RLS及TRUNCATE，违反则拒绝启动。历史测试夹具独立EntityScan不包含正式Employee，继续测试自己的schema；此分支不是生产配置开关。

Flyway冻结实现会恢复连接原始role，不能依赖init-sql的SET ROLE一直有效。本实现不使用该配置；迁移登录身份继承pet_migrator以创建/维护history；正式V1在文件开头显式SET ROLE pet_migrator，业务表由pet_migrator拥有。没有通过提升普通运行账号解决迁移。源码依据见[精确Flyway连接实现](https://raw.githubusercontent.com/flyway/flyway/flyway-11.14.1/flyway-database/flyway-database-postgresql/src/main/java/org/flywaydb/database/postgresql/PostgreSQLConnection.java)。

## 构建、配置与实际命令

先使用[版本矩阵](VERSION-MATRIX.md)的JDK（通过当前进程JAVA_HOME，不改全局），在后端构建：

```sh
cd apps/backend
./mvnw clean verify
```

以下在项目根目录执行。独立迁移命令必须配置PET_MIGRATION_DATABASE_URL、PET_MIGRATION_DATABASE_USERNAME、PET_MIGRATION_DATABASE_PASSWORD；URL仅接受无凭据/查询参数的PostgreSQL JDBC地址。密码经受控进程环境或秘密管理工具注入，不写脚本、公共配置、报告或shell history。

```sh
scripts/backend-identity.sh migrate
```

成功输出为“正式迁移成功：本次执行数量=1”，已经迁移则数量=0。命令只运行打包的正式locations，禁止clean、自动baseline、repair、忽略校验和或乱序。prod应用启动仍关闭Flyway。local开启迁移也必须提供上述独立配置，禁止回退运行凭据。运行进程只配置PET_DATABASE_*及既有Redis输入；迁移完成后可PET_DATABASE_MIGRATION_ENABLED=false，继续JPA validate。

初始化命令独立配置PET_BOOTSTRAP_DATABASE_URL、PET_BOOTSTRAP_DATABASE_USERNAME、PET_BOOTSTRAP_DATABASE_PASSWORD，使用初始化登录身份；不使用PET_DATABASE_*或迁移凭据回退。只有用户明确目标与实际初始化值后，才能对真实本地库执行。下方值是参数占位，不是种子或预设账号：

```sh
scripts/backend-identity.sh bootstrap \
  --tenant-code '<租户编码>' \
  --tenant-name '<租户名称>' \
  --admin-login '<管理员登录名>'
```

有交互Console时密码不回显读取。没有Console则明确失败，不默默读取其他载体。可选门店必须同时显式提供`--store-code '<门店编码>' --store-name '<门店名称>'`；省略时没有默认门店，也没有EmployeeStore行。

自动化支持显式`--password-stdin`，从受控stdin输入一行UTF-8密码；只去除行结束符（LF或CRLF），密码内空格保留，损坏UTF-8失败、不截断。秘密文件应仅当前用户可读，例如权限0600且不纳入Git，由秘密管理工具预先生成：

```sh
scripts/backend-identity.sh bootstrap \
  --tenant-code '<租户编码>' --tenant-name '<租户名称>' \
  --admin-login '<管理员登录名>' --password-stdin < "$INITIAL_PASSWORD_FILE"
```

INITIAL_PASSWORD_FILE是受限文件路径；不要用echo、明文命令参数或报告构造密码。命令拒绝`--password`等未知选项，不支持密码环境变量或普通启动密码重置。读入缓冲与请求自有char[]在结束时清零；编码哈希只经prepared statement进入数据库，不打印、返回或序列化。数据库/代理/采集系统需禁止参数值记录（例如PostgreSQL log_parameter_max_length_on_error=0），应用测试只证明本项目输出没有秘密，不证明外部日志配置。

成功输出只有tenantId、employeeId和可空storeId；该管理员是STAFF租户管理员，**不是PLATFORM平台管理员**。失败退出码2、成功0；没有Token/Cookie或“登录成功”响应。

## 原子性与重跑

密码按冻结JDK PBKDF2方案在数据库事务前派生；BootstrapJdbc独立连接以SET LOCAL ROLE pet_bootstrap调用单一bootstrap_tenant函数并commit。该函数在同一事务创建Tenant、Employee、Role、9项明确RolePermission、EmployeeRole及可选Store/EmployeeStore。按规范化租户编码取得事务级advisory lock，并由数据库唯一约束兜底。并发同租户只有一个完整成功，另一个返回冲突，不产生重复账号/关系。

租户已存在（包括未知半成品）即失败：“租户已存在或初始化冲突；未修改密码或权限”。重跑不更新、不补权限、不重置密码、不自动修复。数据库在事务中拒绝或用例中途失败时全部回滚；测试用独立连接读取七表证明零残留，不能只用异常断言。如果在提交阶段连接中断而无法确认提交结果，命令只报告“未确认成功”，须先独立核查目标数据状态，不能假定已经回滚或通过重跑覆盖。

## 权限与认证接入契约

PermissionCatalog仅声明基础员工、角色与门店管理的9项代码：identity:user:list/create/update/disable/reset-password，identity:role:list/update，platform:store:list/update。初始化全为TENANT，显式清单不含*，不生成未来业务权限或平台跨租户全权。数据库保存RolePermission范围；未登记代码/不支持组合加载失败关闭。

每项RolePermission绑定permission_code+scope_type；同权限多角色通过既有ScopeGrant.mergeForPermission合并，TENANT覆盖该权限其他范围，否则STORES+SELF并存。STORES使用EmployeeStore中有效门店交集，身份门店上限也来自这个明确授权集合；TENANT不自动绕过门店上限或给未来门店自动授权。不同权限分别合并，list:TENANT不会扩大update:STORES/SELF。停用角色不贡献权限，停用门店不进入有效集合；SELF行映射仍由对应资源用例登记。

StaffAuthentication.findCandidate按规范化tenantCode+loginName定位，tenantCode仅为线索，不建立TenantContext。authentication_candidate仅返回7个必要字段：tenant_id、employee_id、password_hash、员工状态、员工安全/授权版本及租户安全版本；有效租户筛选在函数内。AuthenticationCandidate无哈希getter、Jackson禁止字段/getter可见性、toString脱敏，不能作为Controller响应。普通业务/api对内部候选、IdentityLookup、StaffAuthentication、底层适配器引用均被字节码规则拒绝；未来仅identity/api/authentication适配器可使用公开认证契约。

StaffAuthentication.verifyCredentials进行密码比较；无候选使用随机运行时dummy编码执行等成本比较，不产生预设账号。密码失败、无效租户/员工都返回empty，后续HTTP必须统一LOGIN_FAILED并接入冻结频控。成功再次读取staff_authorization，版本变化则拒绝；loadForSession必须从后续已验证会话的可信Tenant/Employee引用调用。staff_authorization只加载指定主体的有效状态、真实版本、有效Store集合及角色权限范围。没有会话时CurrentPrincipalProvider保持empty，加载结果本身不签发会话或安装可信身份。

PasswordService沿用[IDENTITY密码冻结](../contracts/IDENTITY.md)：PBKDF2WithHmacSHA256、600000次、独立32字节SecureRandom盐、256位输出，MessageDigest.isEqual比较；12～128个有效Unicode码点，代理对不合法拒绝，不静默截断。编码`$pbkdf2-sha256$v1$iterations$saltBase64$hashBase64`包含算法/版本/参数，损坏/未知/超计算上限编码校验false；needsRehash提供未来升级判断。只签发冻结参数，不自动重哈希或更新密码。没有新增第三方依赖。[JDK PBEKeySpec](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/javax/crypto/spec/PBEKeySpec.html)、[OWASP参数依据](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)。

正式PostgresStoreOwnershipReader是唯一生产事实Provider。它只在已有BUSINESS中开启/加入JPA事务，沿用ScopedTransaction绑定同连接GUC，固定投影查询当前可信tenant下ACTIVE Store；不递归Guard、不加载其他租户Store、不引入另一套Repository。Guard随后检查身份门店上限及本操作范围。不存在、他租户、停用或未授权均统一404；事实不可用503，不能当不存在或放行。无上下文仍拒绝。

## 故障排查与当前不能做的恢复

配置缺失只报告配置名；先核对正确目标、登录身份成员设置和角色脚本。迁移权限失败要区分history由迁移登录身份维护与正式表由pet_migrator拥有；不要提升runtime。迁移校验和/结构错误保留源SQL、history和目标，禁止自动repair/baseline/clean。初始化失败先检查正式迁移、bootstrap EXECUTE及角色状态，再核对七表独立事务状态；凭据/哈希/SQL参数不得粘贴进报告。

现有租户或未知半成品不会被初始化命令修复。本轮不提供密码重置、账号/角色CRUD、数据删除、权限补授、半成品恢复命令；需要独立授权和可审查的恢复方案。完整登录HTTP、Sa-Token会话、Cookie/CSRF、限流、平台账号、客户微信身份、Organization、会话撤销及异步执行前重验尚未实现。后续合法任务以[路线图](ROADMAP.md)为准，P05-02只建议、不自动执行。

P05-04补充：本文命令只创建租户/员工管理员。平台管理员使用独立账号表、初始化角色和`platform-bootstrap`入口，首次全库创建策略与部署/升级顺序见[PLATFORM-BOOTSTRAP](PLATFORM-BOOTSTRAP.md)。两种初始化凭据不得混用；普通启动均不创建或重置账号。

## B01 管理能力显式补充（2026-10-09）

V6迁移只安装管理表权限和受限函数，不自动扩大现有租户角色。初始化/升级并核对正确租户后，以既有独立 `PET_BOOTSTRAP_*` 配置执行：

```sh
scripts/backend-identity.sh identity-management-upgrade --tenant-id <规范UUID-v4>
```

该命令只补充指定租户保留管理员既有权限的对应管理能力，输出新增范围条数，不输出凭据。重复执行新增0；运行账号/PUBLIC不能调用；无活动保留角色失败。受影响管理员授权版本更新，须重新读取身份。新租户也需显式执行，默认九权限初始化策略保持原样。[具体权限映射](../contracts/EMPLOYEE-MANAGEMENT.md#b01-员工与授权管理2026-10-09)与[B01验收](../testing/B01-ACCEPTANCE.md)为本次新增能力事实源；前文“未实现CRUD/会话”的表述保留为P05-01历史边界。

## B02 页面创建后的受控初始化（2026-10-09）

正式平台创建仅建立 ACTIVE、initialized=false 的 Tenant 元数据，不创建员工或门店，客户入口仅接受已初始化 ACTIVE 租户。现有 bootstrap 兼容两条路径：全新编码原子创建；同编码/当前名称、ACTIVE 且待初始化租户原子完成首位 STAFF 管理员及保留角色。未提供 Store 参数仍合法，不自动创建假门店；初始化后可通过平台正式门店元数据入口建立 Store，再通过 B01 员工授权明确关联。

```sh
scripts/backend-identity.sh bootstrap \
  --tenant-code '<页面创建的编码>' --tenant-name '<当前租户名称>' \
  --admin-login '<首位员工账号>'
```

Console 安全输入，自动化使用已有 --password-stdin 与受控0600文件。独立初始化数据库身份、实际迁移/成员检查及密码输入规则沿用原文。名称不匹配、停用、已初始化或未知员工/角色/门店半成品均拒绝；密码/角色/授权/可选门店/initialized 在同一PG事务，任何失败全回滚。确认未提交后重试；完成后重复拒绝，不覆盖或重复。连接提交结果不明确时核查详情/身份状态，不把重跑当自动恢复。

新管理员的 B01 管理能力和 Organization 能力分别显式升级（UUID为该租户正式ID，不携带密码）：

```sh
scripts/backend-identity.sh identity-management-upgrade --tenant-id '<租户UUID>'
scripts/backend-identity.sh organization-upgrade --tenant-id '<租户UUID>'
```

Organization 升级仅保留 tenant-admin 固定6项TENANT范围权限，依赖原 identity:role:update TENANT 授权；只影响该角色关联员工的授权版本，重复新增0。普通迁移/启动不补权限。最小独立组织没有部门/岗位/层级或隐式员工数据范围。正式生产JAR受控命令与页面链路证据见 [B02](../testing/B02-ACCEPTANCE.md)。
