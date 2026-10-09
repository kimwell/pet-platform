# P06 Web 阶段总验收

日期：2026-10-09。P06-03执行中；最终状态待补验。原始路线已与冻结提交 `60481ce` 的 P06、A06-01/A06-02 对照：应用壳、Router/Query、fetch、会话/权限、表单错误、真实同源联通及清楚的错误/加载/空态。P07的列表、管理CRUD、Modal/版本冲突等原有归属不变。

## 补验前覆盖矩阵

实现路径均相对 `apps/admin-web/src/`。H1=[P06-01报告](P06-01-VERIFICATION.md)，H2=[P06-02报告及续验](P06-02-VERIFICATION.md)。COMPILED中的受控技术测试与RUNTIME_VERIFIED的正式后端/浏览器分开；引用历史证据不表示本轮重跑。

| 需求 | 实现位置 | 已有证据 | 等级 | 本轮补验 | 缺口及责任阶段 |
| --- | --- | --- | --- | --- | --- |
| Provider、中文配置 | app/providers/AppProviders.tsx、main.tsx | H1官方Form/入口 | COMPILED、RUNTIME_VERIFIED | 核对 | 无P06缺口 |
| 同源fetch及地址限制 | shared/api/request.ts、shared/config/api.ts | H1 request测试、真实代理 | COMPILED、RUNTIME_VERIFIED | 核对 | 跨源/生产代理独立验收 |
| data:null、响应/错误解析 | shared/api/request.ts、ApiError.ts | H1 null/非JSON/trace；H2安全错误 | COMPILED、RUNTIME_VERIFIED | 补400/409等完整状态表技术证据 | 真实业务409归P07 |
| 取消和Body超时 | shared/api/request.ts | H1确定性signal/超时 | COMPILED（技术运行） | 补真实浏览器断网/取消 | 不宣称服务器回滚 |
| Cookie/CSRF、不重放 | shared/auth/SessionRuntime.ts | H1双Cookie；H2 CSRF反例/敏感写 | COMPILED、RUNTIME_VERIFIED | 复用 | 真实附件multipart归P08 |
| STAFF/PLATFORM隔离 | shared/auth/spaces.ts、SessionRuntime.ts | H1/H2双域保全 | COMPILED、RUNTIME_VERIFIED | 跨Tab复核 | 无P06缺口 |
| 登录与恢复 | features/auth/api/AuthService.ts、pages/LoginPage.tsx | H1正式两域登录、503恢复 | RUNTIME_VERIFIED | 跨Tab补验 | 无P06缺口 |
| 路由守卫 | app/guards/requireSession.ts、router/router.ts | H1直达/故障；H2受限/403 | COMPILED、RUNTIME_VERIFIED | 复用 | 未来路由P07登记 |
| 合法返回目标 | shared/auth/returnTo.ts | H1 search/外站；H2受限returnTo | COMPILED、RUNTIME_VERIFIED | 核对 | 未来页面P07扩白名单 |
| 导航与操作权限 | app/router/pageAccess.ts、shared/auth/permissions.ts | H2权限下降/本人能力 | COMPILED、RUNTIME_VERIFIED | 核对 | 普通管理页面P07 |
| 权限刷新一致 | app/layout/SessionLayout.tsx、AuthService.ts | H2原生trusted focus/403 | RUNTIME_VERIFIED | 复用原生证据 | 无即时撤权推送承诺 |
| 受限改密 | pageAccess.ts、AccountSecurity.tsx | H2正式受限/直接URL/改密恢复 | RUNTIME_VERIFIED | 390px复核 | 平台无该契约，不伪造 |
| 本人改密 | features/account/AccountSecurity.tsx | H2两域旧密码拒绝/新密码登录 | RUNTIME_VERIFIED | 复用链路 | 无P06缺口 |
| 当前/全部退出 | AuthService.ts、SessionLayout.tsx | H1未确认退出；H2全部失效 | RUNTIME_VERIFIED | 单一错误呈现补验 | 发现重复提示可能性，P06修复 |
| Query范围与定向失效 | features/auth/api/queryKeys.ts、AuthService.ts | H1/H2分域取消/清理测试 | COMPILED、RUNTIME_VERIFIED（身份） | 跨Tab缓存观察 | 普通列表关联失效P07；安全mutation清本域 |
| 旧响应/并发401 | SessionRuntime.ts、AuthService.ts | H1/H2受控竞态 | COMPILED（技术运行） | 复用、核对 | 不外推生产网络競争 |
| 跨Tab账号变化 | SessionLayout.tsx focus、AuthService.ts authority | H2原生focus只证窗口，不证同源Tab账号切换 | RESOLVED；部分RUNTIME_VERIFIED | 必须补真实同源Tab退出/改密/切账号 | 旧身份Form草稿可能保留，P06修复 |
| NotFound/系统错误/加载 | shared/SystemNotFound.tsx、SystemError.tsx、SessionFailure.tsx | H1 404/503；H2权限页 | COMPILED、RUNTIME_VERIFIED | 390px/键盘/故障恢复 | 未实现业务空集合，P07 |
| 桌面与390px | styles/global.css、既有页面 | H1壳；H2安全/受限空密码截图 | RUNTIME_VERIFIED | 补入口/两登录/404/403/故障 | 不重设计 |
| Tab/焦点/label/Enter | SystemEntry.tsx、LoginPage.tsx、AccountSecurity.tsx | H1/H2 label/Tab/Enter | 部分RUNTIME_VERIFIED | 补实际焦点/错误关联/菜单恢复 | 嵌套交互需核对；无Modal，关闭返回归P07 |
| 无敏感持久化/日志 | Form、Mutation变量undefined、Zustand layoutState | H1/H2精确扫描、源码及白名单 | RESOLVED、RUNTIME_VERIFIED（限定扫描） | 源码与本轮证据扫描 | 不承诺JS物理擦除/所有外部日志 |
| 生成契约/CI/脚本 | contracts.ts、根scripts、.github/workflows/check.yml | H1/H2六检查 | RESOLVED、COMPILED | 有代码修复后六检查回归 | 远程CI、多OS未执行 |
| 分页total字符串安全转换 | PAGINATION、DATA-TYPES规范 | 后端/生成类型total:string | DOCUMENTED、RESOLVED | 接入清单给安全范围算法 | Table适配P07，禁止任意Number(total) |
| 日期/金额/查询工具 | 当前无通用日期/金额/列表工具 | 类型规范已冻结 | DOCUMENTED | 明确owner及接入约束 | P07/P08实际页面按需，非P06必选 |
| bundle大小与警告 | Vite实际输出 | H2入口714.55/gzip235.61kB | COMPILED | 修复后构建对比 | 500kB警告保留；首屏性能未测 |

最终结果、门禁和P07接入清单将在补验后追加；本文件不提前关闭P06。
