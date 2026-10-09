# Web 列表页接入规则

日期：2026-10-09；当前实例 `/admin/identity/users`，读取 [员工契约](../contracts/EMPLOYEE-MANAGEMENT.md)，验证见 [P07-02](../testing/P07-02-VERIFICATION.md)。直接组合官方 Ant Design Form/Input/Select/Button/Table/Pagination/Alert/Empty/Tag；没有万能 BaseTable、属性透传 Wrapper 或代码生成模板。

## 状态与 URL

Ant Design Form 拥有未提交草稿；TanStack Router URL 拥有已提交条件；TanStack Query 拥有响应与状态。页面不复制行数据到 Store。关键词和状态输入不请求；Enter/查询提交校验后回 page=1，空输入省略 keyword；其他非空字面量保持首尾空白/大小写/Unicode（员工后端不 trim）。100 Unicode 码点、Java isBlank 语义与非法代理字符检查在 URL/提交共用。状态只有 ACTIVE/DISABLED。

当前 URL 只接纳 keyword/status/page/pageSize/sortBy/sortOrder。后端支持 storeId，但没有公开门店选择事实，本页将其与 roleId/日期/未知参数移除并提示；没有 sort 别名。默认 page=1/pageSize=20/createdAt desc；只有默认排序两参数一起省略，非默认排序同时编码 sortBy/sortOrder。仅 sortBy 合法，缺方向补 desc；只有 sortOrder 非法。参数缺失用默认；空字符串、重复已知参数、非法枚举/数字/组合/超长关键词将整组条件恢复默认并提示，不挑第一个重复值。page/pageSize 允许前导零后按数值规范化，拒绝空白、符号、小数、指数和越界。

原始 URLSearchParams 保留字符串与重复数组，不使用 JSON search 解析改变 `001`/`false` 等关键词；统一 serializer 为普通单值 query。路由入口校验后才进入列表，canonical 地址不同使用 replace，默认排序成对省略避免循环。提示保存在 history state，不增加 URL 字段。Router会在初始守卫前规范化search，启动时先将原始URL校验提示写入同一历史条目，随后规范化保留提示；没有额外push。用户提交、重置、分页、排序使用 push；浏览器前进/后退同步 Form，未提交草稿被当次 URL 覆盖。条件完全相同时显式提交刷新，已有进行中请求合并。没有提交草稿持久化。

## 分页与服务端排序

请求 page 从 1 起，1～2147483647；pageSize 1～100，默认20，组件选项10/20/50/100；直接 URL 的其他合法大小保持原值。改变 pageSize/排序回1，重置恢复默认20/createdAt desc。Table 使用 `rowKey=id`，没有行选择或当前页客户端排序。

`total` 的生成类型和响应保持规范非负十进制字符串。先正则检查，再 BigInt 检查 MAX_SAFE_INTEGER、按整数公式计算末页；仅安全值转 Number 给 Pagination。页号、pageSize 和页数运算分别检查；公开 int32 页号与 JPA int offset 的可访问上限一起约束。组件 total 绝不 clamp，不用当前页条数替代真实 total。

超 MAX_SAFE_INTEGER 显示精确总数和明确“分页范围受限”，不渲染 Pagination，仍可调整筛选/刷新已取得本页，不宣称全部加载。安全 total 对应的部分深页超接口能力时明确提示，组件请求越界被阻止；直接深页由真实422 RESULT_TOO_LARGE提供返回第一页。后续大数据游标/快照/性能方案尚未提供。

超末页按真实 total/末页 replace 回退；total=0 回1。每个用户导航最多纠正一次，history state 记录纠正已执行。并发数量再次变化不自动循环，以明确空页提示和显式返回第一页/刷新处理；新用户导航开启下一轮一次纠正。跨 HTTP 翻页不保证并发快照一致。

六列 id/loginName/displayName/status/createdAt/updatedAt 都支持单列服务端排序。Ant Design ascend→asc、descend→desc；清除恢复 createdAt desc。默认创建时间列按desc→asc→清除（恢复desc）切换，避免受控默认值使点击无法升序。URL 控制排序箭头，刷新与历史恢复；客户端不拼第二个 ID 条件，稳定同向 ID 由后端追加。

## 授权和缓存

导航与 beforeLoad 共用 sessionPages/canAccessPage，STAFF/list独立权限；PLATFORM 不读取员工Cookie之外的身份，无权限不请求列表。安全页保持独立，未实现详情时不放详情链接或提前实现写入/导出按钮。

Query Key 复用 authKeys.protected：STAFF、可信tenant/principal/session、authorizationVersion/sessionEpoch、有效门店和逐权限 dataScope、identity/employees/list、标准化条件。不能只按租户缓存，不包含秘密。当前正式me完整返回逐权限范围，后端 CurrentIdentity.of 已核对；不凭扁平权限猜范围。

列表queryFn传 AbortSignal，经现有 SessionRuntime→RequestClient；身份重验变化推进原有代际、取消旧请求、移除当前空间缓存、原子交付新me。晚到旧epoch结果拒绝写回。list PERMISSION_DENIED 立即隐藏旧行、取消清该Query，并由既有runtime重验me；后续非法页进入403。scope变化即使authorizationVersion不增也比较当前授权事实清理，另一空间保留。条件切换不使用placeholderData；身份变化也不能占位。服务端逐请求授权仍是最终依据。

普通列表 staleTime=30秒/gcTime=5分钟；沿用 GET 网络/超时/502/503/504 最多2次（1秒/2秒），取消和其他4xx不重试。列表关闭自身focus自动刷新，由既有身份focus/可见性重验先确认范围；范围改变时新key立即重新读取。没有实时撤权推送/跨标签广播；未触发重验前已取得记录可能暂时显示，不能声称撤回已完成读取。

## 状态、展示与可访问性

| 场景 | 当前行为 |
| --- | --- |
| 首次加载/新条件 | loading与文字；无旧行、无假空态 |
| 同条件后台刷新 | 保留合法数据，显示“正在刷新…上次成功结果” |
| 首次失败 | 查询错误+重试，无“暂无数据” |
| 刷新失败 | 保留合法数据，明确上次成功结果+重试 |
| total=0无筛选 | 当前授权范围内暂无员工 |
| total=0有筛选 | 没有符合筛选条件的员工 |
| 权限撤销 | 立即隐藏旧行、清缓存，统一合法403/受限路由 |
| 401 | 沿用统一会话失效与安全返回 |
| 403 | PERMISSION_DENIED清范围；CSRF_INVALID沿用独立错误，不混为退出 |
| 422 | keyword字段及未知字段都在查询参数提示保留；深分页单独说明 |
| 503/网络 | 沿用重试、保留有效身份，不当成未登录 |
| 取消 | 不展示失败提示 |
| total超安全范围 | 精确总数、限制提示，移除分页组件 |

六字段来自生成 EmployeeView；没有角色/门店/能力字段。中文状态；空值统一“—”；UTC时间按契约默认 Asia/Shanghai 显示并标注（me未公开zoneId，未假称动态租户时区）。长文本保留完整title/tooltip、键盘可聚焦；不使用 HTML 关键词高亮。Form中文label、首校验错误聚焦、Enter提交；分页键盘可达；表格区域独立横向滚动，390px 页面不整体溢出。

列偏好未纳入本任务，不保存行数据或提前实现列配置；后续确有路线要求时使用 environment/principalType/principalId/tableId/configVersion 域并白名单校验。

## 下一业务列表接入

1. 核对该模块真实路径、生成请求/响应类型、操作权限、逐权限范围、参数边界和默认排序。
2. 在 sessionPages 同时登记导航与守卫；扩展本空间合法returnTo。
3. 模块内实现可读URL规则和官方Form/列；复用原始search解析、分页安全转换与字段格式化，不把员工字段套成通用业务配置。
4. 复用 authKeys.protected/runtime/queryRetry，传 signal，禁止跨scope placeholder；声明范围重验和错误owner。
5. 用确定性测试证明参数/竞争/超限，再用正式认证/隔离PG/Redis/真实浏览器核对集合total、范围、桌面/窄屏/键盘；保留失败、命令和恢复证据。
6. 更新本模块契约及验收；详情返回状态单独依独立detail权限接入，写入/批量按后续任务冻结。正式工程模板留 P11。
