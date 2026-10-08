# 核心契约总索引与摘要

冻结日期：2026-10-07，P01-02。本文仅导航和摘要，不重复字段/数值/接口定义。详细文档中的规则为唯一权威；精确版本只在[版本矩阵](../development/VERSION-MATRIX.md)，当前状态只在[P01-02验证](../testing/P01-02-VERIFICATION.md)。所有接口是设计，未实现。

| 主题 | 唯一权威位置 | 摘要 |
| --- | --- | --- |
| 目录/package/三端职责 | [工程结构](../architecture/PROJECT-STRUCTURE.md) | 根目录建设，固定包名，规划/实际分开 |
| 模块数据/依赖/事务 | [模块边界](../architecture/MODULE-BOUNDARIES.md) | 一个Module，明确应用接口，不跨Repository |
| 认证空间/Cookie/Token/CSRF/撤销 | [认证](../architecture/AUTHENTICATION.md) | 三身份空间，分设备服务端会话 |
| 权限代码与范围组合 | [授权](../architecture/AUTHORIZATION.md) | 操作权限、数据范围、业务状态分开 |
| 可信Tenant/Store/SELF执行 | [多租户](../architecture/MULTI-TENANCY.md) | scoped持久化入口与RLS共同防护 |
| 配置/可选能力/安装迁移 | [配置](../architecture/CONFIGURATION.md) | 初始化选择不等于运行启停，关闭保留数据 |
| HTTP/错误/trace/PUT/PATCH/批量 | [API](API.md) | 状态码真实，稳定错误码，写入不自动重试 |
| 分页/排序/数组查询 | [分页](PAGINATION.md) | 服务端分页、稳定排序，非法参数拒绝 |
| ID/大整数/金额/日期/空值 | [类型](DATA-TYPES.md) | 无精度丢失，时区明确，缺失与null分开 |
| 当前身份字段/登录等端点 | [身份](IDENTITY.md) | PLATFORM/STAFF/CUSTOMER判别字段与接口 |
| 附件HTTP/字段 | [附件](ATTACHMENTS.md) | 临时、查询、绑定、访问、删除 |
| 文件安全/补偿/票据/备份 | [存储](../architecture/FILE-STORAGE.md) | 私有本地文件与PG元数据，一资源绑定 |
| 消息/Outbox/可选微信适配 | [异步](../architecture/ASYNC-EVENTS.md) | 至少一次，幂等、租约、确认及恢复 |
| 任务HTTP/状态/结果 | [任务](ASYNC-TASKS.md) | 模块提交、重新授权、受保护结果 |
| OpenAPI目录/生成/映射/CI | [生成](OPENAPI-GENERATION.md) | 后端事实源、生成不可手改、漂移检测 |
| 后端编码与验证约定 | [后端](../conventions/BACKEND.md) | DTO校验、范围事务、中文及脱敏 |
| Web请求/Query/状态 | [Web状态](../conventions/WEB-STATE.md) | 服务端归Query，提交条件归URL，草稿归Form |
| Web列表/表单/详情 | [Web页面](../conventions/WEB-PAGES.md) | 恢复/空态/冲突/部分失败明确 |
| 小程序会话/导航/分页/媒体 | [小程序](../conventions/MINIPROGRAM-PAGES.md) | 双槽位、域请求、竞态取消、授权媒体 |
| P02～P12实施验收 | [验收矩阵](../testing/ACCEPTANCE-MATRIX.md) | 每契约映射owner/阶段/真实正反例 |

P01-01结果在[历史报告](../testing/P01-01-VERIFICATION.md)保留，不因当前设计完成改写失败/未执行项。后续修改核心规则时只改权威文件及其引用/受影响schema/验证，不能在摘要、README或生成文件另设规则。
