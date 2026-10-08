package com.pet.platform.shared.config;

import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 只校验工程环境与公开来源，不承载认证、租户或业务能力。 */
@ConfigurationProperties(prefix = "pet")
public record EnvironmentSettings(String environment, URI publicOrigin) {
    public EnvironmentSettings {
        if (!Set.of("local", "test", "prod").contains(environment == null ? "" : environment)) {
            throw new IllegalArgumentException("pet.environment 必须显式设置为 local、test 或 prod");
        }
        if (publicOrigin == null || publicOrigin.getHost() == null
                || !Set.of("http", "https").contains(publicOrigin.getScheme())
                || publicOrigin.getUserInfo() != null || publicOrigin.getQuery() != null
                || publicOrigin.getFragment() != null
                || publicOrigin.getPort() == 0 || publicOrigin.getPort() > 65535
                || !(publicOrigin.getPath().isEmpty() || publicOrigin.getPath().equals("/"))) {
            throw new IllegalArgumentException("pet.public-origin 必须是完整的公开 HTTP/HTTPS 来源，不包含凭据、路径或查询，端口须为 1～65535");
        }
        if (environment.equals("prod") && !publicOrigin.getScheme().equals("https")) {
            throw new IllegalArgumentException("生产 pet.public-origin 必须使用 HTTPS");
        }
    }

    /** 防止 profile 顺序或进程变量让生产配置被本地环境覆盖。 */
    public void verifyProfiles(String... profiles) {
        var runtimeProfiles = Arrays.stream(profiles)
                .filter(profile -> Set.of("local", "test", "prod").contains(profile))
                .distinct().toList();
        if (runtimeProfiles.size() > 1) {
            throw new IllegalArgumentException("local、test、prod 运行 profile 只能启用一个，不能混用本地与生产配置");
        }
        if (runtimeProfiles.size() == 1 && !runtimeProfiles.getFirst().equals(environment)) {
            throw new IllegalArgumentException("运行 profile 与 pet.environment 不一致，不能把本地配置用于生产");
        }
    }
}
