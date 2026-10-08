# P01-02 架构与三端契约冻结验证报告

日期：2026-10-07（Asia/Shanghai）。阶段状态：**COMPLETE**；P01整体 **COMPLETE**，P02 **NOT_STARTED**。本报告是P01-02状态事实源；P01-01报告保留历史结果。任务只冻结设计及必要技术验证，不代表运行能力实现，不自动进入P02。

## 1. 前置事实与影响

已实际读取AGENTS、README、范围、技术基线、工程结构、核心契约、版本矩阵、路线、决策及P01-01报告；并核对原始依赖树、测试XML、构建/生成日志和最终审计。P01-01最终COMPLETE，Java技术测试3项0失败/错误/跳过，Web技术运行2项通过，依赖解析/应用源码类型/构建、mini离线组件及OpenAPI探针通过；本轮未重新运行整套P01-01探针，未改写其旧结果。

当前git status --short退出128：not a git repository，无branch/dirty分类。写入前对既有AGENTS/README/docs/ui建立378文件SHA清单：[前置清单](evidence/P01-02/inventory-before.json)、[工作区检查](evidence/P01-02/workspace-check.json)。本轮未读取/更新Product Delivery OS状态，未安装技能/框架，未创建其目录，未移动ui或其他项目成果。

P01-01仍有第三方声明全面检查FAIL（Web TS2430、TDesign TS2344）；源码strict+skipLibCheck基线和隔离miniprogram-ci选择保持。没有影响本轮设计的关键兼容失败，但如果未来要求vendor声明全面检查，先解决上游并重验。PG/JPA迁移/锁、Redis I/O、浏览器Cookie/CSRF、RabbitMQ、DevTools/真机、真实微信仍NOT_VERIFIED，不能从文档冻结推导PASS。

## 2. 核心结论与决策

D19～D35详见[决策记录](../development/DECISION-LOG.md)：模块owner/无环应用依赖/事务；三StpLogic显式载体及独立设备会话；Cookie+CSRF与版本撤销；每权限数据范围；ScopedPersistence+PG RLS；API/字段/PATCH/version；ID/金额/时间；Web代码路由与状态owner；小程序双槽位/竞态/授权媒体；文件补偿/备份；能力安装/启停迁移；Outbox/任务；后端OpenAPI导出及类型生成/复制/漂移CI；微信精确目标；JDK密码哈希。

没有未解决的核心设计阻塞；所有运行验证都落入后续明确任务，详见[验收矩阵](ACCEPTANCE-MATRIX.md)，不是“以后考虑”。限定认证查找/技术调度函数不是通用跨租户口，运行账号与特权函数owner隔离，必须P04真实反例证明。

## 3. 接口清单与三端对应

| 接口族 | Web平台 | Web员工 | 小程序员工 | 小程序客户 | 详细权威 |
| --- | --- | --- | --- | --- | --- |
| csrf / Cookie登录 | platform | admin | 不使用 | 不使用 | [身份](../contracts/IDENTITY.md)、[认证](../architecture/AUTHENTICATION.md) |
| Token员工登录 | 不使用 | 不使用 | admin/auth/token/login | 不使用 | 同上 |
| 微信客户登录/手机号 | 不使用 | 不使用 | 不继承客户域 | customer/auth/wechat/login、auth/phone | 同上 |
| me/当前退出/全部撤销 | platform域 | admin域 | admin域、独立设备 | customer域 | 同上 |
| 密码修改 | platform | admin | admin | 微信账号不适用 | 同上 |
| 授权Store摘要 | 不默认业务访问 | admin | admin | 不自动员工门店权限 | [身份](../contracts/IDENTITY.md) |
| 临时上传/查询/绑定/访问/删除 | 控制面资源 | 当前租户业务 | 同admin权限 | 本人策略 | [附件](../contracts/ATTACHMENTS.md) |
| 任务查询/取消/结果/授权下载 | 控制面任务 | admin任务 | 同admin | customer本人任务 | [任务](../contracts/ASYNC-TASKS.md) |
| 微信回调/媒体票据兑换 | 不继承以上会话 | 同义 | 同义 | 同义 | [API](../contracts/API.md)、[存储](../architecture/FILE-STORAGE.md) |

接口均为设计，没实现Controller或Mock业务。员工两端复用admin是相同身份/权限/范围，不共享Token；API正文只索引，各字段在详细owner文件唯一规定。

## 4. 新增与修改文件

新增20份规范：architecture下MODULE-BOUNDARIES/AUTHENTICATION/AUTHORIZATION/MULTI-TENANCY/CONFIGURATION/FILE-STORAGE/ASYNC-EVENTS；contracts下API/PAGINATION/DATA-TYPES/IDENTITY/ATTACHMENTS/ASYNC-TASKS/OPENAPI-GENERATION；conventions下BACKEND/WEB-PAGES/WEB-STATE/MINIPROGRAM-PAGES；testing下ACCEPTANCE-MATRIX及本报告。

修改9份：AGENTS.md、README.md、SCAFFOLD-SCOPE、TECHNICAL-BASELINE、PROJECT-STRUCTURE、VERSION-MATRIX、ROADMAP、DECISION-LOG、CORE-CONTRACTS。修改旧范围语句依据本轮用户明确授权，避免误称只允许P01-01。矩阵只补协议/微信目标，原53依赖记录保持，未安装新依赖或生成根锁文件。

另新增evidence/P01-02的命令/官方元数据/源码摘要/技术夹具/审计/保全清单；夹具仅供复现，不能当正式工程或真实账号。详细文件差异由最终审计生成。

## 5. 实际验证及证据等级

本轮临时cwd为workspace-check记录的系统临时目录；JDK/Node/依赖复用P01-01下载的隔离工具，版本本轮实际核对：[toolchain-current](evidence/P01-02/toolchain-current.json)。完整路径/argv/cwd/退出码见[Sa命令](evidence/P01-02/sa-token-probe-commands.json)、[其他探针命令](evidence/P01-02/probe-commands.json)。下面短命令仅便于阅读，不替代完整证据。

| 检查 | 命令/方式摘要 | 退出码 | 实际结果及边界 |
| --- | --- | --- | --- |
| git状态 | git status --short，项目根 | 128 | 非仓库，非工程失败 |
| 官方精确Sa源码 | urllib HTTP读取Maven sources.jar | 0 / HTTP200 | RESOLVED，源码SHA和真实API可追踪 |
| Sa API编译 | javac --release 21 -cp 精确core.jar SaTokenApiProbe.java | 0 | COMPILED，API存在 |
| Sa技术会话 | java -cp .:core.jar SaTokenApiProbe | 0 | RUNTIME_VERIFIED仅内存技术断言，跨域/独立设备/当前退出/全部撤销 |
| OpenAPI生成 | node 已冻结生成器 contract.json --alphabetize -o contract.generated.d.ts | 0 | 技术schema真实生成，非正式业务协议导出 |
| 生成一致性 | 上述命令追加--check | 0 | 当前临时产物一致 |
| 类型规则 | tsc --noEmit --strict --skipLibCheck false --lib es2023 声明及断言 | 0 | COMPILED，ID/金额/版本/日期string、enum、null/optional、三身份union；错误赋值反例 |
| 漂移拒绝 | 临时产物追加故意类型后--check | 1（预期1） | 反例PASS：确实拒绝漂移，随后恢复；不是意外生成失败 |
| JDK密码API | javac / java PasswordApiProbe | 0 / 0 | COMPILED及技术运行：算法参数/Unicode一致，固定盐仅夹具；无生产秘密 |
| 微信官方资料 | urllib官方页面+稳定版JS使用的官方JSON | 0 / HTTP200 | DOCUMENTED/版本元数据RESOLVED；工具没下载安装 |
| 最终文档/保全 | python3 docs/testing/evidence/P01-02/audit-reproduction.py，项目根 | 0 | 文档/引用/范围/保全检查PASS，不替代业务运行 |

Web抓取工具访问部分微信页面返回Internal Error，未当成功；随后直接官方HTTP抓取成功。stable页SSR无版本，按其官方脚本读取history_stable和选定日志，原响应/URL/SHA留证；v3.html为历史页，精确目标依据当前release/页面，不误用旧页。

[官方Sa源码记录](evidence/P01-02/sa-token-source-record.json)、[微信抓取](evidence/P01-02/wechat-official-retrieval.json)、[官方资产](evidence/P01-02/wechat-official-assets.json)、[基础库页面](evidence/P01-02/wechat-release-retrieval.json)、[稳定版元数据](evidence/P01-02/wechat-devtools-retrieval.json)、[能力说明](evidence/P01-02/official-capability-notes.json)记录来源与边界。

## 6. 未执行、影响与实施责任

| 未验证项 | 结果 | 影响与后续 |
| --- | --- | --- |
| RLS/scoped Repository/限定登录函数/迁移/连接池 | NOT_EXECUTED / NOT_VERIFIED | 设计不能证明隔离；P03/P04真实PG/Testcontainers必测 |
| Redis会话/期限/版本撤销/Cookie/CSRF/代理/CORS | 同上 | P05真Redis、HTTP、浏览器及故障验证，不降级免保护 |
| Web列表/缓存/表单/并发401 | 同上 | P06/P07实际应用及真后端联调 |
| 附件上传/绑定/票据/Range/补偿/备份 | 同上 | P08真磁盘/PG/Redis故障与恢复 |
| 微信工具/基础库/真机/受保护媒体 | 同上 | P09目标工具实测、合法域名、真机Range和竞态 |
| RabbitMQ/Outbox/任务与微信真实接入 | 同上 | P10启用时真PG/RabbitMQ/外部配置，缺配置如实记录 |
| 正式OpenAPI导出/复制/CI及语义差异脚本 | 同上 | P03创建；临时生成器不能替代正式导出 |
| 全新项目生成/多OS/生产发布 | 同上 | P11/P12；本轮不初始化/提交/推送/发布/部署 |

本轮没有需要索取真实密钥/账号的事项。初次补丁因同文件删除/新增形式不被工具接受而未落盘，已改为合法更新；Sa探针首轮打印的条目数未严格按断言表达式计数，修正为不带数量的真实摘要并重新编译/运行，原输出保留，不影响行为断言。上述不阻碍设计任务完成，但阻止对应运行能力被标为已验收。

## 7. G01～G15设计门禁

各PASS仅表示本轮设计及必要验证条件满足，不升级为真实业务运行PASS。最终文档审计和人工规则一致性核对已执行。

| Gate | 条件 | 结果 | 依据 |
| --- | --- | --- | --- |
| G01 | 前置事实/验证已读取 | PASS | 原文/依赖树/XML/失败与未执行已核对 |
| G02 | 模块职责/依赖/数据/事务 | PASS | MODULE-BOUNDARIES |
| G03 | 三身份/空间 | PASS | AUTHENTICATION/IDENTITY，精确源码及技术断言 |
| G04 | Cookie/CSRF/Token可实施 | PASS | 载体/匿名与设备CSRF/来源/冲突/期限/撤销 |
| G05 | 租户读写/异步范围 | PASS | MULTI-TENANCY、RLS与架构/真实测试联合 |
| G06 | API/错误/分页/类型一致 | PASS | 详细契约和生成类型断言 |
| G07 | Web状态/页面 | PASS | WEB-STATE/WEB-PAGES |
| G08 | 小程序双身份/分页 | PASS | MINIPROGRAM-PAGES及官方媒体能力 |
| G09 | 附件安全/补偿 | PASS | FILE-STORAGE/ATTACHMENTS |
| G10 | 可选能力/迁移 | PASS | CONFIGURATION/ASYNC-EVENTS |
| G11 | OpenAPI流程可实施 | PASS | 计划导出/生成/复制/CI，工具实际生成/漂移反例 |
| G12 | 验收映射P02～P12 | PASS | ACCEPTANCE-MATRIX |
| G13 | 引用有效/无竞争源 | PASS | 总索引及最终审计 |
| G14 | 无提前正式功能 | PASS | 初末清单，仅docs/相关根规则变动 |
| G15 | 无真实秘密 | PASS | 文件审计，夹具不是真实凭据 |

## 8. 最终状态与下一任务

P01-02最终 **COMPLETE**：G01～G15均PASS，无核心设计阻塞；P01-01此前COMPLETE，本轮详细冻结条件满足，因此P01整体 **COMPLETE**。P02仍 **NOT_STARTED**。完成表示可实施的设计与必要技术验证完成，不表示认证/隔离/三端/上传/异步运行已经实现。

满足门禁后下一合法任务：**P02-01：根目录工程骨架、构建工具与本地基础设施初始化**。只报告，不自动执行。

## 9. 最终审计与可复现清单

[最终审计](evidence/P01-02/document-audit.json)保存真实命令/cwd/退出0及逐项结果：22指定核心文件齐全、本轮新增20规范/修改9既有说明、全部本地引用有效、单一版本源56记录、33条验收映射覆盖P02～P12、无正式工程初始化、秘密特征扫描及人工检查。写入前378文件中369文件保持SHA一致，仅9份授权规范修改；既有ui的272文件全部保持路径/大小/SHA，P01-01证据原样保全。

[变更清单](evidence/P01-02/change-summary.json)逐文件列出新增/修改，[证据清单](evidence/P01-02/EVIDENCE-MANIFEST.json)列出文档/证据大小与SHA（不自包含自己的哈希）。本轮没有改全局Java/Node/pnpm配置，没有真实账号、密码、密钥或Token写入；技术夹具仅临时执行后作为证据保存。

复现：先按P01-01复现说明在新的系统临时目录准备版本矩阵工具，复制本轮SaTokenApiProbe.java/PasswordApiProbe.java/contract.json/contract-check.ts；按命令JSON替换临时绝对路径再执行。不得在apps下解压夹具当正式工程。协议漂移反例应退出1；其余必要探针退出0；文档审计可直接在当前根按表执行。
