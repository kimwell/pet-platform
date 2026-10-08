# 生产迁移目录

本目录只存正式结构迁移，当前没有正式业务表，因此不放 SQL。
命名沿用全应用唯一递增编号 `V<整数>__<module>_<英文含义>.sql`。
已执行版本不得改名或修改；不得自动 baseline、repair、clean 或乱序补迁移。
local 启动迁移后才进行 JPA validate；prod 先由独立任务使用迁移角色执行迁移，运行角色仅 validate。
测试迁移位于 `src/test/resources/persistence-migrations/`，由测试显式配置，不能复制到本目录。
完整规则见仓库 `docs/conventions/DATABASE-MIGRATION.md`。
