package com.xerxes.msme.catalog;

import java.math.BigDecimal;

/**
 * A variant may override its product's HSN code, GST rate, or unit of
 * measure; when it doesn't (null), the product's default applies. Kept as a
 * standalone pure function rather than instance methods on the entities so
 * it's trivial to unit test without touching persistence.
 */
public final class VariantAttributeResolver {

    private VariantAttributeResolver() {}

    public static String resolveHsnCode(Product product, ProductVariant variant) {
        return variant.getHsnCode() != null ? variant.getHsnCode() : product.getDefaultHsnCode();
    }

    public static BigDecimal resolveGstRate(Product product, ProductVariant variant) {
        return variant.getGstRate() != null ? variant.getGstRate() : product.getDefaultGstRate();
    }

    public static String resolveUom(Product product, ProductVariant variant) {
        return variant.getUom() != null ? variant.getUom() : product.getDefaultUom();
    }
}
