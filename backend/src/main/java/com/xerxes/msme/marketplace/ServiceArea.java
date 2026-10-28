package com.xerxes.msme.marketplace;

import com.xerxes.msme.common.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * A Super Admin-curated locality under a ServiceCity, e.g. "Nigdi" under
 * "Pune" - carries its own pincode (one per area, not a separate table/list).
 * Vendor registration is gated on picking one of these - see
 * VendorApplicationService.apply().
 */
@Entity
@Table(name = "service_areas")
@Getter
@Setter
public class ServiceArea extends MutableEntity {

    @Column(name = "city_id", nullable = false)
    private UUID cityId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String pincode;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
