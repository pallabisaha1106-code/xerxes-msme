package com.xerxes.msme.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Base for rows that get updated after creation. The DB also carries a
 * BEFORE UPDATE trigger (set_updated_at) as a safety net, so updated_at is
 * always correct even if a code path bypasses this Hibernate annotation.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class MutableEntity extends BaseEntity {

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
