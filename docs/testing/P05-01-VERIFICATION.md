# P05-01 正式身份基础验证

日期：2026-10-08（Asia/Shanghai）。**P05-01 COMPLETE；P05整体IN_PROGRESS。** 当前完成正式数据、初始化和认证依赖，登录尚不可用。用户明确授权替代AGENTS旧P02范围；未使用Product Delivery OS、未自动下一任务、未提交/推送/发布/部署，未操作开发者日常或生产数据库。

## 1. 前置事实与读取

实际仓库P04-03/P04总验收及路线图确认P04三项/整体COMPLETE。原P04完整XML中222项0失败/错误/跳过，本轮逐项比对回归，没有删除旧安全测试。起始git status为空；既有ui、前端/三端产物、锁文件、POM、VERSION-MATRIX、P04报告/证据和测试SQL保持原样。

已核对AGENTS、README、AUTHENTICATION/AUTHORIZATION/MULTI-TENANCY/MODULE-BOUNDARIES/CONFIGURATION，IDENTITY/API/DATA-TYPES，BACKEND/PERSISTENCE/DATABASE-MIGRATION/REDIS/ASYNC-EXECUTION，VERSION-MATRIX/ROADMAP/DECISION-LOG，以及P04总验收/P04-03/ACCEPTANCE-MATRIX。实际实现的CurrentPrincipal/Provider、ScopeGrant、StoreOwnershipReader/Guard、ScopedPersistence/ScopedTransaction、GUC、测试角色与字节码规则按源码核对。生产迁移原来仅README，没有历史正式业务SQL。

## 2. 正式模型与迁移

新增 `V1__platform_identity_foundation.sql` 首次正式建表：platform_tenant、platform_store、identity_employee、identity_role、identity_employee_role、identity_role_permission、identity_employee_store。各实体、字段、唯一约束/索引/复合外键、删除策略的权威说明见[身份初始化](../development/IDENTITY-BOOTSTRAP.md#数据归属与正式结构)。

七表UUID主键、UTC毫秒审计、非负bigint version和JPA @Version；Tenant属于控制面，继承BaseEntity，其余继承TenantScopedEntity，tenant_id非空。业务编码唯一包含tenant_id（Tenant.code本身全局唯一）；关联表复合FK保持Employee/Role/Store同租户，ON DELETE RESTRICT，无cascade或软删除默认。四主体实体ACTIVE/DISABLED，关系存在表达分配。映射只读@Immutable，本轮无CRUD或删除功能。

编码/登录名规范化由IdentityNames唯一实现，strip首尾空白、ASCII小写、固定字符集/长度；数据库CHECK拒绝非规范值，规范值唯一约束限制竞争。密码不trim/大小写转换/归一化。具体规范见初始化说明。

空库正式V1迁移和JPA validate已运行；已有P03/P04的真实生产前态是空history，本轮独立容器验证空history→V1→重复0变更。没有以测试P03/P04业务表模拟历史正式升级，也没有改写原migration、repair、baseline、clean或删除既有数据。

## 3. 数据库角色、RLS与认证前查询

独立管理员脚本 `infra/database/provision-identity-roles.sql` 只预配置NOLOGIN能力角色；实际三个登录身份仅在测试容器生成，不进入正式SQL/配置/产物。迁移登录身份继承pet_migrator维护history，V1显式SET ROLE pet_migrator拥有正式业务表；普通应用登录身份只继承pet_runtime，不能SET ROLE到迁移/初始化/函数owner，非superuser/无BYPASSRLS，不拥有表。

七表ENABLE/FORCE RLS，runtime的USING/WITH CHECK匹配现有事务局部pet.tenant_id；无GUC拒绝。运行权限只SELECT必要数据，Employee直接读取password_hash被权限拒绝。IdentityRuntimePermissions实际启动检查成员关系、schema CREATE、表owner/FORCE RLS/TRUNCATE及密码列权限；升权或密码列授予反例失败。正式WITH CHECK通过临时DML技术探针验证跨租户INSERT和租户迁移UPDATE被拒绝，探针权限在finally撤销，不进入正式权限配置；独立连接证明数据未改变。

认证前唯一方案为固定authentication_candidate(text,text)和staff_authorization(uuid,uuid) SECURITY DEFINER函数，pet_auth_owner拥有、非登录/非superuser/无BYPASSRLS/非表owner，只SELECT必要七表。FORCE RLS通过该角色专用SELECT政策允许函数内读取，不设置任意tenant GUC、不伪造平台身份、不关闭RLS。固定search_path=pg_catalog,pg_temp、显式限定public表、无动态SQL、撤销PUBLIC EXECUTE、只授runtime必要EXECUTE；临时同名表遮蔽反例通过。Bootstrap函数独立pet_bootstrap_owner，只SELECT Tenant/INSERT七表；只授pet_bootstrap EXECUTE，不读表/执行认证入口，不向runtime授予永久旁路。

候选函数最多精确返回一个员工的7项必要事实（租户/员工ID、编码凭据、员工状态、员工安全/授权版本、租户安全版本）。AuthenticationCandidate无哈希getter、Jackson禁止可见性、toString脱敏，根对象序列化无凭据；不返回Employee实体。未找到、错误密码、无效状态的应用契约返回empty，后续HTTP须统一LOGIN_FAILED/频控，本轮没有错误枚举HTTP或登录成功响应。

StructureRulesTest新增普通业务/Controller/泛型引用认证与bootstrap内部类型的反例、Store专用事实入口调用方反例。固定底层白名单只针对两个认证函数、权限目录查询与独立命令；原EntityManager/native/JPQL/JDBC/裸Repository/跨模块/异步规则全部回归，没有对infrastructure全面放宽。

## 4. 密码与权威身份加载

PasswordService沿用IDENTITY冻结方案：JDK SecretKeyFactory/PBEKeySpec PBKDF2WithHmacSHA256、600000次、独立32字节SecureRandom盐、256位输出，MessageDigest.isEqual。编码保存算法、v1、参数、盐与结果，支持needsRehash，损坏/未知/超成本编码返回false，不泄漏内容。12～128有效Unicode码点、损坏代理对拒绝；128个补充平面字符可完整处理，129拒绝，不静默截断。独立Python hashlib UTF-8黄金向量验证中文/空格/emoji，不是仅与本实现互相比较。无第三方依赖变化，密码不进入数据库明文列、响应或本项目输出日志。

StaffAuthentication先查候选，在事务外比较密码；不存在候选使用随机运行时dummy编码等成本比较，没有固定密码/可登录默认账号。比较后再加载有效租户/员工的安全状态和真实版本，版本变化失败。staff_authorization保留RolePermission的每权限范围；代码注册9项STAFF基础管理权限（中文名称、动作、支持范围），同权限用原ScopeGrant并集，不同权限不扩大，停用角色不贡献授权，STORES与有效EmployeeStore集合一致/空集无门店范围，未知权限加载失败关闭。

Tenant.security_version、Employee.security_version/authorization_version是实际持久化值，初始化0。更新责任在初始化说明中明确，但本轮没有修改/撤销API、撤销意图、实时会话或敏感提交前复核。StaffIdentity是内部事实，不生成sessionId/Token或安装CurrentPrincipal；默认Provider始终empty，即使数据库已有员工。

成熟实现/参数依据：[JDK PBEKeySpec](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/javax/crypto/spec/PBEKeySpec.html)、[OWASP密码存储](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)；安全函数依据：[PostgreSQL官方SECURITY DEFINER说明](https://www.postgresql.org/docs/17/sql-createfunction.html)。冻结依赖未更换。

## 5. 显式初始化与正式Store

实际入口：项目根目录 `scripts/backend-identity.sh migrate` / `scripts/backend-identity.sh bootstrap`，调用生产JAR中的独立main。构建、目标配置、权限、全部参数、Console不回显、受控stdin/0600文件、输出、重跑/故障行为见[完整操作说明](../development/IDENTITY-BOOTSTRAP.md)。没有bootstrap HTTP、Spring启动钩子、默认账号、密码参数或普通启动环境重置逻辑；实际Spring装配没有BootstrapWriter/IdentityBootstrap Bean。

bootstrap_tenant在单一数据库事务原子创建Tenant、管理员Employee、Role、9个明确TENANT授权、EmployeeRole，以及显式选择的首个Store/EmployeeStore。省略首店则不建任何默认门店。事务级advisory lock按规范化code串行化并由唯一约束兜底；中途故障七表独立查询均零残留。提交阶段连接中断可能无法确认结果，命令使用“未确认成功”的固定提示并要求独立核查，不能声称网络异常必定已回滚；该网络竞态本轮未注入验证。重跑存在租户即冲突，不重置密码、不补被移除权限、不修复半成品；并发同租户只有一个完整管理员。命令成功0/失败2，只输出非敏感UUID或固定安全消息，管理员是STAFF租户管理员。

正式PostgresStoreOwnershipReader生产装配唯一；测试确认不是内存事实Bean。通过已有ScopedPersistence固定投影、当前BUSINESS与ScopedTransaction同JPA连接GUC，读取可信tenant的ACTIVE Store。事实后既有Guard单独检查门店上限/本权限范围，不递归、不借用business最大范围；未找到/他租户/停用/未授权均404，依赖不可用503，无上下文拒绝。专用固定事实投影是登记例外，不是新Repository框架。

## 6. 命令、数量与证据

使用矩阵冻结Temurin，通过任务进程JAVA_HOME；没有修改全局Java/Node/pnpm。实际版本与Wrapper见[toolchain.json](evidence/P05-01/toolchain.json)。所有检查记录cwd/argv/退出码/起止时间/日志；汇总见[COMMAND-INDEX](evidence/P05-01/COMMAND-INDEX.json)。

| 检查 | 工作目录 / 命令 | 退出码 / 结果 / 等级 |
| --- | --- | --- |
| 最终完整回归 | /Users/kimwell/work/pet-platform/apps/backend；`./mvnw clean verify` | 0；258项，0失败/错误/跳过；COMPILED / RUNTIME_VERIFIED；[元数据](evidence/P05-01/verify-final.json)、[日志](evidence/P05-01/verify-final.log) |
| 单元/结构/默认启动/契约 | 同上，Surefire实际执行 | 132项0失败/错误/跳过，含新增6密码与2结构反例；不替代业务HTTP验收 |
| 真PG/Redis/生产JAR | 同上，Failsafe实际执行 | 126项0失败/错误/跳过，含27身份基础IT+1空history升级IT；真实资源RUNTIME_VERIFIED |
| 原测试逐项保全 | 原P04 XML与本轮XML比较 | 原222全部保留，新36；[test-results](evidence/P05-01/test-results.json) |
| 真实打包命令 | 集成测试从apps/backend调用`../../scripts/backend-identity.sh` | migrate重复0 / bootstrap成功0 / 重跑冲突2，密码只stdin；[命令输出](evidence/P05-01/verify-final-reports/p05-01-command-output.txt) |
| 独立数据库读取 | 27个身份测试@AfterEach独立owner连接 | 七表数量、实际runtime角色与事务外GUC EMPTY；[观察目录](evidence/P05-01/verify-final-reports/p05-01-observations/) |
| 生产包/日志/文件/链接 | 根目录；`python3 docs/testing/evidence/P05-01/final-audit.py` | 0；生产测试隔离、无固定编码凭据/测试账号、无秘密日志、链接及diff检查；[审计](evidence/P05-01/final-audit.json)、[产物](evidence/P05-01/artifact-audit.json) |

最终执行时间：2026-10-08 12:37:19～12:38:20（Asia/Shanghai）；Surefire132+Failsafe126=258。生产JAR SHA-256在artifact-audit.json记录，不用旧产物摘要。

首轮失败证据保留：first-regression是Flyway history权限/命令关闭连接结构登记；migration-smoke确认迁移后出现final @Repository代理和测试向量长度错误；second-regression为测试向量更新未生效；third-regression为打包独立迁移初始role读history和Redis健康测试缺独立迁移配置。分别修复实际角色绑定、明确白名单、正常可代理类、独立UTF-8向量和历史夹具配置；没有降低原安全断言或删除测试。fourth-regression已256全通过，补正式WITH CHECK与临时表遮蔽的两个针对性反例及权限注册元信息后258通过；随后收紧提交确认丢失的错误提示，再次完整clean verify仍258通过。原Redis故障注入日志中的连接超时是预期反例，未当成业务成功或跳过。首次最终审计在索引/自身结果文件生成前检查其链接而退出1，生成后重跑退出0；首轮结果保留在final-audit-first.json，未修改实际测试或门禁。

公开DTO/OpenAPI/paths未变化，原实际生产OpenAPI导出/协议回归通过。没有无理由重生成或修改前端产物；pnpm锁/三端代码不变。本轮没有依赖/POM/版本变动，RESOLVED沿用既有依赖；编译与本轮运行分别记录。本机psql不可用（host-client-check退出1），正式角色SQL已通过测试连接完整执行；操作说明使用明确目标容器内真实psql客户端，版本探针只证明工具可用，不代表真实目标已预配置。

## 7. 文件清单

完整新增/修改源文件：[changed-files.json](evidence/P05-01/changed-files.json)；证据文件在evidence/P05-01，最终XML、技术观察、命令/日志与审计均归档。

新增：platform下Tenant/Store映射、PlatformPermissions和正式Store Provider；identity下Employee/Role/三关系、IdentityNames/PermissionCatalog/PasswordService、认证候选/加载契约与AuthenticationJdbc、实际运行角色核查、独立bootstrap/migration命令；正式V1、角色预配置SQL、命令脚本；IdentityDatabaseSupport及密码/正式身份/空history升级测试；本验证与IDENTITY-BOOTSTRAP文档。

修改：shared.persistence仅配置校验及Store事实投影，application.yml独立迁移输入；结构规则新增窄化入口限制；旧默认EntityScan断言改为真实7实体而仍拒绝测试实体，独立迁移/运行凭据与专用旧夹具准确分开；原测试方法全部保留。README、认证/授权/多租户/模块/配置/持久化/迁移/本地开发/路线/决策/验收矩阵同步当前状态，P04历史报告、规则、冻结版本和ui不变。

## 8. G01～G15

| 门禁 | 结果 / 等级 | 证据 |
| --- | --- | --- |
| G01 P04事实/正式契约 | PASS / DOCUMENTED | 实际源码/全部指定事实源、P04原222 XML |
| G02 模型/迁移完整 | PASS / COMPILED / RUNTIME_VERIFIED | 七实体/V1，正式空库/空history升级/JPA validate |
| G03 租户唯一与关联 | PASS / RUNTIME_VERIFIED | 规范化/同租户冲突/跨租户同名/复合角色及门店FK拒绝 |
| G04 运行角色RLS | PASS / RUNTIME_VERIFIED | 无GUC/跨tenant不可读、WITH CHECK、非owner/升权/DDL/TRUNCATE拒绝 |
| G05 认证前窄化入口 | PASS / COMPILED / RUNTIME_VERIFIED | 函数owner/EXECUTE/PUBLIC/search_path/临时表反例，普通调用方ASM拒绝 |
| G06 密码安全/无明文 | PASS / RUNTIME_VERIFIED / COMPILED | JDK黄金向量/盐/正确错误/上限/坏编码、无明文列/序列化/日志/包固定凭据 |
| G07 正式Store接入 | PASS / RUNTIME_VERIFIED | 唯一生产Bean、正式DB/租户/ACTIVE/授权/503/无上下文 |
| G08 显式初始化 | PASS / RUNTIME_VERIFIED / COMPILED | 普通启动0账号/无bootstrap Bean，生产命令真实执行，拒绝密码参数 |
| G09 初始化原子回滚 | PASS / RUNTIME_VERIFIED | 中途触发器故障、独立连接七表全0 |
| G10 重跑不覆盖/扩大 | PASS / RUNTIME_VERIFIED | 哈希保持、已删除权限不补回、无额外账号 |
| G11 并发一致 | PASS / RUNTIME_VERIFIED | Latch两线程、一个完整成功，唯一管理员/关系 |
| G12 无会话无可信身份 | PASS / RUNTIME_VERIFIED | 有员工数据时Provider仍empty，无Token/Cookie/身份根安装 |
| G13 原/新增回归 | PASS / COMPILED / RUNTIME_VERIFIED | 原222逐项保全+36新增=258，0失败/错误/跳过 |
| G14 生产包秘密/夹具隔离 | PASS / COMPILED | package后测试及JAR全文/测试资源/账号字符串/编码凭据审计 |
| G15 文档与边界一致 | PASS / DOCUMENTED | 真实命令/角色/状态/版本责任/恢复限制、链接/文件保全审计 |

PASS仅针对本轮明确场景；不把文档、解析、编译或容器结果替代生产验收。

## 9. 未实现、未验证与下一合法任务

未实现：完整登录HTTP、Sa-Token会话签发/会话DAO/当前身份接入、Cookie/CSRF/载体跨域隔离、冻结登录频控、密码升级写回/修改/重置、账号/角色/门店管理CRUD、平台账号、客户微信身份、Organization、审计撤销意图、会话撤销与敏感提交前安全版本重验、异步执行前当前状态及权限交集。版本字段/加载存在不证明撤销已生效；短期异步快照限制继续沿用P04。

NOT_EXECUTED / NOT_VERIFIED：真实开发库和生产库角色预配置、正式迁移/真实管理员初始化、生产发布/部署、外部日志参数屏蔽、TLS/ACL/备份恢复、生产密码吞吐/限流容量、远程CI、其他OS、真实三端账号登录/真机业务验收。没有目标与真实初始化值，本轮只运行临时测试容器，不擅自创建本地管理员。

**P05-01 COMPLETE不代表登录可用，也不代表P05完成。** 路线图P05原真实会话/身份域/CSRF完成条件保持，P05整体IN_PROGRESS。核对路线后下一合法任务为 **P05-02：Sa-Token真实认证、会话与当前身份接入**；只建议，未自动执行。
