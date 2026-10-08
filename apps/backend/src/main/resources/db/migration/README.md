# 生产迁移目录

P05-01首次正式V1建立Tenant/Store/Employee/Role及三关系、RLS和受限函数；此前目录仅有说明。迁移前由管理员显式预配置身份能力角色，迁移执行身份与应用运行身份独立。
命名使用全应用唯一递增V<整数>__<module>_<英文含义>.sql，已执行版本不改写，不自动baseline/repair/clean。
prod由独立命令迁移，Application只JPA validate；local启用Flyway需独立迁移凭据。测试migration保持src/test独立，不能复制到此目录。
准确命令与角色边界见仓库docs/development/IDENTITY-BOOTSTRAP.md，迁移规则见docs/conventions/DATABASE-MIGRATION.md。

P05-03追加V2：员工强制改密/保留标记、两张安全记录/清理意图表、受限行锁和授权投影。V1保持原字节/checksum；没有迁移新权限到既有角色。升级时仍使用独立迁移身份；V1已初始化库升级与空库迁移测试见docs/testing/P05-03-VERIFICATION.md。未对用户现有数据库执行迁移。

P05-04追加V3，pet_control独立账号/权限/标记/事件/清理意图及固定受限函数。旧V1/V2不可改，先由DB管理员在明确目标执行追加infra/database/provision-platform-roles.sql，再独立迁移；普通启动不初始化。详见docs/development/PLATFORM-BOOTSTRAP.md（项目根目录）。
