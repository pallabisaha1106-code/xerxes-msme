package com.xerxes.msme.marketplace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceAreaRepository extends JpaRepository<ServiceArea, UUID> {

    List<ServiceArea> findAllByCityIdAndActiveTrueOrderByDisplayOrderAsc(UUID cityId);

    /** Includes inactive ones too - for Super Admin management. */
    List<ServiceArea> findAllByCityIdOrderByDisplayOrderAsc(UUID cityId);

    Optional<ServiceArea> findByCityIdAndNameIgnoreCase(UUID cityId, String name);
}
