# 工程结构与三端边界

更新：2026-10-09，三端架构统一改造。根目录固定 `/Users/kimwell/work/pet-platform`，保留 `ui/`；一个 Maven Module，基础包 `com.pet.platform`，启动类 `com.pet.platform.Application`。技术和精确版本仍以 [VERSION-MATRIX](../development/VERSION-MATRIX.md) 为准，前端仅 pnpm 和根锁文件。历史目录快照见 P02、P03、P06 的原验收报告，当前结构以下文为准。

## Web 实际目录

```text
apps/admin-web/src/
├── api/{auth,identity}/ / control.ts / request.ts / ApiError.ts
├── components/{ProTable,ResourceDialog,ErrorNotice}/ / PermissionBoundary.tsx / SessionFailure.tsx
├── config/{AppProviders.tsx,api.ts}
├── hooks/identity/ / useSession.ts / useDraftGuard.tsx / useLocalDetail.ts / usePageIdentity.ts
├── layouts/BasicLayout/{index,Header,Sidebar,Breadcrumb,UserDropdown}.tsx / SystemLayout.tsx
├── pages/
│   ├── auth/ / account/ / system/
│   ├── admin/identity/{users,roles,organizations}/
│   └── platform/{tenants,accounts}/
├── router/{router.ts,pageAccess.ts,guards/requireSession.ts}
├── stores/app.store.ts
├── styles/global.css
├── types/contracts.ts
├── utils/{auth,identity}/ / format.ts
├── test/responses.ts
└── main.tsx
```

测试随职责迁移，不建无用途空目录。TanStack Router 代码路由注册页面，保护空间父布局用 `Outlet`，动态导入业务页面；布局不根据 pathname 分发业务组件。页面定义列和官方 Ant Design Form，Query 管理请求与响应，原生 fetch 和原身份生命周期继续有效。Zustand 只保存空间侧栏折叠及移动菜单状态。

顶部通栏品牌与账号摘要、白色图标侧栏、面包屑、浅灰独立内容滚动区；桌面侧栏 240/64px，窄屏用官方 Drawer。截图及师生档案参考只提供布局思想，权限、字段和操作来自本项目。全部六处业务表格（含租户内门店）使用承载明确列表状态与布局职责的 `ProTable<T>`，不是自动请求框架。见 [列表规范](../conventions/WEB-LIST-PAGES.md)、[弹框规范](../conventions/WEB-DETAIL-PAGES.md)。

## 后端实际职责

```text
apps/backend/src/main/java/com/pet/platform/
├── Application.java
├── shared/                  # 协议、身份上下文、隔离、持久化等基础能力
├── identity/{api,application,domain,infrastructure}/
├── platform/{api,application,domain,infrastructure}/
└── customeridentity/{api,application,domain,infrastructure}/
```

只在有真实行为时建立分层类型。`identity.api.management` 分员工、角色、内部组织、平台账号、租户门店五个 Controller；`identity.application.management` 中对应 `EmployeeManagementService`、`RoleManagementService`、`OrganizationManagementService`、`ControlAccountManagement`、`ControlTenantManagement`。原锁、范围、授予上限、版本、审计和会话撤销逻辑继续有效。

同包不可公开的 `StaffManagementExecutor` / `PlatformManagementExecutor` 分别集中 STAFF 事务和 PLATFORM 控制面执行及身份重验，只允许结构门禁登记的用例继承。控制面编排属于 identity，租户/门店经过 platform 应用端口及持久化适配器，不建立跨域 TenantContext 绕过。精确允许类型、调用者和越界反例由 `StructureRulesTest` 验证。保留旧 OpenAPI tag、HTTP 路径、operationId、DTO、错误码及数据库结构。本轮没有新接口或迁移。模块方向见 [模块边界](MODULE-BOUNDARIES.md)。

## 小程序实际目录

```text
apps/wechat-miniprogram/
├── app.ts / app.json / app.wxss / sitemap.json
├── project.config.json / project.private.config.json.example
├── package.json / tsconfig.json
├── pages/customer/{home,rooms,orders,mine}/index.{ts,json,wxml,wxss}
├── pages/system/entry/        # 旧入口兼容跳转客户首页
├── components/navigator-bar/
├── custom-tab-bar/
├── constants/navigation.ts
├── utils/navigation.ts
├── styles/customer.wxss
├── types/{navigation.ts,contracts.ts,generated/api.d.ts}
└── assets/navigation/
```

源文件直接在应用根目录，没有 `miniprogram/` 中间层。`miniprogramRoot` 和 npm 输出为 `./`；`miniprogram_npm/` 是忽略的官方构建产物。本轮默认 CUSTOMER：四个原生 Tab 页面及中间助手操作共五列，助手不占原生 Tab 页面。公共组件基于运行时窗口/胶囊布局、页面显示时同步选中态，详情正文未开放。

请求服务、会话、登录、员工分包及正文业务属于后续开发，未创建空实现；未来 STAFF 分包必须保持独立身份与导航，不混用客户 Tab。规范见 [小程序页面](../conventions/MINIPROGRAM-PAGES.md)。

## 共享及验收边界

`packages/api-contracts` 只放纯类型，Web 引用 workspace 包，小程序声明复制到根下 `types/generated/`，生成规则见 [OpenAPI 生成](../contracts/OPENAPI-GENERATION.md)。`ui/`、旧验收与迁移保留。当前改造验证见 [验收报告](../testing/ARCHITECTURE-REFRESH-VERIFICATION.md)，小程序框架完成不代表 B04 使用端完成。
