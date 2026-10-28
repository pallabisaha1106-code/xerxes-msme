package com.xerxes.msme.partner;

import com.xerxes.msme.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** One area a partner covers. A partner can cover several areas and an area can have several partners. */
@Entity
@Table(name = "partner_areas")
@Getter
@Setter
public class PartnerArea extends BaseEntity {

    @Column(name = "partner_id", nullable = false, updatable = false)
    private UUID partnerId;

    @Column(name = "area_id", nullable = false, updatable = false)
    private UUID areaId;
}
