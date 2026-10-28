package com.xerxes.msme.partner;

import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.marketplace.ServiceArea;
import com.xerxes.msme.marketplace.ServiceAreaRepository;
import com.xerxes.msme.marketplace.ServiceCity;
import com.xerxes.msme.marketplace.ServiceCityRepository;
import com.xerxes.msme.partner.dto.AssignWaitingResponse;
import com.xerxes.msme.partner.dto.WaitingAreaResponse;
import com.xerxes.msme.tenant.TenantRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Super Admin's list of shops that need a partner, grouped by area: shops that never had one (the ones that existed
 * before partners did) and shops whose partner has been switched off. One click hands an area's shops out.
 */
@Service
public class WaitingShopsService {

    private final TenantRepository tenantRepository;
    private final ServiceAreaRepository serviceAreaRepository;
    private final ServiceCityRepository serviceCityRepository;
    private final PartnerAreaRepository partnerAreaRepository;
    private final PartnerRepository partnerRepository;
    private final PartnerAssignmentService partnerAssignmentService;

    public WaitingShopsService(
            TenantRepository tenantRepository,
            ServiceAreaRepository serviceAreaRepository,
            ServiceCityRepository serviceCityRepository,
            PartnerAreaRepository partnerAreaRepository,
            PartnerRepository partnerRepository,
            PartnerAssignmentService partnerAssignmentService) {
        this.tenantRepository = tenantRepository;
        this.serviceAreaRepository = serviceAreaRepository;
        this.serviceCityRepository = serviceCityRepository;
        this.partnerAreaRepository = partnerAreaRepository;
        this.partnerRepository = partnerRepository;
        this.partnerAssignmentService = partnerAssignmentService;
    }

    /** Areas that have shops needing a partner, the busiest first. */
    @Transactional(readOnly = true)
    public List<WaitingAreaResponse> list() {
        Map<UUID, Long> waiting = tenantRepository.countNeedingPartnerByArea().stream()
                .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));
        Map<UUID, ServiceArea> areas = serviceAreaRepository.findAllById(waiting.keySet()).stream()
                .collect(Collectors.toMap(ServiceArea::getId, Function.identity()));
        Map<UUID, ServiceCity> cities = serviceCityRepository
                .findAllById(areas.values().stream().map(ServiceArea::getCityId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(ServiceCity::getId, Function.identity()));
        return areas.values().stream()
                .map(area -> new WaitingAreaResponse(
                        area.getId(),
                        area.getName(),
                        cities.containsKey(area.getCityId()) ? cities.get(area.getCityId()).getName() : null,
                        waiting.get(area.getId()),
                        activePartnersCovering(area.getId())))
                .sorted(Comparator.comparingLong(WaitingAreaResponse::waitingShops).reversed())
                .toList();
    }

    /** Hands the area's waiting shops to the partners covering it, taking turns. */
    @Transactional
    public AssignWaitingResponse assign(UUID areaId, UUID actorUserId) {
        serviceAreaRepository.findById(areaId).orElseThrow(() -> ResourceNotFoundException.of("ServiceArea", areaId));
        int assigned = partnerAssignmentService.assignWaiting(areaId, actorUserId);
        return new AssignWaitingResponse(assigned, tenantRepository.findNeedingPartnerInArea(areaId).size());
    }

    private long activePartnersCovering(UUID areaId) {
        List<UUID> partnerIds =
                partnerAreaRepository.findAllByAreaId(areaId).stream().map(PartnerArea::getPartnerId).toList();
        return partnerRepository.findAllById(partnerIds).stream()
                .filter(partner -> partner.getStatus() == PartnerStatus.ACTIVE)
                .count();
    }
}
