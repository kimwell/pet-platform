# Web请求、状态和缓存

冻结日期：2026-10-07，P01-02。本文拥有状态归属、传输和Query；页面交互见 [Web页面](WEB-PAGES.md)。版本和目录分别见矩阵/结构，不新增状态库。

| 状态 | 唯一owner |
| --- | --- |
| 服务端实体、列表、me/权限 | TanStack Query；不复制到Zustand |
| 已提交筛选/page/pageSize/sort/详情tab | TanStack Router URL search |
| 表单与筛选草稿、字段错误 | Ant Design Form |
| 当前页选择、Modal/Drawer可见 | 页面本地状态 |
| 跨页面客户端偏好如主题/侧栏 | 少量Zustand，不存Token/服务端副本/表单 |
| CSRF、请求epoch、取消控制器 | shared/auth内存会话服务；不持久化秘密 |

## fetch请求层

相对URL同源fetch、credentials:'same-origin'；不允许业务传任意外域URL。Cookie按浏览器发送，写请求取当前域内存CSRF设置X-CSRF-Token，trace按API；不尝试读取HttpOnly Cookie。每请求接受AbortSignal，Query queryFn必须传signal到fetch；abort为取消、无错误提示，无权限语义。

ApiError区分HTTP/业务、NETWORK、ABORT、PROTOCOL，包含合法status/code/中文message/fieldErrors/traceId。检查HTTP与JSON信封一致，非JSON代理错误按status处理，不能把解析失败当401。网络故障保留登录状态。公开登录401只表示凭据失败，不触发全局登录过期。

multipart用FormData不手写boundary，支持signal，Cookie仍CSRF；下载检查HTTP及Content-Type，错误JSON不能保存为blob文件，安全解析Content-Disposition，释放object URL。服务端短期URL只在内存使用，不持久化。

写入不自动重试；网络中断可能已提交，显示“结果暂未确认，请查询后再操作”，需要幂等的接口按其声明处理。CSRF403可重新获取凭据但不重发原写操作。

提示只有一个owner：字段错误在Form，页面查询错在页面状态，业务mutation由发起页面显示一次；请求层只转换错误，不逐层toast。全局只处理统一会话失效/必要应用级错误，不再弹相同业务提示。

并发受保护请求401以domain+sessionEpoch single-flight：只一次取消该域请求、清缓存/CSRF/身份、记录合法returnTo并进入对应登录页、一次中文提示；后续同epoch401忽略。旧epoch请求结果不得覆盖新登录身份；仅服务端确定401触发失效，403/429/5xx/网络不触发。退出、账号切换、身份域切换、authorizationVersion变化都取消请求、清Query缓存/页面选择和敏感草稿，再加载新域me。

## Query Key与缓存

统一工厂形状 `[scope,module,resource,kind,normalizedParams]`。scope包含principalType/tenantId/principalId/sessionId/authorizationVersion/sessionEpoch；公共查询使用独立PUBLIC作用域，不能放认证结果。normalizedParams包含已提交URL字段、门店范围和排序，空值/数组排序规范一致。禁止只用资源ID作为跨身份缓存键。

| 分类 | staleTime / gcTime | 重试 |
| --- | --- | --- |
| me、权限、安全配置 | 0 / 0；切换时重取 | 不自动重试401/403；依赖故障显式重试 |
| 普通列表/详情 | 30秒 / 5分钟；focus时可刷新 | 仅GET网络/502/503/504最多2次，1秒/2秒 |
| 低变化公开字典 | 5分钟 / 15分钟 | 同GET策略；仍有独立公共key |
| 用户任务状态 | 根据任务轮询，不永久缓存 | 非终态暂时错误退避，离开停止 |

400/401/403/404/409/422/429不自动重试，429展示Retry-After。Query缓存不持久化到localStorage。Mutation retry=false；成功只失效本scope对应资源列表、详情、关联统计/上下文接口，具体模块queryKey工厂声明依赖。错误不执行成功清理；安全/授权变更先重取me、比较版本并清旧scope。

后台刷新保留已有合法数据并标更新状态；身份变化必须立即去除旧数据。optimistic仅可选低风险可回滚交互；员工停用/权限/文件删除默认等待服务端结果，不能乐观宣称安全操作生效。

路由guard使用当前me和权限，仅显示控制，后端复核。returnTo仅应用内相对路径、当前身份域白名单、无协议/双斜杠/外域；未保存离开由页面guard，不能放URL秘密。

## P06-01 当前实现（2026-10-08）

请求实现为 `shared/api/RequestClient`，默认相对根 `/api`、15秒超时、credentials=same-origin、no-store、redirect=error。路径拒绝绝对/协议相对URL、点段、编码路径、反斜线及内嵌query/hash，query由URLSearchParams编码并拒绝敏感键。JSON与FormData互斥；multipart不设置boundary。返回成功data（包括null）；onTrace只提供合法trace元数据。HTTP与信封必须一致，错误保留status/code/fieldErrors/trace/Retry-After；NETWORK/CANCELLED/TIMEOUT/PROTOCOL独立，非JSON错误页只显示安全中文。没有下载API，字节/Range/Content-Disposition须由独立原始响应客户端扩展，本轮没有下载联调。

服务端身份只有Query投影。`AuthService.meOptions`为0 staleTime/0 gcTime、不自动重试、查询传AbortSignal；普通GET仅网络/超时/502～504最多2次（1/2秒），mutation不自动重试、不缓存密码变量。初始化me key为`[{principalType,sessionEpoch},auth,me,current]`（身份尚未知），后续受保护资源工厂加入可信tenantId/principalId/sessionId/authorizationVersion、门店与dataScope。me授权事实变化清当前空间缓存/请求，并把新me投影交给新代际。真实业务列表尚未实现，权限变更缓存反例只为受控技术测试。

`SessionRuntime`仅保存两套内存CSRF、请求代际、过渡锁/取消器和临时中文通知，没有用户副本或持久化。登录前GET对应csrf、POST携带X-CSRF-Token、成功轮换代际并重新GET csrf、再查me；不采用login响应或表单构造身份。同空间CSRF合并，旧响应须匹配代际才可写回；不同空间并行。CSRF_INVALID只清本空间CSRF，下一次明确操作重取，原写入不重放；PERMISSION_DENIED不刷新CSRF。401 LOGIN_FAILED是表单错误；受保护标准401按空间/代际仅一次清理，旧代际的成功/401都丢弃。非JSON401、网络和503不退出。

同空间登录/退出互斥；过渡先取消旧请求/移除当前范围，再确认服务端结果。logout成功或契约证明已失效后进入本空间登录；退出失败提示“退出未确认，服务端会话可能仍有效”，重新查me/提供明确重试，不宣称撤销。取消本身不证明服务端写入回滚。另一空间的Cookie、CSRF、Query和布局状态不清。导航到另一空间只恢复该空间；当前页面卸载清表单、Query观察和临时页面状态，按空间key隔离缓存，不主动验证所有空间、不撤销其他会话。Zustand只保存实际侧栏折叠，未使用persist。

路由beforeLoad先查me；401进入对应登录，预期网络/503/403返回受控sessionError页面状态，不抛React渲染异常；未知渲染错误仍由系统错误边界处理。合法登录与保护页无未授权内容闪现。返回目标只允许本空间已实现的首页，保留合法search，拒绝外站/双斜杠/编码路径/登录循环/hash/敏感键。会话变化通过router.invalidate重算。

[77项受控测试和真实浏览器证据](../testing/P06-01-VERIFICATION.md)分开。前端不保证跨标签页主动同步、实时推送撤权，也不能回滚已提交操作；每次后端仍权威验证。

## P06-02 权限与本人安全流程（2026-10-08）

身份/权限仍只有 Query 投影。`hasPermission/usePermission/PermissionBoundary`精确检查当前空间代码；未加载、Query失败或过渡中默认拒绝，不借角色名称、通配符或 localStorage 放大授权。STAFF 各权限 dataScope.grants 原样保留，不合并不同权限最大范围。本人 STAFF password/logout/logout-all 无普通管理权限条件；PLATFORM 按实际 session:manage/credential:change 分别判断。

每次保护路由 beforeLoad 重验 me；me staleTime=0，Query窗口恢复可见机制、当前保护空间window focus监听、手动重试/刷新触发重验，渲染不主动发送请求。普通操作明确 PERMISSION_DENIED/PASSWORD_CHANGE_REQUIRED 使当前 me 失效重验，me 自身失败不递归失效。授权/范围/门店/主体/会话或 STAFF passwordChangeRequired 变化推进本空间代际、取消旧请求、移除旧作用域缓存，在通知路由/观察者之前原子交付新 Query 身份；无访问权限隐藏导航/操作，当前非法页面执行统一守卫，进入安全页或403页。身份刷新失败暂停敏感操作并显示可重试状态，不能当作已退出；当前输入若页面仍合法，普通成功 me 刷新不重置 Form。

安全写复用同空间过渡锁：先隔离旧请求、清旧缓存/CSRF，再仅在新 me key 保留既有身份投影供提交中禁用表单展示。password PUT 只发送 currentPassword/newPassword；logout-all POST 只发送 currentPassword，均禁止自动重试。Mutation 变量 undefined、retry=false/gcTime=0，操作完成及卸载 reset；错误仅持安全 ApiError 元数据，不含请求体。Form和局部引用均清密码，只承诺清应用引用，不承诺 JavaScript 内存物理擦除。

200/data:null（包括物理清理 PENDING）立即清本空间身份、CSRF和旧请求，记录单一成功通知、返回对应登录；旧401不覆盖成功通知，新登录再开启代际。前端不重登、不填新密码、不显示 Redis 内部步骤。403 CSRF 只使本空间 CSRF 过期，下次明确操作重新取；其他403不刷新CSRF。409需重新确认，422字段白名单映射，429读Retry-After；503/网络/超时/协议不确定及敏感取消均不证明操作失败或成功，显示“请求结果未确认”，禁止直接重复提交，提供重新确认身份。当前退出仍保持P06-01“退出未确认”语义。结果不确定的通知由 runtime 持有，表单不重复显示同一通知。

两域 Cookie/CSRF/Query/代际不互相清理。权限变化没有跨标签页主动推送；后台普通 me 不延长服务端闲置期。每次后端最终复核，受控竞态测试不能替代真实浏览器或生产网络竞争证明。当前验证见[P06-02](../testing/P06-02-VERIFICATION.md)。

P06-02前轮状态（历史）为IN_PROGRESS：原生窗口focus真实触发补验尚未执行，G11 PARTIAL；不以自动化显示切换或代码监听存在推导真实focus通过。手动重验/权限下降及本人安全链路已有真实证据。

## P06-02 原生focus续验结论（2026-10-08）

原生Chrome窗口菜单切出/切回产生trusted blur/focus，visibility始终visible且无visibilitychange；返回后仅当前PLATFORM me重验，权限不足403不清STAFF会话。最小化恢复另由可见性机制重验，两个触发点均有真实证据。临时监听器只观察事件，收尾已移除；没有跨标签主动推送的承诺。G11已补齐、P06-02 COMPLETE，P06仍IN_PROGRESS；[实际事件及门禁](../testing/P06-02-VERIFICATION.md)。
