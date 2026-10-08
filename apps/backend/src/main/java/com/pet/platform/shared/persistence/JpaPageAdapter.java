package com.pet.platform.shared.persistence;

import com.pet.platform.shared.api.ErrorCode;
import com.pet.platform.shared.api.PageQuery;
import com.pet.platform.shared.api.PageResponse;
import com.pet.platform.shared.api.SortRule;
import com.pet.platform.shared.api.SortWhitelist;
import com.pet.platform.shared.exception.BusinessException;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** 沿用协议模型；只接受接口白名单，不提供任意属性或 SQL 的排序入口。 */
public final class JpaPageAdapter {
    private JpaPageAdapter() { }

    public static Pageable toPageable(PageQuery query, SortWhitelist whitelist, Map<String, String[]> parameters) {
        // JPA setFirstResult 只能接收 int；超范围明确拒绝，不截断 long。
        if (query.offset() > Integer.MAX_VALUE) { throw new BusinessException(ErrorCode.RESULT_TOO_LARGE); }
        return PageRequest.of(query.page() - 1, query.pageSize(), toSort(whitelist, parameters));
    }

    public static Sort toSort(SortWhitelist whitelist, Map<String, String[]> parameters) {
        // 冻结 JPA 版本通过 Jakarta Persistence 3.2 支持显式空值次序。
        return Sort.by(whitelist.resolve(parameters).stream().map(rule -> new Sort.Order(
                rule.direction() == SortRule.Direction.ASC ? Sort.Direction.ASC : Sort.Direction.DESC,
                rule.property()).nullsLast()).toList());
    }

    /** 应用服务先 Page.map 转成 DTO，再映射信封数据；不把 Page 或 Entity 返回 Controller。 */
    public static <T> PageResponse<T> fromPage(Page<T> page) {
        return PageResponse.of(page.getContent(), new PageQuery(Math.addExact(page.getNumber(), 1), page.getSize()),
                page.getTotalElements());
    }
}
