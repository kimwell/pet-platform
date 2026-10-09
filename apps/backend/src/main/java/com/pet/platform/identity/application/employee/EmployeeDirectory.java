package com.pet.platform.identity.application.employee;

import com.pet.platform.shared.api.*;
import com.pet.platform.shared.exception.BusinessException;
import com.pet.platform.shared.persistence.JpaPageAdapter;
import com.pet.platform.shared.tenancy.*;
import jakarta.persistence.PersistenceException;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;

/** 范围包住完整只读事务，提交前范围校验完成后才关闭；列表/count共享快照。 */
@Service
public class EmployeeDirectory {
    public static final String LIST="identity:user:list", DETAIL="identity:user:detail";
    private final EmployeeQueryStore store;
    private final StoreScopeGuard stores;
    private final TransactionTemplate transaction;
    public EmployeeDirectory(EmployeeQueryStore store, StoreScopeGuard stores, PlatformTransactionManager manager) {
        this.store=store; this.stores=stores;
        transaction=new TransactionTemplate(manager); transaction.setReadOnly(true);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    }
    public PageResponse<EmployeeView> list(EmployeeQuery query) {
        return read(LIST, () -> {
            if (query.storeId()!=null) stores.requireStore(query.storeId());
            return JpaPageAdapter.fromPage(store.list(query, JpaPageAdapter.toPageable(query.page(),EmployeeQuery.SORTS,query.sorting())));
        });
    }
    public EmployeeView detail(UUID id) { return read(DETAIL, () -> store.detail(id)); }
    private <T> T read(String permission, Supplier<T> action) {
        try (var scope=TenantExecutionScope.forPermission(permission)) {
            TenantScopeGuard.requirePermission(permission);
            return transaction.execute(status -> action.get());
        } catch (DataAccessException | PersistenceException | TransactionException failure) {
            throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE);
        }
    }
}
