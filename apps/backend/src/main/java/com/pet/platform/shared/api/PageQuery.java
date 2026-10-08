package com.pet.platform.shared.api;

import io.swagger.v3.oas.annotations.media.Schema;

import com.pet.platform.shared.exception.BusinessException;
import java.util.Map;

/** 分页参数直接读取原始标量，避免绑定器把空值当默认或忽略重复值。 */
public record PageQuery(@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "2147483647") int page, @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "100") int pageSize) {
    public PageQuery {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new BusinessException(ErrorCode.PAGINATION_INVALID);
        }
    }

    public static PageQuery from(Map<String, String[]> parameters) {
        return new PageQuery(number(parameters, "page", 1, Integer.MAX_VALUE),
                number(parameters, "pageSize", 20, 100));
    }

    public long offset() {
        return Math.multiplyExact((long) page - 1, (long) pageSize);
    }

    static String scalar(Map<String, String[]> parameters, String name, ErrorCode errorCode) {
        if (!parameters.containsKey(name)) { return null; }
        String[] values = parameters.get(name);
        if (values == null || values.length != 1 || values[0] == null) {
            throw new BusinessException(errorCode);
        }
        return values[0];
    }

    private static int number(Map<String, String[]> parameters, String name, int defaultValue, int max) {
        String value = scalar(parameters, name, ErrorCode.PAGINATION_INVALID);
        if (value == null) { return defaultValue; }
        if (!value.matches("[0-9]+")) { throw new BusinessException(ErrorCode.PAGINATION_INVALID); }
        value = value.replaceFirst("^0+", "");
        if (value.isEmpty() || value.length() > 10) { throw new BusinessException(ErrorCode.PAGINATION_INVALID); }
        long parsed = Long.parseLong(value);
        if (parsed < 1 || parsed > max) { throw new BusinessException(ErrorCode.PAGINATION_INVALID); }
        return (int) parsed;
    }
}
