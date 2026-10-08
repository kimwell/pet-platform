package com.pet.testing.contracts;

import tools.jackson.databind.JsonNode;
import static org.junit.jupiter.api.Assertions.*;

/** 对本轮实际输出使用的JSON Schema关键字做逐字段一致性断言，不冒充完整规范验证器。 */
final class SchemaChecks {
    private SchemaChecks() { }
    static void matches(JsonNode document, JsonNode schema, JsonNode value) {
        if (schema.has("$ref")) {
            matches(document, document.at(schema.path("$ref").asText().substring(1)), value);
            return;
        }
        if (schema.has("anyOf")) {
            boolean matched = false;
            for (var option : schema.path("anyOf")) {
                try { matches(document, option, value); matched = true; break; } catch (AssertionError failure) { /* 检查另一个分支。 */ }
            }
            assertTrue(matched, "输出不符合任何可空/泛型分支：" + value);
            return;
        }
        var type = schema.path("type");
        if (type.isArray()) {
            assertTrue(type.valueStream().anyMatch(t -> accepts(t.asText(), value)), "类型联合不符");
        } else if (!type.isMissingNode()) assertTrue(accepts(type.asText(), value), "类型不符：" + type + " / " + value);
        else fail("本轮输出schema丢失类型：" + schema);
        if (value.isNull()) return;
        if (schema.has("const")) assertEquals(schema.path("const"), value);
        if (schema.has("enum")) assertTrue(schema.path("enum").valueStream().anyMatch(value::equals));
        if (value.isString() && schema.has("pattern")) assertTrue(value.asText().matches(schema.path("pattern").asText()));
        if (value.isNumber()) {
            if (schema.has("minimum")) assertTrue(value.decimalValue().compareTo(schema.path("minimum").decimalValue()) >= 0);
            if (schema.has("maximum")) assertTrue(value.decimalValue().compareTo(schema.path("maximum").decimalValue()) <= 0);
        }
        if (value.isObject()) {
            for (var required : schema.path("required")) assertTrue(value.has(required.asText()), "缺失必填字段" + required);
            value.properties().forEach(entry -> {
                var field = schema.path("properties").path(entry.getKey());
                assertFalse(field.isMissingNode(), "schema遗漏输出字段：" + entry.getKey());
                matches(document, field, entry.getValue());
            });
        }
        if (value.isArray()) {
            if (schema.has("minItems")) assertTrue(value.size() >= schema.path("minItems").asInt());
            for (var item : value) matches(document, schema.path("items"), item);
        }
    }
    private static boolean accepts(String type, JsonNode value) {
        return switch (type) {
            case "null" -> value.isNull();
            case "string" -> value.isString();
            case "boolean" -> value.isBoolean();
            case "integer" -> value.isIntegralNumber();
            case "number" -> value.isNumber();
            case "array" -> value.isArray();
            case "object" -> value.isObject();
            default -> false;
        };
    }
}
