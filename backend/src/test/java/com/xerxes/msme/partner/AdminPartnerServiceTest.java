package com.xerxes.msme.partner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.marketplace.ServiceArea;
import com.xerxes.msme.marketplace.ServiceAreaRepository;
import com.xerxes.msme.marketplace.ServiceCity;
import com.xerxes.msme.marketplace.ServiceCityRepository;
import com.xerxes.msme.partner.dto.AdminPartnerResponse;
import com.xerxes.msme.security.AuthTokenService;
import com.xerxes.msme.tenant.TenantRepository;
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

@ExtendWith(MockitoExtension.class)
class AdminPartnerServiceTest {

    @Mock private PartnerRepository partnerRepository;
    @Mock private PartnerAreaRepository partnerAreaRepository;
    @Mock private ServiceAreaRepository serviceAreaRepository;
    @Mock private ServiceCityRepository serviceCityRepository;
    @Mock private UserRepository userRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private AuthTokenService authTokenService;
    @InjectMocks private AdminPartnerService service;

    private final UUID partnerId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private final UUID kothrud = UUID.randomUUID();
    private final UUID baner = UUID.randomUUID();
    private final UUID puneId = UUID.randomUUID();
    private Partner partner;
    private User login;

    @BeforeEach
    void setUp() {
        partner = new Partner();
        partner.setId(partnerId);
        partner.setUserId(userId);
        partner.setName("Meera");
        partner.setWhatsapp("919876543210");
        partner.setStatus(PartnerStatus.APPLIED);
        login = new User();
        login.setId(userId);
        login.setEmail("meera@example.com");
        login.setActive(false);
        lenient().when(partnerRepository.findById(partnerId)).thenReturn(Optional.of(partner));
        lenient().when(userRepository.findById(userId)).thenReturn(Optional.of(login));
        lenient().when(partnerAreaRepository.findAllByPartnerId(partnerId)).thenReturn(List.of());
        lenient().when(serviceAreaRepository.findAllById(any())).thenReturn(List.of());
        lenient().when(serviceCityRepository.findAllById(any())).thenReturn(List.<ServiceCity>of());
        lenient().when(tenantRepository.countByPartnerId(partnerId)).thenReturn(0L);
    }

    private ServiceArea area(UUID id, boolean active) {
        ServiceArea area = new ServiceArea();
        area.setId(id);
        area.setName("Area");
        area.setCityId(puneId);
        area.setActive(active);
        return area;
    }

    private PartnerArea partnerArea(UUID areaId) {
        PartnerArea pa = new PartnerArea();
        pa.setId(UUID.randomUUID());
        pa.setPartnerId(partnerId);
        pa.setAreaId(areaId);
        return pa;
    }

    @Test
    void approve_makesThePartnerActiveAndSwitchesTheLoginOn() {
        AdminPartnerResponse response = service.approve(partnerId, adminId);

        assertThat(partner.getStatus()).isEqualTo(PartnerStatus.ACTIVE);
        assertThat(partner.getDecidedBy()).isEqualTo(adminId);
        assertThat(partner.getDecidedAt()).isNotNull();
        assertThat(login.isActive()).isTrue();
        assertThat(response.status()).isEqualTo(PartnerStatus.ACTIVE);
        assertThat(response.email()).isEqualTo("meera@example.com");
    }

    @Test
    void approve_refusesAPartnerThatIsNotWaitingForReview() {
        partner.setStatus(PartnerStatus.ACTIVE);

        assertThatThrownBy(() -> service.approve(partnerId, adminId)).isInstanceOf(BusinessRuleViolationException.class);
        partner.setStatus(PartnerStatus.REJECTED);
        assertThatThrownBy(() -> service.approve(partnerId, adminId)).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void reject_recordsTheReasonAndLeavesTheLoginOff() {
        service.reject(partnerId, "  Not enough local knowledge ", adminId);

        assertThat(partner.getStatus()).isEqualTo(PartnerStatus.REJECTED);
        assertThat(partner.getDecisionReason()).isEqualTo("Not enough local knowledge");
        assertThat(login.isActive()).isFalse();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void reject_refusesAPartnerThatIsNotWaitingForReview() {
        partner.setStatus(PartnerStatus.INACTIVE);

        assertThatThrownBy(() -> service.reject(partnerId, "no", adminId)).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void deactivate_switchesTheLoginOffAndEndsEverySignIn() {
        partner.setStatus(PartnerStatus.ACTIVE);
        login.setActive(true);

        service.deactivate(partnerId, adminId);

        assertThat(partner.getStatus()).isEqualTo(PartnerStatus.INACTIVE);
        assertThat(login.isActive()).isFalse();
        verify(authTokenService).revokeAllForUser(userId);
    }

    @Test
    void deactivate_refusesAPartnerThatIsNotActive() {
        assertThatThrownBy(() -> service.deactivate(partnerId, adminId)).isInstanceOf(BusinessRuleViolationException.class);
        verify(authTokenService, never()).revokeAllForUser(any());
    }

    @Test
    void reactivate_onlyWorksForADeactivatedPartner() {
        partner.setStatus(PartnerStatus.INACTIVE);

        service.reactivate(partnerId, adminId);

        assertThat(partner.getStatus()).isEqualTo(PartnerStatus.ACTIVE);
        assertThat(login.isActive()).isTrue();

        partner.setStatus(PartnerStatus.REJECTED);
        assertThatThrownBy(() -> service.reactivate(partnerId, adminId)).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void replaceAreas_addsTheNewOnesAndRemovesTheOnesLeftOut() {
        PartnerArea keep = partnerArea(kothrud);
        PartnerArea drop = partnerArea(UUID.randomUUID());
        when(partnerAreaRepository.findAllByPartnerId(partnerId)).thenReturn(List.of(keep, drop));
        when(serviceAreaRepository.findById(kothrud)).thenReturn(Optional.of(area(kothrud, true)));
        when(serviceAreaRepository.findById(baner)).thenReturn(Optional.of(area(baner, true)));

        service.replaceAreas(partnerId, List.of(kothrud, baner));

        ArgumentCaptor<List<PartnerArea>> removed = ArgumentCaptor.forClass(List.class);
        verify(partnerAreaRepository).deleteAll(removed.capture());
        assertThat(removed.getValue()).containsExactly(drop);
        ArgumentCaptor<PartnerArea> added = ArgumentCaptor.forClass(PartnerArea.class);
        verify(partnerAreaRepository, times(1)).save(added.capture());
        assertThat(added.getValue().getAreaId()).isEqualTo(baner);
    }

    @Test
    void replaceAreas_refusesAnAreaThatIsMissingOrSwitchedOff() {
        when(serviceAreaRepository.findById(kothrud)).thenReturn(Optional.of(area(kothrud, false)));

        assertThatThrownBy(() -> service.replaceAreas(partnerId, List.of(kothrud)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(partnerAreaRepository, never()).deleteAll(any());
    }

    @Test
    void listPartners_filtersByStatusWhenGiven_andShowsAreasAndShopCount() {
        when(partnerRepository.findAllByStatusOrderByCreatedAtDesc(PartnerStatus.APPLIED)).thenReturn(List.of(partner));
        when(partnerAreaRepository.findAllByPartnerId(partnerId)).thenReturn(List.of(partnerArea(kothrud)));
        ServiceArea kothrudArea = area(kothrud, true);
        kothrudArea.setName("Kothrud");
        when(serviceAreaRepository.findAllById(any())).thenReturn(List.of(kothrudArea));
        ServiceCity pune = new ServiceCity();
        pune.setId(puneId);
        pune.setName("Pune");
        when(serviceCityRepository.findAllById(any())).thenReturn(List.of(pune));
        when(tenantRepository.countByPartnerId(partnerId)).thenReturn(7L);

        List<AdminPartnerResponse> result = service.listPartners(PartnerStatus.APPLIED);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).assignedShopCount()).isEqualTo(7);
        assertThat(result.get(0).areas()).singleElement().satisfies(a -> {
            assertThat(a.areaName()).isEqualTo("Kothrud");
            assertThat(a.cityName()).isEqualTo("Pune");
        });
        verify(partnerRepository, never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void getPartner_unknownId_isNotFound() {
        assertThatThrownBy(() -> service.getPartner(UUID.randomUUID())).isInstanceOf(ResourceNotFoundException.class);
    }
}
