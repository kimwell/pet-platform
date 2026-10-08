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
