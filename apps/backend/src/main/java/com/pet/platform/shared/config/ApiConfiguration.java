package com.pet.platform.shared.config;

import com.pet.platform.shared.observability.TraceFilter;
import com.pet.platform.shared.serialization.ScalarCodecs;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import tools.jackson.databind.module.SimpleModule;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.type.LogicalType;

@Configuration(proxyBeanMethods = false)
public class ApiConfiguration {
    @Bean
    FilterRegistrationBean<TraceFilter> traceFilterRegistration() {
        var registration = new FilterRegistrationBean<>(new TraceFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ERROR);
        return registration;
    }

    @Bean
    JsonMapperBuilderCustomizer scalarJson() {
        return builder -> builder.addModule(new SimpleModule("协议时间和ID")
                .addSerializer(Instant.class, new ScalarCodecs.InstantWriter())
                .addDeserializer(Instant.class, new ScalarCodecs.InstantReader())
                .addSerializer(LocalDate.class, new ScalarCodecs.DateWriter())
                .addDeserializer(LocalDate.class, new ScalarCodecs.DateReader())
                .addSerializer(UUID.class, new ScalarCodecs.UuidWriter())
                .addDeserializer(UUID.class, new ScalarCodecs.UuidReader()));
    }

    @Bean
    JsonMapperBuilderCustomizer strictRequestJson() {
        return builder -> builder.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                        DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .enable(EnumFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .withCoercionConfig(LogicalType.Textual, config -> config
                        .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail));
    }
}
