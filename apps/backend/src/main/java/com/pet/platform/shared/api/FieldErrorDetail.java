package com.pet.platform.shared.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** 不承载 rejectedValue 或原始输入。路径只允许 DTO 属性与零基索引。 */
public record FieldErrorDetail(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String field, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String message) {
    private static final Pattern PATH = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(?:\\[[0-9]+\\])*(?:\\.[A-Za-z_][A-Za-z0-9_]*(?:\\[[0-9]+\\])*)*");

    public FieldErrorDetail {
        if (field == null || !PATH.matcher(field).matches() || code == null
                || !code.matches("[A-Z][A-Z0-9_]*") || message == null || message.isBlank()) {
            throw new IllegalArgumentException("字段错误必须包含规范路径、稳定代码和中文提示");
        }
    }

    public static boolean isValidPath(String path) {
        return path != null && PATH.matcher(path).matches();
    }

    /** 同字段不同代码保留；同路径同代码去重；顺序与校验器遍历无关。 */
    public static List<FieldErrorDetail> normalize(List<FieldErrorDetail> errors) {
        var sorted = errors.stream().map(Objects::requireNonNull)
                .sorted(Comparator.comparing(FieldErrorDetail::field)
                        .thenComparing(FieldErrorDetail::code).thenComparing(FieldErrorDetail::message)).toList();
        var unique = new java.util.LinkedHashMap<String, FieldErrorDetail>();
        sorted.forEach(error -> unique.putIfAbsent(error.field() + ":" + error.code(), error));
        return List.copyOf(unique.values());
    }
}
