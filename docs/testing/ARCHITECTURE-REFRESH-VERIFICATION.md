# 三端架构与页面模式统一改造验收

日期：2026-10-09。结论：本轮批准的 Web 迁移、后端职责整理及客户小程序公共框架已完成。保留既有员工、角色、组织、租户、门店和平台账号能力；没有新增 HTTP 接口、数据库迁移或小程序正文业务。完整小程序使用端 B04、原 B02 外部客户门禁未由本轮替代。

证据目录：[ARCHITECTURE-REFRESH](evidence/ARCHITECTURE-REFRESH/)。所有 shell 检查的工作目录、完整命令、退出码、时间及日志见 [commands.jsonl](evidence/ARCHITECTURE-REFRESH/commands.jsonl) 和 [分级命令清单](evidence/ARCHITECTURE-REFRESH/CHECKS.md)；失败轮保留，没有将失败命令改写为 PASS。

## 已交付

- Web 按 api/components/config/hooks/layouts/pages/router/stores/styles/types/utils 迁移现有实现及测试。BasicLayout 的 Header、Sidebar、Breadcrumb、UserDropdown 和独立内容滚动区使用 Outlet 承载页面，240/64px 侧栏及移动官方 Drawer。品牌、白色侧栏、浅灰背景、分隔标题栏和白色列表卡片参考用户截图及师生项目，业务菜单/字段依本项目。
- 全部六处业务表格使用 ProTable，包括租户详情内门店；页面保留 Form 草稿、Query 响应、服务端筛选/排序、安全字符串 total 和权限判断。组织页补齐真实末页一次 replace 回退，其他原分页适配保留。
- 新增、详情、编辑、授权、敏感操作共用一层官方业务 Modal，本地模式切换、取消回父详情、保存刷新；未保存丢弃使用官方确认。旧五类详情地址校验后 replace 到列表，通过一次性 history state 打开，刷新关闭。仅 detail 权限背景无权限且不请求 list。身份/会话/授权事实变化继续取消和清缓存、销毁敏感状态。
- 后端仍一个 Maven Module、com.pet.platform 和四层。五组 Controller/应用用例按职责拆分，包内 StaffManagementExecutor/PlatformManagementExecutor 集中原事务和身份重验。锁、范围、上限、版本、审计和撤会话保留；identity 经 platform 应用端口编排。结构允许类型/调用者及反例同步。显式保留旧 OpenAPI tag，最终生成产物字节一致。
- 小程序已去掉 miniprogram 中间目录，源码直接在 apps/wechat-miniprogram 根下，miniprogramRoot/npm 输出为 ./，类型生成/引用及结构检查同步。四个客户原生 Tab 页面和中间原助手共五列；助手提示未开放。navigator-bar 支持标题、返回、插槽、占位及运行时胶囊留白，TabBar 在显示时同步真实路由，旧入口回首页。正文仅未开放提示，没有假宠物/价格/订单、请求/session/登录/员工分包空实现。

当前规范：[目录](../architecture/PROJECT-STRUCTURE.md)、[列表](../conventions/WEB-LIST-PAGES.md)、[弹框](../conventions/WEB-DETAIL-PAGES.md)、[小程序](../conventions/MINIPROGRAM-PAGES.md)。三个参考工程只取布局、目录及职责思想；师生业务、网球请求/会话和 pet-saas 业务未复制。

## 检查结果

| 检查 | 命令 / 工作目录 | 退出码 | 等级 / 实际结果 |
| --- | --- | --- | --- |
| 前端冻结安装 | 根：pnpm install --frozen-lockfile | 0 | RESOLVED；根 manifest/lock 恢复并清除误装临时 SDK 依赖，精确依赖未变 |
| Web/小程序类型、lint、结构、Web 测试 | 根：pnpm check | 0 | COMPILED；最终 10 文件、267 项 Web 测试通过；两端类型/lint 和页面/组件/资源检查通过 |
| Web 构建 | 根：pnpm build:web | 0 | COMPILED；最终构建通过，738.11kB 主块及原 500kB 警告保留 |
| 完整后端门禁 | apps/backend：./mvnw --batch-mode verify | 0 | RUNTIME_VERIFIED；147 单元/结构/协议 + 343 集成 = 490，失败/错误/跳过 0；真实 Testcontainers PostgreSQL/Redis |
| 契约及共享类型 | 根：pnpm contracts:check；pnpm contracts:test | 0 / 0 | COMPILED / RUNTIME_VERIFIED 技术契约；10 项导出、3 项生成流程测试，schema/两份类型/小程序根声明一致；共享类型检查通过 |
| 最终生产 JAR | apps/backend：./mvnw --batch-mode -DskipTests package；根：restart-final.py | 0 / 0 | COMPILED / RUNTIME_VERIFIED；最终 artifact readiness 200，摘要见 final-runtime-artifact.json；package 跳过测试，不替代完整 verify |
| 真实 Web 管理操作 | 根：browser-stage1.mjs、browser-manage.mjs、browser-supplement.mjs、browser-final.mjs | 最终均 0 | RUNTIME_VERIFIED；正式 local 生产 JAR、独立受限 PG/Redis、正式密码/Cookie/CSRF，非页面或 API 数据替身 |
| 微信 npm | 根：冻结 DevTools CLI build-npm --project apps/wechat-miniprogram | 0 | COMPILED；移至外层后官方 npm 构建 warnings=[]，AppID 脱敏 |
| 微信源码及模拟器 | 冻结 GUI 普通编译；根：mini-capture.cjs 320 / 375 / 430 | 最终均 0 | RUNTIME_VERIFIED；Stable 2.02.2608080、基础库 3.17.2，四页/原生路由/选中态/助手/旧入口实测；12 组安全区域指标通过 |
| 返回和 Tab 处理器 | 根：mini-navigation.cjs；mini-unit.cjs | 最终 0 / 0 | RUNTIME_VERIFIED 的真实 Tab 处理器和无栈回首页；有栈返回/导航失败另为 UNIT_FIXTURE（4项），不伪装实际微信失败或业务验收 |
| 公共框架视觉 | 根：web-visual.mjs；visual-review.py | 0 / 0 | DOCUMENTED 人工评分 + RUNTIME_VERIFIED 布局指标；Web 五页公共框架 96，小程序四页 97；胶囊/底部遮挡为 0 |

后端完整 verify 中的依赖故障、事务回滚、跨租户、无上下文、身份跨域、撤权和最后管理员保护反例使用真实 PG/Redis。日志中的预期故障 ERROR/WARN 不抹除，也不称全运行无错误。客户身份集成测试的外部微信 Gateway 仍为技术边界；本轮未重做真实微信 code 交换。

## 真实浏览器证据

[stage1](evidence/ARCHITECTURE-REFRESH/browser-stage1.json)、[管理整轮](evidence/ARCHITECTURE-REFRESH/browser-manage.json)、[故障整轮](evidence/ARCHITECTURE-REFRESH/browser-supplement.json)、[最终 JAR 整轮](evidence/ARCHITECTURE-REFRESH/browser-final.json)。平台账号启停/重置/全撤销由最终 JAR 第二轮已通过，后续离线检查失败独立保留在 browser-final-second-failure.json，最终整轮引用该证据，不虚构再执行。

已覆盖员工创建/资料/启停/角色/门店/重置/全撤销；角色新增/资料/范围/启停；组织创建/资料/启停/末页；租户创建/正式初始化/资料/启停；门店创建/资料/启停；平台账号创建/资料/权限/启停/重置/全撤销。门店创建不自动授权，员工门店分支使用正式 bootstrap 时已授权门店的独立验收租户，未放宽政策。

筛选历史、五类旧详情地址、刷新关框、非法 UUID、详情独立权限、撤权 401 销毁详情、409 禁盲重试、真实 PG 503/恢复、Redis 暂停/恢复、浏览器断网/恢复、Enter、Escape 和移动 Drawer/焦点/页面无横向溢出均有本轮证据。回归数据是隔离临时环境中经正式接口或正式初始化建立的验收数据，不是产品样例页；SQL 故障注入与权限技术夹具在证据中单列。

## 失败修复与边界

- 早期迁移的导入、Hook、路由断言和 OpenAPI tag 漂移失败保留；按新目录及 Outlet/旧链接行为修复，最终门禁通过，没有覆盖生成文件来假造一致。
- 真实离线保存发现 Query 默认暂停 Mutation，网络恢复会再执行。公共写入改为 networkMode:always、retry:false，显式保存立即反馈未确认并禁重放；新增源码行为测试及真实浏览器恢复后权威数据未变化断言通过。失败轮的自动执行事实保留。
- 回归脚本存在 Ant Design 中文按钮空格、虚拟 Select 可访问节点、同名标题、重复密码、未知查询参数、动画结束前计数/焦点以及已登录登录页等夹具问题，失败及修正保留；重复操作触发真实 429 未关闭限流，后续复用已通过证据。
- SDK 的路由故障 mock/restore 曾破坏测试运行时 wx.switchTab，失败保留为技术工具故障；GUI 重新编译恢复原生 API，最终源码编译及真实处理器复验通过。SDK 的切换失败注入仍 NOT_VERIFIED_BY_SDK；实际源码的失败回调和有栈返回用独立技术夹具验证，未称原生失败已复现。
- 临时自动化 SDK 首次安装误命中根 workspace，已恢复原本干净的 manifest/lock，并以 frozen install 清除 75 项临时依赖。SDK 固定 0.12.1 仅在忽略的验收工具目录，未加入应用依赖。
- Pencil 连接不可用，本轮使用已有终审 PNG 和交付说明；没有改写 ui 或把业务样例实现成可用业务。

本机 Chrome 桌面/390px、冻结 DevTools 模拟器的证据不等于真机、完整辅助技术/WCAG、生产 TLS/跨源部署、多 OS 或远程 CI。真机及完整小程序请求/会话/正文链路 NOT_EXECUTED（不在本轮框架实现和可用设备范围）；未发布、部署或自动提交/推送。

## 视觉与保全

[Web 五页截图和指标](evidence/ARCHITECTURE-REFRESH/web-visual.json)、[用户参考截图](evidence/ARCHITECTURE-REFRESH/web-layout-reference.png)、[四页风格评分及安全指标](evidence/ARCHITECTURE-REFRESH/visual-review.json)、[冻结工具编译状态](evidence/ARCHITECTURE-REFRESH/devtools-final-compile-ax.txt)。visual/ 保存各页终审稿与实际框架归一化裁切、比较图及绝对差异图。只比较标题、背景、导航和安全区，排除正文、原生状态栏和系统手势区；像素 MAE 仅诊断，人工维度评分不冒充自动像素一致性。

布局目标收尾时再次对照当前任务附件复核，六类要求均有源码与实际浏览器证据；当前 Web 源码在最终类型/lint/测试和构建后未变化。最新附件副本及逐项审计见 [web-layout-completion-audit.json](evidence/ARCHITECTURE-REFRESH/web-layout-completion-audit.json)。

保全基线 533 个既有 ui、迁移及 B01/B02 历史文件全部摘要一致；精确版本、根 package/lock、后端 pom 与契约产物未变，生成声明仅移动。最终审计见 [preservation-final.json](evidence/ARCHITECTURE-REFRESH/preservation-final.json)。临时服务/回环端口、容器及 access-bearing 私有验收文件收尾见 [cleanup.json](evidence/ARCHITECTURE-REFRESH/cleanup.json)；四个端口释放，用户既有三个容器状态保全，原工程私有配置及 DevTools 保留。清理脚本早期因 --rm 容器停止即自动移除和错误文本大小写失败，记录保留，幂等检查修正后完成。
