# 精确技术版本矩阵

查询日期：2026-10-07（Asia/Shanghai）。这是项目版本的唯一文档事实源。BOM 管理项保留 BOM，不在正式 POM 中逐项重复覆盖；实际值来自 [依赖树](../testing/evidence/P01-01/backend-dependency-tree.txt) 与 [有效 POM](../testing/evidence/P01-01/backend-effective-pom.xml)。直接 npm 版本与 engine/peer/发布时间见 [官方包元数据快照](../testing/evidence/P01-01/selected-npm-metadata.json)。

DOCUMENTED=官方资料确认；RESOLVED=实际依赖/分发/manifest 解析；COMPILED=实际类型/Java 编译或构建；RUNTIME_VERIFIED=指定探针场景实际运行；NOT_VERIFIED=该场景未验证。等级可以并列；SDK 构造、SSR、MockMvc、npm 打包均不替代真实外部服务/设备验收。

## 后端

| 组件 / 用途 | 精确版本 | 来源 | 查询日期 | 运行要求 | 兼容依据 | 验证等级 | 已知限制 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Java / Eclipse Temurin / 编译和运行 | 21.0.12.1+1（目标 release=21） | [官方来源](https://github.com/adoptium/temurin21-binaries/releases/tag/jdk-21.0.12.1%2B1) | 2026-10-07 | 本次 macOS arm64；其他 OS 使用同发行版对应资产 | Java 21 LTS 维护至少至 2029-12；Boot 4.0 支持 Java 17～26 | DOCUMENTED / RESOLVED / COMPILED / RUNTIME_VERIFIED | 验证是 macOS arm64；Linux/Windows 未运行 |
| Maven Wrapper / 项目工具链 | 3.3.4 / only-script | [官方来源](https://maven.apache.org/tools/wrapper/maven-wrapper-plugin/wrapper-mojo.html) | 2026-10-07 | JDK、POSIX shell 或 Windows cmd；首次下载网络 | 官方轻量脚本下载固定 Maven；本次实际引导并调用 Wrapper | DOCUMENTED / RESOLVED / RUNTIME_VERIFIED | Windows mvnw.cmd 未运行；系统 Maven 仅用于临时引导 |
| Apache Maven / 构建 | 3.9.16 | [官方来源](https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.16/) | 2026-10-07 | Boot 要求 Maven ≥3.6.3 | Wrapper 固定精确分发地址、SHA-256；实际 Maven 3.9.16 运行 | DOCUMENTED / RESOLVED / RUNTIME_VERIFIED | 首次下载网络依赖；不要求全局 Maven |
| Spring Boot / 框架 | 4.0.8 | [官方来源](https://docs.spring.io/spring-boot/4.0/system-requirements.html) | 2026-10-07 | Java ≥17，Maven ≥3.6.3 | Boot 4.0 / Sa-Token Boot4 / springdoc3.0 对齐；JVM 上下文与 MockMvc 实际通过 | DOCUMENTED / RESOLVED / COMPILED / RUNTIME_VERIFIED | 未创建正式服务，未启动 TCP 端口；不代表三端/生产验收 |
| Spring Framework / Spring 基础 | 7.0.9 | [官方来源](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.0.8/spring-boot-dependencies-4.0.8.pom) | 2026-10-07 | Java ≥17 | Boot BOM 实际解析 | DOCUMENTED / RESOLVED / COMPILED / RUNTIME_VERIFIED | 仅本轮 JVM 场景 |
| Spring Data BOM / 依赖对齐 | 2025.1.7 | [官方来源](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.0.8/spring-boot-dependencies-4.0.8.pom) | 2026-10-07 | Spring Boot 4.0 | Boot BOM 实际解析 | DOCUMENTED / RESOLVED | 不手动覆盖子项目版本 |
| Spring Data JPA / 持久化 | 4.0.7 | [官方来源](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.0.8/spring-boot-dependencies-4.0.8.pom) | 2026-10-07 | Java 21 / PostgreSQL | Boot BOM 实际依赖树 | DOCUMENTED / RESOLVED | 本轮无 Entity/Repository/DB 运行验证 |
| Hibernate ORM / JPA 实现 | 7.2.24.Final | [官方来源](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.0.8/spring-boot-dependencies-4.0.8.pom) | 2026-10-07 | Java 21 / Jakarta Persistence | Boot BOM 实际依赖树 | DOCUMENTED / RESOLVED | 无真实 PostgreSQL ORM/事务/锁测试 |
| Spring Data Redis / Redis 访问 | 4.0.7 | [官方来源](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.0.8/spring-boot-dependencies-4.0.8.pom) | 2026-10-07 | Redis 服务用于真实操作 | Boot BOM；自动配置上下文创建成功 | DOCUMENTED / RESOLVED | 未连接真实 Redis |
| Lettuce / Redis 驱动 | 6.8.2.RELEASE | [官方来源](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.0.8/spring-boot-dependencies-4.0.8.pom) | 2026-10-07 | Java 21 / Redis | Boot BOM 实际解析；Bean 初始化 | DOCUMENTED / RESOLVED | macOS native DNS provider 缺失警告，使用系统回退；未验证 DNS/Redis I/O |
| Spring AMQP / spring-rabbit / 可选消息 | 4.0.5 | [官方来源](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.0.8/spring-boot-dependencies-4.0.8.pom) | 2026-10-07 | 真实消息场景需要 RabbitMQ | Boot BOM 实际解析 | DOCUMENTED / RESOLVED | 未运行消息/重试/确认 |
| RabbitMQ Java Client / AMQP 驱动 | 5.27.1 | [官方来源](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.0.8/spring-boot-dependencies-4.0.8.pom) | 2026-10-07 | RabbitMQ 4.3，AMQP 0-9-1 | Boot BOM 实际解析 | DOCUMENTED / RESOLVED | 无真实连接 |
| Flyway Core + PostgreSQL 模块 / 迁移 | 11.14.1 / 11.14.1 | [官方来源](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.0.8/spring-boot-dependencies-4.0.8.pom) | 2026-10-07 | 同时引入 flyway-database-postgresql | Boot BOM 实际解析；Flyway 配置加载通过 | DOCUMENTED / RESOLVED / COMPILED | 未执行 PostgreSQL 迁移；不能以 load() 当 DB 验收 |
| PostgreSQL JDBC / 数据库驱动 | 42.7.13 | [官方来源](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.0.8/spring-boot-dependencies-4.0.8.pom) | 2026-10-07 | PostgreSQL 17 | Boot BOM 实际解析 | DOCUMENTED / RESOLVED | 未真实连接数据库 |
| Jackson 3 / Jackson 2 / 框架及 SDK JSON | 3.1.5 / 2.21.5 | [官方来源](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.0.8/spring-boot-dependencies-4.0.8.pom) | 2026-10-07 | tools.jackson / com.fasterxml.jackson 不同包名 | Boot BOM 分别管理两套 BOM；共存依赖树与 OpenAPI 运行通过 | DOCUMENTED / RESOLVED | WxJava 全部 JSON/支付调用未验证，不强制替换 SDK JSON 栈 |
| Hibernate Validator / Jakarta Validation API / DTO校验 | 9.0.1.Final / 3.1.1 | [Boot BOM](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.0.8/spring-boot-dependencies-4.0.8.pom) | 2026-10-08 | Boot validation starter，Java 21 | BOM 管理；[P03-01 实际依赖树](../testing/evidence/P03-01/backend-dependency-tree.txt)，POM不重复定版本 | DOCUMENTED / RESOLVED / COMPILED / RUNTIME_VERIFIED | 运行仅 DTO/MVC 协议校验，不代表业务/数据库验证 |
| JUnit Jupiter / Java 测试 | 6.0.3 | [官方来源](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.0.8/spring-boot-dependencies-4.0.8.pom) | 2026-10-07 | Java ≥17 | Boot BOM；3 项真实探针运行 | DOCUMENTED / RESOLVED / COMPILED / RUNTIME_VERIFIED | 不把这些技术探针当完整业务验收 |
| Testcontainers / 数据库集成测试框架 | 2.0.5 | [官方来源](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.0.8/spring-boot-dependencies-4.0.8.pom) | 2026-10-07 | 真实容器测试需要 Docker | Boot BOM；使用 testcontainers-postgresql / testcontainers-junit-jupiter 的 2.x 模块名 | DOCUMENTED / RESOLVED / COMPILED | 仅构造 PostgreSQLContainer；未启动容器 |
| Mockito / Boot 测试传递依赖 | 5.20.0 | [官方来源](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.0.8/spring-boot-dependencies-4.0.8.pom) | 2026-10-07 | Java 21 | Boot BOM，测试运行实际加载 | DOCUMENTED / RESOLVED / RUNTIME_VERIFIED | 动态 agent 自附加警告；P02 测试配置应显式处理 |
| Maven Compiler Plugin / Java 编译 | 3.14.1 | [官方来源](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.0.8/spring-boot-dependencies-4.0.8.pom) | 2026-10-07 | 目标 release 21 | Boot parent 实际有效 POM；testCompile 成功 | DOCUMENTED / RESOLVED / RUNTIME_VERIFIED | 本轮编译的是测试探针 |
| Maven Surefire Plugin / JUnit 执行 | 3.5.6 | [官方来源](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.0.8/spring-boot-dependencies-4.0.8.pom) | 2026-10-07 | JUnit 6.0.3 | Boot parent 实际有效 POM；3 tests, 0 skipped | DOCUMENTED / RESOLVED / RUNTIME_VERIFIED | 完整业务测试尚未实现 |
| Sa-Token Boot4 starter + Redis template / 认证会话 | 1.46.0 / 1.46.0 | [官方来源](https://repo.maven.apache.org/maven2/cn/dev33/sa-token-spring-boot4-starter/1.46.0/) | 2026-10-07 | Java 21 / Spring Boot 4.0.8 | 官方 Boot4 starter；包含 sa-token-jackson3；同版本 Redis 模块；上下文与内存多身份会话测试 | DOCUMENTED / RESOLVED / COMPILED / RUNTIME_VERIFIED | Redis 会话持久性、Cookie/CSRF/完整权限未实现或运行 |
| Hutool Core / 受限工具 | 5.8.47 | [官方来源](https://repo.maven.apache.org/maven2/cn/hutool/hutool-core/5.8.47/) | 2026-10-07 | Java 21 / Spring Boot 4.0.8 | 仅 core；实际解析、编译和字符串工具调用 | DOCUMENTED / RESOLVED / COMPILED / RUNTIME_VERIFIED | 不引入 all；不替代安全/存储/协议实现 |
| WxJava 小程序模块 / 可选微信接口 | 4.8.0 | [官方来源](https://repo.maven.apache.org/maven2/com/github/binarywang/weixin-java-miniapp/4.8.0/) | 2026-10-07 | Java 21 / Spring Boot 4.0.8 | 官方正式发行；原始模块；依赖树与 WxMaServiceImpl 构造通过 | DOCUMENTED / RESOLVED / COMPILED / RUNTIME_VERIFIED | RUNTIME_VERIFIED 仅 SDK 基础加载；无真实微信请求 |
| WxJava 支付模块 / 可选支付适配 | 4.8.0 | [官方来源](https://repo.maven.apache.org/maven2/com/github/binarywang/weixin-java-pay/4.8.0/) | 2026-10-07 | Java 21 / Spring Boot 4.0.8 | 与 miniapp 同版本；原始模块；WxPayServiceImpl 构造通过 | DOCUMENTED / RESOLVED / COMPILED / RUNTIME_VERIFIED | 仅基础加载；无商户配置、验签、回调或支付验收 |
| springdoc WebMVC UI / OpenAPI | 3.0.3 | [官方来源](https://repo.maven.apache.org/maven2/org/springdoc/springdoc-openapi-starter-webmvc-ui/3.0.3/) | 2026-10-07 | Java 21 / Spring Boot 4.0.8 | 官方 Boot4.0→springdoc3.0 矩阵；实际 /v3/api-docs 返回通过 | DOCUMENTED / RESOLVED / COMPILED / RUNTIME_VERIFIED | 本轮 MockMvc 技术探针；公开策略在后续明确 |

## 前端

| 组件 / 用途 | 精确版本 | 来源 | 查询日期 | 运行要求 | 兼容依据 | 验证等级 | 已知限制 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Node.js / 前端工具链 | 24.21.0 | [官方来源](https://nodejs.org/dist/v24.21.0/) | 2026-10-07 | 本次 macOS arm64；LTS 至 2028-04-30 | Vite/插件要求 ^20.19.0 或 ≥22.12.0；实际隔离 Node 运行 | DOCUMENTED / RESOLVED / RUNTIME_VERIFIED | 本机默认仍为 22.23.2；其他 OS 未执行 |
| pnpm / 唯一前端包管理器 | 10.34.6 | [官方来源](https://registry.npmjs.org/pnpm/10.34.6) | 2026-10-07 | {'node': '>=18.12'} | 官方注册信息 peer {}；实际安装/对应探针 | DOCUMENTED / RESOLVED / COMPILED / RUNTIME_VERIFIED | 当前只验证技术探针 |
| react / UI 运行时 | 19.2.8 | [官方来源](https://registry.npmjs.org/react/19.2.8) | 2026-10-07 | {'node': '>=0.10.0'} | 官方注册信息 peer {}；实际安装/对应探针 | DOCUMENTED / RESOLVED / COMPILED / RUNTIME_VERIFIED | 当前只验证技术探针 |
| react-dom / DOM/SSR 运行时 | 19.2.8 | [官方来源](https://registry.npmjs.org/react-dom/19.2.8) | 2026-10-07 | 按 Node 24 / TypeScript 5.9 工具链 | 官方注册信息 peer {'react': '^19.2.8'}；实际安装/对应探针 | DOCUMENTED / RESOLVED / COMPILED / RUNTIME_VERIFIED | 当前只验证技术探针 |
| typescript / 三端类型检查 | 5.9.3 | [官方来源](https://registry.npmjs.org/typescript/5.9.3) | 2026-10-07 | {'node': '>=14.17'} | 官方注册信息 peer {}；实际安装/对应探针 | DOCUMENTED / RESOLVED / COMPILED / RUNTIME_VERIFIED | 源码 strict=true；第三方声明采用 skipLibCheck；不宣称 vendor d.ts 全面通过 |
| @vitejs/plugin-react / React 编译/HMR | 5.2.0 | [官方来源](https://registry.npmjs.org/@vitejs%2fplugin-react/5.2.0) | 2026-10-07 | {'node': '^20.19.0 或或 >=22.12.0'} | 官方注册信息 peer {'vite': '^4.2.0 或或 ^5.0.0 或或 ^6.0.0 或或 ^7.0.0 或或 ^8.0.0'}；实际安装/对应探针 | DOCUMENTED / RESOLVED / COMPILED / RUNTIME_VERIFIED | 固定 5.2.0，支持 Vite7；不使用要求 Vite8 的 6 系列 |
| antd / 官方 Web 组件/表单 | 6.6.5 | [官方来源](https://registry.npmjs.org/antd/6.6.5) | 2026-10-07 | 按 Node 24 / TypeScript 5.9 工具链 | 官方注册信息 peer {'react': '>=18.0.0', 'react-dom': '>=18.0.0'}；实际安装/对应探针 | DOCUMENTED / RESOLVED / COMPILED / RUNTIME_VERIFIED | 源码 strict=true/skipLibCheck=true；完整第三方声明检查 FAIL；SSR Form/Button 通过，其他 UI 与浏览器未验收 |
| @ant-design/icons / 官方图标 | 6.3.4 | [官方来源](https://registry.npmjs.org/@ant-design%2ficons/6.3.4) | 2026-10-07 | {'node': '>=8'} | 官方注册信息 peer {'react': '>=16.0.0', 'react-dom': '>=16.0.0'}；实际安装/对应探针 | DOCUMENTED / RESOLVED / COMPILED | 已验证类型和Web构建；未单独验证图标浏览器运行 |
| @tanstack/react-router / 唯一路由 | 1.170.41 | [官方来源](https://registry.npmjs.org/@tanstack%2freact-router/1.170.41) | 2026-10-07 | {'node': '>=20.19'} | 官方注册信息 peer {'react': '>=18.0.0 或或 >=19.0.0', 'react-dom': '>=18.0.0 或或 >=19.0.0'}；实际安装/对应探针 | DOCUMENTED / RESOLVED / COMPILED / RUNTIME_VERIFIED | 当前只验证技术探针 |
| @tanstack/react-query / 服务端状态 | 5.104.1 | [官方来源](https://registry.npmjs.org/@tanstack%2freact-query/5.104.1) | 2026-10-07 | 按 Node 24 / TypeScript 5.9 工具链 | 官方注册信息 peer {'react': '^18 或或 ^19'}；实际安装/对应探针 | DOCUMENTED / RESOLVED / COMPILED / RUNTIME_VERIFIED | 当前只验证技术探针 |
| zustand / 轻量客户端状态 | 5.0.15 | [官方来源](https://registry.npmjs.org/zustand/5.0.15) | 2026-10-07 | {'node': '>=12.20.0'} | 官方注册信息 peer {'immer': '>=9.0.6', 'react': '>=18.0.0', '@types/react': '>=18.0.0', 'use-sync-external-store': '>=1.2.0'}；实际安装/对应探针 | DOCUMENTED / RESOLVED / COMPILED / RUNTIME_VERIFIED | 当前只验证技术探针 |
| @types/react / React 类型 | 19.2.18 | [官方来源](https://registry.npmjs.org/@types%2freact/19.2.18) | 2026-10-07 | 按 Node 24 / TypeScript 5.9 工具链 | 官方注册信息 peer {}；实际安装/对应探针 | DOCUMENTED / RESOLVED / COMPILED | 类型通过不代表对应设备/浏览器运行 |
| @types/react-dom / React DOM 类型 | 19.2.7 | [官方来源](https://registry.npmjs.org/@types%2freact-dom/19.2.7) | 2026-10-07 | 按 Node 24 / TypeScript 5.9 工具链 | 官方注册信息 peer {'@types/react': '^19.2.0'}；实际安装/对应探针 | DOCUMENTED / RESOLVED / COMPILED | 类型通过不代表对应设备/浏览器运行 |
| @types/node / Node 类型 | 24.19.1 | [官方来源](https://registry.npmjs.org/@types%2fnode/24.19.1) | 2026-10-07 | 按 Node 24 / TypeScript 5.9 工具链 | 官方注册信息 peer {}；实际安装/对应探针 | DOCUMENTED / RESOLVED / COMPILED | 类型通过不代表对应设备/浏览器运行 |
| eslint / 静态检查 | 10.12.0 | [官方来源](https://registry.npmjs.org/eslint/10.12.0) | 2026-10-07 | {'node': '^20.19.0 或或 ^22.13.0 或或 >=24'} | 官方注册信息 peer {'jiti': '*'}；实际安装/对应探针 | DOCUMENTED / RESOLVED / COMPILED / RUNTIME_VERIFIED | 当前只验证技术探针 |
| @eslint/js / 基础 ESLint 规则 | 10.0.1 | [官方来源](https://registry.npmjs.org/@eslint%2fjs/10.0.1) | 2026-10-07 | {'node': '^20.19.0 或或 ^22.13.0 或或 >=24'} | 官方注册信息 peer {'eslint': '^10.0.0'}；实际安装/对应探针 | DOCUMENTED / RESOLVED / COMPILED / RUNTIME_VERIFIED | 当前只验证技术探针 |
| typescript-eslint / TypeScript ESLint | 8.71.1 | [官方来源](https://registry.npmjs.org/typescript-eslint/8.71.1) | 2026-10-07 | {'node': '^18.18.0 或或 ^20.9.0 或或 >=21.1.0'} | 官方注册信息 peer {'eslint': '^8.57.0 或或 ^9.0.0 或或 ^10.0.0', 'typescript': '>=4.8.4 <6.1.0'}；实际安装/对应探针 | DOCUMENTED / RESOLVED / COMPILED / RUNTIME_VERIFIED | 当前只验证技术探针 |
| eslint-plugin-react-hooks / Hooks 规则 | 7.1.1 | [官方来源](https://registry.npmjs.org/eslint-plugin-react-hooks/7.1.1) | 2026-10-07 | {'node': '>=18'} | 官方注册信息 peer {'eslint': '^3.0.0 或或 ^4.0.0 或或 ^5.0.0 或或 ^6.0.0 或或 ^7.0.0 或或 ^8.0.0-0 或或 ^9.0.0 或或 ^10.0.0'}；实际安装/对应探针 | DOCUMENTED / RESOLVED / COMPILED / RUNTIME_VERIFIED | 当前只验证技术探针 |
| eslint-plugin-react-refresh / Refresh 规则 | 0.5.7 | [官方来源](https://registry.npmjs.org/eslint-plugin-react-refresh/0.5.7) | 2026-10-07 | 按 Node 24 / TypeScript 5.9 工具链 | 官方注册信息 peer {'eslint': '^9 或或 ^10'}；实际安装/对应探针 | DOCUMENTED / RESOLVED / COMPILED / RUNTIME_VERIFIED | 当前只验证技术探针 |
| vitest / 前端测试 | 4.1.11 | [官方来源](https://registry.npmjs.org/vitest/4.1.11) | 2026-10-07 | {'node': '^20.0.0 或或 ^22.0.0 或或 >=24.0.0'} | 官方注册信息 peer {'vite': '^6.0.0 或或 ^7.0.0 或或 ^8.0.0', 'jsdom': '*', 'happy-dom': '*', '@vitest/ui': '4.1.11', '@types/node': '^20.0.0 或或 ^22.0.0 或或 >=24.0.0', '@edge-runtime/vm': '*', '@opentelemetry/api': '^1.9.0', '@vitest/coverage-v8': '4.1.11', '@vitest/browser-preview': '4.1.11', '@vitest/coverage-istanbul': '4.1.11', '@vitest/browser-playwright': '4.1.11', '@vitest/browser-webdriverio': '4.1.11'}；实际安装/对应探针 | DOCUMENTED / RESOLVED / COMPILED / RUNTIME_VERIFIED | 当前只验证技术探针 |
| vite / Web 构建 | 7.3.7 | [官方来源](https://registry.npmjs.org/vite/7.3.7) | 2026-10-07 | {'node': '^20.19.0 或或 >=22.12.0'} | 官方注册信息 peer {'tsx': '^4.8.1', 'jiti': '>=1.21.0', 'less': '^4.0.0', 'sass': '^1.70.0', 'yaml': '^2.4.2', 'stylus': '>=0.54.8', 'terser': '^5.16.0', 'sugarss': '^5.0.0', '@types/node': '^20.19.0 或或 >=22.12.0', 'lightningcss': '^1.21.0', 'sass-embedded': '^1.70.0'}；实际安装/对应探针 | DOCUMENTED / RESOLVED / COMPILED / RUNTIME_VERIFIED | 当前只验证技术探针 |

## 小程序与契约工具

| 组件 / 用途 | 精确版本 | 来源 | 查询日期 | 运行要求 | 兼容依据 | 验证等级 | 已知限制 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| openapi-typescript / OpenAPI 生成类型 | 7.13.0 | [官方来源](https://registry.npmjs.org/openapi-typescript/7.13.0) | 2026-10-07 | 按 Node 24 / TypeScript 5.9 工具链 | 官方注册信息 peer {'typescript': '^5.x'}；实际安装/对应探针 | DOCUMENTED / RESOLVED / COMPILED / RUNTIME_VERIFIED | peer TypeScript5；本轮中性契约生成与编译，无正式业务契约 |
| tdesign-miniprogram / 官方小程序组件 | 1.17.0 | [官方来源](https://registry.npmjs.org/tdesign-miniprogram/1.17.0) | 2026-10-07 | 按 Node 24 / TypeScript 5.9 工具链 | 官方注册信息 peer {}；实际安装/对应探针 | DOCUMENTED / RESOLVED / COMPILED | 源码 strict=true/skipLibCheck=true；SDK 声明全面检查 FAIL；实际 npm 组件输出通过，DevTools/真机未运行 |
| miniprogram-api-typings / 原生微信 API 类型 | 5.2.3 | [官方来源](https://registry.npmjs.org/miniprogram-api-typings/5.2.3) | 2026-10-07 | 按 Node 24 / TypeScript 5.9 工具链 | 官方注册信息 peer {}；实际安装/对应探针 | DOCUMENTED / RESOLVED / COMPILED | 类型通过不代表对应设备/浏览器运行 |
| miniprogram-ci / 可选独立微信检查/构建工具 | 2.1.48 | [官方来源](https://registry.npmjs.org/miniprogram-ci/2.1.48) | 2026-10-07 | {'node': '>=16.1.0'} | 官方注册信息 peer {}；实际安装/对应探针 | DOCUMENTED / RESOLVED / RUNTIME_VERIFIED | 仅独立 pnpm dlx 工具；未加入主 workspace；内部 parser 依赖 ESLint7/8，本次隔离解析 ESLint8.57.1（EOL）；仅离线 pack API 验证，未预览/上传 |

## 基础设施

| 组件 / 用途 | 精确版本 | 来源 | 查询日期 | 运行要求 | 兼容依据 | 验证等级 | 已知限制 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| PostgreSQL / 数据库 | 17.11（postgres:17.11-bookworm） | [官方来源](https://www.postgresql.org/support/versioning/) | 2026-10-07 | 本次官方 manifest 含 linux/arm64 和 amd64 | 17 系列维护至 2029-11-08；官方镜像 manifest 解析通过 | DOCUMENTED / RESOLVED | 数据库运行、Flyway 迁移、SQL/锁均 NOT_VERIFIED |
| Redis Open Source / 会话/缓存 | 8.2.10（redis:8.2.10-bookworm） | [官方来源](https://redis.io/docs/latest/operate/oss_and_stack/install/version-mgmt/) | 2026-10-07 | Docker 或对应官方二进制；Java客户端 Lettuce | 8.2 Extended 维护至 2030-09-01；官方镜像 manifest 解析 | DOCUMENTED / RESOLVED | 实际 Redis I/O、会话持久性未运行；分发许可须按使用方式核对 |
| RabbitMQ / 可选 AMQP | 4.3.6（rabbitmq:4.3.6-management） | [官方来源](https://www.rabbitmq.com/release-information) | 2026-10-07 | 采用官方镜像携带 Erlang，默认不启用 | 官方当前社区支持线，镜像 manifest 解析；协议 AMQP0-9-1 | DOCUMENTED / RESOLVED | 社区支持至 2026-11-30；P10 启用前复核；无真实消息运行 |

## 特别兼容依据与边界

- [Spring Boot 4.0 要求](https://docs.spring.io/spring-boot/4.0/system-requirements.html)：Java 21 与 Maven 3.9 均满足。
- [springdoc 官方矩阵](https://springdoc.org/)：Boot4.0 配 3.0；[Sa-Token 官方说明](https://sa-token.com/start/example.html)：Boot4 使用 boot4-starter。
- [Flyway PostgreSQL 模块](https://documentation.red-gate.com/flyway/reference/database-driver-reference/postgresql-database)：PostgreSQL 支持需要单独模块；本次 org.flywaydb 两模块都解析为 11.14.1。
- [Ant Design 6 兼容说明](https://ant.design/docs/react/migration-v6-cn/)：React ≥18、支持 React19、图标使用6；不加入 v5 React19补丁。
- [Vite 维护线](https://vite.dev/releases)、[Node.js 生命周期](https://github.com/nodejs/Release)：选 Vite7.3 与 Node24LTS，使用插件5，避开插件6对 Vite8 的要求。
- [ESLint 维护状态](https://eslint.org/version-support/)：ESLint9已于2026-08-06停止维护，采用10。
- [Vite 官方 TypeScript 模板](https://github.com/vitejs/vite/blob/main/packages/create-vite/template-react-ts/tsconfig.app.json)：采用 strict 应用源码检查与 skipLibCheck；本轮 Web/TDesign 全面声明检查失败保留，不宣称修复上游声明。
- [TDesign 官方说明](https://github.com/Tencent/tdesign-miniprogram/blob/develop/README.md)：组件 npm 构建与基础库最低条件；其历史最低基础库2.6.5不作为目标承诺；P01-02新增精确基础库/工具目标见下表，P09执行运行验收。
- [微信官方 CI](https://github.com/wechat-miniprogram/miniprogram-ci-dist)：packNpmManually 可在不提供 AppID/私钥的情况下离线构建。本轮主 workspace 不安装 CI；隔离工具仍含旧传递依赖，不能将它当成全仓 ESLint 工具。

官方镜像的精确多架构 digest 与平台清单见 [infra-images.json](../testing/evidence/P01-01/infra-images.json)。manifest 中 unknown/unknown 条目是附加描述，不能作为可运行平台；本轮没有拉取/启动镜像。

Maven 编译/测试插件由 Boot parent 管理；Dependency Plugin 3.9.0 仅用于本轮依赖证据提取。Docker CLI/daemon/Compose 的本机版本见验证报告，不把本机版本等同于已冻结的生产容器环境。

基线可进入 P01-02 详细设计；它不代表第三方 SDK 全接口、真实基础设施、安全机制、浏览器或微信设备运行已经验证。

## P01-02 补充的协议与微信工具目标

本轮未改动前述53条依赖记录、未新增正式依赖或锁文件。以下精确目标由本轮官方读取冻结；资料/发布信息不等于设备兼容PASS。

| 组件 / 用途 | 精确版本 | 来源 | 查询日期 | 运行要求 | 兼容依据 | 验证等级 | 已知限制 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| OpenAPI / 协议schema | 3.1.0 | [官方规范](https://spec.openapis.org/oas/v3.1.0.html) | 2026-10-07 | 后端导出及既定类型生成器 | P01-02技术schema真实生成、null/union/enum/optional strict编译及漂移反例 | DOCUMENTED / COMPILED / RUNTIME_VERIFIED | 仅技术生成流程；正式后端接口导出P03未执行 |
| 微信小程序目标基础库 | 3.17.2 | [官方更新日志](https://developers.weixin.qq.com/miniprogram/dev/framework/release/) | 2026-10-07 | WebView原生工程、TDesign | 官方已发布2026-07-17；选择稳定已发布目标，不声称为最新；API资料确认下载/媒体方案 | DOCUMENTED | 未安装/模拟/真机；P09验证，项目配置固定libVersion，低于目标给升级提示，不降级假运行 |
| 微信开发者工具 / Stable Build | 2.02.2608080 | [官方稳定版](https://developers.weixin.qq.com/miniprogram/dev/devtools/stable.html)、[官方版本记录](https://devtools.wxqcloud.qq.com.cn/WechatWebDev/nightly/versions/logs/stable_v2.02.2608080.json) | 2026-10-07 | 官方对应OS分发；P02/P09自行验证 | 官方stable历史及日志实际HTTP200，发布日期2026-09-30 | DOCUMENTED / RESOLVED | 仅版本元数据，未下载/安装/运行；不改全局工具 |

新增目标的官方响应SHA、能力说明与临时探针见 [P01-02报告](../testing/P01-02-VERIFICATION.md)。精确版本只在本矩阵定义，页面/生成文档引用此表，不复制第二份版本表。

## P02-01 构建必需的 CI 工具补充

应用依赖版本不变。P01 未指定 CI Actions；只补充执行既定检查所需的官方动作，2026-10-07 实际 git ls-remote 解析 v4 并固定完整 commit SHA。首次 API 查询受 GitHub 限流返回403，后续官方 Git 读取成功，原始命令/输出见 [CI 工具证据](../testing/evidence/P02-01/ci-actions.json)。下表为当前冻结值；2026-10-08 针对用户截图中的 Java 安装失败调整 setup-java，详见下方 CI 修复记录。历史阶段的远程检查仍为 NOT_EXECUTED；修复后远程执行 NOT_VERIFIED，不以本地 macOS 检查替代。

| 组件 / 用途 | 精确版本 | 来源 | 验证等级 | 已知限制 |
| --- | --- | --- | --- | --- |
| actions/checkout / CI | v4，`11d5960a326750d5838078e36cf38b85af677262` | [官方源码](https://github.com/actions/checkout/tree/11d5960a326750d5838078e36cf38b85af677262) | DOCUMENTED / RESOLVED | 远程执行 NOT_VERIFIED |
| actions/setup-node / CI | v4，`49933ea5288caeca8642d1e84afbd3f7d6820020` | [官方源码](https://github.com/actions/setup-node/tree/49933ea5288caeca8642d1e84afbd3f7d6820020) | DOCUMENTED / RESOLVED | 远程执行 NOT_VERIFIED |
| actions/setup-java / CI | v5.7.0，`b6effb05e454b25005698d916606bdc6ffcbf961` | [官方源码](https://github.com/actions/setup-java/tree/b6effb05e454b25005698d916606bdc6ffcbf961) | DOCUMENTED / RESOLVED / RUNTIME_VERIFIED（解析及元数据匹配探针） | Node24 Action runtime；自托管 runner 要求 ≥2.327.1，本项目使用 GitHub 托管 ubuntu-24.04；完整远程执行 NOT_VERIFIED |
| pnpm/action-setup / CI | v4，`b906affcce14559ad1aafd4ab0e942779e9f58b1` | [官方源码](https://github.com/pnpm/action-setup/tree/b906affcce14559ad1aafd4ab0e942779e9f58b1) | DOCUMENTED / RESOLVED | 远程执行 NOT_VERIFIED |

Maven Failsafe 继承同一 Boot parent，实际解析与 Surefire 一致；详见 [P02 依赖和执行证据](../testing/P02-01-VERIFICATION.md)，不引入新的测试框架或额外 Maven Module。托管 CI runner 使用 ubuntu-24.04 标签，不声称其系统镜像全部软件固定；Java/Node/pnpm 输入仍来自上述冻结值。

### 2026-10-08 CI Java 版本输入修复

Temurin 的运行版本保持后端表中的冻结值。Adoptium 官方元数据将该发布表示为 `version_data.semver=21.0.12+101.0.LTS`，`openjdk_version=21.0.12.1+1-LTS`，对应同一 Linux x64 JDK 资产；见 [官方响应提取记录](../testing/evidence/CI-JAVA-SETUP-2026-10-08/adoptium-release.json)。workflow 的 `java-version` 使用前者，因为 setup-java 通过 SemVer 校验并匹配 `version_data.semver`；不改为大版本范围或其他 JDK 发布。单纯升级 Action 仍不能解析原四段带构建号的输入。

setup-java 更新到上表的固定官方 SHA，移除旧 v4 的弃用警告；其 Action 自身 Node24 运行时与应用 `.nvmrc` 管理的 Node 是不同配置。安装后另行断言真实 `java.runtime.version`，防止安装版本偏离基线。解析探针、本地运行版本核验与后端验证范围见 [修复验证报告](../testing/CI-JAVA-SETUP-VERIFICATION.md)；GitHub 远程重跑未执行，不推导为 CI PASS。

## P03-01 依赖接入记录（2026-10-08）

后端新增有实际 DTO/MVC 校验用途的 `spring-boot-starter-validation`，版本由当前 Boot parent 管理；未覆盖 BOM、未升级冻结版本。当前应用实际使用 Jackson 3，annotations 属于其兼容注解包，不代表新增 Jackson 2 数据绑定。解析事实见 [完整依赖树](../testing/evidence/P03-01/backend-dependency-tree.txt)，冻结工具执行见 [toolchain](../testing/evidence/P03-01/toolchain.json)，[clean verify](../testing/evidence/P03-01/verify-final.json) 与 [运行/产物检查](../testing/P03-01-VERIFICATION.md) 分别保留等级。未加入数据库、认证、缓存或消息依赖。

## P03-02 依赖接入与运行记录（2026-10-08）

已正式接入矩阵中的 JPA/Hibernate、Flyway Core+PostgreSQL 模块、PostgreSQL JDBC 及 Testcontainers 2.x 模块，解析值未改。`spring-boot-starter-flyway` 提供 Boot 4 的迁移自动配置，`spring-boot-starter-actuator` 提供必要健康端点，两者均由同一 parent 管理，不重复设置版本。HikariCP 为 BOM 传递依赖，实际解析 7.0.2，不手工覆盖。完整解析见 [dependency tree](../testing/evidence/P03-02/backend-dependency-tree.txt)，冻结工具见 [toolchain](../testing/evidence/P03-02/toolchain.json)。

上述持久化依赖本轮新增 COMPILED / RUNTIME_VERIFIED 等级，范围限于 [P03-02](../testing/P03-02-VERIFICATION.md) 的独立 PostgreSQL 迁移/JPA审计/事务/分页与本机健康启停；PostgreSQL 采用既有镜像标签+digest，未变更基线。历史表内 P01 未执行记录保持原样，不能将本轮结果推导为身份/租户/RLS/生产角色/真实业务验收。Surefire/Failsafe 均由 parent 管理，实际执行及失败传播证据见报告；远程CI仍 NOT_EXECUTED。

## P03-03 实际接入与测试工具复用（2026-10-08）

正式接入既有冻结springdoc WebMVC UI和openapi-typescript，版本没有升级。Swagger解析模型使用Jackson 2传递依赖，应用HTTP仍为Jackson 3；完整 [实际依赖树](../testing/evidence/P03-03/backend-dependency-tree.txt) 留证，两者由Boot BOM分别对齐，不强迫SDK/生成器改JSON栈。

结构门禁复用已有json-smart测试传递依赖中的ASM 9.7.1（仅test scope），未增加直接依赖/版本覆盖；Spring内置ASM没有signature包的首次失败保留，正式检查使用完整ASM的泛型签名API，源码直接编译可用性由每次verify验证。springdoc传递的Swagger Core/annotations/models实际2.2.47、UI5.32.2只记录解析事实，不将其额外冻结为新的直接依赖。生成器peer仍使用冻结TypeScript。

新增验证等级COMPILED/RUNTIME_VERIFIED仅限 [P03-03](../testing/P03-03-VERIFICATION.md) 的标量HTTP、两份OpenAPI、三端类型/生成检查、字节码与生产产物。本机结果不证明远程CI、多OS、租户/身份或真实业务。

## P04-03 Redis接入与实际解析（2026-10-08）

新增有实际资源读写/隔离/健康用途的 `spring-boot-starter-data-redis`，由原parent/BOM管理，不重复指定版本，不升级任何冻结值。实际Spring Data Redis、Lettuce与既有矩阵一致，完整解析见[依赖树](../testing/evidence/P04-03/backend-dependency-tree.txt)，实际命令与退出码见[解析元数据](../testing/evidence/P04-03/dependencies.json)。只增starter，未接入Sa-Token Redis DAO、AMQP或其他测试框架；Awaitility/GenericContainer均复用现有测试传递依赖。

新增COMPILED/RUNTIME_VERIFIED限[P04-03](../testing/P04-03-VERIFICATION.md)中的独立认证Redis真实CRUD/TTL/命名空间/故障与HTTP健康、受限PostgreSQL异步/事务/GUC及确定性线程生命周期。原P01未执行记录保留，不外推会话持久性、Cluster、生产ACL/TLS/角色部署或真实业务。

## P05-02 认证接入与运行记录（2026-10-08）

正式加入后端表已冻结的Sa-Token Boot 4 starter及Redis Template模块，所有Sa-Token解析模块与矩阵版本一致；未覆盖Spring Boot BOM或改变其他依赖。实际传递模块含core、Jakarta Servlet、Boot WebMVC common与Jackson 3适配；[依赖树](../testing/evidence/P05-02/backend-dependency-tree.txt)记录解析事实，[官方同版源码URL/SHA](../testing/evidence/P05-02/sa-token-sources.json)核对配置、显式Token载体、设备、绝对/闲置期限、当前退出和Redis DAO API。

COMPILED / RUNTIME_VERIFIED范围仅[P05-02](../testing/P05-02-VERIFICATION.md)的正式数据、真实PostgreSQL/Redis/HTTP、跨JVM共享会话、同源浏览器Cookie/CSRF及生成类型；平台/客户登录、生产TLS/代理/Redis ACL/HA、跨OS和远程CI仍未验证。历史阶段未执行记录不改写。

## P05-05 解析兼容性修正（2026-10-08）

commons-io固定为2.20.0，恢复[P05-02实际依赖树](../testing/evidence/P05-02/backend-dependency-tree.txt)中原Testcontainers解析值；不升级WxJava或其他冻结组件。新增WxJava4.8.0的较短依赖路径选中旧commons-io，导致commons-compress1.28.0调用FileTimes.toUnixTime出现NoSuchMethodError，Redis技术容器文件复制失败；[冲突记录](../testing/evidence/P05-05/dependency-conflict.log)。该项不受Boot BOM管理，POM显式固定原解析版本；最终解析和运行等级以[P05-05报告](../testing/P05-05-VERIFICATION.md)为准。
