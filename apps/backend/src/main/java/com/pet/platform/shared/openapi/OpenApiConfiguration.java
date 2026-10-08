package com.pet.platform.shared.openapi;

import com.pet.platform.shared.api.*;
import io.swagger.v3.core.converter.*;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import java.util.*;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {
    @Bean
    static BeanFactoryPostProcessor documentPolicy(Environment environment) {
        return factory -> {
            boolean docs = environment.getProperty("springdoc.api-docs.enabled", Boolean.class, false);
            boolean ui = environment.getProperty("springdoc.swagger-ui.enabled", Boolean.class, false);
            if ("prod".equals(environment.getProperty("pet.environment")) && (docs || ui)) {
                throw new IllegalStateException("生产环境必须关闭OpenAPI文档和UI");
            }
            if ((docs || ui) && !Set.of("127.0.0.1", "::1").contains(environment.getProperty("server.address", ""))) {
                throw new IllegalStateException("启用OpenAPI文档或UI时必须绑定回环地址");
            }
        };
    }

    @Bean ProtocolModelConverter protocolModelConverter() { return new ProtocolModelConverter(); }

    @Bean OpenAPI applicationOpenApi(Environment environment) {
        var api = new OpenAPI().openapi("3.1.0").info(new Info()
                .title("企业应用公共接口契约").version(environment.getRequiredProperty("pet.api.version")));
        // 显式模型注册，不通过生产演示Controller制造路径；所有字段来自实际后端类型。
        var converters = new ModelConverters(true);
        converters.addConverter(new ProtocolModelConverter());
        var components = new Components();
        for (var type : List.of(ApiError.class, FieldErrorDetail.class, ApiResponse.Failure.class, PageQuery.class,
                Json.mapper().getTypeFactory().constructParametricType(PageResponse.class, FieldErrorDetail.class),
                Json.mapper().getTypeFactory().constructParametricType(ApiResponse.Success.class, FieldErrorDetail.class),
                Json.mapper().getTypeFactory().constructParametricType(ApiResponse.Success.class, Void.class))) {
            converters.resolveAsResolvedSchema(new AnnotatedType(type).resolveAsRef(true))
                    .referencedSchemas.forEach(components::addSchemas);
        }
        api.components(components);
        return api;
    }

    @Bean GlobalOpenApiCustomizer exactProtocolSchemas() { return OpenApiConfiguration::completeSchemas; }

    /** 补充Jackson条件输出语义；不手写竞争DTO，不修改字段名称或泛型。 */
    public static void completeSchemas(OpenAPI api) {
        if (api.getComponents() == null || api.getComponents().getSchemas() == null) return;
        api.getComponents().getSchemas().forEach((name, schema) -> {
            var properties = schema.getProperties();
            if (properties == null) return;
            schema.setAdditionalProperties(false);
            if (name.startsWith("Success")) {
                schema.setRequired(List.of("success", "data", "traceId"));
                properties.put("success", new Schema<>().types(Set.of("boolean"))._const(true));
                var data = (Schema<?>) properties.get("data");
                if (data == null || name.equals("SuccessVoid")) {
                    properties.put("data", new Schema<>().types(Set.of("null")));
                } else if (data.getAnyOf() == null) {
                    data.setNullable(null);
                    if (data.get$ref() != null) { data.setType(null); data.setTypes(null); }
                    properties.put("data", new Schema<>().anyOf(List.of(data, new Schema<>().types(Set.of("null")))));
                }
            }
            if (name.equals("PlatformCurrentIdentity")) {
                properties.put("tenantId",new Schema<>().types(Set.of("null")));
                properties.put("dataScope",new Schema<>().types(Set.of("null")));
                ((Schema<?>)properties.get("authorizedStoreIds")).setMaxItems(0);
            }
            if (name.equals("CustomerCurrentIdentity")) ((Schema<?>)properties.get("authorizedStoreIds")).setMaxItems(0);
            if (name.equals("Failure")) {
                schema.setRequired(List.of("success", "error", "traceId"));
                properties.put("success", new Schema<>().types(Set.of("boolean"))._const(false));
            }
            if (name.equals("ApiError")) schema.setRequired(List.of("code", "message"));
        });
    }
}
