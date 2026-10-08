package com.pet.platform.shared.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** 最小技术主键和时间审计；不提供租户、软删除或乐观锁语义。 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "timestamp(3) with time zone")
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false, columnDefinition = "timestamp(3) with time zone")
    private Instant updatedAt;

    @PrePersist
    protected void assignTechnicalId() {
        if (id == null) { id = UUID.randomUUID(); }
    }

    public UUID getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
