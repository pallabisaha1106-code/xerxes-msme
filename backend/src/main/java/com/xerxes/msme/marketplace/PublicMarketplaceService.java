package com.xerxes.msme.marketplace;

import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.marketplace.dto.AdvertisementResponse;
import com.xerxes.msme.marketplace.dto.DefaultProductCatalogEntryResponse;
import com.xerxes.msme.marketplace.dto.DefaultProductOptionResponse;
import com.xerxes.msme.marketplace.dto.MarketplaceCategoryResponse;
import com.xerxes.msme.marketplace.dto.PublicAdvertisementsResponse;
import com.xerxes.msme.marketplace.dto.PublicFeedPageResponse;
import com.xerxes.msme.marketplace.dto.PublicFeedPostResponse;
import com.xerxes.msme.marketplace.dto.PublicMarketplaceListingPageResponse;
import com.xerxes.msme.marketplace.dto.PublicMarketplaceListingResponse;
import com.xerxes.msme.marketplace.dto.ServiceAreaResponse;
import com.xerxes.msme.marketplace.dto.ServiceCityResponse;
import com.xerxes.msme.marketplace.dto.ServiceStateResponse;
import com.xerxes.msme.storage.ImageStorageService;
import com.xerxes.msme.subscription.SubscriptionStatus;
import com.xerxes.msme.tenant.BusinessType;
import com.xerxes.msme.tenant.Tenant;
import com.xerxes.msme.tenant.TenantRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guest-facing reads of the marketplace: only ever LIVE listings, never a
 * vendor's own draft/pending work-in-progress (see MarketplaceListingService
 * and AdminMarketplaceListingService for those). No authentication - this is
 * what the public marketplace-web site calls directly.
 */
@Service
public class PublicMarketplaceService {

    private final MarketplaceListingRepository listingRepository;
    private final MarketplaceCategoryRepository categoryRepository;
    private final TenantRepository tenantRepository;
    private final DefaultProductCategoryRepository defaultProductCategoryRepository;
    private final DefaultProductRepository defaultProductRepository;
    private final ServiceCityRepository serviceCityRepository;
    private final ServiceAreaRepository serviceAreaRepository;
    private final ServiceStateRepository serviceStateRepository;
    private final AdvertisementRepository advertisementRepository;
    private final FeedPostRepository feedPostRepository;
    private final ImageStorageService imageStorageService;

    private static final int FEED_WINDOW_DAYS = 14;

    public PublicMarketplaceService(
            MarketplaceListingRepository listingRepository,
            MarketplaceCategoryRepository categoryRepository,
            TenantRepository tenantRepository,
            DefaultProductCategoryRepository defaultProductCategoryRepository,
            DefaultProductRepository defaultProductRepository,
            ServiceCityRepository serviceCityRepository,
            ServiceAreaRepository serviceAreaRepository,
            ServiceStateRepository serviceStateRepository,
            AdvertisementRepository advertisementRepository,
            FeedPostRepository feedPostRepository,
            ImageStorageService imageStorageService) {
        this.listingRepository = listingRepository;
        this.categoryRepository = categoryRepository;
        this.tenantRepository = tenantRepository;
        this.defaultProductCategoryRepository = defaultProductCategoryRepository;
        this.defaultProductRepository = defaultProductRepository;
        this.serviceCityRepository = serviceCityRepository;
        this.serviceAreaRepository = serviceAreaRepository;
        this.serviceStateRepository = serviceStateRepository;
        this.advertisementRepository = advertisementRepository;
        this.feedPostRepository = feedPostRepository;
        this.imageStorageService = imageStorageService;
    }

    @Transactional(readOnly = true)
    public PublicAdvertisementsResponse listAdvertisements() {
        OffsetDateTime now = OffsetDateTime.now();
        return new PublicAdvertisementsResponse(
                currentlyLiveAd(AdvertisementSlot.TOP_CAROUSEL, now),
                currentlyLiveAd(AdvertisementSlot.LEFT_RAIL, now),
                currentlyLiveAd(AdvertisementSlot.RIGHT_RAIL, now));
    }

    private AdvertisementResponse currentlyLiveAd(AdvertisementSlot slot, OffsetDateTime now) {
        return advertisementRepository
                .findFirstBySlotAndPublishedTrueAndStartsAtLessThanEqualAndEndsAtGreaterThanEqualOrderByStartsAtDesc(
                        slot, now, now)
                .map(ad -> new AdvertisementResponse(
                        imageStorageService.mediaUrl(ad.getImagePath()),
                        ad.getLinkUrl(),
                        ad.getAltText(),
                        ad.getHeadline(),
                        ad.getDescription()))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ServiceStateResponse> listServiceStates() {
        return serviceStateRepository.findAllByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(s -> new ServiceStateResponse(s.getCode(), s.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceCityResponse> listServiceCities(String stateCode) {
        List<ServiceCity> cities = stateCode != null
                ? serviceCityRepository.findAllByStateCodeAndActiveTrueOrderByDisplayOrderAsc(stateCode)
                : serviceCityRepository.findAllByActiveTrueOrderByDisplayOrderAsc();
        return cities.stream().map(c -> new ServiceCityResponse(c.getId(), c.getName())).toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceAreaResponse> listServiceAreas(UUID cityId) {
        return serviceAreaRepository.findAllByCityIdAndActiveTrueOrderByDisplayOrderAsc(cityId).stream()
                .map(a -> new ServiceAreaResponse(a.getId(), a.getName(), a.getPincode()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DefaultProductCatalogEntryResponse> getDefaultCatalog(UUID marketplaceCategoryId) {
        return defaultProductCategoryRepository
                .findAllByMarketplaceCategoryIdAndActiveTrueOrderByDisplayOrderAsc(marketplaceCategoryId)
                .stream()
                .map(category -> new DefaultProductCatalogEntryResponse(
                        category.getId(),
                        category.getName(),
                        defaultProductRepository
                                .findAllByDefaultProductCategoryIdAndActiveTrueOrderByDisplayOrderAsc(category.getId())
                                .stream()
                                .map(product -> new DefaultProductOptionResponse(
                                        product.getId(),
                                        product.getName(),
                                        product.getImagePath() == null ? null : imageStorageService.mediaUrl(product.getImagePath())))
                                .toList()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MarketplaceCategoryResponse> listCategories() {
        return categoryRepository.findAllByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(c -> new MarketplaceCategoryResponse(c.getId(), c.getName(), c.getIcon()))
                .toList();
    }

    /**
     * Paged, optionally city/area-scoped browse - cityId/areaId resolve to that city's tenant IDs
     * first (same pattern as listPublicFeed), then page the listings query scoped to just those
     * tenants. No city given means "browse everything, still paginated".
     */
    @Transactional(readOnly = true)
    public PublicMarketplaceListingPageResponse listListingsPage(
            UUID categoryId, UUID cityId, UUID areaId, BusinessType businessType, int page, int size) {
        List<UUID> tenantIds = null;
        if (cityId != null) {
            List<Tenant> candidateTenants = areaId != null
                    ? tenantRepository.findAllByPlatformFalseAndActiveTrueAndCityIdAndAreaId(cityId, areaId)
                    : tenantRepository.findAllByPlatformFalseAndActiveTrueAndCityId(cityId);
            if (candidateTenants.isEmpty()) return new PublicMarketplaceListingPageResponse(List.of(), false);
            tenantIds = candidateTenants.stream().map(Tenant::getId).toList();
        }

        // An account with no business type (older vendors, and the direct sign-up page) counts as a retailer.
        Slice<MarketplaceListing> slice = listingRepository.findLiveListingsPage(
                MarketplaceListingStatus.LIVE, categoryId, tenantIds, businessType,
                businessType == BusinessType.RETAILER, PageRequest.of(page, size));

        // A suspended tenant's listing can drop out here after the DB has already paged by LIMIT/OFFSET,
        // occasionally making a page come back smaller than `size` - hasMore still reflects the DB's own
        // next-page signal, so no data is lost, just an uncommon short page. Same trade-off listListings
        // above already has today, just not previously visible since nothing was paginated.
        List<PublicMarketplaceListingResponse> items =
                slice.getContent().stream().filter(this::hasSubscriptionAccess).map(this::toResponse).toList();
        return new PublicMarketplaceListingPageResponse(items, slice.hasNext());
    }

    @Transactional(readOnly = true)
    public PublicMarketplaceListingResponse getListing(UUID tenantId) {
        MarketplaceListing listing = listingRepository
                .findByTenantId(tenantId)
                .filter(l -> l.getStatus() == MarketplaceListingStatus.LIVE)
                .filter(this::hasSubscriptionAccess)
                .orElseThrow(() -> ResourceNotFoundException.of("MarketplaceListing", tenantId));
        return toResponse(listing);
    }

    /**
     * A shopper's paged "offers near me" browse: approved posts, within the 14-day window, from
     * vendors in the given city (and, if given, area) - only from tenants that are active AND
     * have a live, publicly-reachable shop page (a feed post for a shop with nothing to click
     * through to would be a dead end). areaId narrows further within cityId; omitted means "any
     * area in this city". categoryId, if given, keeps only shops listed in that category. Paged
     * like the listings browse (hasMore, no COUNT query), in the given order - see FeedSort.
     */
    @Transactional(readOnly = true)
    public PublicFeedPageResponse listPublicFeed(
            UUID cityId, UUID areaId, UUID categoryId, BusinessType businessType, FeedSort sort, int page, int size) {
        PublicFeedPageResponse empty = new PublicFeedPageResponse(List.of(), false);
        List<Tenant> candidateTenants = (areaId != null
                        ? tenantRepository.findAllByPlatformFalseAndActiveTrueAndCityIdAndAreaId(cityId, areaId)
                        : tenantRepository.findAllByPlatformFalseAndActiveTrueAndCityId(cityId))
                .stream()
                .filter(tenant -> businessType == null || businessTypeOf(tenant) == businessType)
                .toList();
        if (candidateTenants.isEmpty()) return empty;

        Map<UUID, String> businessNamesByTenantId =
                candidateTenants.stream().collect(Collectors.toMap(Tenant::getId, Tenant::getBusinessName));

        Map<UUID, BusinessType> businessTypesByTenantId =
                candidateTenants.stream().collect(Collectors.toMap(Tenant::getId, this::businessTypeOf));

        Map<UUID, MarketplaceListing> liveListingsByTenantId = listingRepository
                .findAllByTenantIdInAndStatus(businessNamesByTenantId.keySet().stream().toList(), MarketplaceListingStatus.LIVE)
                .stream()
                .filter(listing -> categoryId == null || categoryId.equals(listing.getCategoryId()))
                .filter(this::hasSubscriptionAccess)
                .collect(Collectors.toMap(MarketplaceListing::getTenantId, listing -> listing));
        if (liveListingsByTenantId.isEmpty()) return empty;

        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(FEED_WINDOW_DAYS);
        Slice<FeedPost> slice = feedPostRepository.findPublicFeedPage(
                liveListingsByTenantId.keySet().stream().toList(),
                FeedPostStatus.APPROVED,
                cutoff,
                LocalDate.now(),
                PageRequest.of(page, size, sort.sort()));

        // One lookup for just this page's categories, rather than a findById per post.
        Map<UUID, String> categoryNamesById = categoryRepository
                .findAllById(slice.getContent().stream()
                        .map(post -> liveListingsByTenantId.get(post.getTenantId()).getCategoryId())
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(MarketplaceCategory::getId, MarketplaceCategory::getName));

        List<PublicFeedPostResponse> items = slice.getContent().stream()
                .map(post -> {
                    MarketplaceListing listing = liveListingsByTenantId.get(post.getTenantId());
                    String shopPhotoUrl = listing.getShopPhotoFileName() != null
                            ? imageStorageService.mediaUrl(post.getTenantId() + "/marketplace/" + listing.getShopPhotoFileName())
                            : null;
                    return new PublicFeedPostResponse(
                            post.getTenantId(),
                            businessNamesByTenantId.get(post.getTenantId()),
                            shopPhotoUrl,
                            listing.getArea(),
                            categoryNamesById.get(listing.getCategoryId()),
                            businessTypesByTenantId.get(post.getTenantId()),
                            listing.getEnquiryWhatsapp(),
                            post.getContent(),
                            post.getDiscountLabel(),
                            post.getValidUntil(),
                            post.getCreatedAt());
                })
                .toList();
        return new PublicFeedPageResponse(items, slice.hasNext());
    }

    /** An account with no business type (older vendors, the direct sign-up page) counts as a retailer. */
    private BusinessType businessTypeOf(Tenant tenant) {
        return tenant.getBusinessType() == BusinessType.WHOLESALER ? BusinessType.WHOLESALER : BusinessType.RETAILER;
    }

    /** A SUSPENDED tenant is delisted (same hard-lock moment as their own API access) - don't reveal why. */
    private boolean hasSubscriptionAccess(MarketplaceListing listing) {
        return tenantRepository
                .findById(listing.getTenantId())
                .map(tenant -> tenant.getSubscriptionStatus() != SubscriptionStatus.SUSPENDED)
                .orElse(false);
    }

    private PublicMarketplaceListingResponse toResponse(MarketplaceListing listing) {
        String businessName = tenantRepository.findById(listing.getTenantId()).map(Tenant::getBusinessName).orElse(null);
        MarketplaceCategory category = listing.getCategoryId() != null
                ? categoryRepository.findById(listing.getCategoryId()).orElse(null)
                : null;
        String categoryName = category != null ? category.getName() : null;
        ImageProfile imageProfile = category != null ? category.getImageProfile() : ImageProfile.COMPACT;
        boolean bookable = category != null && category.isBookable();
        String shopPhotoUrl = listing.getShopPhotoFileName() != null
                ? imageStorageService.mediaUrl(listing.getTenantId() + "/marketplace/" + listing.getShopPhotoFileName())
                : null;
        return new PublicMarketplaceListingResponse(
                listing.getTenantId(),
                businessName,
                listing.getCategoryId(),
                categoryName,
                listing.getArea(),
                shopPhotoUrl,
                listing.getShopPhotoAlt(),
                listing.getInstagramUrl(),
                listing.getFacebookUrl(),
                listing.getYoutubeUrl(),
                listing.getGoogleMapsUrl(),
                listing.getWhatsappUrl(),
                listing.getWebsiteUrl(),
                listing.getEnquiryWhatsapp(),
                listing.getOrderMode(),
                imageProfile,
                bookable);
    }
}
