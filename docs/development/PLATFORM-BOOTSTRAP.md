# 平台管理员独立初始化与控制面

P05-04，2026-10-08。平台账号的凭据、安全事务和认证由 `identity` 拥有；Tenant/Store目录仍由 `platform` 拥有。本任务明确允许账号放在identity，集中复用密码与认证机制，不增加platform对identity内部实现的依赖。平台管理员不是STAFF租户管理员，没有tenantId，不继承TenantScopedEntity，没有租户业务全权。

正式账号为 `pet_control.platform_account`：UUID、全局唯一规范化login_name、display_name、PBKDF2哈希、ACTIVE/DISABLED、security_version、authorization_version、资源version、UTC毫秒审计。IdentityNames规范化登录名：strip首尾空白、ASCII小写，1～64位字母数字及点/下划线/连字符，首位字母数字；数据库CHECK/UNIQUE兜底。密码复用原PasswordService，不trim、不改大小写、不归一化或截断；12～128 Unicode码点，最多256 UTF-16单位。

权限只有 `platform:session:manage`（me/当前退出/全退出）、`platform:credential:change`（本人改密）、`platform:redis:operate`（P04已有受限平台Redis端口）。platform_permission固定代码约束和账号关系，不建设平台角色CRUD/RBAC系统，不包含租户业务权限或通配符。初始账号明确授三项，普通启动不补授权。后续修改授权必须锁账号、递增authorization_version；停用/凭据撤销须同事务更新security_version及清理意图。

## 数据库与升级顺序

既有正式V1/V2不改，追加V3。空库先由数据库管理员执行原 `infra/database/provision-identity-roles.sql`，再执行追加的 `infra/database/provision-platform-roles.sql`；已有V2库仅执行追加角色脚本。角色已存在时失败，不能盲目重跑或自动覆盖。使用管理员既有受控连接，在明确目标库执行文件；本轮只在临时容器执行，没有迁移日常/生产库。

新能力角色均NOLOGIN、无SUPERUSER/BYPASSRLS/CREATEROLE/CREATEDB：

| 角色 | 能力与约束 |
| --- | --- |
| pet_migrator | 原独立迁移能力，V3建pet_control schema/表；可SET到两个新函数owner，运行进程不得持有 |
| pet_runtime | 原运行能力；pet_control仅USAGE及七个固定认证/本人安全函数EXECUTE；无平台表SELECT/DML/TRUNCATE、无bootstrap EXECUTE |
| pet_platform_auth_owner | 非表owner、NOLOGIN；限定控制面表读和必要凭据/意图/事件写，仅固定函数；不取得任何租户表特权 |
| pet_platform_bootstrap_owner | NOLOGIN；账号仅SELECT(id)/INSERT、权限INSERT、初始化记录SELECT/INSERT，无UPDATE/DELETE；只拥有首次创建函数 |
| pet_platform_bootstrap | 仅首次创建函数EXECUTE；与pet_bootstrap租户初始化分离 |

pet_control五表全部ENABLE/FORCE RLS，运行角色没有直接表策略；专用函数owner有具体表策略。函数固定search_path=pg_catalog,pg_temp，表名显式schema限定，撤销PUBLIC EXECUTE。认证前仅按规范化loginName取最小候选（id/hash/security_version），没有列表/分页/导出。已验证会话按可信主体ID加载当前状态与明确权限；本人安全事务用独立事务局部pet.platform_id，不用tenant GUC，不关闭租户RLS。

管理员需独立建立平台初始化登录身份，成员配置为 `pet_platform_bootstrap WITH INHERIT FALSE, SET TRUE`，不能同时持有pet_bootstrap、迁移、函数owner或高权限。应用身份只继承pet_runtime，不能SET到能力owner。独立命令核对实际登录成员/高权限；应用启动扩展IdentityRuntimePermissions检查schema CREATE、平台密码列/表读取、bootstrap执行、高角色和FORCE RLS/TRUNCATE。

迁移使用既有 `PET_MIGRATION_DATABASE_*` 及 `scripts/backend-identity.sh migrate`。prod普通启动只JPA validate；local显式启用迁移仍要求独立迁移凭据。初始化另用 `PET_PLATFORM_BOOTSTRAP_DATABASE_URL/USERNAME/PASSWORD`；不回退运行、租户bootstrap或迁移凭据。URL不含凭据/参数；数据库秘密经受控进程环境/秘密管理工具提供，不写参数、仓库或报告。

## 独立命令

先在apps/backend使用冻结JDK执行 `./mvnw clean verify`。项目根目录执行：

```sh
scripts/backend-identity.sh platform-bootstrap \
  --admin-login '<平台登录名>' --admin-name '<平台显示名>'
```

仅Console不回显输入密码；没有Console则明确失败。自动化显式使用受控stdin：

```sh
scripts/backend-identity.sh platform-bootstrap \
  --admin-login '<平台登录名>' --admin-name '<平台显示名>' \
  --password-stdin < "$PLATFORM_INITIAL_PASSWORD_FILE"
```

文件由既有秘密管理工具准备、0600且不进Git；不使用echo/明文参数/固定默认密码。stdin只去除LF/CRLF行结束符，保留密码空格；损坏UTF-8/超长失败，缓冲结束清零。初始密码不来自环境开关，普通启动不创建或重置账号，没有公共HTTP bootstrap。

命令只允许**全库首次创建一个平台管理员**。事务级固定advisory lock、全局login唯一、singleton=true初始化标记共同保护并发。账号、三条权限、标记同事务；任一失败全部回滚。已有任意平台账号或初始化标记（包括停用账号/未知半成品）即冲突；重复不重置密码、不补权限、不新增第二账号。无密码恢复、重新启用或离线清标记后门。成功退出0，仅输出principalId；失败退出2及固定中文结果。提交网络故障可能无法确认结果，应核查目标状态，不能把重跑当恢复方案。

测试使用临时PostgreSQL/Redis、真实独立初始化路径和生产JAR脚本；未提供真实本地初始化值，所以没有创建实际本地平台管理员。准确命令、结果、失败历史及浏览器范围见[P05-04验证](../testing/P05-04-VERIFICATION.md)。

## 平台会话与操作

接口以[IDENTITY](../contracts/IDENTITY.md#p05-04-平台正式接口2026-10-08)为唯一清单。PLATFORM只WEB Cookie，生产__Secure-pet_platform_sid/pre、Path=/api/platform/、HttpOnly/Secure/SameSite=Lax、无Domain；local HTTP使用pet_dev_platform_sid/pre。登录前独立服务器CSRF预会话，写请求均校验CSRF及固定Origin/同源Referer；登录轮换、退出销毁。STAFF Cookie可同时存在，按服务端路径只解析本域Cookie，无关Cookie忽略；本域重复/冲突拒绝。没有Header选择loginType或平台小程序Token入口。

Sa-Token loginType=platform，键pet:<env>:platform:platform:*；辅助频控/pre/账号锁为pet:<env>:auth:platform:*，与staff完全分离。两域共用SaIdentitySessions和WebCookieSecurity，保留STAFF已有线格式/期限。平台8h绝对、30min闲置、最多5设备；me/CSRF/auth维护不续闲置。频控复用原原子Lua：IP60/5min、账号摘要10/15min；敏感操作IP60/5min、本人/目标8/5min，成功亦计数，不永久锁账号；依赖故障503关闭，不降级。

本人改密/全部退出需旧密码重新确认及明确平台权限。行锁事务重载状态/版本/权限、确认设备有效，变更凭据（如适用）、安全/资源版本+1、成功记录、清理意图，提交前再核对；仅本人，无管理员重置他人接口。数据库提交后按security<cutoff精确清平台终端，旧清理不误删新代际，也不影响同UUID STAFF。Redis失败200/X-Session-Cleanup=PENDING，旧会话仍因DB代际失配不可用；合法me/后续安全操作补偿。没有定时/MQ/Outbox补偿，未再登录主体可保持PENDING到后续重试或Redis自然TTL，不提前宣称物理完成。

平台请求CurrentPrincipal.tenantId=null、grants/门店为空，TenantContextFilter不建立租户上下文；客户端tenantId不能改变它。原租户持久化/TrustedTenantExecutor/TenantTaskExecutor默认拒绝，没有runAsTenant/impersonate/跨租户业务查询、导出、平台异步框架或虚构tenantId。PlatformRedisAccess仅真实PLATFORM且platform:redis:operate使用，不能靠STAFF角色名进入。

最小平台记录是独立platform_security_event：PLATFORM主体类型、可识别actor/target、动作、结果、时间、traceId，无tenantId/密码/哈希/Token/CSRF。不存在/错误登录统一LOGIN_FAILED，失败记录不存登录名线索；敏感成功记录同事务，业务失败独立记录。普通租户无该表查询权。P08后续整合追加记录，不能把租户事件表约束简单全局放开。当前退出的Redis删除与随后DB记录不构成跨资源原子事务，503意味着结果未确认，按实际会话状态核查；敏感安全代际的原子承诺是数据库事务。

部署前仍需实际目标角色预配置/迁移、HTTPS与固定来源、独立秘密/数据库日志参数屏蔽、Redis TLS/ACL/持久化/HA、容量与恢复检查；现有server.forward-headers-strategy=none，不能直接开启可信代理头。没有平台账号管理页面/邀请体系、完整租户CRUD、客户微信认证、模拟登录、跨租户导出或生产部署。
