package com.pet.platform.shared.serialization;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import java.lang.annotation.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

/** 仅用于 CNY 金额 BigDecimal 字段，不改变其他数值或持久化类型。 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = ScalarCodecs.AmountWriter.class)
@JsonDeserialize(using = ScalarCodecs.AmountReader.class)
public @interface CnyAmount {
    /** 请求允许省略尾零，响应固定两位；编解码行为保持一致。 */
    boolean input() default false;
}
