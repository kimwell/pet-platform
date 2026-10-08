# P03-03 标量、OpenAPI、三端类型与结构检查验证

日期：2026-10-08（Asia/Shanghai）。**P03-03 COMPLETE；G01～G15 PASS。P03整体 COMPLETE。** 结论来自P03-01/P03-02既有有效运行证据、本轮真实HTTP/PG/生成/编译/产物检查及原阶段条件合并核对。生产独立迁移与角色权限、远程CI、多OS仍未验证，分类见第9节。没有提交、推送、部署或自动开始P04。

## 1. 前置事实与授权

按用户本轮明确P03-03授权直接在根目录执行；它替代AGENTS旧P02-01阶段限制，其技术/包名/保全规则继续适用。未创建子项目/worktree、未移动ui、未使用其他治理框架，未实现业务接口、HTTP Client、认证或租户功能。

已读取用户指定的AGENTS/README、TECHNICAL-BASELINE/MODULE-BOUNDARIES/CONFIGURATION、API/PAGINATION/DATA-TYPES/OPENAPI-GENERATION、BACKEND/PERSISTENCE/DATABASE-MIGRATION、VERSION-MATRIX/ROADMAP/DECISION-LOG、P03-01/P03-02报告与ACCEPTANCE-MATRIX，并核对源码、POM、根锁、前端工作区、CI和脚本。P03-02实际报告和JUnit汇总确为62项无失败/错误/跳过；total已是String，成功/失败已有封闭分支，同步REQUEST/ERROR trace沿用。[修改前SHA](evidence/P03-03/baseline-files.json)与[完整变更/保全审计](evidence/P03-03/final-audit.json)区分本轮变更；main尚无提交，不能用HEAD差异作为唯一依据。

应用实际JSON为Jackson 3；冻结springdoc和生成工具按[唯一矩阵](../development/VERSION-MATRIX.md)接入，未升级Boot/BOM或复制Boot3配置。[依赖解析](evidence/P03-03/backend-dependency-tree.txt)确认模型解析的Jackson 2与应用Jackson 3分包并存。[springdoc官方集成与模型扩展](https://springdoc.org/)、[生成器API](https://openapi-ts.dev/node)和[OpenAPI规范](https://spec.openapis.org/oas/v3.1.0.html)作为API依据；精确版本行为以本地编译/HTTP证据为准。小程序仅类型消费，使用miniprogram-development技能核对原生根目录/类型边界，未引入CloudBase、预览或发布。

## 2. 实现与文件

| 范围 | 新增/修改与目的 |
| --- | --- |
| 标量 | 新增shared/serialization下ScalarCodecs、CnyAmount、DecimalCounter；修改ApiConfiguration与GlobalExceptionHandler，专用金额精度异常422，其余JSON错误保持400 |
| 实际公共类型 | 修改ApiResponse的实际Success/Failure可见性与schema注解，私有构造器/工厂保持；修改ApiError/FieldErrorDetail/PageQuery/PageResponse约束注解；total仍String |
| OpenAPI | 新增shared/openapi下OpenApiConfiguration、ProtocolModelConverter；Hidden标记ProtocolErrorController；POM接入冻结组件与文档版本；application/local文档策略 |
| 测试配置隔离 | application-test.yml从src/main/resources原样移动到src/test/resources，生产不再包含测试环境配置 |
| HTTP/schema/PG/产物 | 新增src/test的ContractFixtures、ContractExportTest、ProductionOpenApiExportTest、SchemaChecks、DocumentPolicyTest、PackagedDocumentPolicyIT、ScalarPostgresIT；architecture下StructureRulesTest、ProductionArtifactIT |
| 类型与生成 | packages/api-contracts的生产/测试快照、两份生成d.ts、纯类型入口、严格tsconfig与类型断言；新增scripts/contracts.mjs、contract-files.mjs、contracts.test.mjs；根精确依赖/脚本与唯一锁更新 |
| 两端消费 | Web src/contracts.ts仅import type，workspace依赖只有types出口；小程序types/contracts.ts手写消费与types/generated/api.d.ts同源生成，未修改页面/config/AppID/组件 |
| CI/仓库 | CI接入导出一致性、类型/脚本检查及完整verify；check-repository明确允许唯一内部workspace依赖，外部精确版本规则不放宽 |
| 文档 | 更新用户指定8份规范/路线/验收、README，并修正VERSION-MATRIX、CONFIGURATION、PROJECT-STRUCTURE和包README的实际事实；历史失败/报告保留 |

源码基础包仍com.pet.platform，一个Maven Module、一个pnpm锁，无额外架构库。结构分析复用已有json-smart传递ASM测试依赖；Spring ASM不带泛型signature包的真实失败记录保留。完整逐文件清单及生成产物SHA见[最终审计](evidence/P03-03/final-audit.json)，不把其他未跟踪成果算成本轮新增。

## 3. 实际标量与JSON

完整语义由[DATA-TYPES](../contracts/DATA-TYPES.md)拥有。UUID/Instant/LocalDate是统一HTTP规则；金额仅注解BigDecimal，Long计数仅DecimalCounter，不改所有数值或数据库类型。请求金额允许省略尾零，响应恰两位；超过2位（即使额外尾零）/17整数位422 AMOUNT_INVALID，格式/类型400。Instant输入0～3小数并带offset/Z，输出UTC固定毫秒；拒绝未经定义的精度截断。未标注BigDecimal技术比例仍number。

真实测试接口成功JSON示例（固定trace只作说明，技术模型不是业务数据）：

```json
{"success":true,"data":{"id":"12345678-1234-4234-8234-123456789abc","amount":"12.30","currency":"CNY","version":"9007199254740993","occurredAt":"2026-10-08T02:20:30.120Z","date":"2026-10-08","state":"OPEN","enabled":true,"progress":80,"ratio":1.2345,"items":[],"requiredNullable":null},"traceId":"0123456789abcdef0123456789abcdef"}
```

```json
{"success":true,"data":{"items":[],"page":1,"pageSize":20,"total":"9007199254740993"},"traceId":"0123456789abcdef0123456789abcdef"}
```

成功null仍必含data:null；错误仍只有success/error/traceId，无data，fieldErrors仅在非空时出现。必填可空字段显式null保存，缺失400；NotNull字段null/缺失422。允许空串的文本字段保留空串；可省略输出字段的null/缺失省略。集合null422，空集合[]。Boolean拒绝0/1/字符串；枚举未知/数字400；Integer溢出/错误类型400，合法类型但进度越界422。UUID不接受非规范v4/大写/非字符串。

真实Tomcat随机回环HTTP覆盖这些规则以及金额最大精度、带时区输入、无时区/过精度、非法日历日期、纯日期无偏移、计数超Long范围、null/缺失与错误输入。错误响应无原始秘密标记、rejectedValue、内部异常内容。原协议/trace/分页/排序全部回归。

真实PG临时技术表验证UUID、NUMERIC(19,2)、bigint、timestamptz(3)、date往返，在America/New_York数据库时区仍保真；不新增业务迁移。JDK自然日反例确认DST的23/25小时，不声称已有租户日期查询接口。原Flyway/JPA迁移、validate、事务及数据库分页测试不跳过。

## 4. OpenAPI与运行一致

生产OpenAPI当前paths={}；只显式注册真实公共模型。测试OpenAPI四个技术路径，只用于生成器验收；没有生产假接口或竞争DTO，测试Controller/Entity/配置/migration不进JAR。标题代码定义，版本从POM过滤。

| 实际字段/分支 | OpenAPI / 生成TS |
| --- | --- |
| ID | string/uuid+小写v4 pattern / string |
| total | string+规范非负十进制pattern / string，绝非integer/int64 |
| amount | string+请求/响应各自小数pattern / string，绝非number |
| version计数 | string/pattern/长度及Long范围说明 / string |
| occurredAt | string/date-time，输出固定毫秒UTC pattern / string |
| date | string/date / string |
| Enum/Boolean/受界整数 | enum union / boolean / number；进度与分页限制保留 |
| Success<T> | success const true；具体泛型data必填、与null联合；Success<Void>只null |
| Failure | success const false；error/traceId必填，无data |
| ApiError | code/message必填；fieldErrors可省略、存在时至少1项 |
| requiredNullable/optional | 必有string|null / 可省略string；输入optional可空、输出null省略 |

SchemaChecks对本轮实际HTTP输出逐字段检查type/ref/required/enum/const/pattern/array/范围与null联合；测试类型断言拒绝丢失泛型、金额/total/version变number和nullable/optional混淆。它不是完整JSON Schema验证器；生成器的未声明HTTP headers为unknown字典元数据，不是data无约束object。

local文档和UI回环开启，文档扫描仅com.pet.platform，框架/error/Actuator/测试路径排除。test默认关闭，专用导出上下文只开启文档；生产与测试分别启动，避免冻结组件的全局过滤覆盖分组。prod默认关闭且启动门禁拒绝开启覆盖。没有认证实现或虚构security schemes。

生产JAR实际运行：[HTTP/退出记录](evidence/P03-03/p03-03-document-runtime.txt)；local /v3/api-docs与/swagger-ui/index.html均200，生产路径为空；prod均404；两环境测试路径均404。正常SIGTERM实际exit143并出现优雅关闭/连接池关闭，不伪写exit0。[local日志](evidence/P03-03/p03-03-local-jar.log)、[prod日志](evidence/P03-03/p03-03-prod-jar.log)。Actuator健康保持工具格式UP、没有信封；原故障健康证据沿用P03-02。

## 5. 类型生成、消费与漂移

根命令contracts:generate/check自行启动专用测试应用及独立Testcontainers PostgreSQL，Wrapper clean→HTTP导出原文/SHA→仅移除动态loopback server并稳定key→openapi-typescript→类型/同步或比较→严格api-contracts tsc。不用开发者已启动服务，不接触生产/日常数据库或Redis。导出原文/SHA已存本证据目录raw-*，生成快照位于packages/api-contracts/openapi。

公共包只有types出口和export type，无DOM/wx/网络/状态实现；测试声明不导出。Web import type从@pet/api-contracts消费；小程序从本地生成d.ts消费，无workspace运行时解析。同步头含公共声明SHA，输出确定；只删除固定生成目录中带标识的旧生成文件，拒绝手写源码、子目录、符号链接。脚本测试覆盖保全/清理。Web构建JS仍621.32kB，与既有入口一致，类型消费没有新增运行时模块；原500kB警告保留。

check在临时目录重新生成、比较五个产物，不写仓库，不依赖HEAD。有差异exit1列文件。实际将生产快照total从string改number，check拒绝且没有覆盖注入文件，随后恢复并核对原SHA：[反例记录](evidence/P03-03/drift-negative.json)、[未覆盖/恢复证明](evidence/P03-03/drift-negative-proof.json)。正常完整重生成check exit0。

这是生成产物一致性检查，不是完整破坏性变更分类/旧端兼容分析。CI已接入同一命令，但远程未运行；不以配置或本机类型编译证明远程PASS。

## 6. 结构检查能力

StructureRulesTest读编译class的符号常量池、描述符、泛型、注解；按模块/层次与Repository继承检查shared不依赖具体模块、domain不依赖api/HTTP/持久化适配/线程上下文/WxJava、跨模块只能登记的application公开包且不能Repository、api不直连持久化、生产不引用编译测试类。不是源码字符串扫描。ASM生成20个普通/仅泛型违规关系均被拒绝，允许关系有正例；当前模块为空也有实际反例证据。

ProductionArtifactIT在package之后比较全部编译测试class/测试资源及测试依赖，对测试配置、Controller、Entity、Repository、migration进行隔离；违规Controller/migration产物夹具被拒绝。实际JAR没有测试夹具或Testcontainers，application-test已移出主资源。[产物SHA/项目类](evidence/P03-03/artifact-audit.json)。

结构约束不能验证SQL内容、反射字符串/动态模块、运行时授权或租户数据隔离。范围Repository、租户表/SQL注册与角色/RLS属于P04，不能替代数据库越权读写测试。application包门禁也不自动证明每个公开接口行为正确。限制已同步[MODULE-BOUNDARIES](../architecture/MODULE-BOUNDARIES.md)。

## 7. 实际命令与原始失败

全部记录含cwd、argv、退出码、开始结束时间及日志摘要；由本阶段run-check.py使用任务专用冻结JAVA_HOME/PATH运行，没有改全局工具。cwd省略根前缀。以下最终检查不是静态文档验收：

| cwd | 命令 | 退出码/结果 | 记录 |
| --- | --- | --- | --- |
| apps/backend | ./mvnw dependency:tree -DoutputFile=../../docs/testing/evidence/P03-03/backend-dependency-tree.txt | 0 / RESOLVED | [dependencies](evidence/P03-03/dependencies.json) |
| 根 | pnpm install；pnpm install --frozen-lockfile | 各0 / 唯一锁、精确新增冻结生成器 | [安装](evidence/P03-03/install.json)、[回放](evidence/P03-03/frozen-install.json) |
| 根 | pnpm contracts:generate | 最终0 / 包含两次独立HTTP文档导出、类型及同步 | [generate-second](evidence/P03-03/generate-second.json) |
| 根 | pnpm contracts:check | 0 / 临时重新生成、五产物一致、严格类型检查 | [check-first](evidence/P03-03/check-first.json) |
| 根 | pnpm contracts:check（注入total number） | 1 / 预期拒绝、未覆盖、恢复SHA | [drift-negative](evidence/P03-03/drift-negative.json) |
| 根 | pnpm contracts:typecheck | 0 / 自产声明skipLibCheck=false，无浏览器/微信类型 | [api-types](evidence/P03-03/api-types.json) |
| 根 | pnpm --filter @pet/admin-web typecheck | 0 / Web纯类型消费 | [Web](evidence/P03-03/web-typecheck.json) |
| 根 | pnpm --filter @pet/wechat-miniprogram typecheck | 0 / 小程序本地声明消费 | [mini](evidence/P03-03/mini-typecheck.json) |
| 根 | pnpm contracts:test | 0 / 3项稳定化/漂移/生成目录保全反例，无跳过 | [脚本测试](evidence/P03-03/generator-tests-final.json) |
| 根 | pnpm check | 0 / 仓库/小程序配置、两端tsc/lint、原14项Web测试 | [frontend-check](evidence/P03-03/frontend-check.json) |
| 根 | pnpm build:web | 0 / 实际Vite构建，保留原体积警告 | [构建](evidence/P03-03/web-build.json) |
| 根 | pnpm exec eslint scripts --max-warnings=0 | 0 / 新脚本检查 | [lint](evidence/P03-03/scripts-lint.json) |
| apps/backend | ./mvnw clean verify | 最终0 / 79 tests：Surefire57+Failsafe22，失败/错误/跳过均0 | [verify-second](evidence/P03-03/verify-second.json)、[完整日志](evidence/P03-03/verify-second.log)、[逐项汇总](evidence/P03-03/test-results.json) |
| 根 | python3 docs/testing/evidence/P03-03/final-audit.py | 0 / 链接、范围保全、产物SHA与生成源一致；不替代运行 | [最终审计](evidence/P03-03/final-audit.json) |

原62项全部回归，新增17项；原真实PostgreSQL迁移/JPA事务/分页测试未跳过，新增HTTP/OpenAPI/产物、PG标量和结构反例。所有TEST-*.xml已复制至本目录，最终验证后未再修改运行代码。新增依赖对应的Web build、小程序静态结构/类型/lint已执行；没有要求重新进行无关微信真机验收。

真实失败记录保留：structure-first/contracts-first编译因Spring ASM无signature包失败；contracts-second测试包在生产包之外未显式指定Application而失败；contracts-third读取了含Unresolved compilation problems的增量class，来源未证实，clean重编后消除；contracts-fourth文档测试因冻结组件全局过滤导致测试分组为空；contracts-sixth误用Schema.minItems API，改用ArraySchema；generate-first/verify-first被金额输入与schema尾零不一致断言拒绝。对应JSON/log原样保留，逐次编号没有覆盖。修复还补足200具体泛型响应和OpenAPI3.1 types/null建模，不能把端点200单独当G05通过。

## 8. G01～G15

| 门禁 | 结果/等级 | 实际依据 |
| --- | --- | --- |
| G01 前置/冻结 | PASS / DOCUMENTED / RESOLVED | 第1节、原62项报告与依赖树，未升级基线 |
| G02 标量输出 | PASS / COMPILED / RUNTIME_VERIFIED | UUID、CNY尾零/精度、UTC毫秒、纯日期、枚举/Boolean/空集合HTTP |
| G03 输入错误映射 | PASS / RUNTIME_VERIFIED | 类型/日期/枚举/整数400；约束422与金额精度AMOUNT_INVALID；响应无原秘密/异常内容 |
| G04 total保真 | PASS / RUNTIME_VERIFIED | 实际HTTP "9007199254740993"、PGbigint往返、原分页回归、TS string断言 |
| G05 JSON/schema一致 | PASS / RUNTIME_VERIFIED / COMPILED | 实际JSON关键字检查、具体泛型/nullable/optional/error分支及三端类型 |
| G06 自主导出 | PASS / RUNTIME_VERIFIED | generate/check分别自行启动独立应用/PG，原文SHA留证 |
| G07 正式无测试接口 | PASS / RUNTIME_VERIFIED | 无测试导入的生产导出paths空、JARlocal/prod无测试路径 |
| G08 重复类型生成 | PASS / RUNTIME_VERIFIED / COMPILED | generate与check重复生成五产物一致，自产d.ts严格检查 |
| G09 Web消费 | PASS / COMPILED | import type workspace，tsc/lint/build |
| G10 小程序消费 | PASS / COMPILED | 同源SHA本地声明、import type、tsc/结构/lint；不等于真机 |
| G11 真实漂移拒绝 | PASS / RUNTIME_VERIFIED | 注入total number→exit1、文件摘要、无覆盖/恢复证明 |
| G12 违规结构拒绝 | PASS / RUNTIME_VERIFIED（结构技术夹具） | 20个普通/泛型违规关系、测试Controller/migration产物反例 |
| G13 后端回归 | PASS / COMPILED / RUNTIME_VERIFIED | clean verify exit0，79 tests、0跳过，原62项全部回归 |
| G14 生产隔离 | PASS / COMPILED / RUNTIME_VERIFIED | 测试class/资源全比较，真实JAR无测试配置/实体/Controller/migration/依赖，文档策略实测 |
| G15 文档证据 | PASS / DOCUMENTED / RESOLVED | 规范/命令/状态/限制更新，真实记录及最终链接/SHA保全审计 |

DOCUMENTED是规范，RESOLVED是实际解析，COMPILED是编译/打包；RUNTIME_VERIFIED仅覆盖本报告明确的技术运行/反例场景。均不替代业务、生产、远程或设备验收。

## 9. 未执行事项与P03关闭判断

| 事项 | 真实状态及原阶段归属 |
| --- | --- |
| 生产独立迁移任务实际运行、迁移/运行数据库角色权限 | NOT_EXECUTED / NOT_VERIFIED。P03-02冻结策略与prod关闭自动迁移已通过；权限/DDL/RLS反例原属A04-02/P04，独立生产任务运行属部署验证。没有生产环境/授权，不把技术管理员PG容器当权限验收 |
| 远程CI | NOT_EXECUTED / NOT_VERIFIED。CI可复现命令已配置并本机执行；main无提交，本轮禁止推送。不以YAML/本地PASS冒充远程PASS |
| Windows/Linux | NOT_EXECUTED / NOT_VERIFIED。只在macOS arm64运行，保留P01/P02已知限制；没有跨OS执行环境，不作跨平台承诺 |
| 完整破坏性分类/同步更新后的旧客户端兼容 | NOT_EXECUTED / NOT_VERIFIED。本轮明确只做生成产物一致性，真实变更会使检查失败；owner接受变化后仍需兼容验证 |
| 租户/身份/范围Repository、SQL注册、RLS、CSRF、异步上下文 | NOT_EXECUTED / NOT_VERIFIED，P04/P05及后续；明确排除，结构扫描不代替这些测试 |
| 业务自然日查询、PATCH/version并发应用、正式金额业务 | NOT_EXECUTED / NOT_VERIFIED，无正式业务接口；JDK/PG/HTTP技术类型证据不扩大为业务验收 |
| 本轮微信工具/真机、HTTP Client/业务页、发布/部署 | NOT_EXECUTED；类型接入无运行配置/依赖变化，用户明确无需无关真机验收，客户端运行功能在P06/P09 |

核对原P03路线完成条件是“协议与生成类型一致；真实PostgreSQL迁移和事务可重复”。P03-01公共协议、P03-02真实迁移/事务、P03-03完整标量/生成/结构门禁均有有效证据，当前无P03必选失败，因此**P03-03和P03整体COMPLETE**。原P03-02报告已经将生产角色、远程CI和多OS列为后续/已知限制；本轮不临时改变门禁来关闭，也不把这些未执行事项写成已验证。

P03当前没有剩余必选任务。路线P04～P12仍NOT_STARTED：多租户/数据范围、身份/权限、Web基础、员工页、附件/审计、小程序基础、可选消息/微信、初始化模板、全新项目/发布验收。**下一合法任务：P04多租户与数据范围**，按后续明确授权细化执行。本轮不自动开始，不提交、推送或部署。
