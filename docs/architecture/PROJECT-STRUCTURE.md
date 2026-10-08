# 工程结构与三端边界

冻结日期：2026-10-07，P01-02；实际目录在 P02-01/P02-02/P03-01/P03-02/P03-03 更新。本文拥有目录、基础包和三端职责；模块详见 [模块边界](MODULE-BOUNDARIES.md)。实现状态见 [P02-01 验证](../testing/P02-01-VERIFICATION.md)及[P02-02](../testing/P02-02-VERIFICATION.md)，下文明确分开实际目录与后续规划。

## 当前实际结构

根目录 `/Users/kimwell/work/pet-platform` 为 Git 仓库，main 尚无提交；ui 保留原位，属于既有视觉成果，不是模板业务。实际结构如下，不列不存在的功能包：

```text
pet-platform/
├── AGENTS.md / README.md / .editorconfig / .gitignore / .gitattributes
├── .nvmrc / .npmrc / eslint.config.mjs
├── package.json / pnpm-workspace.yaml / pnpm-lock.yaml
├── .github/workflows/check.yml
├── apps/
│   ├── backend/
│   │   ├── pom.xml / mvnw / mvnw.cmd / .mvn/wrapper/maven-wrapper.properties
│   │   ├── src/main/java/com/pet/platform/Application.java
│   │   ├── src/main/java/com/pet/platform/shared/config/{EnvironmentSettings,ApiConfiguration}.java
│   │   ├── src/main/java/com/pet/platform/shared/api/{ApiResponse,ApiError,FieldErrorDetail,ErrorCode,PageQuery,PageResponse,SortRule,SortWhitelist,ProtocolErrorController}.java
│   │   ├── src/main/java/com/pet/platform/shared/exception/{BusinessException,ResourceNotFoundException,PermissionDeniedException,TenantAccessDeniedException,ConflictException,GlobalExceptionHandler,ValidationErrors}.java
│   │   ├── src/main/java/com/pet/platform/shared/observability/{TraceContext,TraceFilter}.java
│   │   ├── src/main/java/com/pet/platform/shared/persistence/{BaseEntity,PersistenceConfiguration,DatabaseConfigurationRules,JpaPageAdapter}.java
│   │   ├── src/main/resources/db/migration/README.md
│   │   ├── src/test/java/com/pet/testing/{PostgresIntegrationSupport,persistence/*}.java
│   │   ├── src/test/resources/persistence-migrations/V1__shared_persistence_probe.sql
│   │   ├── src/main/resources/application{,-local,-prod}.yml
│   │   ├── src/test/resources/application-test.yml
│   │   └── src/test/java/com/pet/platform/{ApplicationTest.java,protocol/{ProtocolFixtures,ApiProtocolTest,ApiProtocolIT}.java}
│   ├── admin-web/
│   │   ├── package.json / tsconfig.json / vite.config.ts / dev-config.ts / index.html / .env.example
│   │   └── src/{main.tsx,app/providers,app/router,app/layout,shared,styles}
│   └── wechat-miniprogram/
│       ├── package.json / tsconfig.json / project.config.json
│       ├── project.private.config.json.example
│       └── miniprogram/{app.ts,app.json,app.wxss,sitemap.json,pages/system/entry}
├── packages/api-contracts/  # 实际OpenAPI快照、生成声明、纯类型出口与严格检查
├── scripts/{check-repository.mjs,check-miniprogram.mjs,local-infra.mjs}
├── infra/local/{docker-compose.yml,.env.example,README.md}
├── docs/
└── ui/
```

后端另有 .env.example；Web/小程序入口、错误与路由只承载工程验证。构建生成的 target、dist、node_modules、miniprogram_npm 与本地 .local-data 不属于提交结构。已有正式应用公共OpenAPI与纯类型（当前无正式业务路径），测试文档单独隔离；没有templates、业务features、身份/租户/附件/消息源码或正式业务迁移SQL。P03-02 只有持久化技术基础和测试迁移，未创建正式 Entity/Repository。P01 临时探针归档保留历史证据，不冒充正式实现。

## 目标根目录（规划）

```text
pet-platform/
├── AGENTS.md / README.md
├── package.json / pnpm-workspace.yaml / pnpm-lock.yaml
├── apps/
│   ├── backend/             # 唯一 pom.xml、mvnw、mvnw.cmd、.mvn
│   ├── admin-web/           # React，平台/员工分别入口
│   └── wechat-miniprogram/  # 一个原生工程，客户/员工独立会话
├── packages/api-contracts/  # 生成类型、协议快照、纯类型入口
├── templates/              # P11 三端模板
├── scripts/                # P02起按需创建生成/检查/初始化脚本
├── infra/local/            # PostgreSQL/Redis/可选RabbitMQ/文件卷
├── docs/
└── ui/                     # 保留既有成果
```

前端仅 pnpm workspace、一份根锁文件；小程序组件 npm 构建不构成另一包管理流程。后端不增加聚合 POM/其他 Module。精确版本只见 [版本矩阵](../development/VERSION-MATRIX.md)。

## 后端（规划）

固定基础包 `com.pet.platform`、源目录 `apps/backend/src/main/java/com/pet/platform/`、启动类 `com.pet.platform.Application`。其下 shared、platform、identity、customeridentity、attachment、audit、notification、modules。资源 `src/main/resources`，协议测试 `src/test/java/com/pet/platform/`，持久化技术夹具 `src/test/java/com/pet/testing/`，后者在生产扫描包之外。

有真实行为时业务模块采用 api/application/domain/infrastructure；shared 按 api/exception/config/security/tenancy/persistence/redis/messaging/storage/wechat/observability 等职责按需建包。modules/<实际模块> 是新项目扩展点；不创建完整空目录树、空类或宠物/订单等行业业务。

P11 可为其他新项目显式替换名称/工程标识/package/源码路径，当前项目固定包名不变。

## Web（规划）

```text
src/
├── app/{router,providers,guards,layout}
├── features/<业务模块>/{api,pages,components}
└── shared/{api,auth,config,hooks,table,forms,formatters}
```

TanStack Router **代码路由**，显式模块注册、dynamic import 分包，不增加文件路由插件。平台入口 /platform，员工入口 /admin；同一静态应用，独立身份与缓存生命周期。shared/table 只放 URL/选择/列配置行为工具，页面直接使用官方 Ant Design Table/Form，不创建万能 BaseTable/透传 Wrapper。行为与状态见 [Web页面](../conventions/WEB-PAGES.md)、[Web状态](../conventions/WEB-STATE.md)。

## 小程序（规划）

```text
miniprogram/
├── pages/{system,customer,staff}
├── services/
├── session/
├── navigation/
├── config/
├── components/
├── behaviors/
├── utils/
└── types/generated/
```

一个工程；初始化选择 CUSTOMER/STAFF/BOTH，默认 BOTH，能力选择不等于登录身份。默认 WebView 渲染，不增加 Skyline 条件。两身份域分别普通页面及 TDesign 导航，不用一个原生 tabBar 混合身份。TypeScript 转换和 npm 组件构建于 P02/P09 落实，见 [小程序页面](../conventions/MINIPROGRAM-PAGES.md)。

## 共享与阶段

packages/api-contracts 只承载纯类型，不含 React/DOM/fetch/wx.request/Cookie/Token存储/状态库。Web 引用 workspace 类型包，小程序构建前复制声明并验摘要，见 [生成流程](../contracts/OPENAPI-GENERATION.md)。

P02-01 已初始化上述最小工程壳与工具；P02-02已完成真实启停/配置及冻结微信工具构建/编译验收。P03-01公共协议已COMPLETE（[报告](../testing/P03-01-VERIFICATION.md)），新增/error仅为框架安全兜底，协议Controller仅在测试配置。P03-02持久化、P03-03标量/OpenAPI/类型与结构检查已COMPLETE，P03整体COMPLETE，下一合法P04不自动执行。P03 协议/数据，P04 隔离，P05 身份，P06～P10 页面与能力，P11 模板，P12 全新项目验收。规划目录只在有实际行为时创建，不用空目录/空类充数。

P03-03实际代码新增shared.serialization/openapi，测试契约与字节码夹具仅在src/test；小程序types/generated为独占生成目录，手写消费在types/contracts.ts，Web消费在src/contracts.ts。完整目录与生成范围见 [生成](../contracts/OPENAPI-GENERATION.md)、[验证](../testing/P03-03-VERIFICATION.md)。
