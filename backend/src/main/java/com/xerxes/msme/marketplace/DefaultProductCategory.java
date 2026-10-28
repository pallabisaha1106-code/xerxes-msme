package com.xerxes.msme.marketplace;

import com.xerxes.msme.common.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** A Super Admin-curated sub-category under a MarketplaceCategory, e.g. "Rice" under "Grocery". */
@Entity
@Table(name = "default_product_categories")
@Getter
@Setter
public class DefaultProductCategory extends MutableEntity {

    @Column(name = "marketplace_category_id", nullable = false)
    private UUID marketplaceCategoryId;

    @Column(nullable = false)
    private String name;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
