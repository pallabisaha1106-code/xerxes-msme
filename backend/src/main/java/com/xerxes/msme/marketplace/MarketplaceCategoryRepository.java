package com.xerxes.msme.marketplace;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketplaceCategoryRepository extends JpaRepository<MarketplaceCategory, UUID> {

    List<MarketplaceCategory> findAllByActiveTrueOrderByDisplayOrderAsc();

    /** Includes inactive ones too - for Super Admin management, unlike the public-facing method above. */
    List<MarketplaceCategory> findAllByOrderByDisplayOrderAsc();
}
