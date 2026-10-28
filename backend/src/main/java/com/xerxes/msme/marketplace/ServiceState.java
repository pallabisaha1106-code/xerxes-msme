package com.xerxes.msme.marketplace;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A Super Admin-toggleable GST state/UT the marketplace supports. Keyed by
 * the state's own 2-digit GST code rather than a synthetic UUID - that code
 * is already the natural, stable identifier, and service_cities.state_code
 * references it as a plain string, not a FK. Vendor registration is gated
 * on picking an active one of these - see VendorApplicationService.apply() -
 * the same hard-gate treatment City/Area already get.
 */
@Entity
@Table(name = "service_states")
@Getter
@Setter
public class ServiceState {

    @Id
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
