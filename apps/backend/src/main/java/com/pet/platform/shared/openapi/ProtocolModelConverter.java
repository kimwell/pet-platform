package com.pet.platform.shared.openapi;

import com.pet.platform.shared.api.ApiResponse;
import com.pet.platform.shared.serialization.CnyAmount;
import com.pet.platform.shared.serialization.DecimalCounter;
import io.swagger.v3.core.converter.*;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;
import java.time.Instant;
import java.util.Iterator;

/** 从实际类型解析封闭成功分支及局部标量；禁止泛型 data 退化为 object。 */
public final class ProtocolModelConverter implements ModelConverter {
    @Override
    public Schema resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        if (type.getCtxAnnotations() != null) {
            for (var annotation : type.getCtxAnnotations()) {
                if (annotation instanceof CnyAmount amount) return new Schema<>().types(java.util.Set.of("string"))
                        .pattern(amount.input() ? "^(0|[1-9][0-9]{0,16})(\\.[0-9]{1,2})?$" : "^(0|[1-9][0-9]{0,16})\\.[0-9]{2}$").example("12.30");
                if (annotation instanceof DecimalCounter) return new Schema<>().types(java.util.Set.of("string"))
                        .pattern("^(0|[1-9][0-9]*)$").maxLength(19).description("非负Long，最大9223372036854775807");
            }
        }
        var javaType = Json.mapper().constructType(type.getType());
        if (javaType != null && javaType.getRawClass() == java.util.UUID.class) {
            return new Schema<>().types(java.util.Set.of("string")).format("uuid")
                    .pattern("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");
        }
        if (javaType != null && javaType.getRawClass() == Instant.class) {
            var schema = new Schema<>().types(java.util.Set.of("string")).format("date-time")
                    .description("输入必须带Z或offset、最多3位小数；输出UTC且固定3位毫秒");
            if (type.getCtxAnnotations() != null) for (var annotation : type.getCtxAnnotations()) {
                if (annotation instanceof io.swagger.v3.oas.annotations.media.Schema declared && !declared.pattern().isEmpty()) schema.pattern(declared.pattern());
            }
            return schema;
        }
        if (javaType != null && javaType.getRawClass() == ApiResponse.class) {
            if (javaType.containedTypeCount() != 1 || javaType.containedType(0).getRawClass() == Object.class) {
                throw new IllegalArgumentException("OpenAPI响应必须声明具体data类型");
            }
            return context.resolve(new AnnotatedType(Json.mapper().getTypeFactory()
                    .constructParametricType(ApiResponse.Success.class, javaType.containedType(0))).resolveAsRef(true));
        }
        return chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
    }
}
