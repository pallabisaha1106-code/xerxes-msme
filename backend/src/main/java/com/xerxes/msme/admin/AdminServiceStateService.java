package com.xerxes.msme.admin;

import com.xerxes.msme.admin.dto.AdminServiceStateResponse;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.marketplace.ServiceState;
import com.xerxes.msme.marketplace.ServiceStateRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Super Admin enable/disable of the GST state list (see ServiceState). The
 * list itself is fixed (seeded once by V15__service_states.sql, all 37
 * official codes) - there's no create/delete here, only toggling which ones
 * vendor registration will accept, mirroring AdminMarketplaceCategoryService.
 */
@Service
public class AdminServiceStateService {

    private final ServiceStateRepository serviceStateRepository;

    public AdminServiceStateService(ServiceStateRepository serviceStateRepository) {
        this.serviceStateRepository = serviceStateRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminServiceStateResponse> listAll() {
        return serviceStateRepository.findAllByOrderByDisplayOrderAsc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public AdminServiceStateResponse updateStatus(String code, boolean active) {
        ServiceState state =
                serviceStateRepository.findById(code).orElseThrow(() -> ResourceNotFoundException.of("ServiceState", code));
        state.setActive(active);
        serviceStateRepository.save(state);
        return toResponse(state);
    }

    private AdminServiceStateResponse toResponse(ServiceState state) {
        return new AdminServiceStateResponse(state.getCode(), state.getName(), state.getDisplayOrder(), state.isActive());
    }
}
