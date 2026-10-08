# 生产迁移目录

P05-01首次正式V1建立Tenant/Store/Employee/Role及三关系、RLS和受限函数；此前目录仅有说明。迁移前由管理员显式预配置身份能力角色，迁移执行身份与应用运行身份独立。
命名使用全应用唯一递增V<整数>__<module>_<英文含义>.sql，已执行版本不改写，不自动baseline/repair/clean。
prod由独立命令迁移，Application只JPA validate；local启用Flyway需独立迁移凭据。测试migration保持src/test独立，不能复制到此目录。
准确命令与角色边界见仓库docs/development/IDENTITY-BOOTSTRAP.md，迁移规则见docs/conventions/DATABASE-MIGRATION.md。
