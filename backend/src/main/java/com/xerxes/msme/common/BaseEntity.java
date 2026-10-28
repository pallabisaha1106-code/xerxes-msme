package com.xerxes.msme.common;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Base for append-only rows (line items, ledger entries): an application-assigned
 * UUID id plus a created_at that is never modified after insert.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    @Id
    private UUID id;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void ensureId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
