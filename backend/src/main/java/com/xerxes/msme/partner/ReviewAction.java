package com.xerxes.msme.partner;

import com.xerxes.msme.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * One decision on a shop, kept forever: who did what, to which shop, when, and why. Append-only (no update), so it
 * extends BaseEntity. actorUserId is empty when the system acted by itself (automatic partner assignment); partnerId
 * is empty when Super Admin acted.
 */
@Entity
@Table(name = "review_actions")
@Getter
@Setter
public class ReviewAction extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "actor_user_id", updatable = false)
    private UUID actorUserId;

    @Column(name = "partner_id", updatable = false)
    private UUID partnerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private ReviewActionType action;

    /** The thing acted on when it is not the shop itself, e.g. a feed post's id. */
    @Column(name = "subject_id", updatable = false)
    private UUID subjectId;

    @Column(updatable = false)
    private String reason;
}
