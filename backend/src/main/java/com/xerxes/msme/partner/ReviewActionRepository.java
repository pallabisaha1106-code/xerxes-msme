package com.xerxes.msme.partner;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewActionRepository extends JpaRepository<ReviewAction, UUID> {

    /** A shop's history, newest first. */
    List<ReviewAction> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
