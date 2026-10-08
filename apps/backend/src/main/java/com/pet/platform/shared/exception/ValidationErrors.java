package com.pet.platform.shared.exception;

import com.pet.platform.shared.api.FieldErrorDetail;
import jakarta.validation.ConstraintViolation;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.MethodParameter;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

final class ValidationErrors {
    private ValidationErrors() { }

    static List<FieldErrorDetail> from(Errors errors) {
        return errors.getFieldErrors().stream().filter(error -> FieldErrorDetail.isValidPath(error.getField()))
                .map(error -> detail(error.getField(), error.getCode(), literalTemplate(error))).toList();
    }

    static List<FieldErrorDetail> from(HandlerMethodValidationException exception) {
        var details = new ArrayList<FieldErrorDetail>();
        for (var result : exception.getParameterValidationResults()) {
            if (result instanceof ParameterErrors errors) {
                details.addAll(from(errors));
                continue;
            }
            String field = parameterName(result.getMethodParameter());
            if (result.getContainerIndex() != null) { field += "[" + result.getContainerIndex() + "]"; }
            if (!FieldErrorDetail.isValidPath(field)) { continue; }
            for (var error : result.getResolvableErrors()) {
                var codes = error.getCodes();
                String constraint = codes == null || codes.length == 0 ? "" : codes[codes.length - 1];
                // 方法校验只采用固定中文提示，不读取参数值或插值消息。
                details.add(detail(field, constraint, null));
            }
        }
        return details;
    }

    private static String parameterName(MethodParameter parameter) {
        var annotation = parameter.getParameterAnnotation(RequestParam.class);
        if (annotation != null) {
            if (!annotation.name().isEmpty()) { return annotation.name(); }
            if (!annotation.value().isEmpty()) { return annotation.value(); }
        }
        return parameter.getParameterName();
    }

    private static String literalTemplate(FieldError error) {
        if (!error.contains(ConstraintViolation.class)) { return null; }
        String template = error.unwrap(ConstraintViolation.class).getMessageTemplate();
        // 只允许源码中的中文静态提示；拒绝模板插值、换行和过长文本。
        return template.length() <= 120 && template.matches(".*[\\p{IsHan}].*")
                && !template.matches("(?s).*[{}\\r\\n].*") ? template : null;
    }

    private static FieldErrorDetail detail(String field, String constraint, String literal) {
        String code;
        String message;
        switch (constraint == null ? "" : constraint) {
            case "NotNull", "NotBlank", "NotEmpty" -> { code = "REQUIRED"; message = "请填写必填字段"; }
            case "Min", "Max", "DecimalMin", "DecimalMax", "Positive", "PositiveOrZero",
                 "Negative", "NegativeOrZero", "Size", "Digits" -> { code = "OUT_OF_RANGE"; message = "字段值超出允许范围"; }
            case "Pattern", "Email", "typeMismatch" -> { code = "INVALID_FORMAT"; message = "字段格式不正确"; }
            default -> { code = "INVALID_VALUE"; message = "字段值不符合要求"; }
        }
        return new FieldErrorDetail(field, code, literal == null ? message : literal);
    }
}
