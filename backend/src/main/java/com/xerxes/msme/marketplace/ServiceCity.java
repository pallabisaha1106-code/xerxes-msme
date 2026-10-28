package com.xerxes.msme.marketplace;

import com.xerxes.msme.common.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A Super Admin-curated city the marketplace actually supports, under one of
 * the fixed GST state codes (see GST_STATE_CODES in both frontend apps -
 * states themselves aren't admin-managed, they're a fixed government list).
 * Vendor registration is gated on picking one of these - see
 * VendorApplicationService.apply() - rather than free-typing a city.
 */
@Entity
@Table(name = "service_cities")
@Getter
@Setter
public class ServiceCity extends MutableEntity {

    @Column(name = "state_code", nullable = false)
    private String stateCode;

    @Column(nullable = false)
    private String name;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
