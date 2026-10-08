# 微信小程序页面系统区交付说明

设计文件：`pet.pen`。13 个画板保留现有页面高度；750px 宽仅表示 375pt 宽的 2 倍视觉稿，不是微信要求的固定设备尺寸。

## 自定义导航

- 页面使用 `navigationStyle: "custom"`。状态栏图标和右上角胶囊由微信运行时绘制；Pen 中的对应图层是视觉占位，前端不要复刻一套可点击胶囊。
- 用 `wx.getWindowInfo()` 获取 `statusBarHeight`、`safeArea` 与窗口宽度；用 `wx.getMenuButtonBoundingClientRect()` 获取胶囊的 `top`、`bottom`、`left`、`width` 和 `height`。以胶囊的垂直中心安排返回按钮和页面标题，并给标题预留胶囊左侧空间。
- 首页、找房、我的订单、我的为一级页，不显示返回；其余页面显示返回。房型详情页保持图片延伸至顶部，返回按钮旁不显示标题，并在图片顶部使用局部柔和遮罩保证状态信息清晰。
- 正文从导航安全区下方开始，滚动内容不要穿过胶囊的可点击区域。

## 自定义 TabBar

- 仅首页、找房、我的订单、我的使用五栏 TabBar：`首页｜找房｜宠物智能助手｜订单｜我的`。中间入口只显示图标；其余四项保持图标加文字。微信设计指南允许 2–5 个标签，本设计已达到上限，前端应保留五列等宽触控区域。
- `tabBar.custom: true` 时由自定义组件呈现视觉层；路由配置仍需声明 TabBar 页面。底部留白应根据 `safeArea.bottom` 或安全区 CSS 环境变量在运行时增加，不能将视觉稿底部边距写死为所有机型通用值。
- 二级页保留各自的固定操作区，不叠加 TabBar。固定操作区同样需要底部安全区内边距。

## 官方依据

- [微信小程序设计指南](https://developers.weixin.qq.com/miniprogram/design/index.html)
- [自定义导航栏页面配置](https://developers.weixin.qq.com/miniprogram/dev/reference/configuration/page.html)
- [胶囊位置 API](https://developers.weixin.qq.com/miniprogram/dev/api/ui/menu/wx.getMenuButtonBoundingClientRect.html)
- [窗口和安全区 API](https://developers.weixin.qq.com/miniprogram/dev/api/base/system/wx.getWindowInfo.html)
- [自定义 TabBar](https://developers.weixin.qq.com/miniprogram/dev/framework/ability/custom-tabbar.html)
