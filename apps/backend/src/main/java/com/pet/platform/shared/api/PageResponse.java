package com.pet.platform.shared.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Objects;

/** total 必须来自真实计数；字符串避免 JavaScript 大整数精度丢失。 */
public record PageResponse<T>(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<T> items, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "2147483647") int page, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "100") int pageSize, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, pattern = "^(0|[1-9][0-9]*)$") String total) {
    public PageResponse {
        items = List.copyOf(Objects.requireNonNull(items, "分页集合不能为空"));
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("分页响应的页码或大小不符合协议");
        }
        if (total == null || !total.matches("0|[1-9][0-9]*")) {
            throw new IllegalArgumentException("分页总数必须是非负十进制字符串");
        }
    }

    public static <T> PageResponse<T> of(List<T> items, PageQuery query, long total) {
        return new PageResponse<>(items, query.page(), query.pageSize(), Long.toString(total));
    }
}
