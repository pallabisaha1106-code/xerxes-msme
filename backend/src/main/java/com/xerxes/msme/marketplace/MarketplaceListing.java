package com.xerxes.msme.marketplace;

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
 * A tenant's opt-in profile for the public marketplace - one row per tenant.
 * Separate from Tenant itself: this has its own draft/pending/live lifecycle
 * and a set of marketing-facing fields (area, shop photo, social links) with
 * no bearing on the tenant's own billing/business identity. See V6 migration.
 */
@Entity
@Table(name = "marketplace_listings")
@Getter
@Setter
public class MarketplaceListing extends MutableEntity {

    @Column(name = "tenant_id", nullable = false, unique = true, updatable = false)
    private UUID tenantId;

    @Column(name = "category_id")
    private UUID categoryId;

    private String area;

    @Column(name = "shop_photo_file_name")
    private String shopPhotoFileName;

    @Column(name = "shop_photo_alt")
    private String shopPhotoAlt;

    @Column(name = "instagram_url")
    private String instagramUrl;

    @Column(name = "facebook_url")
    private String facebookUrl;

    @Column(name = "youtube_url")
    private String youtubeUrl;

    @Column(name = "google_maps_url")
    private String googleMapsUrl;

    @Column(name = "whatsapp_url")
    private String whatsappUrl;

    @Column(name = "website_url")
    private String websiteUrl;

    /** Direct WhatsApp number for orders/enquiries - independent of whatsappUrl (a group/channel/chat link). */
    @Column(name = "enquiry_whatsapp")
    private String enquiryWhatsapp;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_mode", nullable = false)
    private MarketplaceOrderMode orderMode = MarketplaceOrderMode.WHATSAPP;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MarketplaceListingStatus status = MarketplaceListingStatus.DRAFT;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;
}
