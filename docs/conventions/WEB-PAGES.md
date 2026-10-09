# Web标准页面行为

冻结日期：2026-10-07，P01-02。本文拥有列表/表单/详情交互；状态与请求见 [Web状态](WEB-STATE.md)，API语义见 [API](../contracts/API.md)。直接使用官方Ant Design Table/Form/Modal/Drawer，不新造万能BaseTable或透传Wrapper。

## 列表

筛选输入保存在Form草稿，提交后规范化写URL；改草稿不立刻查询。查询/重置page=1；重置保留接口默认pageSize/sort，清筛选与选择。URL search在路由入口校验类型/枚举/白名单；非法值规范化为默认并提示一次，未知参数丢弃；不要把非法URL直接发后端。刷新、返回、分享可恢复已提交条件，不保存未提交草稿。

服务端分页/排序/total，客户端不对当前页再排序当全量结果。pageSize或sort变化回第一页；分页规则见 [分页](../contracts/PAGINATION.md)。默认选择仅当前页，换页/筛选/身份/权限版本变化清选择；跨页选择只能专门业务扩展，不能默认“选择全部查询结果”。刷新后移除已不存在/不可访问的选择ID。

列显示/宽度/顺序可存客户端偏好，键包含environment/principalType/principalId/tableId/configVersion；只存偏好、不存行数据，加载时按当前列白名单校验，新版本失配恢复默认。隐藏列不代表授权，服务端不能返回未授权字段。

首屏loading、合法空结果、查询失败分别展示；失败不给“暂无数据”。后台刷新保留已加载合法数据、显示刷新/失败提示，提供重试。删除最后页最后一条成功后重取total：page>1且当前空则以 `min(currentPage,max(1,ceil(total/pageSize)))` 更新URL并重查；并发删除仍需重新判断，不无限回退。

批量按钮仅当前操作权限可用，确认框说明条数/动作（产品交互不构成额外工程审批）。按 [部分失败](../contracts/API.md) 展示成功/失败数，成功项移出选择、失败项保留并给中文原因，刷新受影响缓存；外层200不代表全成功。

## 表单

create/edit各自DTO、规则，Form是唯一草稿。后端fieldErrors按dot+bracket路径转换为AntD NamePath，未知路径显示表单总错误并保留trace。客户端校验帮助输入，后端最终校验。金额保持string，null/缺失遵循具体DTO。

pending时禁重复提交及相关关闭按钮，服务端幂等/版本仍必要。提交失败保持输入/选择/上传临时附件引用；成功才清理/关闭，失效对应详情/列表。网络未知结果不自动重发。

dirty按初始化快照判断；离开/关闭提醒未保存内容，用户选择留在页面或放弃。Modal/Drawer每次打开重建Form和版本，关闭后清错误/草稿/选择及未绑定临时附件本地引用；不要下一次打开继承前一人数据，远端临时文件由生命周期清理。组件销毁不能代替服务端删除证明。

VERSION_CONFLICT保留当前输入，提示“数据已更新，请重新加载后比较”，用户显式重载/复制草稿再决定，不自动用新version覆盖提交。权限失去403保留输入但禁止提交，给合法离开路径。

## 详情

404统一“资源不存在或不可访问”，不区分越权；无操作权限403、网络失败可重试、loading分别显示，不误退出身份。tab进入URL、入口校验白名单，深链恢复。编辑成功失效detail/list及模块声明的关联查询；返回列表保留已提交URL。状态允许操作与权限分别展示和校验。

可访问性：字段label/错误关联、键盘可达、焦点进入/返回Modal、确认操作中文、loading不只靠颜色。P07用真实后端及浏览器覆盖筛选恢复、删除回退、并发版本/批量失败和权限变化，Mock页面不是验收。

## P06-01 已实现页面（2026-10-08）

公开 `/` 只提供员工/平台实际入口；`/admin/login` 为租户编码、账号、密码，`/platform/login` 为账号、密码。采用官方Ant Design Form/Input/Button，中文label、organization/username/current-password autocomplete和键盘提交；同步提交锁与pending防重复。账号按IdentityNames的Java strip/ASCII小写规范化，密码原样传输、不trim/改大小写；失败或成功均清密码，离页Form clearOnDestroy/resetFields。不提供“记住密码”。字段错误只映射本表单合法字段，未知路径消息在表单总错误，trace可复制；429读Retry-After，网络故障不同于密码错误。

保护 `/admin` 与 `/platform` 展示真实me名称、空间及员工tenantId（契约没有租户名称/编码，不能用登录输入补造）。官方Layout/Menu/Breadcrumb/Dropdown/Avatar、侧栏折叠、刷新身份与当前退出可用，只有“当前身份”菜单；没有假统计、业务菜单、权限代码堆叠。强制改密只提示联系管理员，本轮不建设凭据安全页面。全局NotFound和错误边界存在；预期身份服务/权限故障是可重试页面，保持对应路径，不当作未登录。

所有提示为页面内Alert/Result，没有message/modal上下文调用，故继续不安装无用途Ant Design App上下文。页面采用真实路由dynamic import；体积警告保留。历史ui是未确认用于管理壳的寄养/微信成果，本轮未复用或改动。详见[P06-01](../testing/P06-01-VERIFICATION.md)。

## P06-02 账号安全与路由呈现（2026-10-08）

新增 `/admin/security`、`/platform/security`，两个空间分别由真实身份壳承载。`sessionPages` 是路由访问及导航共同元数据：空间、标题、本人会话/明确权限条件、是否允许受限状态；只登记当前身份与账号安全两个已实现页面。直接 URL、刷新、返回和导航均调用统一 beforeLoad 条件；未知路径仍404。无权操作隐藏；有身份但无页面权限显示403，网络/依赖错误为可重试身份状态。没有管理菜单或生产假页面。

STAFF 真实 passwordChangeRequired=true 时，登录成功依据刚取得的 me 直接到 /admin/security，已登录访问登录/普通保护页也优先到安全页；returnTo不能绕过。受限导航仅保留账号安全，改密/当前退出/全部退出不受普通管理权限限制；后端仍拒绝普通接口 PASSWORD_CHANGE_REQUIRED。平台 DTO 没有该字段，不模拟强制改密。

安全页包含本人姓名/空间、官方 Form/Input.Password 的当前密码、新密码及确认、本人退出全部设备表单。中文说明包含“将退出当前账号的全部设备，包括当前设备”；两操作各自确认当前密码，确认新密码不发后端。12～128 Unicode字符/最多256 UTF-16单位、合法代理对，无格式组合；不 trim/变大小写。label、错误、current-password/new-password、键盘提交及同步提交锁可用。提交中禁用；后台成功 me 不覆盖草稿；成功/失败/退出/卸载清密码。422只精确映射 currentPassword/newPassword，未知路径保留总错误，不动态写对象/原型。

改密成功提示“密码已修改，请重新登录”；退出全部成功准确说明全部设备已退出。敏感结果不确定不显示成功，重新确认身份后才允许新的明确操作，原请求不重放。页面内通知不逐层 toast；不显示 Token、会话键、权限代码清单、配置/设备数量或哈希。桌面与390px采用原官方布局，不修改历史ui，不扩展完整个人中心。实际截图及错误/键盘验收见[P06-02](../testing/P06-02-VERIFICATION.md)。

安全页提供“刷新当前身份”按钮；普通成功重验不覆盖未保存密码。最终1280px/390px真实截图、键盘与安全链路已核验；P06-02前轮（历史）IN_PROGRESS，原生窗口focus补验NOT_EXECUTED（G11 PARTIAL），其余结果按报告限定，不自动关闭P06。

## P06-02 原生focus后页面复核（2026-10-08）

真实原生可见窗口focus后，平台撤销改密权限仅隐藏对应表单，仍保留合法本人会话操作；撤销session:manage后当前安全页进入统一403状态，不再显示旧导航/敏感表单。同一无痕上下文直接进入员工安全页仍可改密/退出全部，未重新登录。新增截图无密码；前轮1280px/390px证据保留。[续验结果](../testing/P06-02-VERIFICATION.md)。P06-02 COMPLETE，P06仍IN_PROGRESS，不自动扩展页面或进入下一任务。

## P06-03 当前基础页面（2026-10-09）

系统入口采用两个官方Button导航，每个只有一个键盘停止点。登录和安全Form首错误自动聚焦，保留label/文字及aria关联。账号Dropdown自动将焦点放入菜单，Escape回触发按钮；当前退出未确认只展示一个页面错误，保留排错编号。窄屏主壳页头允许自然换行和自适应高度，避免平台空间/长名称裁切。

安全页普通同身份成功重验保留草稿；主体、租户、会话或授权版本/权限/门店/dataScope/受限事实变化后重建Form并清敏感字段。普通业务表单提交失败/409保留输入的原规则不变；登录/安全密码字段依敏感规则清空。页面/Modal/Table模式原P07按真实接口接入；不新增假业务页面。

桌面/390px、键盘/首错误/菜单恢复的限定范围、系统错误技术故障与真实服务故障的区别见[P06-03](../testing/P06-03-VERIFICATION.md)。没有真实辅助技术全标准验收；未实现Modal/Drawer的焦点恢复不能提前称通过。P07详细接入索引见[总验收](../testing/P06-ACCEPTANCE.md#p07业务接入清单)，本页继续拥有普通列表/表单行为。

## P07-02 当前员工列表（2026-10-09）

新增 `/admin/identity/users`，真实 GET `/api/admin/identity/users`，要求STAFF的identity:user:list；导航和beforeLoad共用sessionPages。官方Form筛选、Table六公开列、受控Pagination、中文空态/错误/状态和默认时区展示已接入；没有详情链接、行选择、新建/编辑/停用/导出/角色管理按钮。本人安全页独立，不借平台登录身份。

URL只有keyword/status/page/pageSize/sortBy/sortOrder；草稿不立即请求，提交/重置/排序/改变pageSize回第一页；已冻结非法URL安全恢复默认并一次提示，未知参数丢弃；重复敏感参数不取首值。原始字符串解析保留数字/布尔样式字面量，关键词不trim；浏览器历史覆盖草稿。条件变化清旧数据，同条件刷新保留并明确旧结果；total超安全范围保真显示且禁用页码组件，超末页最多一次replace纠正。完整拥有者和边界见[WEB-LIST-PAGES](WEB-LIST-PAGES.md)，本轮证据见[P07-02](../testing/P07-02-VERIFICATION.md)。列偏好/详情返回状态/写入与正式模板继续后续任务。
