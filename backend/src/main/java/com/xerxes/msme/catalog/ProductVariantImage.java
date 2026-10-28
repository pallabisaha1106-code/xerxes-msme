package com.xerxes.msme.catalog;

import com.xerxes.msme.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * One photo of a stocked variant. fileName is the main (960w) image; the
 * responsive thumbnail sits alongside it on disk as "<stem>-480.<ext>" by
 * convention (see ProductImageService), so only one name is stored here. The same goes for the optional zoom copy, whose
 * existence is recorded in hasZoom.
 */
@Entity
@Table(name = "product_variant_images")
@Getter
@Setter
public class ProductVariantImage extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "variant_id", nullable = false, updatable = false)
    private UUID variantId;

    @Column(name = "file_name", nullable = false, updatable = false)
    private String fileName;

    @Column(nullable = false, updatable = false)
    private int width;

    @Column(nullable = false, updatable = false)
    private int height;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    /** True when a larger "<stem>-zoom.<ext>" copy was also stored (DETAIL shops, big originals only). */
    @Column(name = "has_zoom", nullable = false, updatable = false)
    private boolean hasZoom;
}
