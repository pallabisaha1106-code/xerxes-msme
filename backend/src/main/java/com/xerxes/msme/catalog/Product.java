package com.xerxes.msme.catalog;

import com.xerxes.msme.common.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "products")
@Getter
@Setter
public class Product extends MutableEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(nullable = false)
    private String name;

    private String brand;

    private String description;

    @Column(name = "default_hsn_code")
    private String defaultHsnCode;

    @Column(name = "default_gst_rate", nullable = false)
    private BigDecimal defaultGstRate = BigDecimal.ZERO;

    @Column(name = "default_uom", nullable = false)
    private String defaultUom = "PCS";

    private String fabric;

    @Column(name = "care_instructions")
    private String careInstructions;

    @Column(name = "included_items")
    private String includedItems;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
