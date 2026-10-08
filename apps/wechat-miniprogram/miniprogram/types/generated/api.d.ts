// 自动生成：请勿手改；来源SHA256=8b4e56afe1ad546d76082c5caa062320d09d7e9316c572afa35613fb2decda19
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
        post: operations["logout_1"];
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
        post: operations["logoutAll_1"];
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
        get: operations["me_1"];
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
            code: "BAD_REQUEST" | "PAGINATION_INVALID" | "SORT_INVALID" | "AUTH_CREDENTIAL_AMBIGUOUS" | "AUTH_REQUIRED" | "SESSION_EXPIRED" | "SESSION_REVOKED" | "AUTH_DOMAIN_MISMATCH" | "LOGIN_FAILED" | "PERMISSION_DENIED" | "SECURITY_CONFIRMATION_FAILED" | "PASSWORD_CHANGE_REQUIRED" | "CSRF_INVALID" | "RESOURCE_NOT_FOUND" | "METHOD_NOT_ALLOWED" | "NOT_ACCEPTABLE" | "BUSINESS_STATE_CONFLICT" | "DUPLICATE_RESOURCE" | "VERSION_CONFLICT" | "SESSION_LIMIT_REACHED" | "IDEMPOTENCY_CONFLICT" | "PAYLOAD_TOO_LARGE" | "UNSUPPORTED_MEDIA_TYPE" | "VALIDATION_FAILED" | "DATE_RANGE_INVALID" | "AMOUNT_INVALID" | "RESULT_TOO_LARGE" | "RATE_LIMITED" | "INTERNAL_ERROR" | "DEPENDENCY_UNAVAILABLE" | "CAPABILITY_DISABLED" | "REQUEST_REJECTED";
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
        SuccessFieldErrorDetail: {
            data: components["schemas"]["FieldErrorDetail"] | null;
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
    logoutAll_1: {
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
