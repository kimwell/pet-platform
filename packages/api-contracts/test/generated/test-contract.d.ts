/** 自动生成：后端OpenAPI → openapi-typescript；禁止手改。 */
export interface paths {
    "/__contracts/error": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["testSafeError"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/__contracts/null": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["testNullSuccess"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/__contracts/page": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["testScalarPage"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/__contracts/scalars": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["testEchoScalars"];
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
        PageResponseScalarOutput: {
            items: components["schemas"]["ScalarOutput"][];
            /** Format: int32 */
            page: number;
            /** Format: int32 */
            pageSize: number;
            total: string;
        };
        ScalarInput: {
            /** @example 12.30 */
            amount: string;
            /** @enum {string} */
            currency: "CNY";
            /** Format: date */
            date: string;
            enabled: boolean;
            /** Format: uuid */
            id: string;
            items: string[];
            /**
             * Format: date-time
             * @description 输入必须带Z或offset、最多3位小数；输出UTC且固定3位毫秒
             */
            occurredAt: string;
            optional?: string | null;
            /** Format: int32 */
            progress: number;
            ratio: number;
            requiredNullable: string | null;
            /** @enum {string} */
            state: "OPEN" | "CLOSED";
            /** @description 非负Long，最大9223372036854775807 */
            version: string;
        };
        ScalarOutput: {
            /** @example 12.30 */
            amount: string;
            /** @enum {string} */
            currency: "CNY";
            /** Format: date */
            date: string;
            enabled: boolean;
            /** Format: uuid */
            id: string;
            items: string[];
            /**
             * Format: date-time
             * @description 输入必须带Z或offset、最多3位小数；输出UTC且固定3位毫秒
             */
            occurredAt: string;
            optional?: string;
            /** Format: int32 */
            progress: number;
            ratio: number;
            requiredNullable: string | null;
            /** @enum {string} */
            state: "OPEN" | "CLOSED";
            /** @description 非负Long，最大9223372036854775807 */
            version: string;
        };
        SuccessFieldErrorDetail: {
            data: components["schemas"]["FieldErrorDetail"] | null;
            /** @constant */
            success: true;
            traceId: string;
        };
        SuccessPageResponseScalarOutput: {
            data: components["schemas"]["PageResponseScalarOutput"] | null;
            /** @constant */
            success: true;
            traceId: string;
        };
        SuccessScalarOutput: {
            data: components["schemas"]["ScalarOutput"] | null;
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
    };
    responses: never;
    parameters: never;
    requestBodies: never;
    headers: never;
    pathItems: never;
}
export type $defs = Record<string, never>;
export interface operations {
    testSafeError: {
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
            /** @description 请求格式错误 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 输入语义校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 安全内部错误 */
            500: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
        };
    };
    testNullSuccess: {
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
            /** @description 请求格式错误 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 输入语义校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 安全内部错误 */
            500: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
        };
    };
    testScalarPage: {
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
                    "application/json": components["schemas"]["SuccessPageResponseScalarOutput"];
                };
            };
            /** @description 请求格式错误 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 输入语义校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 安全内部错误 */
            500: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
        };
    };
    testEchoScalars: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ScalarInput"];
            };
        };
        responses: {
            /** @description 成功 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SuccessScalarOutput"];
                };
            };
            /** @description 请求格式错误 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 输入语义校验失败 */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Failure"];
                };
            };
            /** @description 安全内部错误 */
            500: {
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
