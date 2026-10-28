package com.xerxes.msme.partner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.common.exception.DuplicateResourceException;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.marketplace.ServiceArea;
import com.xerxes.msme.marketplace.ServiceAreaRepository;
import com.xerxes.msme.partner.dto.PartnerApplicationRequest;
import com.xerxes.msme.partner.dto.PartnerApplicationResponse;
import com.xerxes.msme.tenant.PlatformTenant;
import com.xerxes.msme.user.Role;
import com.xerxes.msme.user.User;
import com.xerxes.msme.user.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PartnerApplicationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PartnerRepository partnerRepository;
    @Mock private PartnerAreaRepository partnerAreaRepository;
    @Mock private ServiceAreaRepository serviceAreaRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private PartnerApplicationService service;

    private final UUID kothrud = UUID.randomUUID();
    private final UUID baner = UUID.randomUUID();

    @BeforeEach
    void saves() {
        // Saving gives the row the id the database would; lenient because not every test reaches every save.
        org.mockito.Mockito.lenient().when(userRepository.save(any(User.class))).thenAnswer(call -> {
            User user = call.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        org.mockito.Mockito.lenient().when(partnerRepository.save(any(Partner.class))).thenAnswer(call -> {
            Partner partner = call.getArgument(0);
            partner.setId(UUID.randomUUID());
            return partner;
        });
    }

    private ServiceArea area(UUID id, boolean active) {
        ServiceArea area = new ServiceArea();
        area.setId(id);
        area.setActive(active);
        return area;
    }

    private PartnerApplicationRequest request(String whatsapp, String email, List<UUID> areaIds) {
        return new PartnerApplicationRequest("  Meera Kulkarni ", whatsapp, email, "SecurePass123", areaIds);
    }

    @Test
    void apply_createsAnInactivePartnerLoginOnThePlatformTenantWithItsAreas() {
        when(userRepository.existsByEmailIgnoreCase("meera@example.com")).thenReturn(false);
        when(serviceAreaRepository.findById(kothrud)).thenReturn(Optional.of(area(kothrud, true)));
        when(serviceAreaRepository.findById(baner)).thenReturn(Optional.of(area(baner, true)));
        when(passwordEncoder.encode("SecurePass123")).thenReturn("hashed");

        PartnerApplicationResponse response =
                service.apply(request("98765 43210", "meera@example.com", List.of(kothrud, baner)));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User user = userCaptor.getValue();
        assertThat(user.getTenantId()).isEqualTo(PlatformTenant.ID);
        assertThat(user.getRole()).isEqualTo(Role.PARTNER);
        assertThat(user.isActive()).isFalse();
        assertThat(user.getPasswordHash()).isEqualTo("hashed");
        assertThat(user.getName()).isEqualTo("Meera Kulkarni");

        ArgumentCaptor<Partner> partnerCaptor = ArgumentCaptor.forClass(Partner.class);
        verify(partnerRepository).save(partnerCaptor.capture());
        Partner partner = partnerCaptor.getValue();
        assertThat(partner.getStatus()).isEqualTo(PartnerStatus.APPLIED);
        assertThat(partner.getWhatsapp()).isEqualTo("919876543210");
        assertThat(partner.getUserId()).isEqualTo(user.getId());
        assertThat(response.name()).isEqualTo("Meera Kulkarni");

        ArgumentCaptor<PartnerArea> areaCaptor = ArgumentCaptor.forClass(PartnerArea.class);
        verify(partnerAreaRepository, org.mockito.Mockito.times(2)).save(areaCaptor.capture());
        assertThat(areaCaptor.getAllValues()).extracting(PartnerArea::getAreaId).containsExactly(kothrud, baner);
    }

    @Test
    void apply_aRepeatedAreaIsOnlyCountedOnce() {
        when(userRepository.existsByEmailIgnoreCase("meera@example.com")).thenReturn(false);
        when(serviceAreaRepository.findById(kothrud)).thenReturn(Optional.of(area(kothrud, true)));

        service.apply(request("9876543210", "meera@example.com", List.of(kothrud, kothrud)));

        verify(partnerAreaRepository, org.mockito.Mockito.times(1)).save(any(PartnerArea.class));
    }

    @Test
    void apply_refusesANumberThatIsNotAnIndianMobile() {
        assertThatThrownBy(() -> service.apply(request("12345", "meera@example.com", List.of(kothrud))))
                .isInstanceOf(BusinessRuleViolationException.class);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void apply_refusesAnEmailAlreadyUsedByAnyAccount() {
        when(userRepository.existsByEmailIgnoreCase("meera@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.apply(request("9876543210", "meera@example.com", List.of(kothrud))))
                .isInstanceOf(DuplicateResourceException.class);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void apply_refusesAnAreaThatDoesNotExistOrIsSwitchedOff() {
        when(userRepository.existsByEmailIgnoreCase("meera@example.com")).thenReturn(false);
        when(serviceAreaRepository.findById(kothrud)).thenReturn(Optional.of(area(kothrud, false)));

        assertThatThrownBy(() -> service.apply(request("9876543210", "meera@example.com", List.of(kothrud))))
                .isInstanceOf(ResourceNotFoundException.class);

        UUID missing = UUID.randomUUID();
        when(serviceAreaRepository.findById(missing)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.apply(request("9876543210", "meera@example.com", List.of(missing))))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(userRepository, never()).save(any(User.class));
    }
}
