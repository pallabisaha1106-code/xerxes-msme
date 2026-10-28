package com.xerxes.msme.partner;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartnerRepository extends JpaRepository<Partner, UUID> {

    Optional<Partner> findByUserId(UUID userId);

    List<Partner> findAllByOrderByCreatedAtDesc();

    List<Partner> findAllByStatusOrderByCreatedAtDesc(PartnerStatus status);
}
