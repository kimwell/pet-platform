/** 自动生成：后端OpenAPI → openapi-typescript；禁止手改。 */
export interface paths {
    "/api/admin/auth/csrf": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取员工Web CSRF凭据
         * @description 匿名创建10分钟服务器预会话；已登录绑定当前WEB设备；no-store，不续闲置期限
         */
        get: operations["csrf_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/admin/auth/login": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 员工Web账号密码登录
         * @description 必须先获取预会话Cookie与CSRF；提交X-CSRF-Token和固定Origin或Referer。成功轮换会话与CSRF，仅Set-Cookie，不返回Token。
         */
        post: operations["web"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/admin/auth/logout": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 退出当前员工设备
         * @description Cookie需CSRF与来源校验，Token渠道无需浏览器CSRF。只撤销当前设备并删除对应Cookie/CSRF；重复无有效凭据返回401。
         */
        post: operations["logout_2"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/admin/auth/logout-all": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 本人退出全部员工会话
         * @description 当前密码重新确认；数据库安全版本递增；WEB和小程序设备全部失效，重复旧会话401
         */
        post: operations["logoutAll_2"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/admin/auth/me": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 当前员工身份
         * @description 每请求读取正式有效租户、员工、角色、权限和门店授权；不续闲置期限
         */
        get: operations["me_2"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/admin/auth/password": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 本人修改员工密码
         * @description 当前密码重新确认；不接受employeeId；成功含当前设备的全部STAFF会话失效，清WEB Cookie与CSRF，必须重新登录
         */
        put: operations["change"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/admin/auth/token/login": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 员工小程序账号密码登录
         * @description 不得提交Web会话/预会话Cookie或身份Header；成功建立独立MINIPROGRAM设备，仅此响应返回原始Token。
         */
        post: operations["mini"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/admin/identity/organizations": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 本租户内部组织列表
         * @description identity:organization:list，TENANT范围；组织不决定业务数据范围。
         */
        get: operations["listOrganizations"];
        put?: never;
        /** 创建本租户内部组织 */
        post: operations["createOrganization"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/admin/identity/organizations/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** 本租户组织详情 */
        get: operations["getOrganization"];
        /** 修改本租户组织名称 */
        put: operations["editOrganization"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/admin/identity/organizations/{id}/status": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /** 启停本租户内部组织 */
        put: operations["setOrganizationStatus"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/admin/identity/permissions": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 正式权限及可授予范围
         * @description identity:role:detail；服务端计算可授予范围，写入仍独立检查。
         */
        get: operations["listPermissionOptions"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/admin/identity/roles": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 角色列表
         * @description identity:role:list；独立TENANT读取，分页total字符串。
         */
        get: operations["listRoles"];
        put?: never;
        /**
         * 创建角色
         * @description identity:role:create；ACTIVE空权限，不创建保留角色。
         */
        post: operations["createRole"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/admin/identity/roles/{roleId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 角色详情
         * @description identity:role:detail；独立TENANT读取。
         */
        get: operations["getRole"];
        /**
         * 修改角色名称与启停
         * @description identity:role:update；保护角色不可修改，关联身份全部失效。
         */
        put: operations["editRole"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/admin/identity/roles/{roleId}/grants": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 配置角色逐权限数据范围
         * @description identity:role:grant；全量替换；同权限并集，不同权限不互借，受操作者逐权限可授予范围约束。
         */
        put: operations["setRoleGrants"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/admin/identity/users": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 员工分页查询
         * @description identity:user:list；TENANT本租户、STORES有效授权门店与目标关系任一交集、SELF本人；同权限并集。账号和姓名大小写不敏感的字面量包含匹配。列表及count同一快照，total十进制字符串；不输出角色、门店关系或写能力。
         */
        get: operations["listEmployees"];
        put?: never;
        /**
         * 创建员工
         * @description identity:user:create；初始ACTIVE、无关联、强制首次改密。
         */
        post: operations["createEmployee"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/admin/identity/users/{employeeId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 员工详情查询
         * @description 独立identity:user:detail及其自身范围；字段与列表一致。不存在、跨租户、范围外统一404；不授权任何修改或凭据操作。
         */
        get: operations["getEmployee"];
        /**
         * 修改员工姓名
         * @description identity:user:update；版本冲突409；账号固定。
         */
        put: operations["editEmployee"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/admin/identity/users/{employeeId}/management": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 员工管理资料
         * @description 独立detail权限及全门店覆盖；关联ID、版本、保护及改密状态，不含凭据。
         */
        get: operations["getEmployeeManagement"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/admin/identity/users/{employeeId}/password": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 授权管理员重置员工临时密码
         * @description identity:user:reset-password及独立目标管理策略；version为目标资源版本；禁止本人、系统保留和tenant-admin账号；同事务强制改密并撤销目标全部设备
         */
        put: operations["reset"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/admin/identity/users/{employeeId}/revoke-sessions": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 授权管理员撤销目标员工全部会话
         * @description 独立identity:user:revoke-sessions权限及目标管理范围；操作者当前密码确认；不改密码/角色/归属；version防止并发与旧请求重放
         */
        post: operations["revoke"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/admin/identity/users/{employeeId}/roles": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 配置员工角色
         * @description identity:user:roles；全量替换，租户/授予上限检查，同事务撤销旧身份。
         */
        put: operations["setEmployeeRoles"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/admin/identity/users/{employeeId}/status": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 启用或停用员工
         * @description 分别identity:user:enable/disable；安全代际失效；禁止本人及保留管理员。
         */
        put: operations["setEmployeeStatus"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/admin/identity/users/{employeeId}/stores": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 配置员工门店授权
         * @description identity:user:stores；全量替换，ACTIVE归属和逐权限授予上限检查。
         */
        put: operations["setEmployeeStores"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/admin/platform/stores/options": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 门店授权选择读取
         * @description platform:store:list；仅本租户ACTIVE、当前操作范围与身份门店上限交集，分页total字符串。
         */
        get: operations["listStoreOptions"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/customer/auth/logout": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 退出当前客户会话
         * @description 撤销当前客户设备，其他身份域不受影响；重复已退出请求401
         */
        post: operations["logout_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/customer/auth/logout-all": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 退出客户全部会话
         * @description 无需密码；有效当前客户Token再次验证，数据库同事务递增安全代际。旧会话即刻逻辑失效，物理清理失败标PENDING，后续新登录清旧代际；不能自动重放。
         */
        post: operations["logoutAll_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/customer/auth/me": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 当前客户身份
         * @description 真实会话及有效租户/客户/绑定/安全版本重验，不续闲置期限；不返回微信标识
         */
        get: operations["me_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/customer/auth/wechat/login": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 客户微信登录
         * @description 服务端入口白名单解析有效租户和应用；一次code交换，不自动重试。共享AppID按租户独立注册，不提供员工权限。
         */
        post: operations["login_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/platform/accounts": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 平台账号列表
         * @description platform:account:list；筛选分页排序，id稳定次序，total字符串。
         */
        get: operations["listControlAccounts"];
        put?: never;
        /**
         * 创建平台账号与权限
         * @description platform:account:create + platform:account:grant；正式PLATFORM身份，写请求CSRF，版本和治理约束在事务中复核。
         */
        post: operations["createControlAccount"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/platform/accounts/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 平台账号详情
         * @description platform:account:detail；正式PLATFORM身份，写请求CSRF，版本和治理约束在事务中复核。
         */
        get: operations["getControlAccount"];
        /**
         * 修改平台账号基本资料
         * @description platform:account:update；正式PLATFORM身份，写请求CSRF，版本和治理约束在事务中复核。
         */
        put: operations["editControlAccount"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/platform/accounts/{id}/password": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 重置平台账号密码
         * @description platform:account:reset-password；正式PLATFORM身份，写请求CSRF，版本和治理约束在事务中复核。
         */
        put: operations["resetControlAccountPassword"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/platform/accounts/{id}/permissions": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 配置平台账号权限
         * @description platform:account:grant；正式PLATFORM身份，写请求CSRF，版本和治理约束在事务中复核。
         */
        put: operations["setControlAccountGrants"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/platform/accounts/{id}/sessions/revoke": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 撤销平台账号全部会话
         * @description platform:account:revoke-sessions；正式PLATFORM身份，写请求CSRF，版本和治理约束在事务中复核。
         */
        post: operations["revokeControlAccountSessions"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/platform/accounts/{id}/status": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 启停平台账号
         * @description platform:account:enable / platform:account:disable；正式PLATFORM身份，写请求CSRF，版本和治理约束在事务中复核。
         */
        put: operations["setControlAccountStatus"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/platform/auth/csrf": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 获取平台Web CSRF
         * @description 匿名10分钟独立服务器预会话；已登录取当前设备绑定值，不续闲置
         */
        get: operations["csrf"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/platform/auth/login": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 平台Web登录
         * @description 先取平台pre Cookie和CSRF；登录成功销毁pre，轮换Token/CSRF，只Set-Cookie，不签发小程序Token
         */
        post: operations["login"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/platform/auth/logout": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 退出当前平台会话
         * @description platform:session:manage；独立CSRF与同源来源；只退出当前平台设备
         */
        post: operations["logout"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/platform/auth/logout-all": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 本人退出全部平台会话
         * @description platform:session:manage；当前密码确认，递增数据库安全代际，不改变密码或STAFF会话
         */
        post: operations["logoutAll"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/platform/auth/me": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 当前平台身份
         * @description platform:session:manage；每请求DB重验，tenantId/dataScope=null，不建立TenantContext
         */
        get: operations["me"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/platform/auth/password": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 本人修改平台密码
         * @description platform:credential:change；旧密码确认，同事务改密/代际/记录/撤销意图；全部旧平台会话失效，员工不受影响
         */
        put: operations["password"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/platform/permissions": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 平台可授予权限
         * @description platform:account:grant；正式PLATFORM身份，写请求CSRF，版本和治理约束在事务中复核。
         */
        get: operations["listControlPermissions"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/platform/tenants": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 平台租户列表
         * @description platform:tenant:list；筛选分页排序，id稳定次序，total字符串。
         */
        get: operations["listControlTenants"];
        put?: never;
        /**
         * 创建待初始化租户
         * @description platform:tenant:create；正式PLATFORM身份，写请求CSRF，版本和治理约束在事务中复核。
         */
        post: operations["createControlTenant"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/platform/tenants/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 租户详情
         * @description platform:tenant:detail；正式PLATFORM身份，写请求CSRF，版本和治理约束在事务中复核。
         */
        get: operations["getControlTenant"];
        /**
         * 修改租户基本资料
         * @description platform:tenant:update；正式PLATFORM身份，写请求CSRF，版本和治理约束在事务中复核。
         */
        put: operations["editControlTenant"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/platform/tenants/{id}/status": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 启用或停用租户
         * @description platform:tenant:enable / platform:tenant:disable；正式PLATFORM身份，写请求CSRF，版本和治理约束在事务中复核。
         */
        put: operations["setControlTenantStatus"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/platform/tenants/{tenantId}/stores": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 租户最小门店目录
         * @description platform:store:control-list；正式PLATFORM身份，写请求CSRF，版本和治理约束在事务中复核。
         */
        get: operations["listControlStores"];
        put?: never;
        /**
         * 创建正式门店元数据
         * @description platform:store:control-create；正式PLATFORM身份，写请求CSRF，版本和治理约束在事务中复核。
         */
        post: operations["createControlStore"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/platform/tenants/{tenantId}/stores/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 修改门店名称
         * @description platform:store:control-update；正式PLATFORM身份，写请求CSRF，版本和治理约束在事务中复核。
         */
        put: operations["editControlStore"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/platform/tenants/{tenantId}/stores/{id}/status": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 修改门店状态
         * @description platform:store:control-status；正式PLATFORM身份，写请求CSRF，版本和治理约束在事务中复核。
         */
        put: operations["setControlStoreStatus"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
}
export type webhooks = Record<string, never>;
export interface components {
    schemas: {
        ApiError: {
            /** @enum {string} */
            code: "BAD_REQUEST" | "PAGINATION_INVALID" | "SORT_INVALID" | "AUTH_CREDENTIAL_AMBIGUOUS" | "AUTH_REQUIRED" | "SESSION_EXPIRED" | "SESSION_REVOKED" | "AUTH_DOMAIN_MISMATCH" | "WECHAT_CODE_INVALID" | "WECHAT_RESULT_UNCERTAIN" | "WECHAT_UPSTREAM_ERROR" | "WECHAT_RESPONSE_INVALID" | "WECHAT_CONFIGURATION_MISSING" | "LOGIN_FAILED" | "PERMISSION_DENIED" | "SECURITY_CONFIRMATION_FAILED" | "PASSWORD_CHANGE_REQUIRED" | "CSRF_INVALID" | "RESOURCE_NOT_FOUND" | "METHOD_NOT_ALLOWED" | "NOT_ACCEPTABLE" | "BUSINESS_STATE_CONFLICT" | "DUPLICATE_RESOURCE" | "VERSION_CONFLICT" | "SESSION_LIMIT_REACHED" | "IDEMPOTENCY_CONFLICT" | "PAYLOAD_TOO_LARGE" | "UNSUPPORTED_MEDIA_TYPE" | "VALIDATION_FAILED" | "DATE_RANGE_INVALID" | "AMOUNT_INVALID" | "RESULT_TOO_LARGE" | "RATE_LIMITED" | "INTERNAL_ERROR" | "DEPENDENCY_UNAVAILABLE" | "CAPABILITY_DISABLED" | "REQUEST_REJECTED";
            fieldErrors?: components["schemas"]["FieldErrorDetail"][];
            message: string;
        };
        ChangePasswordInput: {
            /** Format: password */
            currentPassword: string;
            /** Format: password */
            newPassword: string;
        };
        ChangeStatus: {
            status: string;
            version: string;
        };
        ConfirmationInput: {
            /** Format: password */
            currentPassword: string;
        };
        ControlAccountGrants: {
            permissions: string[];
            version: string;
        };
        ControlAccountReset: {
            /** Format: password */
            newPassword: string;
            version: string;
        };
        ControlAccountRevoke: {
            version: string;
        };
        ControlAccountView: {
            /**
             * Format: date-time
             * @description 输入必须带Z或offset、最多3位小数；输出UTC且固定3位毫秒
             */
            createdAt: string;
            displayName: string;
            /** Format: uuid */
            id: string;
            loginName: string;
            permissions: string[];
            /** @enum {string} */
            status: "ACTIVE" | "DISABLED";
            /**
             * Format: date-time
             * @description 输入必须带Z或offset、最多3位小数；输出UTC且固定3位毫秒
             */
            updatedAt: string;
            version: string;
        };
        ControlMutation: {
            /** Format: uuid */
            id: string;
            sessionCleanupComplete: boolean;
            version: string;
        };
        ControlPermissionOption: {
            code: string;
            grantable: boolean;
        };
        ControlStatus: {
            status: string;
            version: string;
        };
        ControlStoreView: {
            code: string;
            /**
             * Format: date-time
             * @description 输入必须带Z或offset、最多3位小数；输出UTC且固定3位毫秒
             */
            createdAt: string;
            /** Format: uuid */
            id: string;
            name: string;
            /** @enum {string} */
            status: "ACTIVE" | "DISABLED";
            /** Format: uuid */
            tenantId: string;
            /**
             * Format: date-time
             * @description 输入必须带Z或offset、最多3位小数；输出UTC且固定3位毫秒
             */
            updatedAt: string;
            version: string;
        };
        CreateControlAccount: {
            displayName: string;
            /** Format: password */
            initialPassword: string;
            loginName: string;
            permissions: string[];
        };
        CreateControlStore: {
            code: string;
            name: string;
        };
        CreateEmployee: {
            displayName: string;
            /** Format: password */
            initialPassword: string;
            loginName: string;
        };
        CreateOrganization: {
            code: string;
            name: string;
        };
        CreateRole: {
            code: string;
            name: string;
        };
        CreateTenant: {
            code: string;
            name: string;
        };
        CsrfResult: {
            csrfToken: string;
            /**
             * Format: date-time
             * @description 输入必须带Z或offset、最多3位小数；输出UTC且固定3位毫秒
             */
            expiresAt: string;
        };
        CurrentIdentity: {
            /** @description 当前数据库授权版本；每请求重载授权，不仅依赖版本 */
            authorizationVersion: string;
            authorizedStoreIds: string[];
            dataScope: components["schemas"]["ScopeData"];
            displayName: string;
            /**
             * Format: date-time
             * @description 输入必须带Z或offset、最多3位小数；输出UTC且固定3位毫秒
             */
            expiresAt: string;
            /** Format: int32 */
            idleTimeoutSeconds: number;
            passwordChangeRequired: boolean;
            permissionCodes: string[];
            /** Format: uuid */
            principalId: string;
            /** @enum {string} */
            principalType: "STAFF";
            /** Format: uuid */
            sessionId: string;
            /** Format: uuid */
            tenantId: string;
        };
        CustomerCurrentIdentity: {
            authorizationVersion: string;
            authorizedStoreIds: string[];
            dataScope: components["schemas"]["CustomerScopeData"];
            displayName: string;
            /**
             * Format: date-time
             * @description 输入必须带Z或offset、最多3位小数；输出UTC且固定3位毫秒
             */
            expiresAt: string;
            /** Format: int32 */
            idleTimeoutSeconds: number;
            permissionCodes: string[];
            /** Format: uuid */
            principalId: string;
            /** @enum {string} */
            principalType: "CUSTOMER";
            /** Format: uuid */
            sessionId: string;
            /** Format: uuid */
            tenantId: string;
        };
        CustomerGrant: {
            permissionCode: string;
            scopes: components["schemas"]["CustomerSelfScope"][];
        };
        CustomerScopeData: {
            grants: components["schemas"]["CustomerGrant"][];
        };
        CustomerSelfScope: {
            /** @enum {string} */
            type: "SELF";
        };
        CustomerTokenLoginResult: {
            identity: components["schemas"]["CustomerCurrentIdentity"];
            token: components["schemas"]["CustomerTokenResult"];
        };
        CustomerTokenResult: {
            /**
             * Format: date-time
             * @description 输入必须带Z或offset、最多3位小数；输出UTC且固定3位毫秒
             */
            expiresAt: string;
            /** @enum {string} */
            headerName: "X-Customer-Token";
            value: string;
        };
        EditControlAccount: {
            displayName: string;
            version: string;
        };
        EditControlName: {
            name: string;
            version: string;
        };
        EditEmployee: {
            displayName: string;
            version: string;
        };
        EditOrganization: {
            name: string;
            version: string;
        };
        EditRole: {
            name: string;
            status: string;
            version: string;
        };
        EmployeeAssignments: {
            ids: string[];
            version: string;
        };
        EmployeeManagement: {
            displayName: string;
            /** Format: uuid */
            id: string;
            loginName: string;
            passwordChangeRequired: boolean;
            protectedAccount: boolean;
            roleIds: string[];
            /** @enum {string} */
            status: "ACTIVE" | "DISABLED";
            storeIds: string[];
            version: string;
        };
        EmployeeView: {
            /**
             * Format: date-time
             * @description 输入必须带Z或offset、最多3位小数；输出UTC且固定3位毫秒
             */
            createdAt: string;
            displayName: string;
            /** Format: uuid */
            id: string;
            loginName: string;
            /** @enum {string} */
            status: "ACTIVE" | "DISABLED";
            /**
             * Format: date-time
             * @description 输入必须带Z或offset、最多3位小数；输出UTC且固定3位毫秒
             */
            updatedAt: string;
        };
        Failure: {
            error: components["schemas"]["ApiError"];
            /** @constant */
            success: false;
            traceId: string;
        };
        FieldErrorDetail: {
            code: string;
            field: string;
            message: string;
        };
        Grant: {
            permissionCode: string;
            scopes: components["schemas"]["Scope"][];
        };
        LoginInput: {
            loginName: string;
            /** Format: password */
            password: string;
            tenantCode: string;
        };
        MutationResult: {
            /** Format: uuid */
            id: string;
            sessionCleanupComplete: boolean;
            version: string;
        };
        OrganizationView: {
            code: string;
            /** Format: uuid */
            id: string;
            name: string;
            /** @enum {string} */
            status: "ACTIVE" | "DISABLED";
            version: string;
        };
        PageQuery: {
            /** Format: int32 */
            page: number;
            /** Format: int32 */
            pageSize: number;
        };
        PageResponseControlAccountView: {
            items: components["schemas"]["ControlAccountView"][];
            /** Format: int32 */
            page: number;
            /** Format: int32 */
            pageSize: number;
            total: string;
        };
        PageResponseEmployeeView: {
            items: components["schemas"]["EmployeeView"][];
            /** Format: int32 */
            page: number;
            /** Format: int32 */
            pageSize: number;
            total: string;
        };
        PageResponseFieldErrorDetail: {
            items: components["schemas"]["FieldErrorDetail"][];
            /** Format: int32 */
            page: number;
            /** Format: int32 */
            pageSize: number;
            total: string;
        };
        PageResponseOrganizationView: {
            items: components["schemas"]["OrganizationView"][];
            /** Format: int32 */
            page: number;
            /** Format: int32 */
            pageSize: number;
            total: string;
        };
        PageResponseRoleSummary: {
            items: components["schemas"]["RoleSummary"][];
            /** Format: int32 */
            page: number;
            /** Format: int32 */
            pageSize: number;
            total: string;
        };
        PageResponseStoreOption: {
            items: components["schemas"]["StoreOption"][];
            /** Format: int32 */
            page: number;
            /** Format: int32 */
            pageSize: number;
            total: string;
        };
        PageResponseTenantView: {
            items: components["schemas"]["TenantView"][];
            /** Format: int32 */
            page: number;
            /** Format: int32 */
            pageSize: number;
            total: string;
        };
        PermissionGrant: {
            permissionCode: string;
            /** @enum {string} */
            scopeType: "TENANT" | "STORES" | "SELF";
        };
        PermissionOption: {
            code: string;
            grantableScopes: ("TENANT" | "STORES" | "SELF")[];
            name: string;
            scopes: ("TENANT" | "STORES" | "SELF")[];
        };
        PlatformCurrentIdentity: {
            authorizationVersion: string;
            authorizedStoreIds: string[];
            dataScope: null;
            displayName: string;
            /**
             * Format: date-time
             * @description 输入必须带Z或offset、最多3位小数；输出UTC且固定3位毫秒
             */
            expiresAt: string;
            /** Format: int32 */
            idleTimeoutSeconds: number;
            permissionCodes: string[];
            /** Format: uuid */
            principalId: string;
            /** @enum {string} */
            principalType: "PLATFORM";
            /** Format: uuid */
            sessionId: string;
            tenantId: null;
        };
        PlatformLoginInput: {
            loginName: string;
            /** Format: password */
            password: string;
        };
        ResetPasswordInput: {
            /** Format: password */
            currentPassword: string;
            /**
             * Format: password
             * @description 操作者通过安全人工渠道交付的临时密码；响应不回显
             */
            newPassword: string;
            version: string;
        };
        RevokeSessionsInput: {
            /** Format: password */
            currentPassword: string;
            version: string;
        };
        RoleDetail: {
            grants: components["schemas"]["PermissionGrant"][];
            role: components["schemas"]["RoleSummary"];
        };
        RoleGrants: {
            grants: components["schemas"]["PermissionGrant"][];
            version: string;
        };
        RoleSummary: {
            code: string;
            /** Format: uuid */
            id: string;
            name: string;
            protectedRole: boolean;
            /** @enum {string} */
            status: "ACTIVE" | "DISABLED";
            version: string;
        };
        Scope: components["schemas"]["TenantScope"] | components["schemas"]["StoresScope"] | components["schemas"]["SelfScope"];
        ScopeData: {
            grants: components["schemas"]["Grant"][];
        };
        SelfScope: {
            /** @enum {string} */
            type: "SELF";
        };
        StoreOption: {
            code: string;
            /** Format: uuid */
            id: string;
            name: string;
        };
        StoresScope: {
            storeIds: string[];
            /** @enum {string} */
            type: "STORES";
        };
        SuccessControlAccountView: {
            data: components["schemas"]["ControlAccountView"] | null;
            /** @constant */
            success: true;
            traceId: string;
        };
        SuccessControlMutation: {
            data: components["schemas"]["ControlMutation"] | null;
            /** @constant */
            success: true;
            traceId: string;
        };
        SuccessCsrfResult: {
            data: components["schemas"]["CsrfResult"] | null;
            /** @constant */
            success: true;
            traceId: string;
        };
        SuccessCurrentIdentity: {
            data: components["schemas"]["CurrentIdentity"] | null;
            /** @constant */
            success: true;
            traceId: string;
        };
        SuccessCustomerCurrentIdentity: {
            data: components["schemas"]["CustomerCurrentIdentity"] | null;
            /** @constant */
            success: true;
            traceId: string;
        };
        SuccessCustomerTokenLoginResult: {
            data: components["schemas"]["CustomerTokenLoginResult"] | null;
            /** @constant */
            success: true;
            traceId: string;
        };
        SuccessEmployeeManagement: {
            data: components["schemas"]["EmployeeManagement"] | null;
            /** @constant */
            success: true;
            traceId: string;
        };
        SuccessEmployeeView: {
            data: components["schemas"]["EmployeeView"] | null;
            /** @constant */
            success: true;
            traceId: string;
        };
        SuccessFieldErrorDetail: {
            data: components["schemas"]["FieldErrorDetail"] | null;
            /** @constant */
            success: true;
            traceId: string;
        };
        SuccessListControlPermissionOption: {
            data: (components["schemas"]["ControlPermissionOption"][] | null) | null;
            /** @constant */
            success: true;
            traceId: string;
        };
        SuccessListControlStoreView: {
            data: (components["schemas"]["ControlStoreView"][] | null) | null;
            /** @constant */
            success: true;
            traceId: string;
        };
        SuccessListPermissionOption: {
            data: (components["schemas"]["PermissionOption"][] | null) | null;
            /** @constant */
            success: true;
            traceId: string;
        };
        SuccessMutationResult: {
            data: components["schemas"]["MutationResult"] | null;
            /** @constant */
            success: true;
            traceId: string;
        };
        SuccessOrganizationView: {
            data: components["schemas"]["OrganizationView"] | null;
            /** @constant */
            success: true;
            traceId: string;
        };
        SuccessPageResponseControlAccountView: {
            data: components["schemas"]["PageResponseControlAccountView"] | null;
            /** @constant */
            success: true;
            traceId: string;
        };
        SuccessPageResponseEmployeeView: {
            data: components["schemas"]["PageResponseEmployeeView"] | null;
            /** @constant */
            success: true;
            traceId: string;
        };
        SuccessPageResponseOrganizationView: {
            data: components["schemas"]["PageResponseOrganizationView"] | null;
            /** @constant */
            success: true;
            traceId: string;
        };
        SuccessPageResponseRoleSummary: {
            data: components["schemas"]["PageResponseRoleSummary"] | null;
            /** @constant */
            success: true;
            traceId: string;
        };
        SuccessPageResponseStoreOption: {
            data: components["schemas"]["PageResponseStoreOption"] | null;
            /** @constant */
            success: true;
            traceId: string;
        };
        SuccessPageResponseTenantView: {
            data: components["schemas"]["PageResponseTenantView"] | null;
            /** @constant */
            success: true;
            traceId: string;
        };
        SuccessPlatformCurrentIdentity: {
            data: components["schemas"]["PlatformCurrentIdentity"] | null;
            /** @constant */
            success: true;
            traceId: string;
        };
        SuccessRoleDetail: {
            data: components["schemas"]["RoleDetail"] | null;
            /** @constant */
            success: true;
            traceId: string;
        };
        SuccessTenantView: {
            data: components["schemas"]["TenantView"] | null;
            /** @constant */
            success: true;
            traceId: string;
        };
        SuccessTokenLoginResult: {
            data: components["schemas"]["TokenLoginResult"] | null;
            /** @constant */
            success: true;
            traceId: string;
        };
        SuccessVoid: {
            data: null;
            /** @constant */
            success: true;
            traceId: string;
        };
        TenantScope: {
            /** @enum {string} */
            type: "TENANT";
        };
        TenantView: {
            code: string;
            /**
             * Format: date-time
             * @description 输入必须带Z或offset、最多3位小数；输出UTC且固定3位毫秒
             */
            createdAt: string;
            /** Format: uuid */
            id: string;
            initialized: boolean;
            name: string;
            /** @enum {string} */
            status: "ACTIVE" | "DISABLED";
            /**
             * Format: date-time
             * @description 输入必须带Z或offset、最多3位小数；输出UTC且固定3位毫秒
             */
            updatedAt: string;
            version: string;
        };
        TokenLoginResult: {
            identity: components["schemas"]["CurrentIdentity"];
            token: components["schemas"]["TokenResult"];
        };
        TokenResult: {
            /**
             * Format: date-time
             * @description 输入必须带Z或offset、最多3位小数；输出UTC且固定3位毫秒
             */
            expiresAt: string;
            headerName: string;
            value: string;
        };
        WechatLoginInput: {
            code: string;
            entryId: string;
            tenantCode: string;
        };
    };
    responses: never;
    parameters: never;
    requestBodies: never;
    headers: never;
    pathItems: never;
}
export type $defs = Record<string, never>;
export interface operations {
    csrf_1: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 成功，统一响应信封 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SuccessCsrfResult"];
                };
            };
            /** @description 非法格式或凭据混用 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 认证失败、未登录或会话失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description CSRF或权限拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 输入校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 频控，响应Retry-After */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 数据库或Redis不可用 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
        };
    };
    web: {
        parameters: {
            query?: never;
            header: {
                /** @description 必须匹配固定来源；缺失时必须提供同源Referer */
                Origin?: string;
                /** @description GET csrf取得的服务器绑定凭据 */
                "X-CSRF-Token": string;
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["LoginInput"];
            };
        };
        responses: {
            /** @description 成功，统一响应信封 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SuccessCurrentIdentity"];
                };
            };
            /** @description 非法格式或凭据混用 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 认证失败、未登录或会话失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description CSRF或权限拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 设备数上限 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 输入校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 频控，响应Retry-After */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 数据库或Redis不可用 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
        };
    };
    logout_2: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 成功，统一响应信封 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SuccessVoid"];
                };
            };
            /** @description 非法格式或凭据混用 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 认证失败、未登录或会话失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description CSRF或权限拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 输入校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 频控，响应Retry-After */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 数据库或Redis不可用 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
        };
    };
    logoutAll_2: {
        parameters: {
            query?: never;
            header?: {
                /** @description WEB必须匹配固定来源，缺失时提供同源Referer */
                Origin?: string;
                /** @description WEB Cookie写入必须；MINIPROGRAM Token无需浏览器CSRF */
                "X-CSRF-Token"?: string;
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ConfirmationInput"];
            };
        };
        responses: {
            /** @description 数据库变更与记录成功；X-Session-Cleanup为COMPLETE或PENDING，data=null */
            200: {
                headers: {
                    "X-Session-Cleanup"?: "COMPLETE" | "PENDING";
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SuccessVoid"];
                };
            };
            /** @description 输入格式或载体错误 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或旧会话已失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 权限、目标保护、重新确认或CSRF失败 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 目标不存在或超出管理范围 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 目标版本竞争或状态冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 输入校验失败或新旧密码相同 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 频控，响应Retry-After */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 变更未确认成功；依赖不可用，禁止自动重试 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
        };
    };
    me_2: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 成功，统一响应信封 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SuccessCurrentIdentity"];
                };
            };
            /** @description 非法格式或凭据混用 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 认证失败、未登录或会话失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description CSRF或权限拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 输入校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 频控，响应Retry-After */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 数据库或Redis不可用 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
        };
    };
    change: {
        parameters: {
            query?: never;
            header?: {
                /** @description WEB必须匹配固定来源，缺失时提供同源Referer */
                Origin?: string;
                /** @description WEB Cookie写入必须；MINIPROGRAM Token无需浏览器CSRF */
                "X-CSRF-Token"?: string;
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ChangePasswordInput"];
            };
        };
        responses: {
            /** @description 数据库变更与记录成功；X-Session-Cleanup为COMPLETE或PENDING，data=null */
            200: {
                headers: {
                    "X-Session-Cleanup"?: "COMPLETE" | "PENDING";
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SuccessVoid"];
                };
            };
            /** @description 输入格式或载体错误 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或旧会话已失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 权限、目标保护、重新确认或CSRF失败 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 目标不存在或超出管理范围 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 目标版本竞争或状态冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 输入校验失败或新旧密码相同 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 频控，响应Retry-After */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 变更未确认成功；依赖不可用，禁止自动重试 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
        };
    };
    mini: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["LoginInput"];
            };
        };
        responses: {
            /** @description 成功，统一响应信封 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SuccessTokenLoginResult"];
                };
            };
            /** @description 非法格式或凭据混用 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 认证失败、未登录或会话失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description CSRF或权限拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 设备数上限 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 输入校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 频控，响应Retry-After */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 数据库或Redis不可用 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
        };
    };
    listOrganizations: {
        parameters: {
            query?: {
                page?: number;
                pageSize?: number;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 成功，统一响应信封 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SuccessPageResponseOrganizationView"];
                };
            };
            /** @description 未知或格式错误输入 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或旧身份失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 无权限、保护账号、授予上限或CSRF拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 目标或关联不存在或不可访问 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 版本或生命周期冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 字段验证失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖故障或写入结果未确认，禁止自动重试 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
        };
    };
    createOrganization: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CreateOrganization"];
            };
        };
        responses: {
            /** @description 成功，统一响应信封 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SuccessMutationResult"];
                };
            };
            /** @description 未知或格式错误输入 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或旧身份失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 无权限、保护账号、授予上限或CSRF拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 目标或关联不存在或不可访问 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 版本或生命周期冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 字段验证失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖故障或写入结果未确认，禁止自动重试 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
        };
    };
    getOrganization: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 成功，统一响应信封 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SuccessOrganizationView"];
                };
            };
            /** @description 未知或格式错误输入 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或旧身份失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 无权限、保护账号、授予上限或CSRF拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 目标或关联不存在或不可访问 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 版本或生命周期冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 字段验证失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖故障或写入结果未确认，禁止自动重试 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
        };
    };
    editOrganization: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["EditOrganization"];
            };
        };
        responses: {
            /** @description 成功，统一响应信封 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SuccessMutationResult"];
                };
            };
            /** @description 未知或格式错误输入 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或旧身份失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 无权限、保护账号、授予上限或CSRF拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 目标或关联不存在或不可访问 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 版本或生命周期冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 字段验证失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖故障或写入结果未确认，禁止自动重试 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
        };
    };
    setOrganizationStatus: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ChangeStatus"];
            };
        };
        responses: {
            /** @description 成功，统一响应信封 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SuccessMutationResult"];
                };
            };
            /** @description 未知或格式错误输入 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或旧身份失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 无权限、保护账号、授予上限或CSRF拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 目标或关联不存在或不可访问 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 版本或生命周期冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 字段验证失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖故障或写入结果未确认，禁止自动重试 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
        };
    };
    listPermissionOptions: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 成功，统一响应信封 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SuccessListPermissionOption"];
                };
            };
            /** @description 未知或格式错误输入 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或旧身份失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 无权限、保护账号、授予上限或CSRF拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 目标或关联不存在或不可访问 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 版本或生命周期冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 字段验证失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖故障或写入结果未确认，禁止自动重试 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
        };
    };
    listRoles: {
        parameters: {
            query?: {
                page?: number;
                pageSize?: number;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 成功，统一响应信封 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SuccessPageResponseRoleSummary"];
                };
            };
            /** @description 未知或格式错误输入 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或旧身份失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 无权限、保护账号、授予上限或CSRF拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 目标或关联不存在或不可访问 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 版本或生命周期冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 字段验证失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖故障或写入结果未确认，禁止自动重试 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
        };
    };
    createRole: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CreateRole"];
            };
        };
        responses: {
            /** @description 成功，统一响应信封 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SuccessMutationResult"];
                };
            };
            /** @description 未知或格式错误输入 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或旧身份失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 无权限、保护账号、授予上限或CSRF拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 目标或关联不存在或不可访问 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 版本或生命周期冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 字段验证失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖故障或写入结果未确认，禁止自动重试 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
        };
    };
    getRole: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                roleId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 成功，统一响应信封 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SuccessRoleDetail"];
                };
            };
            /** @description 未知或格式错误输入 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或旧身份失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 无权限、保护账号、授予上限或CSRF拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 目标或关联不存在或不可访问 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 版本或生命周期冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 字段验证失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖故障或写入结果未确认，禁止自动重试 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
        };
    };
    editRole: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                roleId: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["EditRole"];
            };
        };
        responses: {
            /** @description 成功，统一响应信封 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SuccessMutationResult"];
                };
            };
            /** @description 未知或格式错误输入 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或旧身份失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 无权限、保护账号、授予上限或CSRF拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 目标或关联不存在或不可访问 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 版本或生命周期冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 字段验证失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖故障或写入结果未确认，禁止自动重试 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
        };
    };
    setRoleGrants: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                roleId: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["RoleGrants"];
            };
        };
        responses: {
            /** @description 成功，统一响应信封 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SuccessMutationResult"];
                };
            };
            /** @description 未知或格式错误输入 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或旧身份失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 无权限、保护账号、授予上限或CSRF拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 目标或关联不存在或不可访问 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 版本或生命周期冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 字段验证失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖故障或写入结果未确认，禁止自动重试 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
        };
    };
    listEmployees: {
        parameters: {
            query?: {
                /** @description 1～100个Unicode码点，不能全空白；不trim；%、_、反斜线按字面量 */
                keyword?: string;
                page?: number;
                pageSize?: number;
                sortBy?: "id" | "loginName" | "displayName" | "status" | "createdAt" | "updatedAt";
                /** @description 有方向须有sortBy；同向追加id，NULLS LAST */
                sortOrder?: "asc" | "desc";
                status?: "ACTIVE" | "DISABLED";
                /** @description 有效本租户门店且当前操作范围和身份门店上限均允许；仅SELF拒绝404 */
                storeId?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 成功，统一响应信封 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SuccessPageResponseEmployeeView"];
                };
            };
            /** @description 未知/重复查询字段、非法状态、ID、分页或排序 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录、会话失效或身份域错误 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 缺少当前读取权限或强制改密限制 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 不存在或当前操作范围不可访问 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description keyword约束失败或分页offset过深 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 数据库或认证依赖不可用 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
        };
    };
    createEmployee: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CreateEmployee"];
            };
        };
        responses: {
            /** @description 成功，统一响应信封 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SuccessMutationResult"];
                };
            };
            /** @description 未知或格式错误输入 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或旧身份失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 无权限、保护账号、授予上限或CSRF拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 目标或关联不存在或不可访问 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 版本或生命周期冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 字段验证失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖故障或写入结果未确认，禁止自动重试 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
        };
    };
    getEmployee: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                employeeId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 成功，统一响应信封 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SuccessEmployeeView"];
                };
            };
            /** @description 未知/重复查询字段、非法状态、ID、分页或排序 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录、会话失效或身份域错误 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 缺少当前读取权限或强制改密限制 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 不存在或当前操作范围不可访问 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description keyword约束失败或分页offset过深 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 数据库或认证依赖不可用 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
        };
    };
    editEmployee: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                employeeId: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["EditEmployee"];
            };
        };
        responses: {
            /** @description 成功，统一响应信封 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SuccessMutationResult"];
                };
            };
            /** @description 未知或格式错误输入 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或旧身份失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 无权限、保护账号、授予上限或CSRF拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 目标或关联不存在或不可访问 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 版本或生命周期冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 字段验证失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖故障或写入结果未确认，禁止自动重试 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
        };
    };
    getEmployeeManagement: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                employeeId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 成功，统一响应信封 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SuccessEmployeeManagement"];
                };
            };
            /** @description 未知或格式错误输入 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或旧身份失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 无权限、保护账号、授予上限或CSRF拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 目标或关联不存在或不可访问 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 版本或生命周期冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 字段验证失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖故障或写入结果未确认，禁止自动重试 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
        };
    };
    reset: {
        parameters: {
            query?: never;
            header?: {
                /** @description WEB必须匹配固定来源，缺失时提供同源Referer */
                Origin?: string;
                /** @description WEB Cookie写入必须；MINIPROGRAM Token无需浏览器CSRF */
                "X-CSRF-Token"?: string;
            };
            path: {
                employeeId: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ResetPasswordInput"];
            };
        };
        responses: {
            /** @description 数据库变更与记录成功；X-Session-Cleanup为COMPLETE或PENDING，data=null */
            200: {
                headers: {
                    "X-Session-Cleanup"?: "COMPLETE" | "PENDING";
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SuccessVoid"];
                };
            };
            /** @description 输入格式或载体错误 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或旧会话已失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 权限、目标保护、重新确认或CSRF失败 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 目标不存在或超出管理范围 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 目标版本竞争或状态冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 输入校验失败或新旧密码相同 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 频控，响应Retry-After */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 变更未确认成功；依赖不可用，禁止自动重试 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
        };
    };
    revoke: {
        parameters: {
            query?: never;
            header?: {
                /** @description WEB必须匹配固定来源，缺失时提供同源Referer */
                Origin?: string;
                /** @description WEB Cookie写入必须；MINIPROGRAM Token无需浏览器CSRF */
                "X-CSRF-Token"?: string;
            };
            path: {
                employeeId: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["RevokeSessionsInput"];
            };
        };
        responses: {
            /** @description 数据库变更与记录成功；X-Session-Cleanup为COMPLETE或PENDING，data=null */
            200: {
                headers: {
                    "X-Session-Cleanup"?: "COMPLETE" | "PENDING";
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SuccessVoid"];
                };
            };
            /** @description 输入格式或载体错误 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或旧会话已失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 权限、目标保护、重新确认或CSRF失败 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 目标不存在或超出管理范围 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 目标版本竞争或状态冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 输入校验失败或新旧密码相同 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 频控，响应Retry-After */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 变更未确认成功；依赖不可用，禁止自动重试 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
        };
    };
    setEmployeeRoles: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                employeeId: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["EmployeeAssignments"];
            };
        };
        responses: {
            /** @description 成功，统一响应信封 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SuccessMutationResult"];
                };
            };
            /** @description 未知或格式错误输入 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或旧身份失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 无权限、保护账号、授予上限或CSRF拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 目标或关联不存在或不可访问 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 版本或生命周期冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 字段验证失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖故障或写入结果未确认，禁止自动重试 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
        };
    };
    setEmployeeStatus: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                employeeId: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ChangeStatus"];
            };
        };
        responses: {
            /** @description 成功，统一响应信封 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SuccessMutationResult"];
                };
            };
            /** @description 未知或格式错误输入 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或旧身份失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 无权限、保护账号、授予上限或CSRF拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 目标或关联不存在或不可访问 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 版本或生命周期冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 字段验证失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖故障或写入结果未确认，禁止自动重试 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
        };
    };
    setEmployeeStores: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                employeeId: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["EmployeeAssignments"];
            };
        };
        responses: {
            /** @description 成功，统一响应信封 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SuccessMutationResult"];
                };
            };
            /** @description 未知或格式错误输入 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或旧身份失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 无权限、保护账号、授予上限或CSRF拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 目标或关联不存在或不可访问 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 版本或生命周期冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 字段验证失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖故障或写入结果未确认，禁止自动重试 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
        };
    };
    listStoreOptions: {
        parameters: {
            query?: {
                page?: number;
                pageSize?: number;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 成功，统一响应信封 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SuccessPageResponseStoreOption"];
                };
            };
            /** @description 未知或格式错误输入 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或旧身份失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 无权限、保护账号、授予上限或CSRF拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 目标或关联不存在或不可访问 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 版本或生命周期冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 字段验证失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖故障或写入结果未确认，禁止自动重试 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
        };
    };
    logout_1: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 成功，统一响应信封 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SuccessVoid"];
                };
            };
            /** @description 格式或载体错误 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 登录失败或会话无效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 输入校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 频控，含Retry-After */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖/微信不可用；结果不确定须重新取得code */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
        };
    };
    logoutAll_1: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 全部旧客户会话已失效 */
            200: {
                headers: {
                    "X-Session-Cleanup"?: "COMPLETE" | "PENDING";
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SuccessVoid"];
                };
            };
            /** @description 格式或载体错误 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 登录失败或会话无效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 输入校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 频控，含Retry-After */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖/微信不可用；结果不确定须重新取得code */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
        };
    };
    me_1: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 成功，统一响应信封 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SuccessCustomerCurrentIdentity"];
                };
            };
            /** @description 格式或载体错误 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 登录失败或会话无效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 输入校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 频控，含Retry-After */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖/微信不可用；结果不确定须重新取得code */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
        };
    };
    login_1: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["WechatLoginInput"];
            };
        };
        responses: {
            /** @description 成功，统一响应信封 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SuccessCustomerTokenLoginResult"];
                };
            };
            /** @description 格式或载体错误 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 登录失败或会话无效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 设备上限 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 输入校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 频控，含Retry-After */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖/微信不可用；结果不确定须重新取得code */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["Failure"];
                };
            };
        };
    };
    listControlAccounts: {
        parameters: {
            query?: {
                keyword?: string;
                page?: number;
                pageSize?: number;
                sortBy?: string;
                sortOrder?: "asc" | "desc";
                status?: "ACTIVE" | "DISABLED";
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 成功 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SuccessPageResponseControlAccountView"];
                };
            };
            /** @description 参数格式或未知字段 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或平台身份失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 权限、授予上限或CSRF拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 资源不存在 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 版本、状态或最后管理入口冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 字段校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 敏感操作频控 */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖故障，拒绝执行 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
        };
    };
    createControlAccount: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CreateControlAccount"];
            };
        };
        responses: {
            /** @description 成功 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SuccessControlMutation"];
                };
            };
            /** @description 参数格式或未知字段 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或平台身份失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 权限、授予上限或CSRF拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 资源不存在 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 版本、状态或最后管理入口冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 字段校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 敏感操作频控 */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖故障，拒绝执行 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
        };
    };
    getControlAccount: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 成功 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SuccessControlAccountView"];
                };
            };
            /** @description 参数格式或未知字段 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或平台身份失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 权限、授予上限或CSRF拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 资源不存在 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 版本、状态或最后管理入口冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 字段校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 敏感操作频控 */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖故障，拒绝执行 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
        };
    };
    editControlAccount: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["EditControlAccount"];
            };
        };
        responses: {
            /** @description 成功 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SuccessControlMutation"];
                };
            };
            /** @description 参数格式或未知字段 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或平台身份失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 权限、授予上限或CSRF拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 资源不存在 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 版本、状态或最后管理入口冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 字段校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 敏感操作频控 */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖故障，拒绝执行 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
        };
    };
    resetControlAccountPassword: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ControlAccountReset"];
            };
        };
        responses: {
            /** @description 成功 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SuccessControlMutation"];
                };
            };
            /** @description 参数格式或未知字段 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或平台身份失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 权限、授予上限或CSRF拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 资源不存在 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 版本、状态或最后管理入口冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 字段校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 敏感操作频控 */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖故障，拒绝执行 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
        };
    };
    setControlAccountGrants: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ControlAccountGrants"];
            };
        };
        responses: {
            /** @description 成功 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SuccessControlMutation"];
                };
            };
            /** @description 参数格式或未知字段 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或平台身份失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 权限、授予上限或CSRF拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 资源不存在 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 版本、状态或最后管理入口冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 字段校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 敏感操作频控 */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖故障，拒绝执行 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
        };
    };
    revokeControlAccountSessions: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ControlAccountRevoke"];
            };
        };
        responses: {
            /** @description 成功 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SuccessControlMutation"];
                };
            };
            /** @description 参数格式或未知字段 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或平台身份失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 权限、授予上限或CSRF拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 资源不存在 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 版本、状态或最后管理入口冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 字段校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 敏感操作频控 */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖故障，拒绝执行 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
        };
    };
    setControlAccountStatus: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ControlStatus"];
            };
        };
        responses: {
            /** @description 成功 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SuccessControlMutation"];
                };
            };
            /** @description 参数格式或未知字段 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或平台身份失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 权限、授予上限或CSRF拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 资源不存在 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 版本、状态或最后管理入口冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 字段校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 敏感操作频控 */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖故障，拒绝执行 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
        };
    };
    csrf: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 成功 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SuccessCsrfResult"];
                };
            };
            /** @description 格式或载体冲突 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录、失效或统一登录失败 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 平台权限、CSRF或密码确认失败 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 设备数上限 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 频控，Retry-After */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖不可用；不自动重试写入 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
        };
    };
    login: {
        parameters: {
            query?: never;
            header: {
                /** @description 固定同源，缺失时同源Referer */
                Origin?: string;
                "X-CSRF-Token": string;
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["PlatformLoginInput"];
            };
        };
        responses: {
            /** @description 成功 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SuccessPlatformCurrentIdentity"];
                };
            };
            /** @description 格式或载体冲突 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录、失效或统一登录失败 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 平台权限、CSRF或密码确认失败 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 设备数上限 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 频控，Retry-After */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖不可用；不自动重试写入 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
        };
    };
    logout: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 成功 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SuccessVoid"];
                };
            };
            /** @description 格式或载体冲突 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录、失效或统一登录失败 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 平台权限、CSRF或密码确认失败 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 设备数上限 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 频控，Retry-After */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖不可用；不自动重试写入 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
        };
    };
    logoutAll: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ConfirmationInput"];
            };
        };
        responses: {
            /** @description data=null；数据库已提交，物理清理状态在Header */
            200: {
                headers: {
                    "X-Session-Cleanup"?: "COMPLETE" | "PENDING";
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SuccessVoid"];
                };
            };
            /** @description 格式或载体冲突 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录、失效或统一登录失败 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 平台权限、CSRF或密码确认失败 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 设备数上限 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 频控，Retry-After */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖不可用；不自动重试写入 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
        };
    };
    me: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 成功 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SuccessPlatformCurrentIdentity"];
                };
            };
            /** @description 格式或载体冲突 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录、失效或统一登录失败 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 平台权限、CSRF或密码确认失败 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 设备数上限 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 频控，Retry-After */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖不可用；不自动重试写入 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
        };
    };
    password: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ChangePasswordInput"];
            };
        };
        responses: {
            /** @description data=null；数据库已提交，物理清理状态在Header */
            200: {
                headers: {
                    "X-Session-Cleanup"?: "COMPLETE" | "PENDING";
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SuccessVoid"];
                };
            };
            /** @description 格式或载体冲突 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录、失效或统一登录失败 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 平台权限、CSRF或密码确认失败 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 设备数上限 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 频控，Retry-After */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖不可用；不自动重试写入 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
        };
    };
    listControlPermissions: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 成功 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SuccessListControlPermissionOption"];
                };
            };
            /** @description 参数格式或未知字段 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或平台身份失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 权限、授予上限或CSRF拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 资源不存在 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 版本、状态或最后管理入口冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 字段校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 敏感操作频控 */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖故障，拒绝执行 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
        };
    };
    listControlTenants: {
        parameters: {
            query?: {
                keyword?: string;
                page?: number;
                pageSize?: number;
                sortBy?: string;
                sortOrder?: "asc" | "desc";
                status?: "ACTIVE" | "DISABLED";
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 成功 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SuccessPageResponseTenantView"];
                };
            };
            /** @description 参数格式或未知字段 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或平台身份失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 权限、授予上限或CSRF拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 资源不存在 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 版本、状态或最后管理入口冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 字段校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 敏感操作频控 */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖故障，拒绝执行 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
        };
    };
    createControlTenant: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CreateTenant"];
            };
        };
        responses: {
            /** @description 成功 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SuccessControlMutation"];
                };
            };
            /** @description 参数格式或未知字段 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或平台身份失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 权限、授予上限或CSRF拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 资源不存在 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 版本、状态或最后管理入口冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 字段校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 敏感操作频控 */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖故障，拒绝执行 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
        };
    };
    getControlTenant: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 成功 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SuccessTenantView"];
                };
            };
            /** @description 参数格式或未知字段 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或平台身份失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 权限、授予上限或CSRF拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 资源不存在 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 版本、状态或最后管理入口冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 字段校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 敏感操作频控 */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖故障，拒绝执行 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
        };
    };
    editControlTenant: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["EditControlName"];
            };
        };
        responses: {
            /** @description 成功 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SuccessControlMutation"];
                };
            };
            /** @description 参数格式或未知字段 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或平台身份失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 权限、授予上限或CSRF拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 资源不存在 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 版本、状态或最后管理入口冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 字段校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 敏感操作频控 */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖故障，拒绝执行 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
        };
    };
    setControlTenantStatus: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ControlStatus"];
            };
        };
        responses: {
            /** @description 成功 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SuccessControlMutation"];
                };
            };
            /** @description 参数格式或未知字段 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或平台身份失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 权限、授予上限或CSRF拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 资源不存在 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 版本、状态或最后管理入口冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 字段校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 敏感操作频控 */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖故障，拒绝执行 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
        };
    };
    listControlStores: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                tenantId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 成功 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SuccessListControlStoreView"];
                };
            };
            /** @description 参数格式或未知字段 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或平台身份失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 权限、授予上限或CSRF拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 资源不存在 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 版本、状态或最后管理入口冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 字段校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 敏感操作频控 */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖故障，拒绝执行 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
        };
    };
    createControlStore: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                tenantId: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CreateControlStore"];
            };
        };
        responses: {
            /** @description 成功 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SuccessControlMutation"];
                };
            };
            /** @description 参数格式或未知字段 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或平台身份失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 权限、授予上限或CSRF拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 资源不存在 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 版本、状态或最后管理入口冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 字段校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 敏感操作频控 */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖故障，拒绝执行 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
        };
    };
    editControlStore: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
                tenantId: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["EditControlName"];
            };
        };
        responses: {
            /** @description 成功 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SuccessControlMutation"];
                };
            };
            /** @description 参数格式或未知字段 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或平台身份失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 权限、授予上限或CSRF拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 资源不存在 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 版本、状态或最后管理入口冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 字段校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 敏感操作频控 */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖故障，拒绝执行 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
        };
    };
    setControlStoreStatus: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
                tenantId: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ControlStatus"];
            };
        };
        responses: {
            /** @description 成功 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SuccessControlMutation"];
                };
            };
            /** @description 参数格式或未知字段 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 未登录或平台身份失效 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 权限、授予上限或CSRF拒绝 */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 资源不存在 */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 版本、状态或最后管理入口冲突 */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 字段校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 敏感操作频控 */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 依赖故障，拒绝执行 */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
        };
    };
}
