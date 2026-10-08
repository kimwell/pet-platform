package com.pet.testing.persistence;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** 测试独有 Repository；生产没有无范围的公共 Repository/BaseService。 */
public interface ProbeRepository extends JpaRepository<PersistenceProbe, UUID>, JpaSpecificationExecutor<PersistenceProbe> {
    Page<PersistenceProbe> findByTag(String tag, Pageable pageable);
}
