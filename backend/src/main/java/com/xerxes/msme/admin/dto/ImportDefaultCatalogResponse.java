package com.xerxes.msme.admin.dto;

import java.util.List;

/**
 * Summary of what an import did, so the admin UI can confirm the run without dumping every row.
 * skippedImages lists photos that were left out (e.g. too small), each with the reason.
 */
public record ImportDefaultCatalogResponse(
        int categoriesCreated,
        int categoriesUpdated,
        int productsCreated,
        int productsUpdated,
        int imagesAttached,
        List<String> skippedImages) {}
