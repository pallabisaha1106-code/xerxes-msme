package com.xerxes.msme.partner;

import com.xerxes.msme.common.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * An independent local partner: the point of contact for the shops assigned to them, and (once approved) a reviewer of
 * those shops. The partner's login is an ordinary User with Role.PARTNER on the platform tenant; this row carries
 * everything that only a partner has.
 */
@Entity
@Table(name = "partners")
@Getter
@Setter
public class Partner extends MutableEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    /** Digits with the country code, e.g. "919876543210" - the form shop enquiry numbers already use. */
    @Column(nullable = false)
    private String whatsapp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartnerStatus status = PartnerStatus.APPLIED;

    /** When this partner was last given a new shop; the one assigned longest ago gets the next one. */
    @Column(name = "last_assigned_at")
    private OffsetDateTime lastAssignedAt;

    @Column(name = "decided_by")
    private UUID decidedBy;

    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;

    @Column(name = "decision_reason")
    private String decisionReason;
}
