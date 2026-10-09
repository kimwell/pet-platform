# 小程序客户框架与后续开发规范

冻结日期：2026-10-07，P01-02。本文拥有原生小程序状态、导航、分页与媒体行为。原生TypeScript、tdesign-miniprogram、页面data和轻量session服务；不用Taro/uni-app/复杂状态库。目录见 [结构](../architecture/PROJECT-STRUCTURE.md)，精确基础库/DevTools目标见 [版本矩阵](../development/VERSION-MATRIX.md)，当前框架的 DevTools 验证见 [统一改造验证](../testing/ARCHITECTURE-REFRESH-VERIFICATION.md)，真机及完整使用端未验收。

## 能力与两个会话槽位

当前一个工程默认 CUSTOMER，源码直接位于 `apps/wechat-miniprogram/` 根下。实际交付四页公共框架及旧 system/entry 跳转；正文显示未开放，设计样例只留在 ui。没有请求、session、登录或员工分包实现。以下会话、请求、分享、分页、附件规则均是后续实现规范，不是本轮已交付能力。

session服务维护独立customer/staff槽位：token、sessionId、expiresAt、identity、epoch。持久化仅必要Token/到期/session标识，键 `pet.session.customer.v1` / `pet.session.staff.v1`，启动后必须me复核，缓存身份不是可信权限。wx本地存储没有本方案可证明的安全加密，不能声称等价HttpOnly；设备风险以有限期限/撤销控制。不记录Token到日志/分享URL。

切换activeDomain先增加epoch、abort旧域request/upload/download、清当前页面data/选择/分页/内存缓存/临时媒体，再载入新域me；另一槽位可保留登录Token但其请求不能在后台继续写页面。退出当前身份清该槽位/文件并服务端logout，不影响另一槽位；退出全部设备对应域logout-all。401只清发生错误的domain+epoch，统一一次跳登录；网络故障不清Token。

## request与登录

services请求必须显式 `domain: public|customer|staff` 和相对注册路径；customer只用X-Customer-Token、staff只用X-Staff-Token，值为Bearer+原始Token，名称/解析唯一见 [认证](../architecture/AUTHENTICATION.md)。public不读任一槽位、不自动带Token；不能把“有哪个Token”当路由选择。域和URL不符拒绝发请求。

wx.request返回success回调只代表网络传输；检查statusCode/信封/trace，区分HTTP业务、网络、取消/协议错误，与Web同语义。wx.uploadFile的data字符串解析；wx.downloadFile检查HTTP/类型，不将失败JSON当文件。记录RequestTask并abort；回调还需epoch/pageVersion检查，因为abort不保证无后续回调。写请求不自动重试，提示单owner，禁止请求层+页面双toast。

客户wx.login code发到客户wechat/login，服务端WxJava换取身份，微信session_key不下发。手机号由用户动作取得独立phoneCode，经已登录/customer/auth/phone补充；不是用手机号接口创建员工身份。员工tenantCode+账号密码token/login，Web同员工仍为另一设备。微信能力关闭给明确不可用说明，不Mock登录。

## 导航、入口、分享和扫码

当前客户首页、找房、订单、我的为四个原生 TabBar 页面，`custom-tab-bar` 展示五列等宽区域。中间复用原助手图标，点击仅提示“功能暂未开放”，不新增第五个路由。页面 onShow 及组件 pageLifetimes.show 同步实际路由；切换成功才更新，失败保留原选中项并提示。

`navigator-bar` 支持 title、back、默认插槽及 placeholder；一级页无返回。使用 getWindowInfo 与 getMenuButtonBoundingClientRect 计算状态栏、标题右侧胶囊留白和底部安全区，异常胶囊采用保守留白；resize/show 重新计算。有历史栈优先 navigateBack，无栈返回客户首页。文本和形状用 WXML/WXSS，图标引用根 assets，不截整页设计当正文。

未来 STAFF 放独立分包并单独导航，不加入客户原生 Tab；跨域必须经过明确 session 切换，不 navigate 到员工页就继承客户数据。

route/query/scene均不可信：只白名单页面、参数名、UUID/稳定code格式、长度（scene解码前后均限制）、单次decodeURIComponent；拒绝双重编码/外链/任意API地址。tenantCode仅入口线索，storeId只表达意图，服务端复核租户/门店，不能靠扫码自动授权。

登录前保存合法returnTarget（域、白名单path、验证过参数），登录成功重新校验所属能力/权限和tenant，再reLaunch或域内导航；无法访问回域首页中文提示。分享只包含允许公开定位参数，不带Token/票据/权限/个人资料；未登录接收者按正常认证和范围处理。

## 页码分页与竞态

页面data区分items/page/hasMore/loading/refreshing/error，已提交过滤单独保存。初始page=1；下拉刷新增加requestVersion、取消旧请求，成功后原子替换items/page/total，finally停止动画，失败保留已有合法数据及重试提示。首次失败显示错误，不显示空态。

触底仅hasMore且无同版本pending才请求nextPage；成功才推进page并追加，失败不推进。每请求捕获domainEpoch、pageVersion、过滤摘要，只有全部仍匹配才能setData；页面onUnload取消或忽略所有任务、onHide停轮询/敏感请求，恢复按过期策略重取。刷新与触底互斥，旧成功不能覆盖新筛选。total/count遵循字符串协议，页面用安全整数转换。ID去重只能改善重复显示，不保证并发跨页完整，必要业务采用明确游标/快照扩展。

## 受保护附件

官方 [downloadFile](https://developers.weixin.qq.com/miniprogram/dev/api/network/download/wx.downloadFile.html) 支持header及本地临时文件，单次上限200MB且不能设置Referer；项目附件上限更小。官方 [image](https://developers.weixin.qq.com/miniprogram/dev/component/image.html)、[video](https://developers.weixin.qq.com/miniprogram/dev/component/video.html) 属性表没有通用header参数，不能假设与wx.request一致。[官方能力证据](../testing/evidence/P01-02/official-capability-notes.json)。

图片用对应域wx.downloadFile认证content得到tempFilePath，再给image/预览；文件下载同样认证，打开文档前验证成功。video用已认证POST access获得短期MEDIA票据URL，支持服务端受限Range；封面图片仍走认证下载。可下载的小视频也可使用临时路径，但不默认整文件预下载。不能将长期Token拼在src query。

票据到期取新票据，限一次显式重取后恢复播放位置，错误区分401/404/网络，不无限刷新会话。页面离开/切换清临时路径与未完成下载，退出删除可控临时文件；设备已缓存字节不可远程保证销毁。HTTPS/request/upload/download合法域名及video Range/票据重取在P09真机验证；当前官方资料只是DOCUMENTED，未将类型检查当微信运行PASS。
