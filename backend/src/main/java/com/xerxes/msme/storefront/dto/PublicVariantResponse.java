package com.xerxes.msme.storefront.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Deliberately doesn't expose exact stock counts publicly - just whether it's currently orderable.
 * thumbnailUrls runs parallel to imageUrls (smaller copies for grids/strips; empty when there are none).
 * zoomUrls also runs parallel to imageUrls, with a null wherever there is no larger copy (or an empty list when none has one).
 * sampleImage is true when imageUrls holds only the starter-catalogue placeholder, not a photo of the vendor's own stock.
 * categoryId/categoryName are the vendor's own product category (categories.id/name) - null when the product has none.
 */
public record PublicVariantResponse(
        UUID productId,
        String productName,
        String productDescription,
        UUID variantId,
        String sku,
        Map<String, String> attributes,
        BigDecimal sellingPrice,
        List<String> imageUrls,
        boolean inStock,
        List<String> thumbnailUrls,
        boolean sampleImage,
        List<String> zoomUrls,
        UUID categoryId,
        String categoryName) {

    public PublicVariantResponse(
            UUID productId,
            String productName,
            String productDescription,
            UUID variantId,
            String sku,
            Map<String, String> attributes,
            BigDecimal sellingPrice,
            List<String> imageUrls,
            boolean inStock) {
        this(productId, productName, productDescription, variantId, sku, attributes, sellingPrice, imageUrls, inStock, List.of(), false, List.of(), null, null);
    }
}
