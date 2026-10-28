package com.xerxes.msme.marketplace;

import com.xerxes.msme.common.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * A short (<=140 char), Twitter-style update a vendor publishes about a current offer/sale -
 * "latest news" in the public feed. Pre-publish reviewed by Super Admin (starts PENDING, same
 * reviewed-by-staff shape as MarketplaceListing) - but unlike a listing, a rejected post is
 * terminal, not resubmitted, since it's one-shot ephemeral content rather than a long-lived
 * draft. One tenant can have many posts over time (one row per post, not one-per-tenant like
 * marketplace_listings). See V20 migration.
 */
@Entity
@Table(name = "feed_posts")
@Getter
@Setter
public class FeedPost extends MutableEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 140)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedPostStatus status = FeedPostStatus.PENDING;

    /** Short badge text ("20% off", "Buy 1 Get 1"), optional - see V21. */
    @Column(name = "discount_label", length = 30)
    private String discountLabel;

    /** Last day (inclusive) the offer stays public; null means the default 14-day window from createdAt. */
    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;
}
