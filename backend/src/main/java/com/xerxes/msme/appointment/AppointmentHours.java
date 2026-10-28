package com.xerxes.msme.appointment;

import com.xerxes.msme.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One opening range on one weekday of a business's weekly template, e.g. Monday 10:00-13:00; a split shift is two
 * rows for the same day. Local wall-clock times. The whole week is replaced at once, so rows are never edited. See
 * V23 migration.
 */
@Entity
@Table(name = "appointment_hours")
@Getter
@Setter
public class AppointmentHours extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    /** ISO weekday: 1 = Monday ... 7 = Sunday. The column is SMALLINT (see AppointmentSettings for why that matters). */
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "day_of_week", nullable = false, updatable = false)
    private int dayOfWeek;

    @Column(name = "opens_at", nullable = false, updatable = false)
    private LocalTime opensAt;

    @Column(name = "closes_at", nullable = false, updatable = false)
    private LocalTime closesAt;
}
