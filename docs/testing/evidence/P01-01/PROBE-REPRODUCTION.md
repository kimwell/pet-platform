# 临时探针复现说明

这是 P01-01 测试证据，不是正式应用工程。源码、精确依赖、单份 pnpm 锁文件和 Maven Wrapper 在 temporary-probes.tar.gz；下载工具链与 node_modules/target/dist 没有归档。

原始工作目录见 all-command-records.json。执行脚本保存原始临时绝对路径；复现时应创建新的明确临时目录并替换路径，不能直接在正式 apps/ 下解压初始化。使用矩阵固定的 JDK/Node/pnpm，读取 Wrapper properties 的精确 Maven URL 和 SHA-256；Maven 使用临时 settings.xml 和临时 m2，避免引入本机全局仓库配置。

前端只在 workspace 用 pnpm install/frozen-lockfile。miniprogram-ci 2.1.48 通过独立 pnpm dlx 环境加载，其内部 ESLint8 peer 不加入主 workspace；归档 pack-api.cjs 的 require 位置应改成新独立工具目录。packNpmManually 的 miniprogramNpmDistDir 指向小程序根，API 会自动生成 miniprogram_npm；必须检查具体组件 JS/JSON/WXML/WXSS 产物，不能只看进程退出0。

应用源码采用 strict=true、skipLibCheck=true；全面 vendor d.ts 检查失败另存，不宣称通过。JVM 探针排除 JDBC/JPA/Flyway 自动连接，Sa-Token 多身份测试临时切换内存 DAO 后恢复；不调用真实微信，不启动数据库、Redis、RabbitMQ 或 Testcontainers 容器。

当前正式文档根为 `/Users/kimwell/work/pet-platform`；原始证据中的同级目录属于历史来源。当前基础包/启动类只在文档冻结，中性探针包不替代正式工程初始化或启动验收。
