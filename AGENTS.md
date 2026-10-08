# 企业应用工程脚手架开发规则

本规则适用于当前项目根目录 `/Users/kimwell/work/pet-platform`。所有工程与文档直接在该根目录创建或更新，不创建 `enterprise-app-scaffold` 子目录，不将现有成果整体移动；修改前检查冲突并保留既有 `ui/`。本项目独立建立工程基线，不使用 Product Delivery OS；不得安装、调用、引入该框架，不创建 `.delivery-os`，不读取或更新其状态。用户于 2026-10-07 明确授权 P02-01：根目录工程骨架、构建工具与本地基础设施初始化。本轮允许三端最小入口、构建检查、配置、本地基础设施与 CI，不实现正式业务，不自动执行 P02-02。

## 技术与依赖

后端采用 Java、Spring Boot、模块化单体、一个 Maven Module、Maven Wrapper、Spring Data JPA/Hibernate、PostgreSQL、Flyway、Sa-Token、范围受限的 Hutool、Spring Data Redis、Spring AMQP；RabbitMQ 和 binarywang/WxJava 小程序/支付能力按需启用。存储默认本地文件，协议使用 OpenAPI，测试使用 JUnit 和 Testcontainers。

Web 使用 React、TypeScript、Vite、Ant Design、TanStack Router、TanStack Query、Zustand、Ant Design Form 和原生 fetch。微信端使用原生微信小程序、TypeScript、tdesign-miniprogram、页面 data 和轻量会话管理；客户与员工是独立身份域，不引入额外复杂状态库。

精确版本的唯一文档事实源是 [VERSION-MATRIX](docs/development/VERSION-MATRIX.md)。直接依赖固定版本，不以 latest、^、~ 冻结基线；受 Spring Boot BOM 管理的依赖不重复指定版本，实际解析版本必须留证。全仓前端只使用 pnpm 和一份根 `pnpm-lock.yaml`。当前 P02-01 只初始化有实际构建、启动或检查用途的文件，不创建空类或假业务目录。

不得新增微服务架构、MySQL、作为生产等价替代的 H2、MinIO、Kafka、Elasticsearch、Next.js、React Router、Redux、MobX、Taro、uni-app。发现实际兼容问题，先记录证据并给出最小调整；不得擅自更换技术体系。

## 实现和文案

业务说明、注释、错误提示和用户文案使用中文；技术名称、标识符可用英文。直接使用官方 Ant Design 和 TDesign 组件，不创建仅透传属性或重复官方 API 的无意义 Wrapper。只有承载明确业务规则、可访问性或跨端协议的封装才有价值。Ant Design Form 负责表单，Query 负责服务端数据，Zustand 仅管理少量跨页面客户端状态。

当前后端基础包固定为 `com.pet.platform`，源码目录固定为 `apps/backend/src/main/java/com/pet/platform/`，启动类固定为 `com.pet.platform.Application`。后续模块均位于该基础包下：`com.pet.platform.shared`、`com.pet.platform.platform`、`com.pet.platform.identity`、`com.pet.platform.customeridentity`、`com.pet.platform.attachment`、`com.pet.platform.audit`、`com.pet.platform.notification`、`com.pet.platform.modules`。P02-01 创建启动入口与必要工程配置，其他模块仍为规划；未来初始化工具生成其他项目时可支持显式替换 package、项目名和工程标识，不能改变当前项目的固定包名。业务模块采用 api/application/domain/infrastructure；不要为分层创建无行为的空类。基础能力与项目特定业务分开；本模板不包含宠物、寄养、房间、预约、订单、优惠券、积分、具体经营统计。

## 身份和租户

采用 Sa-Token 服务端会话，不叠加自建 JWT 刷新体系。平台管理员、租户员工、客户认证空间隔离，令牌和 Cookie 名称、读取策略、会话键前缀也需要隔离。Web 默认同源安全 Cookie，配套独立 CSRF 机制；不能仅靠 SameSite 声称解决 CSRF。小程序使用 Token，跨源 Web 部署是需要单独验证的可选方案。

tenantId 只能来自经过验证的可信身份；请求中的 tenantId 不授予访问权。单租户和多租户共用隔离机制；Tenant、可选 Store、内部 Organization 不得混淆。查询、写入、批量操作、原生 SQL、附件、导出、异步任务、消息、缓存都必须隔离；无可信上下文默认拒绝。平台管理通道须显式授权和审计，不得作为隐式绕过租户限制的路径。

## 变更与验证

先读适用规则和现有实现，保持变更聚焦，不修改无关文件，不删除或覆盖其他项目成果，不修改全局 Java/Node.js/包管理器配置，不输出秘密环境变量，不生成真实账号、密码、密钥。可在明确临时目录进行必要探针。

禁止用 Mock 页面、假业务数据、文档检查或内存数据库作为真实业务验收。单元测试的技术夹具须与业务验收明确区分。后续数据库集成用真实 PostgreSQL/Testcontainers；测试覆盖租户越权的查询和写入、身份跨域、无上下文拒绝、附件与异步传播。

每项检查保留工作目录、命令、退出码、输出摘要及证据；分别记录 DOCUMENTED、RESOLVED、COMPILED、RUNTIME_VERIFIED、NOT_VERIFIED，等级不得相互代替。无法执行记 NOT_EXECUTED 和原因，不伪造 PASS。新增或修改依赖后执行受影响的编译、类型、构建或集成检查，链接和事实源保持一致。

不自动提交、推送、发布或部署；不建立逐个可逆操作都请求确认的流程，不建立审批工作流。阶段推进以当前明确任务授权为边界，P01-01 完成不等于 P01 全部完成。
