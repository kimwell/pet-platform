package com.pet.platform.identity.application.employee;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** 本模块固定读取用例端口，不暴露Entity或任意查询表达式。 */
public interface EmployeeQueryStore {
    Page<EmployeeView> list(EmployeeQuery query, Pageable page);
    EmployeeView detail(UUID id);
}
