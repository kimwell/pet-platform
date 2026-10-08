# P05 认证与身份权限阶段验收

**最新阶段结论（2026-10-08）：P05 COMPLETE；P05-05 COMPLETE；G12 PASS / RUNTIME_VERIFIED。** 真实微信四步和重复登录复用已完成，综合既定条件满足，详见文末。本文件此前各节保留当时BLOCKED/NOT_EXECUTED历史，不作本轮当前状态。

日期：2026-10-08。**P05 IN_PROGRESS；P05-05 BLOCKED。** 三类身份后端和自动化验证已完成，但真实微信门禁未闭合；生成本文不等于阶段完成。原P05完成条件“会话失效真实生效、身份跨域拒绝、CSRF正反例通过”及实际外部认证要求保留。

同日续验已完成隔离正式环境和安全配置位置准备；用户确认暂时无法提供AppSecret，真实四步仍未执行，状态维持不变。末节为本轮追加结论，此前失败及NOT_EXECUTED保留。

## 前置与原路线复核

P05-01～04已COMPLETE，分别见[数据/初始化](P05-01-VERIFICATION.md)、[员工会话](P05-02-VERIFICATION.md)、[凭据撤销](P05-03-VERIFICATION.md)、[平台身份](P05-04-VERIFICATION.md)。最新P05-04通过基线366，P05-03原328，以最终XML逐项比较均保留。P05-05本轮421项全通过，外部微信Gateway使用明确测试替身，不能据此称真实微信认证通过，详情见[P05-05](P05-05-VERIFICATION.md)。

回到冻结路线提交`60481ce`：P05含“角色权限”，P07含员工/组织/角色页面、创建/修改/停用及真实业务基础API；原IDENTITY具体管理接口也安排P07。因此P05必选是正式角色权限数据、权威授权加载、范围与撤销安全，不是完整管理CRUD。P05-03已交付管理密码重置/会话撤销安全子集，不能把它说成全部员工/角色管理完成。**员工、角色、权限/授权赋予与停用管理API依然未实现，P07继续必选。** 此判断依据[原始条目](evidence/P05-05/original-stage-boundary.json)，没有临时降低或改写P05原门禁。

## 综合结果

| P05要求 | 当前事实 / 等级 | 证据及限制 |
| --- | --- | --- |
| 正式Tenant/Store/Employee/Role基础 | RUNTIME_VERIFIED | V1及P05-01正式七表/复合关联/初始化，原27项基础IT再次全通过；Organization/管理CRUD未实现 |
| 正式客户主体/微信绑定 | RUNTIME_VERIFIED（本地身份链） | V4、CustomerSubject/WechatBinding、原子并发/tenant+app+open键；真实微信入口未验 |
| 平台独立身份/初始化 | RUNTIME_VERIFIED | V3/pet_control、独立生产JAR初始化测试；没有创建实际开发/生产管理员 |
| 三个Sa空间/Redis命名空间 | RUNTIME_VERIFIED | STAFF/PLATFORM/CUSTOMER同UUID真实HTTP仍隔离，各自设备索引/撤销 |
| 密码存储/本人改密/管理重置 | RUNTIME_VERIFIED | PBKDF2/独立盐、Unicode、旧密码确认、强制改密与权限反例；客户无密码；生产负载未验证 |
| 同源Web Cookie与独立CSRF | RUNTIME_VERIFIED（本机） | P05-02/04浏览器证据及本轮旧用例回归；未验证生产TLS/可信代理/跨源方案 |
| 小程序Token载体 | RUNTIME_VERIFIED（后端） | STAFF员工密码Token与CUSTOMER微信后端Token，不签平台Token或客户Cookie；无正式页面/恢复交互 |
| CurrentPrincipalProvider/SELF | RUNTIME_VERIFIED | 唯一Provider三域路由，客户SELF含CUSTOMER主体域和customerId；平台无TenantContext |
| 凭据/全部会话撤销 | RUNTIME_VERIFIED | STAFF/PLATFORM数据库版本+清理意图；客户数据库代际+下次新登录/TTL清理，延迟不删新代际 |
| 当前状态/角色/门店授权重载 | RUNTIME_VERIFIED | 每请求正式DB加载，版本变化失效；客户仅固定session权限/SELF；状态管理API尚未实现 |
| 敏感提交前重验/竞争 | RUNTIME_VERIFIED | P05-03/04用例回归；客户logout-all锁/预期版本/原子事件；普通在途请求不承诺远程回滚 |
| 异步执行前重验 | RUNTIME_VERIFIED（STAFF） | 当前租户/员工/会话/权限重验与捕获范围上限；CUSTOMER明确403未入队，PLATFORM无租户任务入口；Servlet ASYNC未扩展 |
| DB/Redis故障关闭 | RUNTIME_VERIFIED | 临时真PG/Redis暂停/权限反例/清理失败及恢复，未用H2或内存会话替代；生产HA/TLS/ACL未验 |
| 频控/必要安全记录 | RUNTIME_VERIFIED | Redis原子跨实例、固定摘要Key、租户/域/已知主体/结果/时间/trace；不含秘密 |
| OpenAPI/判别类型/生产包 | COMPILED、扫描 | 19真实路径；三类严格类型；生成/检查/Web/小程序typecheck通过；生产JAR无测试Gateway/探针/秘密配置 |
| 真实微信code→会话→me→退出 | NOT_EXECUTED / BLOCKED | 本轮没有载入安全AppSecret和入口配置、无新鲜code；不能用Gateway替身或历史AppID/工具编译证明 |

最终 `apps/backend/./mvnw clean verify` exit0：146单元+275集成=**421**，0失败/错误/跳过，原366和328用例无缺失，[汇总](evidence/P05-05/test-summary.json)。测试Gateway仅src/test。历史失败/未执行不改写，最新命令、产物、G01～G15详见[P05-05验证](P05-05-VERIFICATION.md)。

## 剩余必选与独立边界

P05当前剩余必选是**P05-05真实微信后端联调四步与G12补证、随后重新综合阶段结论**；安全外部配置和新鲜code均须实际具备。此条件不足时保持P05-05 BLOCKED/P05 IN_PROGRESS，不以文档、自动化数量或三类模型齐备标COMPLETE。

| 尚未执行的事项 | 既定归属 / 状态 |
| --- | --- |
| 员工、组织、角色/权限赋予、状态管理完整API及页面 | P07必选，NOT_STARTED；现有敏感密码/撤销接口不等于完整CRUD |
| 小程序登录页面、会话恢复、隐私授权、真机完整链路 | P09，NOT_STARTED；本轮只纯类型更新 |
| 订阅消息、支付、RabbitMQ、Outbox | P10，NOT_STARTED；WxJava登录后端已按明确授权前移P05 |
| 客户资料/手机号、员工OpenID、手机号/UnionID合并、注销 | 当前任务排除，未实现；不能无授权补做 |
| 生产角色预配置、独立迁移/初始化、秘密/TLS/ACL、备份/恢复及部署 | NOT_EXECUTED / NOT_VERIFIED，未操作实际目标 |
| 远程CI、Windows/Linux | NOT_EXECUTED / NOT_VERIFIED，macOS本机结果不替代 |

**下一合法任务仅P05-05续验**：按[本地开发](../development/LOCAL-DEVELOPMENT.md)安全加载指定入口；临时工具探针获取新鲜code；真实交换、建立客户会话、me和退出失效分别留证且不保存秘密；补G12后重新判断P05。没有自动进入P06、P09或P10，没有提交、推送或部署。

## 2026-10-08 续验综合复核

用户确认暂时无法提供AppSecret，故本轮只准备，不请求微信。实际安全属性`PET_WECHAT_LOCAL_SECRET`的专用文件存在、可读、为空、0600，父目录0700且Git忽略；本机入口白名单已明确。冻结PG/Redis专用临时容器、正式角色/V1～V4迁移、正式bootstrap租户及既有生产JAR启动均实际通过；readiness=200 UP、匿名me=401 AUTH_REQUIRED。客户/绑定仍0/0，不存在真实会话或复用结论。工具当前AppID/版本/基础库匹配；CLI因服务端口关闭退出246，未验证当前wx.login权限，未擅自开启。

详见[P05-05第10节](P05-05-VERIFICATION.md#10-2026-10-08-真实微信续验准备完成appsecret仍缺少)及[本轮证据](evidence/P05-05/resume-2026-10-08/isolation-preparation.json)。仅本轮资源已停止，原进程/容器/卷保全；无后端/依赖/公开契约修改，没有无理由重跑421项历史通过回归。

逐项复核上表及既定P05条件：正式三身份数据/授权/会话/CSRF/隔离与撤销维持P05-01～05既有证据；真实微信四步仍NOT_EXECUTED、G12 BLOCKED，因此**P05-05 BLOCKED / P05 IN_PROGRESS**。员工、组织、角色/权限完整管理仍P07必选且未实现；P09、P10、生产/远程CI限制不变。当前人工缺项是安全文件中的实际AppSecret；下一合法任务仍P05-05真实续验，不能先进入P06。

## 2026-10-08 真实微信通过后的阶段验收

安全配置随后就绪，正式隔离环境按原角色/migration/bootstrap路径重新建立。20:04:52.920～20:04:56.424 +08:00，指定AppID、Stable2.02.2608080/基础库3.17.2和真实WxJava4.8.0 Gateway下：登录200，PG客户/绑定1/1且Redis CUSTOMER会话正确，me200且同租户/CUSTOMER/SELF，logout200后同旧Token me401 SESSION_EXPIRED、Redis旧会话不存在。第二个新鲜code登录200复用同客户，计数仍1/1，再退出200。两次微信交换，无秘密采集。详见[第11节](P05-05-VERIFICATION.md#11-2026-10-08-真实微信续验通过与阶段关闭)、[真实结果](evidence/P05-05/resume-2026-10-08/real-run-second/real-wechat-result.json)。

| 既定完成条件 | 综合依据 / 当前等级 |
| --- | --- |
| 正式身份/租户/角色权限基础 | P05-01正式数据、受限初始化；P05-02～04权威员工/平台状态、角色/门店加载；P05-05正式客户/绑定及固定SELF权限，RUNTIME_VERIFIED |
| 会话失效真实生效 | 原Sa/Redis三域、当前/全撤销、期限/状态/版本/补偿证据；本轮真实微信会话logout及旧Token401/Redis删除，RUNTIME_VERIFIED |
| 身份跨域及租户越权拒绝 | 原同UUID三域真实HTTP/Cookie/Token、受限PG/RLS、无上下文/跨租户/SELF反例；本轮CUSTOMER/验收租户、无STAFF/PLATFORM权限，RUNTIME_VERIFIED |
| Cookie/Token与CSRF正反例 | P05-02/04真实本机浏览器、双域Cookie/CSRF及P05-05回归保持；客户Token会话真实生效，RUNTIME_VERIFIED；生产TLS/代理/跨源独立未验 |
| 权限/撤销/敏感操作/异步与依赖故障 | P05-03/04及原421项综合回归；权威数据重验、范围上限、客户异步拒绝、DB/Redis关闭失败、必要安全记录，RUNTIME_VERIFIED |
| 公开类型、生产包、既有回归保全 | 原421项0失败/错误/跳过及原366/328逐项保留、生成/漂移/三端类型通过；本轮JAR SHA/迁移/源码不变，COMPILED及限定RUNTIME_VERIFIED |
| 实际外部微信认证 | 本轮新鲜code→正式交换→PG/Redis会话→me→退出旧会话失效、客户复用，G12 PASS / RUNTIME_VERIFIED |

上述条件全部满足，**P05-05 COMPLETE / P05 COMPLETE**。完整员工/组织/角色权限赋予和状态管理API按冻结路线仍为P07必选、未实现；并未因阶段关闭声称这些管理功能已交付。P09完整交互/隐私/真机、P10消息支付、生产独立角色/迁移/秘密/TLS/ACL/部署、远程CI和多OS仍各自NOT_EXECUTED/NOT_VERIFIED。

首轮自动化因子进程HOME缺失在取得code前失败，调整本轮工具环境后通过，原失败记录保留；没有认证代码、公开契约或生产依赖修改，故未重复全套421项。收尾[审计](evidence/P05-05/resume-2026-10-08/real-run-second/final-audit.json)及[阶段记录](evidence/P05-05/resume-2026-10-08/real-run-second/stage-review.json)包含证据链接、历史/源码保全与资源恢复。当前工具服务端口已恢复关闭、专用后端/容器结束，用户原进程/卷未改。

没有剩余P05人工验收阻塞；聊天中提供过的凭据建议由用户轮换并在既定0600安全文件更新，不能在聊天提供新秘密。下一合法建议为**P06-01：Web应用壳、请求与同源会话基础**（NOT_STARTED，现有P06/A06-01/02范围）；只提出，没有自动执行P06/P09/P10，也没有提交、推送或部署。
