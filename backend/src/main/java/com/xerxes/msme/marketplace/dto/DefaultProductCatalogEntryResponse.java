package com.xerxes.msme.marketplace.dto;

import java.util.List;
import java.util.UUID;

public record DefaultProductCatalogEntryResponse(UUID id, String name, List<DefaultProductOptionResponse> products) {}
