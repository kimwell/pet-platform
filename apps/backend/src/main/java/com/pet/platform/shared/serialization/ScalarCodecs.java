package com.pet.platform.shared.serialization;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.UUID;
import tools.jackson.core.*;
import tools.jackson.databind.*;

/** Jackson 3 HTTP 编解码；异常只包含固定提示，禁止带原始输入。 */
public final class ScalarCodecs {
    private ScalarCodecs() { }
    public static final String INSTANT_PATTERN = "^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3}Z$";
    private static final DateTimeFormatter MILLIS = new DateTimeFormatterBuilder().appendInstant(3).toFormatter();
    private static String text(JsonParser parser, DeserializationContext context, Class<?> type) {
        if (!parser.hasToken(JsonToken.VALUE_STRING)) {
            return context.reportInputMismatch(type, "字段必须是协议字符串");
        }
        return parser.getString();
    }
    public static final class AmountInputException extends RuntimeException {
        public AmountInputException() { super("金额超出允许精度或范围"); }
    }
    public static final class AmountWriter extends ValueSerializer<BigDecimal> {
        @Override public void serialize(BigDecimal value, JsonGenerator generator, SerializationContext context) {
            // 输出无效金额属于编程错误，由既定500兜底，不暗中舍入。
            if (value.signum() < 0 || value.precision() - value.scale() > 17) throw new IllegalArgumentException("金额超出范围");
            generator.writeString(value.setScale(2, RoundingMode.UNNECESSARY).toPlainString());
        }
    }
    public static final class AmountReader extends ValueDeserializer<BigDecimal> {
        @Override public BigDecimal deserialize(JsonParser parser, DeserializationContext context) {
            String value = text(parser, context, BigDecimal.class);
            if (!value.matches("(0|[1-9][0-9]*)(\\.[0-9]+)?")) {
                return context.reportInputMismatch(BigDecimal.class, "金额字符串格式错误");
            }
            BigDecimal amount = new BigDecimal(value);
            if (amount.scale() > 2 || amount.precision() - amount.scale() > 17) throw new AmountInputException();
            return amount.setScale(2, RoundingMode.UNNECESSARY);
        }
    }
    public static final class CounterWriter extends ValueSerializer<Long> {
        @Override public void serialize(Long value, JsonGenerator generator, SerializationContext context) {
            if (value < 0) throw new IllegalArgumentException("计数不能为负数");
            generator.writeString(value.toString());
        }
    }
    public static final class CounterReader extends ValueDeserializer<Long> {
        @Override public Long deserialize(JsonParser parser, DeserializationContext context) {
            String value = text(parser, context, Long.class);
            if (!value.matches("0|[1-9][0-9]*")) return context.reportInputMismatch(Long.class, "计数字符串格式错误");
            try { return Long.valueOf(value); }
            catch (NumberFormatException exception) { return context.reportInputMismatch(Long.class, "计数超出范围"); }
        }
    }
    public static final class InstantWriter extends ValueSerializer<Instant> {
        @Override public void serialize(Instant value, JsonGenerator generator, SerializationContext context) {
            if (value.getNano() % 1_000_000 != 0) throw new IllegalArgumentException("时间点必须为毫秒精度");
            String output = MILLIS.format(value);
            if (!output.matches(INSTANT_PATTERN)) throw new IllegalArgumentException("HTTP时间点超出四位年份范围");
            generator.writeString(output);
        }
    }
    public static final class InstantReader extends ValueDeserializer<Instant> {
        @Override public Instant deserialize(JsonParser parser, DeserializationContext context) {
            String value = text(parser, context, Instant.class);
            if (!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(\\.[0-9]{1,3})?(Z|[+-][0-9]{2}:[0-9]{2})")) {
                return context.reportInputMismatch(Instant.class, "时间点需要明确时区和至多毫秒精度");
            }
            try { return OffsetDateTime.parse(value).toInstant(); }
            catch (DateTimeException exception) { return context.reportInputMismatch(Instant.class, "时间点格式错误"); }
        }
    }
    public static final class DateWriter extends ValueSerializer<LocalDate> {
        @Override public void serialize(LocalDate value, JsonGenerator generator, SerializationContext context) {
            if (value.getYear() < 0 || value.getYear() > 9999) throw new IllegalArgumentException("HTTP日期超出四位年份范围");
            generator.writeString(value.toString());
        }
    }
    public static final class UuidWriter extends ValueSerializer<UUID> {
        @Override public void serialize(UUID value, JsonGenerator generator, SerializationContext context) {
            if (value.version() != 4 || value.variant() != 2) throw new IllegalArgumentException("HTTP ID必须为UUID v4");
            generator.writeString(value.toString());
        }
    }
    public static final class DateReader extends ValueDeserializer<LocalDate> {
        @Override public LocalDate deserialize(JsonParser parser, DeserializationContext context) {
            String value = text(parser, context, LocalDate.class);
            if (!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) return context.reportInputMismatch(LocalDate.class, "日期格式错误");
            try { return LocalDate.parse(value); }
            catch (DateTimeException exception) { return context.reportInputMismatch(LocalDate.class, "日期无效"); }
        }
    }
    public static final class UuidReader extends ValueDeserializer<UUID> {
        @Override public UUID deserialize(JsonParser parser, DeserializationContext context) {
            String value = text(parser, context, UUID.class);
            if (!value.matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")) {
                return context.reportInputMismatch(UUID.class, "ID必须是规范UUID v4字符串");
            }
            return UUID.fromString(value);
        }
    }
}
