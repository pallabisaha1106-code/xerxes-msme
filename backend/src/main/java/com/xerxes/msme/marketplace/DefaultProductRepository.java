package com.xerxes.msme.marketplace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DefaultProductRepository extends JpaRepository<DefaultProduct, UUID> {

    List<DefaultProduct> findAllByDefaultProductCategoryIdAndActiveTrueOrderByDisplayOrderAsc(UUID defaultProductCategoryId);

    List<DefaultProduct> findAllByDefaultProductCategoryIdOrderByDisplayOrderAsc(UUID defaultProductCategoryId);

    Optional<DefaultProduct> findByDefaultProductCategoryIdAndNameIgnoreCase(UUID defaultProductCategoryId, String name);
}
