package com.xerxes.msme.marketplace;

import com.xerxes.msme.marketplace.dto.DefaultProductCatalogEntryResponse;
import com.xerxes.msme.marketplace.dto.MarketplaceCategoryResponse;
import com.xerxes.msme.marketplace.dto.PublicAdvertisementsResponse;
import com.xerxes.msme.marketplace.dto.PublicFeedPageResponse;
import com.xerxes.msme.marketplace.dto.PublicMarketplaceListingPageResponse;
import com.xerxes.msme.marketplace.dto.PublicMarketplaceListingResponse;
import com.xerxes.msme.marketplace.dto.ServiceAreaResponse;
import com.xerxes.msme.marketplace.dto.ServiceCityResponse;
import com.xerxes.msme.marketplace.dto.ServiceStateResponse;
import com.xerxes.msme.tenant.BusinessType;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** No authentication - this is what the public marketplace-web site calls. Rate-limited; see WebConfig. */
@RestController
@RequestMapping("/api/public/marketplace")
public class PublicMarketplaceController {

    private final PublicMarketplaceService publicMarketplaceService;

    public PublicMarketplaceController(PublicMarketplaceService publicMarketplaceService) {
        this.publicMarketplaceService = publicMarketplaceService;
    }

    @GetMapping("/categories")
    public List<MarketplaceCategoryResponse> categories() {
        return publicMarketplaceService.listCategories();
    }

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    /** cityId/areaId scope to that city's tenants (see listPublicFeed); size is capped so a caller can't force an unbounded query. */
    @GetMapping("/listings")
    public PublicMarketplaceListingPageResponse listings(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID cityId,
            @RequestParam(required = false) UUID areaId,
            @RequestParam(required = false) BusinessType businessType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
        return publicMarketplaceService.listListingsPage(
                categoryId, cityId, areaId, businessType, Math.max(0, page), Math.max(1, Math.min(size, MAX_PAGE_SIZE)));
    }

    @GetMapping("/listings/{tenantId}")
    public PublicMarketplaceListingResponse listing(@PathVariable UUID tenantId) {
        return publicMarketplaceService.getListing(tenantId);
    }

    @GetMapping("/default-catalog")
    public List<DefaultProductCatalogEntryResponse> defaultCatalog(@RequestParam UUID marketplaceCategoryId) {
        return publicMarketplaceService.getDefaultCatalog(marketplaceCategoryId);
    }

    @GetMapping("/advertisements")
    public PublicAdvertisementsResponse advertisements() {
        return publicMarketplaceService.listAdvertisements();
    }

    @GetMapping("/service-states")
    public List<ServiceStateResponse> serviceStates() {
        return publicMarketplaceService.listServiceStates();
    }

    /** Omit stateCode for a flat, state-agnostic list of every city (the "search/pick your city" UI). */
    @GetMapping("/service-cities")
    public List<ServiceCityResponse> serviceCities(@RequestParam(required = false) String stateCode) {
        return publicMarketplaceService.listServiceCities(stateCode);
    }

    @GetMapping("/service-areas")
    public List<ServiceAreaResponse> serviceAreas(@RequestParam UUID cityId) {
        return publicMarketplaceService.listServiceAreas(cityId);
    }

    /** Same paging contract as /listings: size capped, hasMore instead of a total. sort is NEWEST (default) or ENDING_SOON. */
    @GetMapping("/feed")
    public PublicFeedPageResponse feed(
            @RequestParam UUID cityId,
            @RequestParam(required = false) UUID areaId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) BusinessType businessType,
            @RequestParam(defaultValue = "NEWEST") FeedSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
        return publicMarketplaceService.listPublicFeed(
                cityId, areaId, categoryId, businessType, sort, Math.max(0, page), Math.max(1, Math.min(size, MAX_PAGE_SIZE)));
    }
}
