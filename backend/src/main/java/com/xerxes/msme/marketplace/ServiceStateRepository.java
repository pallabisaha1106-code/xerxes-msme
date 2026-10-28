package com.xerxes.msme.marketplace;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceStateRepository extends JpaRepository<ServiceState, String> {

    List<ServiceState> findAllByActiveTrueOrderByDisplayOrderAsc();

    /** Includes inactive ones too - for Super Admin management. */
    List<ServiceState> findAllByOrderByDisplayOrderAsc();
}
