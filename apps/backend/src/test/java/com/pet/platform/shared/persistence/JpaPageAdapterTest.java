package com.pet.platform.shared.persistence;

import com.pet.platform.shared.api.*;
import com.pet.platform.shared.exception.BusinessException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

class JpaPageAdapterTest {
    private static final SortWhitelist SORTS = new SortWhitelist(Map.of("name", "displayName", "id", "id"),
            "name", SortRule.Direction.DESC, "id");

    @Test void convertsOneBasedPageAndDefaultSize() {
        var query = PageQuery.from(Map.of());
        var page = JpaPageAdapter.toPageable(query, SORTS, Map.of());
        assertEquals(0, page.getPageNumber());
        assertEquals(20, page.getPageSize());
        assertEquals(0, page.getOffset());
        assertEquals(60, JpaPageAdapter.toPageable(new PageQuery(4, 20), SORTS, Map.of()).getOffset());
    }

    @Test void rejectsOffsetBeyondJpaIntWithoutOverflow() {
        assertEquals(2147483646L, JpaPageAdapter.toPageable(new PageQuery(Integer.MAX_VALUE, 1), SORTS, Map.of()).getOffset());
        var failure = assertThrows(BusinessException.class,
                () -> JpaPageAdapter.toPageable(new PageQuery(Integer.MAX_VALUE, 100), SORTS, Map.of()));
        assertEquals(ErrorCode.RESULT_TOO_LARGE, failure.error().code());
    }

    @Test void mapsOnlyWhitelistedPropertiesAndDoesNotDuplicateUniqueSort() {
        var sort = JpaPageAdapter.toSort(SORTS, Map.of("sortBy", new String[]{"id"}, "sortOrder", new String[]{"asc"}));
        assertEquals(1, sort.stream().count());
        assertTrue(sort.getOrderFor("id").isAscending());
        assertEquals(org.springframework.data.domain.Sort.NullHandling.NULLS_LAST, sort.getOrderFor("id").getNullHandling());
        var name = JpaPageAdapter.toSort(SORTS, Map.of("sortBy", new String[]{"name"}));
        assertEquals(List.of("displayName", "id"), name.stream().map(order -> order.getProperty()).toList());
        assertThrows(BusinessException.class, () -> JpaPageAdapter.toSort(SORTS, Map.of("sortBy", new String[]{"owner.name"})));
        assertThrows(BusinessException.class, () -> JpaPageAdapter.toSort(SORTS, Map.of("sortBy", new String[]{"displayName"})));
    }

    @Test void preservesLargeTotalAsStringAndExactResponseShape() {
        long total = 9_007_199_254_740_993L;
        var page = new PageImpl<>(List.of("技术DTO"), PageRequest.of(0, 20), total);
        var response = JpaPageAdapter.fromPage(page);
        assertEquals("9007199254740993", response.total());
        var json = JsonMapper.builder().build().readTree(JsonMapper.builder().build().writeValueAsString(response));
        assertEquals(4, json.size());
        assertTrue(json.path("total").isString());
        assertEquals("9007199254740993", json.path("total").asText());
        assertFalse(json.has("pageable"));
    }

    @Test void mapsEmptyOutOfRangePageWithoutChangingRequestedNumber() {
        var response = JpaPageAdapter.fromPage(new PageImpl<String>(List.of(), PageRequest.of(7, 20), 3));
        assertEquals(8, response.page());
        assertEquals("3", response.total());
        assertTrue(response.items().isEmpty());
    }
}
