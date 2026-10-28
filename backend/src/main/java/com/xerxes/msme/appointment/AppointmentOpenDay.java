package com.xerxes.msme.appointment;

import com.xerxes.msme.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * A date the business has published for appointments. A date with no row shows no slots, which is how a holiday or
 * leave is handled: the business simply doesn't publish it. Rows are added and removed, never edited. See V23.
 */
@Entity
@Table(name = "appointment_open_days")
@Getter
@Setter
public class AppointmentOpenDay extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "day", nullable = false, updatable = false)
    private LocalDate day;
}
