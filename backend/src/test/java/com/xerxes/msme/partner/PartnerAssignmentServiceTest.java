package com.xerxes.msme.partner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xerxes.msme.tenant.Tenant;
import com.xerxes.msme.tenant.TenantRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PartnerAssignmentServiceTest {

    @Mock private PartnerRepository partnerRepository;
    @Mock private PartnerAreaRepository partnerAreaRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private ReviewActionService reviewActionService;
    @InjectMocks private PartnerAssignmentService service;

    private final UUID areaId = UUID.randomUUID();

    private Partner partner(PartnerStatus status, OffsetDateTime lastAssignedAt, OffsetDateTime createdAt) {
        Partner partner = new Partner();
        partner.setId(UUID.randomUUID());
        partner.setStatus(status);
        partner.setLastAssignedAt(lastAssignedAt);
        partner.setCreatedAt(createdAt);
        return partner;
    }

    private Tenant shopIn(UUID area) {
        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setAreaId(area);
        return tenant;
    }

    @Test
    void chooseNext_takesTurns_aPartnerNeverGivenAShopGoesFirstThenTheOneGivenOneLongestAgo() {
        OffsetDateTime now = OffsetDateTime.now();
        Partner recent = partner(PartnerStatus.ACTIVE, now.minusHours(1), now.minusDays(10));
        Partner longAgo = partner(PartnerStatus.ACTIVE, now.minusDays(3), now.minusDays(10));
        Partner never = partner(PartnerStatus.ACTIVE, null, now.minusDays(1));

        assertThat(PartnerAssignmentService.chooseNext(List.of(recent, longAgo, never))).contains(never);
        assertThat(PartnerAssignmentService.chooseNext(List.of(recent, longAgo))).contains(longAgo);
    }

    @Test
    void chooseNext_breaksATieByTheEarlierApplication_andSkipsAnyoneNotActive() {
        OffsetDateTime now = OffsetDateTime.now();
        Partner older = partner(PartnerStatus.ACTIVE, null, now.minusDays(9));
        Partner newer = partner(PartnerStatus.ACTIVE, null, now.minusDays(2));
        Partner off = partner(PartnerStatus.INACTIVE, null, now.minusDays(30));
        Partner waiting = partner(PartnerStatus.APPLIED, null, now.minusDays(40));

        assertThat(PartnerAssignmentService.chooseNext(List.of(newer, older, off, waiting))).contains(older);
        assertThat(PartnerAssignmentService.chooseNext(List.of(off, waiting))).isEmpty();
    }

    @Test
    void assign_recordsThePartnerOnTheShopAndInTheLog() {
        Partner chosen = partner(PartnerStatus.ACTIVE, null, OffsetDateTime.now().minusDays(5));
        Tenant tenant = shopIn(areaId);
        PartnerArea link = new PartnerArea();
        link.setPartnerId(chosen.getId());
        link.setAreaId(areaId);
        when(partnerAreaRepository.findAllByAreaId(areaId)).thenReturn(List.of(link));
        when(partnerRepository.findAllById(List.of(chosen.getId()))).thenReturn(List.of(chosen));

        Optional<Partner> result = service.assign(tenant);

        assertThat(result).contains(chosen);
        assertThat(tenant.getPartnerId()).isEqualTo(chosen.getId());
        assertThat(tenant.getPartnerAssignedAt()).isNotNull();
        assertThat(chosen.getLastAssignedAt()).isNotNull();
        verify(tenantRepository).save(tenant);
        verify(partnerRepository).save(chosen);
        // An automatic assignment has no acting user.
        verify(reviewActionService)
                .record(tenant.getId(), null, chosen.getId(), ReviewActionType.PARTNER_ASSIGNED, null, null);
    }

    @Test
    void assign_leavesAShopUnassignedWhenNoActivePartnerCoversItsArea() {
        Tenant tenant = shopIn(areaId);
        Partner off = partner(PartnerStatus.INACTIVE, null, OffsetDateTime.now());
        PartnerArea link = new PartnerArea();
        link.setPartnerId(off.getId());
        link.setAreaId(areaId);
        when(partnerAreaRepository.findAllByAreaId(areaId)).thenReturn(List.of(link));
        when(partnerRepository.findAllById(List.of(off.getId()))).thenReturn(List.of(off));

        assertThat(service.assign(tenant)).isEmpty();
        assertThat(tenant.getPartnerId()).isNull();
        verify(tenantRepository, never()).save(any(Tenant.class));
        verify(reviewActionService, never()).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void assign_doesNothingForAShopWithNoAreaOrAnAreaNobodyCovers() {
        assertThat(service.assign(shopIn(null))).isEmpty();

        when(partnerAreaRepository.findAllByAreaId(areaId)).thenReturn(List.of());
        assertThat(service.assign(shopIn(areaId))).isEmpty();
        verify(partnerRepository, never()).findAllById(any());
    }

    @Test
    void assignWaiting_handsEachWaitingShopToAPartnerInTurnAndLogsTheAdmin() {
        UUID admin = UUID.randomUUID();
        Partner first = partner(PartnerStatus.ACTIVE, null, OffsetDateTime.now().minusDays(9));
        Partner second = partner(PartnerStatus.ACTIVE, null, OffsetDateTime.now().minusDays(2));
        PartnerArea firstLink = new PartnerArea();
        firstLink.setPartnerId(first.getId());
        PartnerArea secondLink = new PartnerArea();
        secondLink.setPartnerId(second.getId());
        Tenant a = shopIn(areaId);
        Tenant b = shopIn(areaId);
        Tenant stuck = shopIn(areaId);
        stuck.setPartnerId(UUID.randomUUID()); // its partner was switched off
        when(tenantRepository.findNeedingPartnerInArea(areaId)).thenReturn(List.of(a, b, stuck));
        when(partnerAreaRepository.findAllByAreaId(areaId)).thenReturn(List.of(firstLink, secondLink));
        when(partnerRepository.findAllById(List.of(first.getId(), second.getId()))).thenReturn(List.of(first, second));

        assertThat(service.assignWaiting(areaId, admin)).isEqualTo(3);

        assertThat(a.getPartnerId()).isEqualTo(first.getId());
        assertThat(b.getPartnerId()).isEqualTo(second.getId());
        assertThat(stuck.getPartnerId()).isEqualTo(first.getId());
        verify(reviewActionService).record(a.getId(), admin, first.getId(), ReviewActionType.PARTNER_ASSIGNED, null, null);
        verify(reviewActionService).record(stuck.getId(), admin, first.getId(), ReviewActionType.PARTNER_REASSIGNED, null, null);
    }

    @Test
    void assignWaiting_countsOnlyTheShopsThatGotAPartner() {
        when(tenantRepository.findNeedingPartnerInArea(areaId)).thenReturn(List.of(shopIn(areaId)));
        when(partnerAreaRepository.findAllByAreaId(areaId)).thenReturn(List.of());

        assertThat(service.assignWaiting(areaId, UUID.randomUUID())).isZero();
    }
}
