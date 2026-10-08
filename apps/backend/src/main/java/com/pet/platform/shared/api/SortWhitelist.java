package com.pet.platform.shared.api;

import com.pet.platform.shared.exception.BusinessException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 每个接口显式声明公开字段、默认值与唯一字段；不接收客户端内部属性。 */
public final class SortWhitelist {
    private final Map<String, String> fields;
    private final String defaultField;
    private final SortRule.Direction defaultDirection;
    private final String uniqueProperty;

    public SortWhitelist(Map<String, String> fields, String defaultField,
                         SortRule.Direction defaultDirection, String uniqueProperty) {
        this.fields = Map.copyOf(fields);
        this.defaultField = defaultField;
        this.defaultDirection = Objects.requireNonNull(defaultDirection);
        this.uniqueProperty = uniqueProperty;
        if (!this.fields.containsKey(defaultField) || !this.fields.containsValue(uniqueProperty)) {
            throw new IllegalArgumentException("默认字段和唯一字段必须在排序白名单中");
        }
        this.fields.forEach((publicField, internalProperty) -> {
            if (!publicField.matches("[A-Za-z_][A-Za-z0-9_]*")) {
                throw new IllegalArgumentException("公开排序字段必须是单个属性名");
            }
            new SortRule(internalProperty, defaultDirection);
        });
    }

    public List<SortRule> resolve(Map<String, String[]> parameters) {
        String field = PageQuery.scalar(parameters, "sortBy", ErrorCode.SORT_INVALID);
        String order = PageQuery.scalar(parameters, "sortOrder", ErrorCode.SORT_INVALID);
        if (field == null && order != null) { throw new BusinessException(ErrorCode.SORT_INVALID); }
        field = field == null ? defaultField : field;
        String property = fields.get(field);
        if (property == null) { throw new BusinessException(ErrorCode.SORT_INVALID); }
        var direction = order == null ? defaultDirection : switch (order) {
            case "asc" -> SortRule.Direction.ASC;
            case "desc" -> SortRule.Direction.DESC;
            default -> throw new BusinessException(ErrorCode.SORT_INVALID);
        };
        var primary = new SortRule(property, direction);
        return property.equals(uniqueProperty) ? List.of(primary)
                : List.of(primary, new SortRule(uniqueProperty, direction));
    }
}
