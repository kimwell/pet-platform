# 企业应用工程脚手架

项目直接在 `/Users/kimwell/work/pet-platform` 建设，既有ui保留原位，不创建enterprise-app-scaffold子目录，不使用Product Delivery OS。后端固定 `com.pet.platform`，三端结构见文档；模板不包含宠物/订单等行业业务。

P01 整体 COMPLETE，P02-01 COMPLETE；P02-02 已完成，工程基础验收见 [P02-02 验证报告](docs/testing/P02-02-VERIFICATION.md)。P02-02 COMPLETE，P02整体COMPLETE：后端、Web、基础设施真实启停及冻结微信工具npm/源码编译/模拟器入口通过；基础真机预览有用户反馈，完整P09验收尚未执行。P03-01 已 COMPLETE，公共响应/错误/trace/分页排序见 [P03-01 验证报告](docs/testing/P03-01-VERIFICATION.md)，P03-02、P03-03及P03整体 COMPLETE，完整标量/OpenAPI/类型与结构结果见 [P03-03](docs/testing/P03-03-VERIFICATION.md)。历史初始化结论见 [P02-01](docs/testing/P02-01-VERIFICATION.md)。三端最小工程、单锁、Wrapper、配置、CI 和本地 Compose 已建立；P03-02 已接入 PostgreSQL/JPA/Flyway 与持久化基础；认证、用户/角色/租户 CRUD、隔离、附件和消息业务尚未实现。当前是Git仓库，P04-01启动时已有提交且工作区干净；本轮不自动提交/推送/发布/部署。

## 阅读入口

- [开发规则](AGENTS.md)与[范围](docs/product/SCAFFOLD-SCOPE.md)
- [技术组合](docs/architecture/TECHNICAL-BASELINE.md)与[唯一精确版本矩阵](docs/development/VERSION-MATRIX.md)
- [工程结构](docs/architecture/PROJECT-STRUCTURE.md)与[模块边界](docs/architecture/MODULE-BOUNDARIES.md)
- [核心契约总索引](docs/contracts/CORE-CONTRACTS.md)：认证/授权/隔离/API/三端行为/附件/异步/生成的权威入口
- [决策记录](docs/development/DECISION-LOG.md)与[P01～P12路线](docs/development/ROADMAP.md)
- [P02～P12验收矩阵](docs/testing/ACCEPTANCE-MATRIX.md)
- [P01-01历史验证](docs/testing/P01-01-VERIFICATION.md)与[P01-02当前验证](docs/testing/P01-02-VERIFICATION.md)

实际目录与规划目录分别见工程结构；P01 旧报告的失败/未执行结果保持原样。完整环境设置、启动与限制见 [本地开发](docs/development/LOCAL-DEVELOPMENT.md)。精确版本以矩阵为唯一文档事实源；Node 由 .nvmrc 固定，pnpm 由 packageManager 固定，Java 使用矩阵中的 Temurin。

## 安装、启动与检查

```sh
pnpm install --frozen-lockfile
pnpm contracts:check
pnpm contracts:typecheck
pnpm check
pnpm build:web
pnpm dev:web
pnpm preview:web
```

Web 开发入口为 http://127.0.0.1:5173，构建后的预览为 http://127.0.0.1:4173；仅系统入口、错误和 NotFound，开发 /api 经同源代理。预览不使用开发代理，Vite preview 不代表生产部署；生产静态服务器须配置 SPA fallback。apps/admin-web/.env.example 可复制为同目录 .env.local；仅公开配置允许进入 VITE_*。

```sh
cd apps/backend
./mvnw clean verify
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Windows 使用 mvnw.cmd。无需全局 Maven，首次下载需网络；默认回环监听。后端当前没有业务 Controller，访问 / 返回标准错误信封404；/error仅为框架错误兜底。P03-02 已暴露 /actuator/health、/liveness、/readiness 工具格式健康端点，完整路径见本地开发；local 启动需要真实 PostgreSQL 和必填数据源配置。协议测试端点不会进入生产JAR。Java 不自动加载 .env；通过进程环境/外部 Spring 配置设置，prod 模板不提供开发回退或秘密值。

小程序在根执行 `pnpm check:miniprogram`，然后用矩阵中的微信开发者工具导入 apps/wechat-miniprogram，复制私有配置示例、填写实际 AppID，执行“工具 → 构建 npm”，再编译。依赖统一由根 pnpm 安装，产物为 miniprogram/miniprogram_npm；TypeScript 转换由工具插件负责，typecheck 不是小程序真实编译。没有实际 AppID或目标工具时，工具编译/预览不能声明 PASS。

本地 PostgreSQL、Redis、RabbitMQ 配置见 [基础设施说明](infra/local/README.md)：先复制 infra/local/.env.example 为同目录 .env，在本机填写专用凭据，再执行 `pnpm check:infra`、`pnpm dev:infra`、`pnpm stop:infra`。端口可调且只绑定回环，named volumes 保留数据；不复用生产凭据，不删除其他项目容器/卷。

当前已安装 PostgreSQL/JPA/Flyway/Actuator，未安装认证/消息客户端，未创建正式业务表或接口；OpenAPI生产paths为空，显式注册真实公共模型，packages/api-contracts已提供可重复生成的纯类型。Docker 与微信运行状态分别留证，静态检查不替代真实服务/设备验收。第三方声明检查边界沿用 P01 的 strict+skipLibCheck；CI 平台执行、Windows/Linux、真机和发布未被本地检查覆盖。

当前P02、P03均已完成工程阶段门禁，限定运行证据见各任务报告。后续Controller显式调用 `ApiResponse.success(dto)`，输入与分页用法见 [API](docs/contracts/API.md)、[分页](docs/contracts/PAGINATION.md) 和 [后端约定](docs/conventions/BACKEND.md)。P03-02 已 COMPLETE，详情见 [持久化验证报告](docs/testing/P03-02-VERIFICATION.md)。P03-03 COMPLETE，P04-01现已授权执行；最新状态见下文，下一P04-02不自动执行。认证/租户/三端业务、完整真机、远程CI、多OS仍未验证；基础真机预览保留P02用户反馈边界。

生成与同步规则见 [OpenAPI](docs/contracts/OPENAPI-GENERATION.md)：Web只import type，小程序从固定生成目录读取同源声明。local文档和UI回环可访问，prod两者关闭；测试环境配置和测试契约均不进入生产JAR。生产独立迁移任务与数据库角色权限归P04/部署验证，远程CI、多OS仍是已知未验证项，不因P03完成而写成运行PASS。

P04-01关闭时的历史结论：COMPLETE（114项测试0跳过），已建立内部可信身份Provider（默认无身份）、不可变租户上下文/数据范围、门店事实端口/Guard、同步REQUEST/ERROR和同步后台执行范围；验证与状态见 [P04-01](docs/testing/P04-01-VERIFICATION.md)。P04整体仍IN_PROGRESS；真实认证、正式门店数据源、JPA/RLS隔离、数据库越权、Redis与异步尚未实现。公开DTO/OpenAPI及三端产物未改变，下一合法任务为P04-02，只报告、不自动执行。

P04-02 COMPLETE（167项测试0失败/错误/跳过，原114项全部回归），已实现受控JPA、TENANT/STORES/SELF SQL策略、归属基类与事务范围绑定；独立PostgreSQL中的复合关联/RLS/角色/批次回滚验证见[P04-02](docs/testing/P04-02-VERIFICATION.md)。P04整体仍IN_PROGRESS；生产角色、真实认证/正式Store数据源、Redis与异步尚未验收。下一合法P04-03只报告、不自动执行；没有提交、推送或部署。
