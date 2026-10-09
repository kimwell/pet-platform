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
        post?: never;
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
        ConfirmationInput: {
            /** Format: password */
            currentPassword: string;
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
        PageQuery: {
            /** Format: int32 */
            page: number;
            /** Format: int32 */
            pageSize: number;
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
        Scope: components["schemas"]["TenantScope"] | components["schemas"]["StoresScope"] | components["schemas"]["SelfScope"];
        ScopeData: {
            grants: components["schemas"]["Grant"][];
        };
        SelfScope: {
            /** @enum {string} */
            type: "SELF";
        };
        StoresScope: {
            storeIds: string[];
            /** @enum {string} */
            type: "STORES";
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
        SuccessPageResponseEmployeeView: {
            data: components["schemas"]["PageResponseEmployeeView"] | null;
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
}
