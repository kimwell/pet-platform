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
        get: operations["csrf"];
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
        post: operations["logout"];
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
        get: operations["me"];
        put?: never;
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
}
export type webhooks = Record<string, never>;
export interface components {
    schemas: {
        ApiError: {
            /** @enum {string} */
            code: "BAD_REQUEST" | "PAGINATION_INVALID" | "SORT_INVALID" | "AUTH_CREDENTIAL_AMBIGUOUS" | "AUTH_REQUIRED" | "SESSION_EXPIRED" | "SESSION_REVOKED" | "AUTH_DOMAIN_MISMATCH" | "LOGIN_FAILED" | "PERMISSION_DENIED" | "CSRF_INVALID" | "RESOURCE_NOT_FOUND" | "METHOD_NOT_ALLOWED" | "NOT_ACCEPTABLE" | "BUSINESS_STATE_CONFLICT" | "DUPLICATE_RESOURCE" | "VERSION_CONFLICT" | "SESSION_LIMIT_REACHED" | "IDEMPOTENCY_CONFLICT" | "PAYLOAD_TOO_LARGE" | "UNSUPPORTED_MEDIA_TYPE" | "VALIDATION_FAILED" | "DATE_RANGE_INVALID" | "AMOUNT_INVALID" | "RESULT_TOO_LARGE" | "RATE_LIMITED" | "INTERNAL_ERROR" | "DEPENDENCY_UNAVAILABLE" | "CAPABILITY_DISABLED" | "REQUEST_REJECTED";
            fieldErrors?: components["schemas"]["FieldErrorDetail"][];
            message: string;
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
    csrf: {
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
    logout: {
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
    me: {
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
}
