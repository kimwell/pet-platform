# OpenAPI 类型生成与协议漂移

冻结协议来自P01-02，P03-03于2026-10-08接入实际流程。协议及生成工具精确版本唯一见 [版本矩阵](../development/VERSION-MATRIX.md)。后端实际类型、注解、编解码与springdoc导出是事实源；不手写竞争DTO，不创建生产演示接口。运行证据见 [P03-03](../testing/P03-03-VERIFICATION.md)。

## 产物与边界

```text
packages/api-contracts/
├── openapi/backend.openapi.json           # 实际生产应用：P05-02有五个STAFF认证路径
├── openapi/test-contract.openapi.json     # 测试专用Controller：仅生成器验收
├── src/generated/api.d.ts                 # 生产公共模型
├── src/index.ts                           # 只export type
├── test/generated/test-contract.d.ts      # 测试模型/具体泛型，不对外导出
├── test/contracts.typecheck.ts            # 严格类型断言
└── package.json / tsconfig.json
```

以下为空paths的P03历史；P05-02当前五个认证路径见文末。生产文档当时没有正式业务路径。OpenApiConfiguration通过生成器显式注册实际 ApiError/FieldErrorDetail/Failure/PageQuery，以及实际PageResponse与Success的具体FieldErrorDetail/Void特化；字段仍来自Java类型。这些特化是公共泛型验收模型，不表示存在分页字段错误业务接口。没有CurrentIdentity类型/认证实现，因此本轮不伪造它们或security schemes；未来身份阶段按冻结契约接入，不设假的全局security。

测试契约来自src/test的ContractFixtures，只有`/__contracts/**`技术路径。独立的ProductionOpenApiExportTest没有导入测试Controller；ContractExportTest单独启动测试文档上下文。冻结组件的全局扫描条件先于分组，测试上下文显式限定测试包；生产上下文仅扫描com.pet.platform。测试文档不发布为业务API清单，不进入公共包导出或小程序同步。

## 环境策略

| 环境 | OpenAPI JSON / UI / 访问 |
| --- | --- |
| local | `/v3/api-docs`、`/v3/api-docs.yaml`启用；`/swagger-ui/index.html`启用、交互提交按钮关闭；绑定回环地址，否则启动拒绝 |
| test | 默认都关闭；导出测试显式开启JSON、UI保持关闭；随机回环端口、自有Testcontainers PostgreSQL；无开发者服务/数据库依赖 |
| prod | 文档和UI默认关闭，启用任一项的外部覆盖被启动门禁拒绝；生产JAR路径404已实测 |

文档标题来自OpenApiConfiguration，版本从POM的pet.api.version经资源过滤进入配置；不含构建时间。生产扫描不含测试包，排除/error、Actuator和测试路径；ProtocolErrorController有Hidden声明。Actuator仍返回原工具格式，不包装信封。

## 实际schema映射

| 类型 | schema / TS |
| --- | --- |
| UUID | string/uuid与小写v4 pattern → string |
| PageResponse.total | string与非负十进制pattern → string；绝非integer/int64 |
| CNY金额 | string，输入0～2位小数、输出恰2位与范围pattern → string；来自金额注解的ModelConverter |
| DecimalCounter | string/pattern、19字符上限和Long范围说明 → string；越界由HTTP校验拒绝 |
| Instant/LocalDate | string/date-time（输出模型包含固定毫秒UTC pattern）；string/date → string |
| Enum/Boolean/受界整数 | 字面量union / boolean / number；分页、进度schema有边界 |
| 成功 | 实际Success<T>解析保留具体泛型，success const true；data必填且与null联合；Void仅null |
| 失败 | 实际Failure，success const false；error/traceId必填，无data。ApiError code/message必填，fieldErrors可省略且存在时至少1项 |
| null/省略 | required + null联合 → `T | null`且必有字段；非required → `field?:T`；输出optional字段省略null，输入可空必须独立声明 |

对象schema默认additionalProperties:false；以后合法字典须明确专门声明。public Success/Failure仍只由原工厂创建，私有构造器保留；HTTP信封未改变。没有未约束的data object或前端手写修补。生成器对未声明响应headers保留unknown索引，这是工具元数据，不是DTO数据模型。SchemaChecks逐字段检查本轮输出所用关键字，不是完整JSON Schema规范验证器。

## 确定性生成与检查

根目录使用矩阵的Java/Node/pnpm，Docker必须可用：

```sh
pnpm install --frozen-lockfile
pnpm contracts:generate
pnpm contracts:check
pnpm contracts:typecheck
pnpm contracts:test
pnpm --filter @pet/admin-web typecheck
pnpm --filter @pet/wechat-miniprogram typecheck
cd apps/backend
./mvnw clean verify
```

`contracts:generate/check`均自行执行Wrapper clean及专用导出测试，将真实HTTP原始文档与SHA写入任务临时目录；从未依赖恰好运行的后端。隔离PostgreSQL只使用技术容器，不接触开发者库或生产库。链路：后端HTTP导出→规范化JSON→openapi-typescript生成→小程序同步/比较→api-contracts严格tsc。CI运行相同check，再运行完整clean verify与两端类型/构建检查。

规范化只移除springdoc按临时请求生成的loopback server，稳定对象key；保留显式servers以及路径、required、null、enum、security等语义。没有时间戳、绝对路径、随机端口或本机地址进入提交产物。原始SHA留在导出目录，验证证据保留本轮原始快照；它不等于规范化文件SHA。

check在临时目录重新生成，比对两份OpenAPI、两份声明和小程序生成声明，发现差异exit1并列文件；不写仓库产物，不依赖git HEAD，main没有提交也可运行。实际注入total string→number已被拒绝并验证未被check覆盖。此为**生成产物一致性检查**，不是完整破坏性变更分类/兼容性分析；即使开发者同步接受破坏性变化，本检查也不能代替owner兼容评审。该能力限制保持明确，不用生成成功证明旧客户端兼容。

Web以import type从@pet/api-contracts读取components；workspace依赖只有types出口，不含fetch/DOM/wx/状态/请求实现。小程序将同份公共声明同步到`miniprogram/types/generated/api.d.ts`，首行含来源SHA；import type使用本地相对路径，微信运行时无需解析workspace包。手写消费文件位于生成目录外。同步只清理固定生成目录中带生成标识的旧文件，拒绝子目录、符号链接及未标记源码；check发现旧生成文件直接失败。脚本反例验证不覆盖手写文件，也不保留已删除生成类型。

本轮类型接入不实现HTTP Client/业务页面，tsc不代替微信工具或真机运行。完整破坏性分析、远程CI及多OS运行未验证；[验收矩阵](../testing/ACCEPTANCE-MATRIX.md)保留各后续阶段owner。

## P05-02 正式认证文档（2026-10-08）

生产paths由历史空集变为STAFF五个实际接口（csrf、Web login、token/login、me、logout）；实际CurrentIdentity/Scope union/CsrfResult/TokenLoginResult及具体Success泛型由Controller导出，成功200与各错误信封显式声明。StaffCookie是生产Cookie名称，StaffToken是X-Staff-Token（Bearer格式），受保护端点为OR安全要求；Web登录Header CSRF和来源要求已声明。匿名端点不加假的全局认证。

生成流程仍为独立后端导出、规范化、openapi-typescript、同步小程序声明和严格类型检查；原测试OpenAPI保留技术路径，只进入test/generated，不混入公共产物。命令、真实结果与SHA见[P05-02](../testing/P05-02-VERIFICATION.md)。本轮不实现前端请求层或页面。

## P05-03 安全接口生成（2026-10-08）

生产路径增至九个STAFF接口，新增ChangePasswordInput、ConfirmationInput、ResetPasswordInput、RevokeSessionsInput及CurrentIdentity.passwordChangeRequired必填布尔字段。密码字段writeOnly/password，管理员version为十进制字符串，成功data=null、X-Session-Cleanup为COMPLETE/PENDING，400/401/403/404/409/422/429/503均统一失败信封。Cookie/Token二选一及独立CSRF/Origin语义保留；测试故障/队列路径不进入生产schema或公共声明。按既有脚本从实际Controller生成三端纯类型，不新增页面或请求实现。

## P05-04 双身份类型（2026-10-08）

正式路径新增六项PLATFORM，合计15。原CurrentIdentity保持STAFF判别枚举、非空tenantId/ScopeData及passwordChangeRequired；平台独立PlatformCurrentIdentity为PLATFORM、tenantId/dataScope必填null、authorizedStoreIds空数组约束，没有无约束object或将STAFF tenant改可空。端点返回明确DTO，消费者用principalType判别联合。PlatformCookie只COOKIE，不提供平台Token安全方案或小程序签发接口。类型从实际Controller生成，平台密码复用原writeOnly安全输入，响应null与清理Header准确，不修改前端页面。

## P05-05 当前实际导出

生产19路径（原15+客户4），实际CustomerCurrentIdentity/CustomerTokenLoginResult/CustomerToken安全方案加入；身份分别严格STAFF/PLATFORM/CUSTOMER，AuthenticatedIdentity只组合生成模型。小程序只同步生成声明，不开发登录页面/请求层。contracts:generate/check仍由真实生产Java应用导出，独立测试schema不进公共包/生产JAR。[当前验证](../testing/P05-05-VERIFICATION.md)记录实际命令，不把类型通过当真实微信认证或P09验收。
