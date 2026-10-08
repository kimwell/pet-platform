package com.pet.platform.shared.serialization;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.annotation.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

/** 有界 Long 计数/version 的保真传输；分页 total 已由 String 保真。 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = ScalarCodecs.CounterWriter.class)
@JsonDeserialize(using = ScalarCodecs.CounterReader.class)
@Schema(type = "string", pattern = "^(0|[1-9][0-9]*)$", maxLength = 19)
public @interface DecimalCounter { }
