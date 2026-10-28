package com.xerxes.msme.marketplace;

import com.xerxes.msme.marketplace.dto.MarketplaceCategoryResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The shared, platform-curated category list every marketplace listing picks one entry from. Read-only for now. */
@RestController
@RequestMapping("/api/marketplace/categories")
public class MarketplaceCategoryController {

    private final MarketplaceCategoryRepository categoryRepository;

    public MarketplaceCategoryController(MarketplaceCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public List<MarketplaceCategoryResponse> list() {
        return categoryRepository.findAllByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(c -> new MarketplaceCategoryResponse(c.getId(), c.getName(), c.getIcon()))
                .toList();
    }
}
