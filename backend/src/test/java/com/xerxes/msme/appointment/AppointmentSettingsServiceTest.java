package com.xerxes.msme.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xerxes.msme.appointment.dto.AppointmentSettingsResponse;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentSettingsServiceTest {

    @Mock private AppointmentSettingsRepository settingsRepository;

    private AppointmentSettingsService service;
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AppointmentSettingsService(settingsRepository);
    }

    private AppointmentSettings savedSettings(int slotMinutes) {
        AppointmentSettings settings = new AppointmentSettings();
        settings.setTenantId(tenantId);
        settings.setSlotMinutes(slotMinutes);
        return settings;
    }

    @Test
    void get_neverSaved_returnsTheDefaultAndStoresNothing() {
        when(settingsRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());

        AppointmentSettingsResponse response = service.get(tenantId);

        assertThat(response.slotMinutes()).isEqualTo(30);
        assertThat(response.allowedSlotMinutes()).containsExactly(10, 15, 20, 30, 45, 60, 90, 120);
        verify(settingsRepository, never()).save(any());
    }

    @Test
    void get_saved_returnsTheSavedLength() {
        when(settingsRepository.findByTenantId(tenantId)).thenReturn(Optional.of(savedSettings(45)));

        assertThat(service.get(tenantId).slotMinutes()).isEqualTo(45);
    }

    @Test
    void update_firstTime_createsTheRowForThatBusiness() {
        when(settingsRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());

        AppointmentSettingsResponse response = service.update(tenantId, 20);

        ArgumentCaptor<AppointmentSettings> saved = ArgumentCaptor.forClass(AppointmentSettings.class);
        verify(settingsRepository).save(saved.capture());
        assertThat(saved.getValue().getTenantId()).isEqualTo(tenantId);
        assertThat(saved.getValue().getSlotMinutes()).isEqualTo(20);
        assertThat(response.slotMinutes()).isEqualTo(20);
    }

    @Test
    void update_existing_changesTheSameRow() {
        AppointmentSettings existing = savedSettings(30);
        when(settingsRepository.findByTenantId(tenantId)).thenReturn(Optional.of(existing));

        AppointmentSettingsResponse response = service.update(tenantId, 60);

        verify(settingsRepository).save(existing);
        assertThat(existing.getSlotMinutes()).isEqualTo(60);
        assertThat(response.slotMinutes()).isEqualTo(60);
    }

    @Test
    void update_everyAllowedLength_isAccepted() {
        when(settingsRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());

        for (int minutes : AppointmentRules.SLOT_LENGTHS) {
            assertThat(service.update(tenantId, minutes).slotMinutes()).isEqualTo(minutes);
        }
    }

    @Test
    void update_lengthNotInTheList_isRejectedAndNothingIsSaved() {
        BusinessRuleViolationException error =
                assertThrows(BusinessRuleViolationException.class, () -> service.update(tenantId, 25));

        assertThat(error.getMessage()).contains("10, 15, 20, 30, 45, 60, 90, 120");
        verify(settingsRepository, never()).save(any());
    }

    @Test
    void update_zeroOrNegative_isRejected() {
        assertThrows(BusinessRuleViolationException.class, () -> service.update(tenantId, 0));
        assertThrows(BusinessRuleViolationException.class, () -> service.update(tenantId, -30));
        verify(settingsRepository, never()).save(any());
    }
}
