package com.pet.testing.persistence;

import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/** 测试显式经过 Spring 代理的应用服务事务，不依赖测试方法自动回滚。 */
public class ProbeApplicationService {
    private final ProbeRepository repository;
    public ProbeApplicationService(ProbeRepository repository) { this.repository = repository; }

    @Transactional
    public UUID create(String code, String name, String tag) {
        return repository.save(new PersistenceProbe(code, name, tag)).getId();
    }

    @Transactional
    public void rename(UUID id, String name) { repository.findById(id).orElseThrow().rename(name); }

    @Transactional(readOnly = true)
    public PersistenceProbe read(UUID id) { return repository.findById(id).orElseThrow(); }

    @Transactional
    public void failAfterWrite(String code) {
        repository.saveAndFlush(new PersistenceProbe(code, "回滚夹具", "rollback"));
        throw new IllegalStateException("测试运行时异常");
    }

    @Transactional
    public void failAfterTwoWrites(String first, String second) {
        repository.saveAndFlush(new PersistenceProbe(first, "第一笔", "rollback"));
        repository.saveAndFlush(new PersistenceProbe(second, "第二笔", "rollback"));
        throw new IllegalStateException("测试多次写入回滚");
    }

    @Transactional
    public void failOnConstraint(String code) {
        repository.saveAndFlush(new PersistenceProbe(code, "第一笔", "constraint"));
        repository.saveAndFlush(new PersistenceProbe(code, "重复技术键", "constraint"));
    }
}
