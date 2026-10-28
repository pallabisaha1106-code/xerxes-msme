package com.xerxes.msme.marketplace;

import com.xerxes.msme.common.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A small, platform-curated taxonomy every marketplace listing picks exactly
 * one entry from - the shared filter facet across every vendor's storefront.
 * Deliberately separate from the tenant-private {@code categories} table
 * (see Category), which organizes one tenant's own products rather than
 * classifying a storefront for cross-tenant browsing. Seeded by V6; Super
 * Admin can enable/disable one via AdminMarketplaceCategoryService - naming
 * and ordering are still migration-only, only the active flag is admin-managed.
 */
@Entity
@Table(name = "marketplace_categories")
@Getter
@Setter
public class MarketplaceCategory extends MutableEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String icon;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_profile", nullable = false)
    private ImageProfile imageProfile = ImageProfile.COMPACT;

    /** Shops in a bookable category (a salon, a clinic) take appointment requests; Super Admin decides which categories are. */
    @Column(nullable = false)
    private boolean bookable = false;
}
