package com.xerxes.msme.appointment;

import com.xerxes.msme.common.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One row per business, created the first time it saves its settings (until then it behaves as if
 * {@link AppointmentRules#DEFAULT_SLOT_MINUTES} were set). {@code slotMinutes} is how long each appointment is, and
 * also the step start times follow. See V23 migration.
 */
@Entity
@Table(name = "appointment_settings")
@Getter
@Setter
public class AppointmentSettings extends MutableEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    // The column is SMALLINT; without this Hibernate would expect INTEGER and refuse to start (ddl-auto=validate).
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "slot_minutes", nullable = false)
    private int slotMinutes;
}
