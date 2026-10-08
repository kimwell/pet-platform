# 技术基线

建立日期：2026-10-07。本文件记录组合与使用规则；精确版本、来源、解析值和验证等级统一见 [版本矩阵](../development/VERSION-MATRIX.md)，当前证据见 [验证报告](../testing/P01-01-VERIFICATION.md)。

## 后端

项目位置固定为当前根目录 `/Users/kimwell/work/pet-platform`。后端基础包为 `com.pet.platform`，源码目录为 `apps/backend/src/main/java/com/pet/platform/`，启动类为 `com.pet.platform.Application`；模块包边界见 [工程结构](PROJECT-STRUCTURE.md)。P01 冻结位置和包名，P02-01 已建立最小启动与检查工程；实际接入依赖及验证见 [P02-01](../testing/P02-01-VERIFICATION.md)，未接入的规划能力不能由技术基线推导为完成。

Java 21 LTS，目标发行版 Eclipse Temurin。Spring Boot 采用 4.0 稳定维护线，与 Sa-Token Boot 4 starter、springdoc 3.0 对齐；不追逐所有组件的最新主/次版本。后端只有一个 Maven Module，按包划分模块化单体；不引入 Spring Modulith 等额外框架作为分层前提。

以 Spring Boot starter parent/BOM 统一管理 Spring Framework、Spring Data JPA、Hibernate、JDBC 驱动、Flyway、Spring Data Redis、Spring AMQP、JUnit 与 Testcontainers 的解析版本。正式工程使用 Maven Wrapper `only-script` 方式，固定 Maven 分发地址及 SHA-256；开发者和 CI 无须预装全局 Maven。当前系统 Maven 仅用于临时探针的 Wrapper 引导，不成为正式运行要求。

PostgreSQL 是唯一数据库。Flyway 管理版本迁移，显式加入 `org.flywaydb:flyway-database-postgresql`，禁止依赖 Hibernate 自动修改生产结构；后续采用 schema 验证。真实 SQL、事务、锁和隔离以 PostgreSQL 集成测试验证。

Sa-Token 维护服务端会话。Redis 作为跨实例会话/缓存存储，采用与 starter 相同版本的 `sa-token-redis-template`；内存 DAO 仅可用于隔离技术单元测试，不作为会话上线或持久性验收。Boot 4 默认 Jackson 3，WxJava 可能携带 Jackson 2/Gson，按不同包名并存核查，不盲目把 SDK 强制迁移到 Jackson 3。

Hutool 仅引入 `hutool-core`，允许必要字符串/集合等工具；不得替代 JPA、Spring 配置、JSON 协议、HTTP 请求、认证、加密、文件权限和租户控制。新增 Hutool 子模块须有实际需求和验证，不使用 `hutool-all`。

Spring AMQP 对接可选 RabbitMQ；默认不开启消息业务，关闭时不创建连接、监听器或依赖启动条件。WxJava 仅按需引入 `weixin-java-miniapp` / `weixin-java-pay` 原始模块，不引入其 Spring Boot starter，避免额外框架版本覆盖。当前不配置微信凭证，不实现真实微信登录/支付。

本地文件存储是默认适配器，元数据在 PostgreSQL，目录和下载均受租户授权控制；不把裸物理路径返回客户端，不引入 MinIO。OpenAPI 是接口事实源，JUnit/Testcontainers 为测试基础。

## 管理后台

React 与 React DOM 使用同一精确版本，Ant Design 6 支持 React 19，不使用 v5 React 19 补丁。Ant Design Form 处理表单，直接使用官方组件；只在有明确业务语义时进行封装。

Vite 7.3 的官方维护策略覆盖重要修复和安全补丁，React 插件使用与其兼容的 5 系列。TypeScript 固定 5.9，满足类型生成和静态检查工具 peer 要求。Node.js 24 LTS 与 pnpm 10 在临时目录运行，不改变本机的 Node.js 22 或全局 pnpm。

TanStack Router 是唯一路由；P01 探针使用代码路由，P01-02 已选代码路由，不增加官方文件路由插件，详细结构见 [工程结构](PROJECT-STRUCTURE.md)。TanStack Query 管理服务端状态、失效和请求缓存；Zustand 仅管理少量客户端共享状态；表单数据不复制到两套状态库。请求基于 fetch，处理 HTTP 状态和统一响应，默认同源 credentials，业务错误不吞掉。

静态检查采用 ESLint 10、typescript-eslint、官方 Hooks/Refresh 插件及 `tsc`；测试采用 Vitest 4，不预装大量浏览器/覆盖率工具。真实浏览器、视觉、可访问性和端到端验收按后续任务需要增加。

## 微信小程序及共享协议

原生微信小程序、TypeScript、tdesign-miniprogram；页面 data 与轻量会话管理，不引入 Taro/uni-app。客户和员工会话域独立，原生 `wx.request` 实现请求，不把 Web fetch、DOM 或 React 类型带入小程序共享层。

`packages/api-contracts` 未来只承载 OpenAPI 生成类型与纯协议辅助类型，不运行 Cookie、fetch、wx.request 或状态管理。类型由 OpenAPI 生成，Web/小程序保留各自传输实现；禁止双向手工修改生成文件。

全仓仅使用 pnpm 和一份根锁文件，直接版本精确，CI 使用 frozen lockfile。微信 npm 构建采用官方 miniprogram-ci 的离线 `packNpmManually` API 探针验证 pnpm 依赖布局与实际组件产物；CI 固定版本作为独立 pnpm dlx 工具使用，不加入主 workspace，避免其内部 ESLint 7/8 peer 与项目 ESLint 10 冲突；预览/上传与真机验证另行执行，当前不创建 AppID 或上传密钥。

## 维护和限制

基线冻结表示可复现，不表示永不升级。P02 执行前、P12 发布前以及官方支持窗口变化时复核受影响版本和安全更新；补丁更新需记录矩阵、决策和受影响验证，不改 BOM 来追求每行显式版本。

PostgreSQL 选择成熟 17 系列；Redis 选择 8.2 Extended 系列；RabbitMQ 选择当前社区支持的 4.3 系列。RabbitMQ 社区支持计划于 2026-11-30 结束，P10 启用前必须重新核对，不能将商业支持期限当成免费维护承诺。Redis 8 的授权选项需要随分发方式检查，技术兼容探针不作许可证法律结论。

Web 与小程序采用 `strict=true`、`skipLibCheck=true` 检查应用源码及公开 API 使用，与 Vite 官方模板的声明检查边界一致。本轮全面检查第三方声明时，Ant Design 间接 image/picker 类型出现 TS2430，TDesign superComponent 泛型出现 TS2344；这些失败原样留证，没有修补、覆盖或谎称通过上游声明。若后续要求全面检查 vendor d.ts，需要单独解决并重新验证；本轮兼容基线不作这项保证。

miniprogram-ci 独立工具的旧传递依赖和 ESLint 8.57.1 仅用于隔离离线打包，不作为项目静态检查标准或默认初始化依赖。本轮 Java 测试存在 Mockito 自附加 agent 与 macOS Netty DNS 回退警告；P02 测试配置需要明确 agent 使用，本机 DNS 回退不代表已验证 Redis I/O。

本文只定义技术组合及P01-01兼容边界；详细认证、隔离、类型和生命周期已由P01-02专门文档定义，入口见 [核心契约索引](../contracts/CORE-CONTRACTS.md)。P01-01验证结论保持历史原样。

## P03-02 实施记录（2026-10-08）

当前正式后端已接入 BOM 管理的 JPA、Flyway starter（Boot 4 模块化自动配置）、PostgreSQL 支持模块、JDBC 与 Actuator；测试接入 Testcontainers 2.x PostgreSQL/JUnit 模块。冻结版本未升级或覆盖。真实迁移、结构校验、应用服务事务、时间审计与分页结果见 [P03-02](../testing/P03-02-VERIFICATION.md)。没有正式业务实体/迁移、身份、租户隔离或生产角色验收。
