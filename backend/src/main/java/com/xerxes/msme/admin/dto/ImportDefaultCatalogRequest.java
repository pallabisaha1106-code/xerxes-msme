package com.xerxes.msme.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * Bulk-seeds/updates the starter catalog under one marketplace category in a
 * single request, upserting by name (case-insensitive) so re-running the
 * same manifest is safe - see AdminDefaultProductCatalogService.importCatalog.
 * Sent as the "manifest" part of a multipart request alongside optional
 * "images" file parts; a product's imageFileName is matched against those.
 */
public record ImportDefaultCatalogRequest(@NotNull UUID marketplaceCategoryId, @NotEmpty @Valid List<Category> categories) {

    public record Category(@NotBlank String name, @NotEmpty @Valid List<Product> products) {}

    public record Product(@NotBlank String name, String imageFileName) {}
}
