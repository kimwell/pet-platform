package com.pet.platform.identity.application.bootstrap;

import java.util.UUID;

/** 只有必要的非秘密标识；不会返回凭据或隐含平台身份。 */
public record BootstrapResult(UUID tenantId,UUID employeeId,UUID storeId) { }
