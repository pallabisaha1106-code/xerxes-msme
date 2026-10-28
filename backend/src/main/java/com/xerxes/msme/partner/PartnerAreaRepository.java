package com.xerxes.msme.partner;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartnerAreaRepository extends JpaRepository<PartnerArea, UUID> {

    List<PartnerArea> findAllByPartnerId(UUID partnerId);

    List<PartnerArea> findAllByPartnerIdIn(Collection<UUID> partnerIds);

    List<PartnerArea> findAllByAreaId(UUID areaId);

    void deleteAllByPartnerId(UUID partnerId);
}
