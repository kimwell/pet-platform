package com.pet.platform.shared.security;

import java.util.Optional;

/** 仅可信服务端认证适配器提供。公共请求返回空；不得采信客户端身份/租户/角色/范围。 */
@FunctionalInterface
public interface CurrentPrincipalProvider {
    Optional<CurrentPrincipal> currentPrincipal();
}
