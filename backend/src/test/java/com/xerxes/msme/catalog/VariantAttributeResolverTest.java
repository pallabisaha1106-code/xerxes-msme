package com.xerxes.msme.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class VariantAttributeResolverTest {

    private Product productWithDefaults() {
        Product product = new Product();
        product.setDefaultHsnCode("6109");
        product.setDefaultGstRate(new BigDecimal("12.00"));
        product.setDefaultUom("PCS");
        return product;
    }

    @Test
    void variantWithNoOverrides_fallsBackToProductDefaults_plainProductCase() {
        Product product = productWithDefaults();
        ProductVariant variant = new ProductVariant();

        assertThat(VariantAttributeResolver.resolveHsnCode(product, variant)).isEqualTo("6109");
        assertThat(VariantAttributeResolver.resolveGstRate(product, variant)).isEqualByComparingTo("12.00");
        assertThat(VariantAttributeResolver.resolveUom(product, variant)).isEqualTo("PCS");
    }

    @Test
    void variantWithOverrides_takesPrecedenceOverProductDefaults() {
        Product product = productWithDefaults();
        ProductVariant variant = new ProductVariant();
        variant.setHsnCode("6110");
        variant.setGstRate(new BigDecimal("5.00"));
        variant.setUom("SET");

        assertThat(VariantAttributeResolver.resolveHsnCode(product, variant)).isEqualTo("6110");
        assertThat(VariantAttributeResolver.resolveGstRate(product, variant)).isEqualByComparingTo("5.00");
        assertThat(VariantAttributeResolver.resolveUom(product, variant)).isEqualTo("SET");
    }

    @Test
    void variantWithPartialOverride_mixesOverrideAndDefault() {
        Product product = productWithDefaults();
        ProductVariant variant = new ProductVariant();
        variant.setGstRate(new BigDecimal("0.00"));

        assertThat(VariantAttributeResolver.resolveHsnCode(product, variant)).isEqualTo("6109");
        assertThat(VariantAttributeResolver.resolveGstRate(product, variant)).isEqualByComparingTo("0.00");
        assertThat(VariantAttributeResolver.resolveUom(product, variant)).isEqualTo("PCS");
    }
}
