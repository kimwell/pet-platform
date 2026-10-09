# 企业应用工程脚手架

项目直接在 `/Users/kimwell/work/pet-platform` 建设，既有ui保留原位，不创建enterprise-app-scaffold子目录，不使用Product Delivery OS。后端固定 `com.pet.platform`，三端结构见文档；模板不包含宠物/订单等行业业务。

**最新：P07-02 COMPLETE；P07 IN_PROGRESS；G01～G14 PASS。** `/admin/identity/users` 接入正式员工列表，完成URL校验/历史恢复、Form草稿、服务端分页与六字段排序、精确total安全适配和身份范围缓存清理。Web188项、79项真实浏览器检查、13项确定性组件场景和4项390px补验通过；命令、原始失败、资源恢复及边界见[P07-02验证](docs/testing/P07-02-VERIFICATION.md)和[列表接入规则](docs/conventions/WEB-LIST-PAGES.md)。下一建议P07-03员工详情页与列表返回状态，未执行；未提交、推送或部署。

**P07-01历史：P07-01 COMPLETE；P07 IN_PROGRESS；G01～G14 PASS。** 正式列表/详情读取接口、独立权限、TENANT/STORES/SELF、六字段DTO完成；完整后端455项为P07-01历史，本轮后端未改，只执行10项契约导出/模型检查，详见[员工读取契约](docs/contracts/EMPLOYEE-MANAGEMENT.md)与[P07-01验证](docs/testing/P07-01-VERIFICATION.md)。

**P06-03历史：P06-03 COMPLETE；P06 COMPLETE；G01～G14 PASS。** 25项覆盖矩阵对照原P06路线，补齐真实网络/取消、跨标签与限定窄屏/键盘验收，完成最小修复；5个文件127项Web测试、六项检查及diff/保全审计通过。见[P06-03验证](docs/testing/P06-03-VERIFICATION.md)与[P06总验收及P07接入](docs/testing/P06-ACCEPTANCE.md)。421项完整后端为历史，本轮只运行10项契约导出/模型检查。P07 NOT_STARTED，首项建议为员工管理后端查询与授权契约，不自动执行、提交、推送或部署。

以下为此前阶段历史记录：P06-01 COMPLETE、P05 COMPLETE；421项为最近完整后端基线，本轮没有后端/公开契约修改或完整后端回归。下方P01～P05旧描述保留阶段历史。P01 整体 COMPLETE，P02-01 COMPLETE；P02-02 已完成，工程基础验收见 [P02-02 验证报告](docs/testing/P02-02-VERIFICATION.md)。P02-02 COMPLETE，P02整体COMPLETE：后端、Web、基础设施真实启停及冻结微信工具npm/源码编译/模拟器入口通过；基础真机预览有用户反馈，完整P09验收尚未执行。P03-01 已 COMPLETE，公共响应/错误/trace/分页排序见 [P03-01 验证报告](docs/testing/P03-01-VERIFICATION.md)，P03-02、P03-03及P03整体 COMPLETE，完整标量/OpenAPI/类型与结构结果见 [P03-03](docs/testing/P03-03-VERIFICATION.md)。历史初始化结论见 [P02-01](docs/testing/P02-01-VERIFICATION.md)。三端最小工程、单锁、Wrapper、配置、CI 和本地 Compose 已建立；P03-02 已接入 PostgreSQL/JPA/Flyway 与持久化基础，P04隔离基础已完成；本轮已实现客户微信认证后端，真实外部认证仍待验证，完整用户/角色/租户 CRUD、附件和消息业务尚未实现。当前是Git仓库，P04-01启动时已有提交且工作区干净；本轮不自动提交/推送/发布/部署。

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

Web 开发入口为 http://127.0.0.1:5173，构建后的预览为 http://127.0.0.1:4173；当前包含系统入口、/admin/login、/platform/login、对应受保护身份壳、/admin/security、/platform/security、/admin/identity/users、错误和NotFound，开发 /api 经同源代理。预览不使用开发代理，Vite preview 不代表生产部署；生产静态服务器须配置 SPA fallback。apps/admin-web/.env.example 可复制为同目录 .env.local；仅公开配置允许进入 VITE_*。

```sh
cd apps/backend
./mvnw clean verify
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Windows 使用 mvnw.cmd。无需全局 Maven，首次下载需网络；默认回环监听。后端沿用既有三身份认证及凭据安全路径，新增两条正式员工读取路径，详见[员工读取契约](docs/contracts/EMPLOYEE-MANAGEMENT.md)；其他企业管理Controller按后续任务开发；访问 / 返回标准错误信封404；/error仅为框架错误兜底。P03-02 已暴露 /actuator/health、/liveness、/readiness 工具格式健康端点，完整路径见本地开发；local 启动需要真实 PostgreSQL 和必填数据源配置。协议测试端点不会进入生产JAR。Java 不自动加载 .env；通过进程环境/外部 Spring 配置设置，prod 模板不提供开发回退或秘密值。

小程序在根执行 `pnpm check:miniprogram`，然后用矩阵中的微信开发者工具导入 apps/wechat-miniprogram，复制私有配置示例、填写实际 AppID，执行“工具 → 构建 npm”，再编译。依赖统一由根 pnpm 安装，产物为 miniprogram/miniprogram_npm；TypeScript 转换由工具插件负责，typecheck 不是小程序真实编译。没有实际 AppID或目标工具时，工具编译/预览不能声明 PASS。

本地 PostgreSQL、Redis、RabbitMQ 配置见 [基础设施说明](infra/local/README.md)：先复制 infra/local/.env.example 为同目录 .env，在本机填写专用凭据，再执行 `pnpm check:infra`、`pnpm dev:infra`、`pnpm stop:infra`。端口可调且只绑定回环，named volumes 保留数据；不复用生产凭据，不删除其他项目容器/卷。

P04关闭时已安装 PostgreSQL/JPA/Flyway/Actuator和Redis资源客户端，未安装认证/消息客户端，当时未创建正式业务表或接口；OpenAPI生产paths为空，显式注册真实公共模型，packages/api-contracts已提供可重复生成的纯类型。Docker 与微信运行状态分别留证，静态检查不替代真实服务/设备验收。第三方声明检查边界沿用 P01 的 strict+skipLibCheck；CI 平台执行、Windows/Linux、真机和发布未被本地检查覆盖。

当前P02、P03均已完成工程阶段门禁，限定运行证据见各任务报告。后续Controller显式调用 `ApiResponse.success(dto)`，输入与分页用法见 [API](docs/contracts/API.md)、[分页](docs/contracts/PAGINATION.md) 和 [后端约定](docs/conventions/BACKEND.md)。P03-02 已 COMPLETE，详情见 [持久化验证报告](docs/testing/P03-02-VERIFICATION.md)。P03-03 COMPLETE，P04-01现已授权执行；最新状态见下文，下一P04-02不自动执行。认证/租户/三端业务、完整真机、远程CI、多OS仍未验证；基础真机预览保留P02用户反馈边界。

生成与同步规则见 [OpenAPI](docs/contracts/OPENAPI-GENERATION.md)：Web只import type，小程序从固定生成目录读取同源声明。local文档和UI回环可访问，prod两者关闭；测试环境配置和测试契约均不进入生产JAR。生产独立迁移任务与数据库角色权限归P04/部署验证，远程CI、多OS仍是已知未验证项，不因P03完成而写成运行PASS。

P04-01关闭时的历史结论：COMPLETE（114项测试0跳过），已建立内部可信身份Provider（默认无身份）、不可变租户上下文/数据范围、门店事实端口/Guard、同步REQUEST/ERROR和同步后台执行范围；验证与状态见 [P04-01](docs/testing/P04-01-VERIFICATION.md)。P04整体仍IN_PROGRESS；真实认证、正式门店数据源、JPA/RLS隔离、数据库越权、Redis与异步尚未实现。公开DTO/OpenAPI及三端产物未改变，下一合法任务为P04-02，只报告、不自动执行。

P04-02 COMPLETE（167项测试0失败/错误/跳过，原114项全部回归），已实现受控JPA、TENANT/STORES/SELF SQL策略、归属基类与事务范围绑定；独立PostgreSQL中的复合关联/RLS/角色/批次回滚验证见[P04-02](docs/testing/P04-02-VERIFICATION.md)。P04整体仍IN_PROGRESS；生产角色、真实认证/正式Store数据源、Redis与异步尚未验收。下一合法P04-03只报告、不自动执行；没有提交、推送或部署。


P04-03与P04整体 COMPLETE，当前范围与限制见[P04-03验证](docs/testing/P04-03-VERIFICATION.md)、[P04总验收](docs/testing/P04-ACCEPTANCE.md)。Redis受控原始资源空间与有限进程内任务已实现，生产默认仍无真实身份/Store事实，权限过滤结果禁止缓存，短期快照未处理权威撤销重验。启动需同时提供PostgreSQL与Redis配置，readiness包含db/redis；用法见[Redis](docs/conventions/REDIS.md)、[异步执行](docs/conventions/ASYNC-EXECUTION.md)、[本地开发](docs/development/LOCAL-DEVELOPMENT.md)。下一合法P05-01先处理正式身份数据基础、初始化与认证依赖，P05仍NOT_STARTED；没有提交、推送或部署。

## P05-01 当前身份基础（2026-10-08）

最新范围为正式身份数据基础：七表及首次正式V1、受限认证前函数、PBKDF2、权威员工权限加载、正式Store事实Provider和独立初始化命令。前述各阶段“没有正式表/默认Store事实”的陈述保留为历史；当前准确状态见[P05-01验证](docs/testing/P05-01-VERIFICATION.md)。登录HTTP、Sa-Token会话、Cookie/CSRF仍未实现，Provider无会话仍empty，租户管理员不是平台管理员。

正式模型启动需要受限运行身份，local启用迁移另需PET_MIGRATION_DATABASE_*；不能继续用Compose管理员作为应用运行账号。管理员角色预配置、实际独立迁移/初始化命令及密码安全输入见[身份初始化](docs/development/IDENTITY-BOOTSTRAP.md)。本轮没有操作日常/生产库或创建真实管理员。P05整体IN_PROGRESS，下一P05-02只建议、不自动执行；不提交、推送或部署。

## P05-02 当前认证（2026-10-08）

正式STAFF Sa-Token/Redis会话、员工账号密码登录、生产CurrentPrincipalProvider已接入，GET csrf、POST login/token/login、GET me、POST logout实际路径见[身份契约](docs/contracts/IDENTITY.md#p05-02-正式staff接口清单2026-10-08)。Web为同源HttpOnly Cookie+服务器同步CSRF，小程序为独立设备Header Token；权限/门店每请求从正式数据库重载，身份/RLS链路复用已有范围与受限函数，客户端不能提供可信tenantId。真实身份异步任务目前禁止提交。

292项后端测试、实际本地浏览器、跨JVM共享Redis会话、生产安全配置反例及OpenAPI/三端类型通过；全部命令、失败历史、门禁、产物和限制见[P05-02验证](docs/testing/P05-02-VERIFICATION.md)。前期“登录尚未实现/默认Provider无身份/生产paths为空”为历史状态。没有Web登录页、小程序页面/请求层、平台/客户登录或完整管理CRUD；生产TLS/代理/ACL/HA、远程CI/部署与完整设备验收未验证。P05仍IN_PROGRESS；下一建议P05-03需后续授权，不自动执行、不提交/推送/部署。


## P05-03 当前安全闭环（2026-10-08）

最新范围包含本人修改密码/退出全部、授权管理员重置密码/撤销全部四项接口，详见[身份契约](docs/contracts/IDENTITY.md#p05-03-员工凭据与会话安全接口2026-10-08)。追加V2；复用security_version作为持久化失效代际，每请求检查；Redis提交后按员工终端索引清理旧代际，失败留PENDING记录，由该员工下一次合法me/安全操作重试。重置使用操作者明确提供的临时密码并要求目标登录后先改密；无固定密码和外部交付服务。Web敏感写保持CSRF/来源校验，成功本人操作清当前Cookie；小程序按契约清对应员工Token与CSRF状态。

真实会话来源的进程内异步现已开放执行前权威重验、设备检查与捕获范围交集；前节“禁止提交”保留为P05-02历史。P05-03 COMPLETE：328项0失败/错误/跳过，G01～G15 PASS；完整验证/门禁见[P05-03报告](docs/testing/P05-03-VERIFICATION.md)。P05整体仍IN_PROGRESS；下一建议平台管理员身份基础任务尚未授权，不自动执行。未开发前端页面/请求层或完整管理CRUD；没有操作用户开发/生产库、提交、推送或部署。

## P05-05 客户微信认证

正式CustomerSubject/WechatBinding、追加V4和受限角色、服务端入口配置/冻结WxJava Gateway、四客户接口、CUSTOMER Sa/Redis/SELF与三域隔离已落地。三端只生成类型，客户身份/会话HTTP以[IDENTITY](docs/contracts/IDENTITY.md#p05-05-客户正式接口2026-10-08)为准。应用默认关闭微信且无AppID/秘密默认；真实联调需安全外部配置和新鲜code。[P05-05报告](docs/testing/P05-05-VERIFICATION.md)区分自动化真实PG/Redis与外部Gateway替身、真实微信未执行、失败历史/门禁；[P05总验收](docs/testing/P05-ACCEPTANCE.md)保留剩余阶段限制。

## P06-03 综合验收（2026-10-09）

本轮已核对原P06/A06条件、补齐原生跨标签/连接失败/取消/390px/键盘及P07接入，修复入口、表单定位、菜单焦点、敏感草稿/退出通知和窄屏页头。P06-03/P06 COMPLETE，G01～G14 PASS，见[P06-03](docs/testing/P06-03-VERIFICATION.md)、[P06总验收及P07接入](docs/testing/P06-ACCEPTANCE.md)。本轮没有后端/契约/依赖改动，421项完整后端为历史；P07未启动，不提交/推送/部署。各早期IN_PROGRESS及P06-02的“下一P06-03”为当时历史，不删除或覆盖旧报告。
