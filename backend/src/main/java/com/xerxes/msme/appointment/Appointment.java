package com.xerxes.msme.appointment;

import com.xerxes.msme.common.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * One appointment request. The service name and price are copies made at request time, so editing or removing the
 * product later never changes what was asked for. See V24 migration.
 */
@Entity
@Table(name = "appointments")
@Getter
@Setter
public class Appointment extends MutableEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "variant_id", updatable = false)
    private UUID variantId;

    @Column(name = "service_name", nullable = false, updatable = false, length = 200)
    private String serviceName;

    @Column(updatable = false)
    private BigDecimal price;

    @Column(name = "customer_name", nullable = false, updatable = false, length = 120)
    private String customerName;

    @Column(name = "customer_phone", nullable = false, updatable = false, length = 20)
    private String customerPhone;

    @Column(updatable = false, length = 200)
    private String note;

    @Column(name = "starts_at", nullable = false, updatable = false)
    private OffsetDateTime startsAt;

    @Column(name = "ends_at", nullable = false, updatable = false)
    private OffsetDateTime endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.PENDING;

    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;
}
