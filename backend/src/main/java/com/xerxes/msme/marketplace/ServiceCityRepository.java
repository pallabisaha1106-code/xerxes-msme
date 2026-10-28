package com.xerxes.msme.marketplace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceCityRepository extends JpaRepository<ServiceCity, UUID> {

    List<ServiceCity> findAllByStateCodeAndActiveTrueOrderByDisplayOrderAsc(String stateCode);

    /** Across every state - for a state-agnostic "search/pick your city" UI. */
    List<ServiceCity> findAllByActiveTrueOrderByDisplayOrderAsc();

    /** Includes inactive ones too - for Super Admin management. */
    List<ServiceCity> findAllByStateCodeOrderByDisplayOrderAsc(String stateCode);

    Optional<ServiceCity> findByStateCodeAndNameIgnoreCase(String stateCode, String name);
}
