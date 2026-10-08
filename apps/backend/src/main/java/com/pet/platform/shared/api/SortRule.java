package com.pet.platform.shared.api;

import java.util.Objects;

/** property 来自接口固定映射；持久化适配器必须落实 NULLS LAST。 */
public record SortRule(String property, Direction direction) {
    public SortRule {
        if (property == null || !property.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("内部排序属性必须是固定单层属性");
        }
        Objects.requireNonNull(direction);
    }

    public enum Direction { ASC, DESC }
}
