package com.xerxes.msme.marketplace;

import com.xerxes.msme.common.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** A Super Admin-curated starter product under a DefaultProductCategory, e.g. "Kohinoor Basmati Rice" under "Rice". */
@Entity
@Table(name = "default_products")
@Getter
@Setter
public class DefaultProduct extends MutableEntity {

    @Column(name = "default_product_category_id", nullable = false)
    private UUID defaultProductCategoryId;

    @Column(nullable = false)
    private String name;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /** Bare GCS object path (not a URL) - resolved to a mediaUrl at response time, same split as MarketplaceListing.shopPhotoFileName. */
    @Column(name = "image_path")
    private String imagePath;
}
