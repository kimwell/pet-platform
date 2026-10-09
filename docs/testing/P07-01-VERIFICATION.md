# P07-01 员工管理后端查询与授权验证

日期：2026-10-09；根目录 `/Users/kimwell/work/pet-platform`；后端基础包 `com.pet.platform`。本轮依据用户明确授权执行；不使用治理框架、不提交/推送/部署、不自动执行下一任务。P06 COMPLETE 与 Web 127 项为本轮输入及既有证据；后端历史 421 项不能替代当前回归。

## 结果与实施边界

P07-01 **COMPLETE**；G01～G14 **PASS**；P07 **IN_PROGRESS**。当前真实员工读取接口、独立权限、范围映射、白名单DTO、查询/分页、显式权限补充及三端类型完成。没有员工创建/编辑/停用、角色或门店授权修改API、批量/导入导出、平台跨租户员工查询、客户资料管理、Web Table或假页面。既有P05安全API不计为本轮新增管理功能。

详细供页面接入的权威语义见 [EMPLOYEE-MANAGEMENT](../contracts/EMPLOYEE-MANAGEMENT.md)，生产声明见 [OpenAPI](../../packages/api-contracts/openapi/backend.openapi.json)。

| 接口 | 权限 | 返回 |
| --- | --- | --- |
| GET `/api/admin/identity/users` | `identity:user:list` 查看员工 | items/page/pageSize/total；total为十进制字符串 |
| GET `/api/admin/identity/users/{employeeId}` | `identity:user:detail` 查看员工详情 | EmployeeView；独立行范围 |

两接口均STAFF正式会话。列表/详情相同六字段：id/loginName/displayName/status/createdAt/updatedAt；UTC时间三位毫秒+Z，UUID和枚举沿用协议。不返回Entity、tenantId副本、密码、内部版本、Token/OpenID/会话、角色/门店关系、权限全集或伪写能力。无列表权限不影响本人改密/退出。

## 前置事实和结构

核对AGENTS.md、README、P04/P05/P06验收、AUTHORIZATION/MULTI-TENANCY/MODULE-BOUNDARIES、API/IDENTITY/PAGINATION/DATA-TYPES、PERSISTENCE/BACKEND、ROADMAP/DECISION-LOG/ACCEPTANCE-MATRIX及实际生产模型/加载/迁移/测试。Employee、Role、EmployeeStore沿用现有正式模型；Employee没有单一storeId，Store归platform事实源。现有identity:user:list复用，新detail只登记PermissionCatalog，不改原ADMIN_PERMISSIONS/V1九项授权。

EmployeeController只协议转换；EmployeeDirectory选当前权限并协调只读REPEATABLE_READ事务；EmployeeQueryStore为本模块固定端口；EmployeeQueries在identity.infrastructure使用ScopedPersistence投影。shared只注册静态关系类型/属性，不依赖identity、不跨模块访问Repository。16项结构/导出检查在完整146项单元检查内实际执行；原结构反例及生产包隔离检查保留。

## 操作范围、筛选和数据库

| 范围 | 实际映射 / 验证 |
| --- | --- |
| TENANT | 当前可信租户全部员工，含无门店、多门店、停用；跨租户记录/count不可见 |
| SELF | 当前STAFF主体ID等于Employee.id；无门店本人仍可见，不按createdBy |
| STORES | 目标EmployeeStore与**当前权限**有效门店集合任一交集；集合已与操作者ACTIVE门店上限相交；多门店只返回员工基础字段 |
| 空STORES / 无有效授权门店 | 无行、total="0"；无门店他人不可见 |
| 混合 | 同权限STORES与SELF并集、TENANT归一化；不同权限互不扩权；目标状态不改变读取，身份/角色/门店当前事实每请求加载 |

查询固定tenant AND 当前操作范围 AND 筛选。关系用相关EXISTS，不join根行，不在内存/分页后过滤；count统计员工根行，详情同策略投影。一次列表items/count同事务快照；不承诺不同HTTP翻页间并发快照一致。实测未筛storeId列表2条Hibernate SQL、详情1条、0 Entity加载；指定storeId先额外检查正式Store事实，没有关联N+1。

仅keyword/status/storeId/page/pageSize/sortBy/sortOrder；没有日期或未实现筛选。keyword账号或姓名大小写不敏感字面量包含，1～100 Unicode码点、非全空白、不trim；反斜线/%/_转义，Criteria绑定参数。status实际ACTIVE/DISABLED。storeId须本租户ACTIVE且当前权限与身份门店上限允许，SELF不能单独授门店筛选；不合法/跨租户/无权同404。

分页1起、20默认、100最大、total保持string；六公开字段排序白名单，默认createdAt desc/id desc，其他主字段同向追加id，显式NULLS LAST。超末页保留请求page与真实total，超int offset能力422；重复、未知、非法值沿用400/422协议。不存在、跨租户、范围外详情相同404；无权限403，无身份401；数据库故障503并可恢复。

Employee与EmployeeStore原FORCE RLS提供事务局部tenant底线；运行登录角色为NOSUPERUSER/NOBYPASSRLS，列权限禁止password_hash。应用负责当前操作、SELF/STORES及字段白名单。无GUC0行、当前租户实际7行、跨租户ID0行、清理后关系0行、凭据列和升级函数拒绝均实际验证；超用户仅建立/检查隔离技术夹具，不作为RLS有效证据。

V5增加默认排序索引和受限显式补充函数，不改V1～V4、不自动写角色授权。`scripts/backend-identity.sh employee-read-upgrade --tenant-id <规范UUID v4>`只给指定租户ACTIVE tenant-admin复制已有list范围到detail，不接受任意权限/角色参数；新增时原子递增关联员工authorizationVersion/version/updatedAt，重复/并发幂等。NOLOGIN owner、固定search_path、明确schema、局部tenant政策，PUBLIC/runtime无EXECUTE/runtime不能SET ROLE。生产JAR命令成功0、幂等0、runtime凭据拒绝2；普通角色管理仍待后续。

## 测试环境和证据性质

使用 [冻结工具链核验](evidence/P07-01/toolchain-final.json) / [实际输出](evidence/P07-01/toolchain-final.log)，只为本轮子进程设置JAVA_HOME/PATH，没有改全局配置或依赖。PostgreSQL/Redis镜像沿用测试事实源中的冻结digest；独立Testcontainers、正式角色provision和Flyway V1～V5，不连接日常或生产数据库。

EmployeeDirectoryIT的操作者通过正式IdentityBootstrap与独立bootstrap登录角色建立，STAFF Token和Web Cookie经真实HTTP登录/CSRF，唯一生产SessionPrincipalProvider；目标员工及角色/门店正反例由隔离SQL技术夹具布置，没有TestProvider绕认证。这些夹具不表示已实现员工写API。CUSTOMER跨域反例使用正式客户登录/会话/Redis链，外部微信交换边界使用测试Gateway；PLATFORM使用正式初始化与真实Cookie登录。**本轮未验证外部微信**，不替代P05已有外部认证证据。

401跨域码保留已有载体语义：错误域Header AUTH_DOMAIN_MISMATCH；CUSTOMER opaque放STAFF Header SESSION_EXPIRED；仅PLATFORM Cookie AUTH_REQUIRED。三者均无员工访问权，没有为统一断言修改冻结认证策略。

## 本轮命令及退出码

| 工作目录 | 实际命令 | 退出码 / 摘要 |
| --- | --- | --- |
| apps/backend | `./mvnw clean verify` | 0；455项0失败/错误/跳过；[记录](evidence/P07-01/verify-final.json) |
| 项目根目录 | `pnpm contracts:generate` | 0；生产及测试导出10项+类型检查；生成成功；[记录](evidence/P07-01/contracts-generate.json) |
| 项目根目录 | `pnpm contracts:check` | 0；重新导出10项、产物一致且类型检查通过；[记录](evidence/P07-01/contracts-check.json) |
| 项目根目录 | `pnpm --filter @pet/api-contracts typecheck` | 0；严格类型检查；[记录](evidence/P07-01/api-types.json) |
| 项目根目录 | `pnpm --filter @pet/admin-web typecheck` | 0；Web类型检查；未改业务页面；[记录](evidence/P07-01/web-types.json) |
| 项目根目录 | `pnpm --filter @pet/wechat-miniprogram typecheck` | 0；小程序类型检查；不等于DevTools编译；[记录](evidence/P07-01/mini-types.json) |
| 项目根目录 | `pnpm contracts:test` | 0；3项通过；[记录](evidence/P07-01/contracts-tests.json) |
| 项目根目录 | `python3 /tmp/pet-p07-snapshot.py` | 0；原421逐项保留+34；生产包无测试class重叠；[记录](evidence/P07-01/snapshot-final.json) |
| 项目根目录 | `pnpm check:repo` | 0；冻结依赖/单锁/源码秘密特征与小程序结构；[记录](evidence/P07-01/repo-check.json) |
| apps/backend | `./mvnw -DskipTests package` | 0；契约clean后补回生产JAR，跳过测试仅限本补包命令；[记录](evidence/P07-01/package-final.json) |
| apps/backend | `./mvnw -Dit.test=ProductionArtifactIT,EmployeeReadUpgradeRuntimeIT failsafe:integration-test failsafe:verify` | 0；补包后3项产物/命令复验通过，不重复计算为新增；[记录](evidence/P07-01/package-check.json) |
| 项目根目录 | 产物/diff/链接/保全审计 | 0；[产物审计](evidence/P07-01/artifact-final.json)、[最终记录](evidence/P07-01/audit-complete.json)、[审计结果](evidence/P07-01/final-audit.json) |

完整回归：**455项（146单元/结构/协议 + 309集成），失败0、错误0、跳过0**。与P05-05最近完整基线逐项比较，原421项全部保留，新增34项（员工HTTP32、V4升级1、生产包命令1）；见 [计数及逐项比较](evidence/P07-01/test-summary.json)、[完整报告](evidence/P07-01/verify-final-reports/)、[回归日志](evidence/P07-01/verify-final.log)。Web127项在本轮未重跑，类型检查是当前执行；不把历史Web测试称成本轮测试。

实际公开21条路径，新增2条读取路径；生产Controller/DTO动态导出，HTTP公开文档与响应字段核对，类型生成后再独立一致性检查。schema、共享类型及小程序同步类型见文件清单；无手写DTO副本。

生产JAR通过ProductionArtifactIT/PackagedDocumentPolicyIT与实际离线命令；[归档清单及SHA](evidence/P07-01/production-artifact.json)无测试class重叠、无编译占位错误，包含V1～V5。最终完整报告和实际HTTP观察先归档，再运行会clean target的contracts任务，避免用后续10项导出报告替代455项结果。契约任务完成后重新打生产包并单独复验3项产物/命令测试，当前target保留可执行JAR；[当前产物SHA/边界](evidence/P07-01/production-artifact-final.json)与[3项复验报告](evidence/P07-01/package-final-reports/)另存，不覆盖原完整报告。HTTP观察不存Token/密码/Cookie，请求成功集合、计数及字段不是只断言异常。

## 必选测试覆盖

EmployeeDirectoryIT下方法名与需求对应，详细观察见 [脱敏HTTP输出](evidence/P07-01/verify-final-reports/p07-01-observations/)；原回归继续覆盖NULLS LAST技术实体和认证故障/撤销/并发边界。

| 需求 | 实际场景 / 方法 |
| --- | --- |
| 1～3 无身份、无权限、身份域 | anonymousListAndDetailRequireIdentity；missingIndependentReadPermissionsAre403；validCustomerAndPlatformCredentialsCannotReadStaffDirectory；webCookieUsesTheSameFormalReadContract |
| 4～8 TENANT/SELF/STORES/无门店/多门店/各权限 | tenantListAndDetailContainOnlyThisTenant；selfListAndDetailMapToStaffSubjectId；storesUseAnySharedActiveAuthorizedStoreWithoutRelationLeak；noAuthorizedStoresMeansEmptyStoresScope；storesAndSelfUnionIncludesNoStoreSelfOnly；tenantMixedWithOtherScopesNormalizesWithoutCrossTenant；listScopeCannotEnlargeDetailOrSensitiveManagement |
| 9～12 total、跨租户/隐藏/不存在、门店过滤 | 上述集合+total断言；hiddenUnknownAndForeignDetailsHaveIdenticalErrors；storeFilterNeverEnlargesCurrentScope；tenantStoreFilterStillChecksIdentityStoreCeiling |
| 13～14 keyword边界及特殊字符、非法/重复参数 | keywordMatchesAccountAndNameIgnoringCaseLiterally；keywordLengthAndBlankBoundaries（含100/101非BMP码点）；illegalStatusIdUnknownAndRepeatedFiltersAre400；invalidPaginationAndSortFollowFrozenErrors |
| 15～16 稳定排序/空页/多对多去重count | sameValueSortAndManyToManyPagesHaveStableUniqueIds；emptyFilterAndBeyondLastPagePreserveTotalAndRequestedPage；allPublicSortFieldsWorkInBothDirections |
| 17 DTO白名单 | storesUseAnySharedActiveAuthorizedStoreWithoutRelationLeak；publicOpenApiSchemaMatchesActualDtoAndQueryFields；投影不加载Entity或凭据列 |
| 18 撤权新请求 | permissionAndRoleRemovalImmediatelyAffectNewRequests；nextRequestRechecksStoreAssignmentEvenWithoutVersionBump；inactiveStoreRevokesStoresVisibilityOnNextRequest；explicitUpgradeIsLimitedIdempotentAndChangesCurrentAuthority |
| 19 账号/线程/连接复用 | accountAndSingleConnectionReuseNeverMixTenantRows：两个正式STAFF会话交替12次，单线程客户端、服务Hikari最大1连接，匿名和无GUC另核；每例结束Holder/Provider为空 |
| 20 公开OpenAPI与HTTP | publicOpenApiSchemaMatchesActualDtoAndQueryFields；ProductionOpenApiExportTest；contracts:generate/check；类型编译 |
| 额外有意义检查 | listAndDetailProjectionHaveNoNPlusOne；databaseReadFailureReturns503ThenRecovers；rlsRestrictedRoleDefaultsToNoRowsAndCannotSelectCredentials；noReadPermissionDoesNotBlockSelfPasswordChange；targetStatusDoesNotGrantOrRemoveReadVisibility；V4升级/并发/幂等及生产命令 |

## 失败历史及修复

失败不删除、不伪造PASS。compile-first退出1（ApiResponse导入歧义）后compile-second0；focused-first1（Java复合var声明），修复测试编译；focused-second1（测试微信配置secret属性名/timeout上限）修正测试配置；focused-third1（非clean测试class出现Unresolved compilation problems，发生来源未确证），后续clean重新编译；focused-fourth1（CUSTOMER opaque误期望域错误码），改为既有STAFF空间SESSION_EXPIRED。相应日志/报告保留在本证据目录；这些早期尝试未确认冻结工具链，只作故障诊断，不作为最终门禁。

verify-first完整455项退出1：旧生产路径数19→21、受限函数总数5→6、PLATFORM Cookie在STAFF路由应为AUTH_REQUIRED三处断言；保留安全属性检查并新增函数runtime拒绝检查，没有降低隔离策略。随后确认shell默认Java偏离冻结版本，最终clean verify用冻结Temurin重跑。首次toolchain检查因旧临时pnpm目录不完整退出1，利用已有冻结tgz在本轮临时目录恢复；toolchain-final0。未改用户全局工具、未升级依赖。既有故障注入日志的DB/Redis拒绝和连接警告由相应测试验证，不表示最终测试失败；事件仅限测试边界。首轮保全审计退出1是自身将要生成的索引/审计链接尚不存在，修正输出顺序后复验；不涉及生产代码失败，记录保留audit-first。

## G01～G14

| 门禁 | 结果 | 证据 |
| --- | --- | --- |
| G01 前置模型/权限/路线 | PASS | 实际模型/初始化/权威加载/RLS核对；独立权限及本契约 |
| G02 列表/详情真实可用 | PASS | 正式Token/Cookie + PG/Redis随机端口HTTP实际集合/详情 |
| G03 操作权限和目标范围 | PASS | 固定list/detail适配器、缺权403与有权范围外404 |
| G04 TENANT/STORES/SELF | PASS | 单权限/混合/无门店/多门店/状态和门店撤销 |
| G05 多对多分页/去重/total | PASS | EXISTS、稳定分页实际唯一集合与root total |
| G06 筛选不能扩权 | PASS | Store事实+范围上限、keyword转义、非法/重复参数 |
| G07 DTO不泄漏 | PASS | 六字段一致，真实HTTP字段全集和0Entity加载 |
| G08 分页/排序协议 | PASS | 页码/默认/最大/total字符串、六字段/稳定id/NULLS LAST、超末页/深offset |
| G09 RLS与受控查询 | PASS | 受限登录角色无GUC/跨租户/凭据列/函数拒绝；完整PG回归 |
| G10 正式认证/撤销 | PASS | 唯一Provider、Token/Cookie/跨域、角色权限门店撤销新请求 |
| G11 原有和新增测试 | PASS | 455项0失败/错误/跳过，原421逐项保留+34 |
| G12 OpenAPI/类型/运行一致 | PASS | HTTP schema+JSON、generate/check与三端typecheck0 |
| G13 未提前写入/假页面 | PASS | 生产新路径仅GET；离线受限权限补充；既有Web源码保全 |
| G14 文档/证据完整 | PASS | 契约/规则/路线/决策/矩阵、命令退出/报告、文件和保全审计 |

## 文件、保全与限制

本轮新增/修改文件及SHA见 [文件清单](evidence/P07-01/file-manifest.json)；与启动前工作区对照见 [最终保全审计](evidence/P07-01/final-audit.json)。既有ui、Web业务源码、P06文档/历史证据、依赖/POM/锁、V1～V4及其他无关成果保留；README/ROADMAP/DECISION-LOG/ACCEPTANCE-MATRIX在既有修改上新增当前P07记录。秘密私有配置未读取或改写。没有提交、推送或部署。

| 边界 | 等级 / 限制 |
| --- | --- |
| 契约及范围决策 | DOCUMENTED；实际响应/生成schema已RUNTIME_VERIFIED |
| 冻结工具、依赖基线 | RESOLVED；没有新增/升级依赖；受影响Java/类型COMPILED |
| 员工读取与授权 | RUNTIME_VERIFIED，仅本机真实HTTP/独立PG/Redis正式认证，技术目标/授权夹具 |
| 生产角色部署、TLS/代理/跨源、远程CI、多OS/生产性能 | NOT_VERIFIED；本轮部署NOT_EXECUTED（未授权） |
| 外部微信、浏览器员工Table、小程序业务/真机、写入/组织角色管理 | 本轮NOT_EXECUTED（非本任务范围）；不能以类型检查宣称完成 |

P07-01 COMPLETE不等于P07 COMPLETE。按原路线下一合法建议：**P07-02：企业级员工列表页、URL筛选分页排序与查询状态基础**（NOT_STARTED）；复用正式API、P06请求/完整身份Query Key和当前权限，total安全转换及真实空/错/加载态。只报告，不自动执行；详情入口仍须独立detail权限，门店选择事实、写功能与角色管理按后续实际授权拆分。
