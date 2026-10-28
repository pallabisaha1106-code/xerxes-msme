package com.xerxes.msme.catalog;

import com.xerxes.msme.common.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * The sellable/stockable unit. Every product has at least one row here, even
 * with an empty attributes map - see the schema header comment in
 * V1__init_schema.sql for why stock, pricing, and GST all live here rather
 * than on Product.
 */
@Entity
@Table(name = "product_variants")
@Getter
@Setter
public class ProductVariant extends MutableEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(nullable = false)
    private String sku;

    private String barcode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, String> attributes = new LinkedHashMap<>();

    @Column(name = "purchase_price", nullable = false)
    private BigDecimal purchasePrice = BigDecimal.ZERO;

    @Column(name = "selling_price", nullable = false)
    private BigDecimal sellingPrice = BigDecimal.ZERO;

    /** Overrides {@link Product#getDefaultHsnCode()} when set; see {@link VariantAttributeResolver}. */
    @Column(name = "hsn_code")
    private String hsnCode;

    /** Overrides {@link Product#getDefaultGstRate()} when set; see {@link VariantAttributeResolver}. */
    @Column(name = "gst_rate")
    private BigDecimal gstRate;

    /** Overrides {@link Product#getDefaultUom()} when set; see {@link VariantAttributeResolver}. */
    private String uom;

    @Column(name = "reorder_threshold", nullable = false)
    private BigDecimal reorderThreshold = BigDecimal.ZERO;

    @Column(name = "current_stock", nullable = false)
    private BigDecimal currentStock = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /**
     * Bare GCS object path (not a URL) pointing at the SAME image a default
     * catalog product used, set only when this variant was created from a
     * starter-catalog pick (see StarterCatalogService) - never duplicated in
     * storage. Only used as a fallback display image (StorefrontCatalogService)
     * when this variant has no real uploaded photos of its own in
     * product_variant_images; a real photo always takes priority.
     */
    @Column(name = "starter_image_path")
    private String starterImagePath;
}
