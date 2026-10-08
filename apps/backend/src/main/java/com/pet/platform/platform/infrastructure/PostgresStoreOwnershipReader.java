package com.pet.platform.platform.infrastructure;

import com.pet.platform.platform.domain.Store;
import com.pet.platform.shared.persistence.*;
import com.pet.platform.shared.tenancy.*;
import com.pet.platform.shared.api.ErrorCode;
import com.pet.platform.shared.exception.BusinessException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.time.Clock;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

/** 门店事实先于范围Guard读取；沿用当前同连接RLS，不递归调用Guard。 */
@Component
public class PostgresStoreOwnershipReader extends ScopedPersistence<Store> implements StoreOwnershipReader {
    private final TransactionTemplate transaction;
    public PostgresStoreOwnershipReader(EntityManager em,Clock clock,PlatformTransactionManager manager) {
        super(em,"platform:store:list",Store.class,ResourceAccessPolicy.tenantOnly(),Set.of(),clock,null);
        transaction=new TransactionTemplate(manager);transaction.setReadOnly(true);
    }
    @Override
    public Optional<UUID> findTenantId(UUID storeId) {
        try { return transaction.execute(status -> findActiveStoreTenant(storeId)); }
        catch (DataAccessException | PersistenceException | TransactionException failure) { throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE); }
    }
}
