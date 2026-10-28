package com.xerxes.msme.partner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.marketplace.ServiceArea;
import com.xerxes.msme.marketplace.ServiceAreaRepository;
import com.xerxes.msme.marketplace.ServiceCity;
import com.xerxes.msme.marketplace.ServiceCityRepository;
import com.xerxes.msme.partner.dto.AssignWaitingResponse;
import com.xerxes.msme.partner.dto.WaitingAreaResponse;
import com.xerxes.msme.tenant.Tenant;
import com.xerxes.msme.tenant.TenantRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WaitingShopsServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private ServiceAreaRepository serviceAreaRepository;
    @Mock private ServiceCityRepository serviceCityRepository;
    @Mock private PartnerAreaRepository partnerAreaRepository;
    @Mock private PartnerRepository partnerRepository;
    @Mock private PartnerAssignmentService partnerAssignmentService;
    @InjectMocks private WaitingShopsService service;

    private ServiceArea area(UUID id, UUID cityId, String name) {
        ServiceArea area = new ServiceArea();
        area.setId(id);
        area.setCityId(cityId);
        area.setName(name);
        return area;
    }

    @Test
    void list_showsEachAreaWithItsWaitingShopsAndActivePartners_busiestFirst() {
        UUID cityId = UUID.randomUUID();
        UUID kothrud = UUID.randomUUID();
        UUID nigdi = UUID.randomUUID();
        ServiceCity pune = new ServiceCity();
        pune.setId(cityId);
        pune.setName("Pune");
        when(tenantRepository.countNeedingPartnerByArea())
                .thenReturn(List.of(new Object[] {nigdi, 3L}, new Object[] {kothrud, 638L}));
        when(serviceAreaRepository.findAllById(Set.of(nigdi, kothrud)))
                .thenReturn(List.of(area(nigdi, cityId, "Nigdi"), area(kothrud, cityId, "Kothrud")));
        when(serviceCityRepository.findAllById(List.of(cityId))).thenReturn(List.of(pune));
        Partner active = new Partner();
        active.setId(UUID.randomUUID());
        active.setStatus(PartnerStatus.ACTIVE);
        Partner off = new Partner();
        off.setId(UUID.randomUUID());
        off.setStatus(PartnerStatus.INACTIVE);
        PartnerArea activeLink = new PartnerArea();
        activeLink.setPartnerId(active.getId());
        PartnerArea offLink = new PartnerArea();
        offLink.setPartnerId(off.getId());
        when(partnerAreaRepository.findAllByAreaId(kothrud)).thenReturn(List.of(activeLink, offLink));
        when(partnerRepository.findAllById(List.of(active.getId(), off.getId()))).thenReturn(List.of(active, off));
        when(partnerAreaRepository.findAllByAreaId(nigdi)).thenReturn(List.of());
        when(partnerRepository.findAllById(List.of())).thenReturn(List.of());

        List<WaitingAreaResponse> rows = service.list();

        assertThat(rows).extracting(WaitingAreaResponse::areaName).containsExactly("Kothrud", "Nigdi");
        assertThat(rows.get(0)).isEqualTo(new WaitingAreaResponse(kothrud, "Kothrud", "Pune", 638, 1));
        assertThat(rows.get(1).activePartners()).isZero();
    }

    @Test
    void assign_handsOutTheAreasShopsAndSaysHowManyStillWait() {
        UUID areaId = UUID.randomUUID();
        UUID admin = UUID.randomUUID();
        when(serviceAreaRepository.findById(areaId)).thenReturn(Optional.of(area(areaId, UUID.randomUUID(), "Kothrud")));
        when(partnerAssignmentService.assignWaiting(areaId, admin)).thenReturn(5);
        when(tenantRepository.findNeedingPartnerInArea(areaId)).thenReturn(List.of(new Tenant(), new Tenant()));

        assertThat(service.assign(areaId, admin)).isEqualTo(new AssignWaitingResponse(5, 2));
    }

    @Test
    void assign_anUnknownArea_isNotFound() {
        UUID areaId = UUID.randomUUID();
        when(serviceAreaRepository.findById(areaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assign(areaId, UUID.randomUUID())).isInstanceOf(ResourceNotFoundException.class);
    }
}
