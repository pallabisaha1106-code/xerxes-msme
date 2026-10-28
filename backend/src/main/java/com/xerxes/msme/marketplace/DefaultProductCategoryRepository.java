package com.xerxes.msme.marketplace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DefaultProductCategoryRepository extends JpaRepository<DefaultProductCategory, UUID> {

    List<DefaultProductCategory> findAllByMarketplaceCategoryIdAndActiveTrueOrderByDisplayOrderAsc(UUID marketplaceCategoryId);

    List<DefaultProductCategory> findAllByMarketplaceCategoryIdOrderByDisplayOrderAsc(UUID marketplaceCategoryId);

    Optional<DefaultProductCategory> findByMarketplaceCategoryIdAndNameIgnoreCase(UUID marketplaceCategoryId, String name);
}
