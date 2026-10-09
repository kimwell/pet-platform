# Web 详情页接入规则

日期：2026-10-09；当前实例 `/admin/identity/users/$employeeId`。员工字段与行范围唯一见[员工管理契约](../contracts/EMPLOYEE-MANAGEMENT.md)，状态归属见[WEB-STATE](WEB-STATE.md)，列表参数唯一沿用[WEB-LIST-PAGES](WEB-LIST-PAGES.md)，本轮验证见[P07-03](../testing/P07-03-VERIFICATION.md)。模块内直接组合官方 Ant Design Button/Descriptions/Tag/Spin/Alert/Result；不创建 BaseDetail、配置框架或透传 Wrapper。

## 独立入口与路由

员工详情独立要求 STAFF `identity:user:detail`，列表 `identity:user:list` 不授详情，详情也不授列表；PLATFORM 不借该路径或权限。sessionPages 同时拥有访问条件，`navigation:false` 表示带目标参数的页面不创建无目标静态菜单。列表只在当前详情权限有效时显示可键盘操作的“查看详情”按钮，不整行隐式点击，不表示此行一定处于详情操作范围。

详情 route 与列表是同级受保护路由，均使用既有 requireSession。目标按 DATA-TYPES 的小写 UUID v4 校验；格式非法显示中文反馈，Query 不执行，API 入口再次拒绝。格式合法不证明目标存在/有权访问。合法直接地址允许仅有详情权限的 STAFF；强制改密、身份失败、PLATFORM 和未登录沿用 P06 守卫。

## 正式请求与缓存

API 从生成 `operations['getEmployee']['parameters']['path']` 和 `components['schemas']['SuccessEmployeeView']['data']`/`EmployeeView` 接类型；只 GET `/api/admin/identity/users/{employeeId}`，不发送详情 search 给接口（后端不接受 query）。列表行不是事实源，不预热详情、不传 DTO 作导航参数。传递 Query AbortSignal 至 SessionRuntime/RequestClient；响应验证目标一致、六字段类型/非 null、状态枚举和固定毫秒 UTC 时间，格式错误保留合法 trace。

Key 复用 authKeys.protected：可信 STAFF/tenantId/principalId/sessionId、真实 authorizationVersion/sessionEpoch、有效门店与逐权限 dataScope、identity/employees/detail、employeeId。当前正式 me 包含逐权限 grants；不从扁平权限集猜数据范围，不借 list 范围，不虚构权限版本。后端仍逐请求最终复核。同权限范围取并集，不同权限不互借。

staleTime=30秒/gcTime=5分钟、详情自身关闭 focus 自动刷新，沿用 P06 保护路由/window focus/可见性身份重验。详情 `networkMode:always` 使明确刷新实际尝试传输，并沿用 GET 网络/超时/502～504 最多两次（1秒/2秒）重试，避免浏览器离线暂停后误显示最新成功。4xx、协议错误和取消不自动重试。没有跨标签即时同步或撤权推送。

目标变化采用独立 key、无 placeholder；页面以主体/代际/目标重新挂载。离开后等观察者卸载再取消/清该详情 query，StrictMode 探测重挂载仍有观察者时不误清。身份/租户/会话/门店/任一授权事实变化复用既有 runtime 清当前空间所有旧缓存与请求，原子交付新 me；即使版本未增也比较真实 grants。旧代际/取消的响应不能覆盖新详情。

## 状态与展示

| 状态 | 行为 |
| --- | --- |
| 首次加载 | Spin 与中文加载文字，无旧详情、无假数据 |
| 成功 | 只展示 id/loginName/displayName/status/createdAt/updatedAt 六字段 |
| 同身份同目标刷新 | 保留仍合法内容，明确“正在刷新，以下仍为上次成功结果” |
| 无 detail | 守卫与 Query 默认拒绝，零详情请求 |
| 401 | 既有按空间/代际合并的失效、取消、清缓存与登录返回；页面不重复通知 |
| 403 | PERMISSION_DENIED/PASSWORD_CHANGE_REQUIRED 由 runtime 重验；所有正式403均停止展示旧详情并清数据，不把 CSRF_INVALID 当退出 |
| 404 | 一律“员工不存在或不可访问”，不区分不存在、跨租户、范围外；停止展示旧详情并清数据 |
| 503/NETWORK/TIMEOUT/PROTOCOL | 中文错误与明确重试；保留有效身份；若已有合法内容必须标为上次成功且刷新失败 |
| CANCELLED | 静默，不产生错误通知 |

普通错误由页面 ErrorNotice 一次展示，保留可复制排错编号；401 通知仍归会话层。公开六字段必填、非 null；null 为协议错误，不虚构可空字段。已有 displayText/displayInstant 提供“—”兜底，空字符串技术反例与真实数据库约束分开。状态中文与列表复用生成枚举映射。时间按已声明 Asia/Shanghai，me 未提供动态 zoneId；不声称租户动态时区。

Descriptions 长文本完整换行，UUID/长账号/姓名可聚焦并保留完整 title，不使用未转义 HTML。390px 不产生页面级横向溢出。进入详情及错误变化聚焦页面标题；返回列表后聚焦列表标题。重试、详情入口及返回使用官方 Button，双中文按钮显式 aria-label 避免组件自动空格影响可访问名称。没有编辑、启停、删除、重置密码、角色/门店/审计轨迹或内部版本字段。

## 可刷新列表返回与安全回退

详情 URL 只保存受限 `returnTo`：完整路径必须恰为 `/admin/identity/users`，原始字符串≤2048；没有外部/协议相对 URL、反斜线、控制字符、hash、点段别名、其他空间或业务路径。仅接纳原列表 keyword/status/page/pageSize/sortBy/sortOrder 白名单。复用 parseWebSearch/normalizeSearch/employeeHref；重复已知参数或任何非法/未知参数整组拒绝，不挑重复第一个；合法前导零与默认值按已有规则规范化。详情重复 returnTo 也拒绝，未知外层 search 移除。

列表入口只以当前规范化 URL 生成 returnTo，不读取 Form 草稿，不建立第二套状态，不用 location.state 跳任意 URL，不存 Zustand，不携带详情对象或秘密。TanStack Router 导航将返回信息放 URL，刷新、原生前后退仍可恢复。登录后的应用返回白名单接纳合法 UUID 详情并重新验证受限嵌套返回；原有全局2048字符上限仍保留，超长登录返回安全回当前身份页。

“返回列表”是确定的规范化目标，不使用 history.back。有 list 权限：合法返回优先，无/非法返回回默认员工列表；无 list 权限：不显示列表入口，提供 `/admin` 当前身份页。权限重验撤销 list 后及时替换去向；原生浏览器后退仍由列表守卫保护。返回后的总数变化和页越界沿用 P07-02 一次 replace 纠正，没有另写循环跳转。

本项只保证已提交 URL 查询恢复；不承诺未提交草稿、滚动位置或完整视觉位置恢复。开发 StrictMode 初挂载可能有一次被取消的旧请求，验证统计将传输尝试与完成响应区分；页纠正只完成一次，不将取消探测当跳转循环。

## 后续管理详情接入

先核对模块生成类型、UUID/目标边界、独立操作权限与真实 grants；同级登记保护路由和有依据的业务入口。模块内声明目标查询/字段，不照搬员工业务字段。复用请求、完整身份 Key、取消/清理、ErrorNotice 与统一404；按模块列表白名单冻结可校验返回和无权安全去向。确定性竞态/错误测试与正式认证、真实数据库/Redis、浏览器的范围/故障/桌面/窄屏验证分别记等级，未通过门禁不得宣称 COMPLETE。
