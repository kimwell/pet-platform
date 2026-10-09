package com.pet.platform.identity.application.employee;

import com.pet.platform.shared.api.*;
import com.pet.platform.shared.exception.BusinessException;
import java.util.*;

/** 原始查询标量先校验，不将空值、重复值或未知字段静默转换为默认。 */
public record EmployeeQuery(String keyword, String status, UUID storeId, PageQuery page, Map<String,String[]> sorting) {
    public static final SortWhitelist SORTS = new SortWhitelist(Map.of("id","id","loginName","loginName",
            "displayName","displayName","status","status","createdAt","createdAt","updatedAt","updatedAt"),
            "createdAt", SortRule.Direction.DESC, "id");
    public static EmployeeQuery from(Map<String,String[]> parameters) {
        if (!Set.of("keyword","status","storeId","page","pageSize","sortBy","sortOrder").containsAll(parameters.keySet()))
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        String keyword = scalar(parameters,"keyword"), status = scalar(parameters,"status"), store = scalar(parameters,"storeId");
        if (keyword != null && (keyword.isBlank() || keyword.codePointCount(0,keyword.length()) > 100)) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        if (status != null && !Set.of("ACTIVE","DISABLED").contains(status)) throw new BusinessException(ErrorCode.BAD_REQUEST);
        var query = PageQuery.from(parameters);
        SORTS.resolve(parameters);
        var sorting = new HashMap<String,String[]>();
        for (String key : List.of("sortBy","sortOrder")) if (parameters.containsKey(key)) sorting.put(key,parameters.get(key).clone());
        return new EmployeeQuery(keyword,status,store == null ? null : id(store),query,Map.copyOf(sorting));
    }
    public static UUID id(String value) {
        if (value == null || !value.matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}"))
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        return UUID.fromString(value);
    }
    private static String scalar(Map<String,String[]> parameters,String name) {
        if (!parameters.containsKey(name)) return null;
        var values = parameters.get(name);
        if (values == null || values.length != 1 || values[0] == null) throw new BusinessException(ErrorCode.BAD_REQUEST);
        return values[0];
    }
}
