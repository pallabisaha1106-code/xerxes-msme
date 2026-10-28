package com.xerxes.msme.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.xerxes.msme.admin.dto.AdminServiceStateResponse;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.marketplace.ServiceState;
import com.xerxes.msme.marketplace.ServiceStateRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminServiceStateServiceTest {

    @Mock private ServiceStateRepository serviceStateRepository;

    private AdminServiceStateService serviceStateService;

    @BeforeEach
    void setUp() {
        serviceStateService = new AdminServiceStateService(serviceStateRepository);
    }

    private ServiceState state(String code, String name, boolean active) {
        ServiceState state = new ServiceState();
        state.setCode(code);
        state.setName(name);
        state.setDisplayOrder(1);
        state.setActive(active);
        return state;
    }

    @Test
    void listAll_returnsEveryStateRegardlessOfActiveFlag() {
        when(serviceStateRepository.findAllByOrderByDisplayOrderAsc())
                .thenReturn(List.of(state("27", "Maharashtra", true), state("38", "Ladakh", false)));

        List<AdminServiceStateResponse> result = serviceStateService.listAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(AdminServiceStateResponse::active).containsExactly(true, false);
    }

    @Test
    void updateStatus_deactivating_flipsTheActiveFlag() {
        ServiceState state = state("27", "Maharashtra", true);
        when(serviceStateRepository.findById("27")).thenReturn(Optional.of(state));

        AdminServiceStateResponse response = serviceStateService.updateStatus("27", false);

        assertThat(response.active()).isFalse();
        assertThat(state.isActive()).isFalse();
    }

    @Test
    void updateStatus_reactivating_flipsBack() {
        ServiceState state = state("27", "Maharashtra", false);
        when(serviceStateRepository.findById("27")).thenReturn(Optional.of(state));

        AdminServiceStateResponse response = serviceStateService.updateStatus("27", true);

        assertThat(response.active()).isTrue();
    }

    @Test
    void updateStatus_unknownState_throwsResourceNotFound() {
        when(serviceStateRepository.findById("99")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> serviceStateService.updateStatus("99", false));
    }
}
