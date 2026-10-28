package com.xerxes.msme.marketplace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.marketplace.dto.MarketplaceCategoryResponse;
import com.xerxes.msme.marketplace.dto.PublicFeedPageResponse;
import com.xerxes.msme.marketplace.dto.PublicFeedPostResponse;
import com.xerxes.msme.marketplace.dto.PublicMarketplaceListingPageResponse;
import com.xerxes.msme.marketplace.dto.PublicMarketplaceListingResponse;
import com.xerxes.msme.storage.ImageStorageService;
import com.xerxes.msme.subscription.SubscriptionStatus;
import com.xerxes.msme.tenant.BusinessType;
import com.xerxes.msme.tenant.Tenant;
import com.xerxes.msme.tenant.TenantRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;

@ExtendWith(MockitoExtension.class)
class PublicMarketplaceServiceTest {

    @Mock private MarketplaceListingRepository listingRepository;
    @Mock private MarketplaceCategoryRepository categoryRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private DefaultProductCategoryRepository defaultProductCategoryRepository;
    @Mock private DefaultProductRepository defaultProductRepository;
    @Mock private ServiceCityRepository serviceCityRepository;
    @Mock private ServiceAreaRepository serviceAreaRepository;
    @Mock private ServiceStateRepository serviceStateRepository;
    @Mock private AdvertisementRepository advertisementRepository;
    @Mock private FeedPostRepository feedPostRepository;
    @Mock private ImageStorageService imageStorageService;

    private PublicMarketplaceService publicMarketplaceService;
    private UUID tenantId;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        publicMarketplaceService = new PublicMarketplaceService(
                listingRepository,
                categoryRepository,
                tenantRepository,
                defaultProductCategoryRepository,
                defaultProductRepository,
                serviceCityRepository,
                serviceAreaRepository,
                serviceStateRepository,
                advertisementRepository,
                feedPostRepository,
                imageStorageService);
        tenantId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
    }

    private MarketplaceListing liveListing() {
        MarketplaceListing listing = new MarketplaceListing();
        listing.setId(UUID.randomUUID());
        listing.setTenantId(tenantId);
        listing.setCategoryId(categoryId);
        listing.setArea("Gujar Nagar");
        listing.setOrderMode(MarketplaceOrderMode.WHATSAPP);
        listing.setStatus(MarketplaceListingStatus.LIVE);
        return listing;
    }

    private void stubTenantAndCategory() {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setBusinessName("Veera Collections");
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        MarketplaceCategory category = new MarketplaceCategory();
        category.setId(categoryId);
        category.setName("Ladies Wear");
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
    }

    @Test
    void listCategories_returnsActiveCategoriesMapped() {
        MarketplaceCategory category = new MarketplaceCategory();
        category.setId(categoryId);
        category.setName("Ladies Wear");
        category.setIcon("icon");
        when(categoryRepository.findAllByActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of(category));

        List<MarketplaceCategoryResponse> result = publicMarketplaceService.listCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Ladies Wear");
    }

    @Test
    void listListingsPage_withCategoryFilter_passesItToTheQuery() {
        stubTenantAndCategory();
        when(listingRepository.findLiveListingsPage(eq(MarketplaceListingStatus.LIVE), eq(categoryId), isNull(), isNull(), anyBoolean(), any()))
                .thenReturn(new SliceImpl<>(List.of(liveListing()), PageRequest.of(0, 20), false));

        PublicMarketplaceListingPageResponse result = publicMarketplaceService.listListingsPage(categoryId, null, null, null, 0, 20);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).businessName()).isEqualTo("Veera Collections");
        assertThat(result.items().get(0).categoryName()).isEqualTo("Ladies Wear");
    }

    @Test
    void listListingsPage_retailers_passesRetailerAndCountsUntypedAccountsAsRetail() {
        stubTenantAndCategory();
        when(listingRepository.findLiveListingsPage(eq(MarketplaceListingStatus.LIVE), isNull(), isNull(), eq(BusinessType.RETAILER), eq(true), any()))
                .thenReturn(new SliceImpl<>(List.of(liveListing()), PageRequest.of(0, 20), false));

        PublicMarketplaceListingPageResponse result =
                publicMarketplaceService.listListingsPage(null, null, null, BusinessType.RETAILER, 0, 20);

        assertThat(result.items()).hasSize(1);
    }

    @Test
    void listListingsPage_wholesalers_passesWholesalerAndDoesNotCountUntypedAccounts() {
        stubTenantAndCategory();
        when(listingRepository.findLiveListingsPage(eq(MarketplaceListingStatus.LIVE), isNull(), isNull(), eq(BusinessType.WHOLESALER), eq(false), any()))
                .thenReturn(new SliceImpl<>(List.of(liveListing()), PageRequest.of(0, 20), false));

        PublicMarketplaceListingPageResponse result =
                publicMarketplaceService.listListingsPage(null, null, null, BusinessType.WHOLESALER, 0, 20);

        assertThat(result.items()).hasSize(1);
    }

    @Test
    void listListingsPage_noCityFilter_passesNullTenantIdsAndReturnsSlice() {
        stubTenantAndCategory();
        when(listingRepository.findLiveListingsPage(eq(MarketplaceListingStatus.LIVE), isNull(), isNull(), isNull(), anyBoolean(), any()))
                .thenReturn(new SliceImpl<>(List.of(liveListing()), PageRequest.of(0, 20), true));

        PublicMarketplaceListingPageResponse result = publicMarketplaceService.listListingsPage(null, null, null, null, 0, 20);

        assertThat(result.items()).hasSize(1);
        assertThat(result.hasMore()).isTrue();
    }

    @Test
    void listListingsPage_withCityOnly_resolvesTenantsByCityNotArea() {
        UUID cityId = UUID.randomUUID();
        stubTenantAndCategory();
        Tenant tenantInCity = new Tenant();
        tenantInCity.setId(tenantId);
        when(tenantRepository.findAllByPlatformFalseAndActiveTrueAndCityId(cityId)).thenReturn(List.of(tenantInCity));
        when(listingRepository.findLiveListingsPage(eq(MarketplaceListingStatus.LIVE), isNull(), eq(List.of(tenantId)), isNull(), anyBoolean(), any()))
                .thenReturn(new SliceImpl<>(List.of(liveListing()), PageRequest.of(0, 20), false));

        PublicMarketplaceListingPageResponse result = publicMarketplaceService.listListingsPage(null, cityId, null, null, 0, 20);

        assertThat(result.items()).hasSize(1);
        assertThat(result.hasMore()).isFalse();
        verify(tenantRepository, never()).findAllByPlatformFalseAndActiveTrueAndCityIdAndAreaId(any(), any());
    }

    @Test
    void listListingsPage_withCityAndArea_resolvesTenantsByBoth() {
        UUID cityId = UUID.randomUUID();
        UUID areaId = UUID.randomUUID();
        Tenant tenantInArea = new Tenant();
        tenantInArea.setId(tenantId);
        when(tenantRepository.findAllByPlatformFalseAndActiveTrueAndCityIdAndAreaId(cityId, areaId))
                .thenReturn(List.of(tenantInArea));
        when(listingRepository.findLiveListingsPage(eq(MarketplaceListingStatus.LIVE), isNull(), eq(List.of(tenantId)), isNull(), anyBoolean(), any()))
                .thenReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 20), false));

        PublicMarketplaceListingPageResponse result =
                publicMarketplaceService.listListingsPage(null, cityId, areaId, null, 0, 20);

        assertThat(result.items()).isEmpty();
    }

    @Test
    void listListingsPage_cityWithNoTenants_returnsEmptyPageWithoutQueryingListings() {
        UUID cityId = UUID.randomUUID();
        when(tenantRepository.findAllByPlatformFalseAndActiveTrueAndCityId(cityId)).thenReturn(List.of());

        PublicMarketplaceListingPageResponse result = publicMarketplaceService.listListingsPage(null, cityId, null, null, 0, 20);

        assertThat(result.items()).isEmpty();
        assertThat(result.hasMore()).isFalse();
        verify(listingRepository, never()).findLiveListingsPage(any(), any(), any(), any(), anyBoolean(), any());
    }

    @Test
    void getListing_liveListing_returnsResolvedResponse() {
        stubTenantAndCategory();
        when(listingRepository.findByTenantId(tenantId)).thenReturn(Optional.of(liveListing()));

        PublicMarketplaceListingResponse result = publicMarketplaceService.getListing(tenantId);

        assertThat(result.tenantId()).isEqualTo(tenantId);
        assertThat(result.businessName()).isEqualTo("Veera Collections");
        assertThat(result.imageProfile()).isEqualTo(ImageProfile.COMPACT);
        assertThat(result.bookable()).isFalse();
    }

    @Test
    void getListing_bookableCategory_exposesBookable() {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setBusinessName("Glow Salon");
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        MarketplaceCategory category = new MarketplaceCategory();
        category.setId(categoryId);
        category.setName("Salon & Spa");
        category.setBookable(true);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(listingRepository.findByTenantId(tenantId)).thenReturn(Optional.of(liveListing()));

        assertThat(publicMarketplaceService.getListing(tenantId).bookable()).isTrue();
    }

    @Test
    void getListing_detailCategory_exposesTheImageProfile() {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setBusinessName("Veera Collections");
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        MarketplaceCategory category = new MarketplaceCategory();
        category.setId(categoryId);
        category.setName("Ladies Wear");
        category.setImageProfile(ImageProfile.DETAIL);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(listingRepository.findByTenantId(tenantId)).thenReturn(Optional.of(liveListing()));

        assertThat(publicMarketplaceService.getListing(tenantId).imageProfile()).isEqualTo(ImageProfile.DETAIL);
    }

    @Test
    void listListingsPage_tenantSuspended_excludesTheirListing() {
        Tenant suspendedTenant = new Tenant();
        suspendedTenant.setId(tenantId);
        suspendedTenant.setBusinessName("Veera Collections");
        suspendedTenant.setSubscriptionStatus(com.xerxes.msme.subscription.SubscriptionStatus.SUSPENDED);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(suspendedTenant));
        when(listingRepository.findLiveListingsPage(eq(MarketplaceListingStatus.LIVE), isNull(), isNull(), isNull(), anyBoolean(), any()))
                .thenReturn(new SliceImpl<>(List.of(liveListing()), PageRequest.of(0, 20), false));

        PublicMarketplaceListingPageResponse result = publicMarketplaceService.listListingsPage(null, null, null, null, 0, 20);

        assertThat(result.items()).isEmpty();
    }

    @Test
    void getListing_tenantSuspended_throwsResourceNotFoundWithoutLeakingWhy() {
        Tenant suspendedTenant = new Tenant();
        suspendedTenant.setId(tenantId);
        suspendedTenant.setSubscriptionStatus(com.xerxes.msme.subscription.SubscriptionStatus.SUSPENDED);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(suspendedTenant));
        when(listingRepository.findByTenantId(tenantId)).thenReturn(Optional.of(liveListing()));

        assertThrows(ResourceNotFoundException.class, () -> publicMarketplaceService.getListing(tenantId));
    }

    @Test
    void getListing_notLive_throwsResourceNotFound() {
        MarketplaceListing draft = liveListing();
        draft.setStatus(MarketplaceListingStatus.DRAFT);
        when(listingRepository.findByTenantId(tenantId)).thenReturn(Optional.of(draft));

        assertThrows(ResourceNotFoundException.class, () -> publicMarketplaceService.getListing(tenantId));
    }

    @Test
    void getListing_unknownTenant_throwsResourceNotFound() {
        when(listingRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> publicMarketplaceService.getListing(tenantId));
    }

    @Test
    void getDefaultCatalog_returnsActiveSubCategoriesWithActiveProductsNested() {
        UUID marketplaceCategoryId = UUID.randomUUID();
        DefaultProductCategory rice = new DefaultProductCategory();
        rice.setId(UUID.randomUUID());
        rice.setMarketplaceCategoryId(marketplaceCategoryId);
        rice.setName("Rice");
        when(defaultProductCategoryRepository.findAllByMarketplaceCategoryIdAndActiveTrueOrderByDisplayOrderAsc(marketplaceCategoryId))
                .thenReturn(List.of(rice));

        DefaultProduct kohinoor = new DefaultProduct();
        kohinoor.setId(UUID.randomUUID());
        kohinoor.setDefaultProductCategoryId(rice.getId());
        kohinoor.setName("Kohinoor Basmati Rice");
        when(defaultProductRepository.findAllByDefaultProductCategoryIdAndActiveTrueOrderByDisplayOrderAsc(rice.getId()))
                .thenReturn(List.of(kohinoor));

        var result = publicMarketplaceService.getDefaultCatalog(marketplaceCategoryId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Rice");
        assertThat(result.get(0).products()).hasSize(1);
        assertThat(result.get(0).products().get(0).name()).isEqualTo("Kohinoor Basmati Rice");
    }

    @Test
    void listAdvertisements_onlyTheCurrentlyLiveOnesArePopulatedPerSlot() {
        Advertisement topAd = new Advertisement();
        topAd.setSlot(AdvertisementSlot.TOP_CAROUSEL);
        topAd.setImagePath("advertisements/top.jpg");
        topAd.setAltText("Top banner");
        when(advertisementRepository
                        .findFirstBySlotAndPublishedTrueAndStartsAtLessThanEqualAndEndsAtGreaterThanEqualOrderByStartsAtDesc(
                                eq(AdvertisementSlot.TOP_CAROUSEL), any(), any()))
                .thenReturn(Optional.of(topAd));
        when(advertisementRepository
                        .findFirstBySlotAndPublishedTrueAndStartsAtLessThanEqualAndEndsAtGreaterThanEqualOrderByStartsAtDesc(
                                eq(AdvertisementSlot.LEFT_RAIL), any(), any()))
                .thenReturn(Optional.empty());
        when(advertisementRepository
                        .findFirstBySlotAndPublishedTrueAndStartsAtLessThanEqualAndEndsAtGreaterThanEqualOrderByStartsAtDesc(
                                eq(AdvertisementSlot.RIGHT_RAIL), any(), any()))
                .thenReturn(Optional.empty());
        when(imageStorageService.mediaUrl("advertisements/top.jpg")).thenReturn("/media/images/advertisements/top.jpg");

        var result = publicMarketplaceService.listAdvertisements();

        assertThat(result.topCarousel()).isNotNull();
        assertThat(result.topCarousel().imageUrl()).isEqualTo("/media/images/advertisements/top.jpg");
        assertThat(result.topCarousel().altText()).isEqualTo("Top banner");
        assertThat(result.leftRail()).isNull();
        assertThat(result.rightRail()).isNull();
    }

    @Test
    void listServiceStates_returnsOnlyActiveOnes() {
        ServiceState maharashtra = new ServiceState();
        maharashtra.setCode("27");
        maharashtra.setName("Maharashtra");
        when(serviceStateRepository.findAllByActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of(maharashtra));

        var result = publicMarketplaceService.listServiceStates();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("27");
        assertThat(result.get(0).name()).isEqualTo("Maharashtra");
    }

    @Test
    void listServiceCities_returnsOnlyActiveOnesForTheGivenState() {
        ServiceCity pune = new ServiceCity();
        pune.setId(UUID.randomUUID());
        pune.setStateCode("27");
        pune.setName("Pune");
        when(serviceCityRepository.findAllByStateCodeAndActiveTrueOrderByDisplayOrderAsc("27")).thenReturn(List.of(pune));

        var result = publicMarketplaceService.listServiceCities("27");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Pune");
    }

    @Test
    void listServiceCities_withNoStateCode_returnsEveryStatesCities() {
        ServiceCity pune = new ServiceCity();
        pune.setId(UUID.randomUUID());
        pune.setName("Pune");
        when(serviceCityRepository.findAllByActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of(pune));

        var result = publicMarketplaceService.listServiceCities(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Pune");
        verify(serviceCityRepository, never()).findAllByStateCodeAndActiveTrueOrderByDisplayOrderAsc(any());
    }

    @Test
    void listServiceAreas_returnsOnlyActiveOnesForTheGivenCity() {
        UUID cityId = UUID.randomUUID();
        ServiceArea nigdi = new ServiceArea();
        nigdi.setId(UUID.randomUUID());
        nigdi.setCityId(cityId);
        nigdi.setName("Nigdi");
        nigdi.setPincode("411044");
        when(serviceAreaRepository.findAllByCityIdAndActiveTrueOrderByDisplayOrderAsc(cityId)).thenReturn(List.of(nigdi));

        var result = publicMarketplaceService.listServiceAreas(cityId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Nigdi");
        assertThat(result.get(0).pincode()).isEqualTo("411044");
    }

    private Tenant activeTenant(UUID cityId) {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setBusinessName("Veera Collections");
        tenant.setActive(true);
        tenant.setCityId(cityId);
        return tenant;
    }

    private FeedPost approvedPost() {
        FeedPost post = new FeedPost();
        post.setId(UUID.randomUUID());
        post.setTenantId(tenantId);
        post.setContent("50% off this week!");
        post.setStatus(FeedPostStatus.APPROVED);
        post.setCreatedAt(OffsetDateTime.now().minusDays(1));
        return post;
    }

    @Test
    void listPublicFeed_tenantHasALiveListing_returnsThePost() {
        UUID cityId = UUID.randomUUID();
        when(tenantRepository.findAllByPlatformFalseAndActiveTrueAndCityId(cityId)).thenReturn(List.of(activeTenant(cityId)));
        MarketplaceListing listing = liveListing();
        listing.setEnquiryWhatsapp("919876543210");
        when(listingRepository.findAllByTenantIdInAndStatus(List.of(tenantId), MarketplaceListingStatus.LIVE))
                .thenReturn(List.of(listing));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(activeTenant(cityId)));
        MarketplaceCategory category = new MarketplaceCategory();
        category.setId(categoryId);
        category.setName("Ladies Wear");
        when(categoryRepository.findAllById(List.of(categoryId))).thenReturn(List.of(category));
        FeedPost post = approvedPost();
        when(feedPostRepository.findPublicFeedPage(
                        eq(List.of(tenantId)), eq(FeedPostStatus.APPROVED), any(), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(post)));

        PublicFeedPageResponse result = publicMarketplaceService.listPublicFeed(cityId, null, null, null, FeedSort.NEWEST, 0, 20);

        assertThat(result.items()).hasSize(1);
        assertThat(result.hasMore()).isFalse();
        assertThat(result.items().get(0).businessName()).isEqualTo("Veera Collections");
        assertThat(result.items().get(0).content()).isEqualTo("50% off this week!");
        assertThat(result.items().get(0).area()).isEqualTo("Gujar Nagar");
        assertThat(result.items().get(0).categoryName()).isEqualTo("Ladies Wear");
        assertThat(result.items().get(0).enquiryWhatsapp()).isEqualTo("919876543210");
    }

    @Test
    void listPublicFeed_postsCarryTheirBusinessType_andAnUntypedAccountIsReportedAsRetailer() {
        UUID cityId = UUID.randomUUID();
        Tenant wholesaler = activeTenant(cityId);
        wholesaler.setBusinessType(BusinessType.WHOLESALER);
        when(tenantRepository.findAllByPlatformFalseAndActiveTrueAndCityId(cityId)).thenReturn(List.of(wholesaler));
        when(listingRepository.findAllByTenantIdInAndStatus(List.of(tenantId), MarketplaceListingStatus.LIVE))
                .thenReturn(List.of(liveListing()));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(wholesaler));
        when(feedPostRepository.findPublicFeedPage(
                        eq(List.of(tenantId)), eq(FeedPostStatus.APPROVED), any(), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(approvedPost())));

        assertThat(publicMarketplaceService.listPublicFeed(cityId, null, null, null, FeedSort.NEWEST, 0, 20)
                        .items().get(0).businessType())
                .isEqualTo(BusinessType.WHOLESALER);

        wholesaler.setBusinessType(null);
        assertThat(publicMarketplaceService.listPublicFeed(cityId, null, null, null, FeedSort.NEWEST, 0, 20)
                        .items().get(0).businessType())
                .isEqualTo(BusinessType.RETAILER);
    }

    @Test
    void listPublicFeed_wholesalerFilter_dropsRetailersAndUntypedAccounts() {
        UUID cityId = UUID.randomUUID();
        Tenant untyped = activeTenant(cityId);
        untyped.setBusinessType(null);
        Tenant retailer = activeTenant(cityId);
        retailer.setBusinessType(BusinessType.RETAILER);
        when(tenantRepository.findAllByPlatformFalseAndActiveTrueAndCityId(cityId)).thenReturn(List.of(untyped, retailer));

        PublicFeedPageResponse result =
                publicMarketplaceService.listPublicFeed(cityId, null, null, BusinessType.WHOLESALER, FeedSort.NEWEST, 0, 20);

        assertThat(result.items()).isEmpty();
        verify(feedPostRepository, never()).findPublicFeedPage(any(), any(), any(), any(), any());
    }

    @Test
    void listPublicFeed_retailerFilter_keepsUntypedAccountsAndDropsWholesalers() {
        UUID cityId = UUID.randomUUID();
        Tenant untyped = activeTenant(cityId);
        untyped.setBusinessType(null);
        Tenant wholesaler = activeTenant(cityId);
        wholesaler.setId(UUID.randomUUID());
        wholesaler.setBusinessType(BusinessType.WHOLESALER);
        when(tenantRepository.findAllByPlatformFalseAndActiveTrueAndCityId(cityId)).thenReturn(List.of(untyped, wholesaler));
        when(listingRepository.findAllByTenantIdInAndStatus(List.of(tenantId), MarketplaceListingStatus.LIVE))
                .thenReturn(List.of(liveListing()));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(untyped));
        when(feedPostRepository.findPublicFeedPage(
                        eq(List.of(tenantId)), eq(FeedPostStatus.APPROVED), any(), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(approvedPost())));

        PublicFeedPageResponse result =
                publicMarketplaceService.listPublicFeed(cityId, null, null, BusinessType.RETAILER, FeedSort.NEWEST, 0, 20);

        assertThat(result.items()).hasSize(1);
    }

    @Test
    void listPublicFeed_noTenantsInThatCity_returnsEmptyWithoutQueryingFeedPosts() {
        UUID cityId = UUID.randomUUID();
        when(tenantRepository.findAllByPlatformFalseAndActiveTrueAndCityId(cityId)).thenReturn(List.of());

        PublicFeedPageResponse result = publicMarketplaceService.listPublicFeed(cityId, null, null, null, FeedSort.NEWEST, 0, 20);

        assertThat(result.items()).isEmpty();
        verify(feedPostRepository, never())
                .findPublicFeedPage(any(), any(), any(), any(), any());
    }

    @Test
    void listPublicFeed_tenantHasNoLiveListing_excludedEvenIfActive() {
        UUID cityId = UUID.randomUUID();
        when(tenantRepository.findAllByPlatformFalseAndActiveTrueAndCityId(cityId)).thenReturn(List.of(activeTenant(cityId)));
        when(listingRepository.findAllByTenantIdInAndStatus(List.of(tenantId), MarketplaceListingStatus.LIVE))
                .thenReturn(List.of());

        PublicFeedPageResponse result = publicMarketplaceService.listPublicFeed(cityId, null, null, null, FeedSort.NEWEST, 0, 20);

        assertThat(result.items()).isEmpty();
        verify(feedPostRepository, never())
                .findPublicFeedPage(any(), any(), any(), any(), any());
    }

    @Test
    void listPublicFeed_tenantSuspended_excludedEvenWithALiveListing() {
        UUID cityId = UUID.randomUUID();
        when(tenantRepository.findAllByPlatformFalseAndActiveTrueAndCityId(cityId)).thenReturn(List.of(activeTenant(cityId)));
        when(listingRepository.findAllByTenantIdInAndStatus(List.of(tenantId), MarketplaceListingStatus.LIVE))
                .thenReturn(List.of(liveListing()));
        Tenant suspended = activeTenant(cityId);
        suspended.setSubscriptionStatus(SubscriptionStatus.SUSPENDED);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(suspended));

        PublicFeedPageResponse result = publicMarketplaceService.listPublicFeed(cityId, null, null, null, FeedSort.NEWEST, 0, 20);

        assertThat(result.items()).isEmpty();
    }

    @Test
    void listPublicFeed_withAreaId_filtersByCityAndArea() {
        UUID cityId = UUID.randomUUID();
        UUID areaId = UUID.randomUUID();
        when(tenantRepository.findAllByPlatformFalseAndActiveTrueAndCityIdAndAreaId(cityId, areaId))
                .thenReturn(List.of());

        PublicFeedPageResponse result = publicMarketplaceService.listPublicFeed(cityId, areaId, null, null, FeedSort.NEWEST, 0, 20);

        assertThat(result.items()).isEmpty();
        verify(tenantRepository, never()).findAllByPlatformFalseAndActiveTrueAndCityId(any());
    }

    @Test
    void listPublicFeed_categoryFilter_excludesShopsListedInOtherCategories() {
        UUID cityId = UUID.randomUUID();
        when(tenantRepository.findAllByPlatformFalseAndActiveTrueAndCityId(cityId)).thenReturn(List.of(activeTenant(cityId)));
        when(listingRepository.findAllByTenantIdInAndStatus(List.of(tenantId), MarketplaceListingStatus.LIVE))
                .thenReturn(List.of(liveListing()));

        PublicFeedPageResponse result = publicMarketplaceService.listPublicFeed(cityId, null, UUID.randomUUID(), null, FeedSort.NEWEST, 0, 20);

        assertThat(result.items()).isEmpty();
        verify(feedPostRepository, never())
                .findPublicFeedPage(any(), any(), any(), any(), any());
    }

    @Test
    void listPublicFeed_categoryFilter_keepsShopsListedInThatCategory() {
        UUID cityId = UUID.randomUUID();
        when(tenantRepository.findAllByPlatformFalseAndActiveTrueAndCityId(cityId)).thenReturn(List.of(activeTenant(cityId)));
        when(listingRepository.findAllByTenantIdInAndStatus(List.of(tenantId), MarketplaceListingStatus.LIVE))
                .thenReturn(List.of(liveListing()));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(activeTenant(cityId)));
        when(feedPostRepository.findPublicFeedPage(
                        eq(List.of(tenantId)), eq(FeedPostStatus.APPROVED), any(), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(approvedPost())));

        PublicFeedPageResponse result = publicMarketplaceService.listPublicFeed(cityId, null, categoryId, null, FeedSort.NEWEST, 0, 20);

        assertThat(result.items()).hasSize(1);
    }

    @Test
    void listPublicFeed_passesPageAndSizeToTheQuery_andReportsHasMore() {
        UUID cityId = UUID.randomUUID();
        when(tenantRepository.findAllByPlatformFalseAndActiveTrueAndCityId(cityId)).thenReturn(List.of(activeTenant(cityId)));
        when(listingRepository.findAllByTenantIdInAndStatus(List.of(tenantId), MarketplaceListingStatus.LIVE))
                .thenReturn(List.of(liveListing()));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(activeTenant(cityId)));
        when(feedPostRepository.findPublicFeedPage(
                        eq(List.of(tenantId)), eq(FeedPostStatus.APPROVED), any(), any(LocalDate.class), eq(PageRequest.of(2, 10, FeedSort.NEWEST.sort()))))
                .thenReturn(new SliceImpl<>(List.of(approvedPost()), PageRequest.of(2, 10, FeedSort.NEWEST.sort()), true));

        PublicFeedPageResponse result = publicMarketplaceService.listPublicFeed(cityId, null, null, null, FeedSort.NEWEST, 2, 10);

        assertThat(result.items()).hasSize(1);
        assertThat(result.hasMore()).isTrue();
    }

    @Test
    void listPublicFeed_returnsThePostsDiscountLabelAndEndDate() {
        UUID cityId = UUID.randomUUID();
        when(tenantRepository.findAllByPlatformFalseAndActiveTrueAndCityId(cityId)).thenReturn(List.of(activeTenant(cityId)));
        when(listingRepository.findAllByTenantIdInAndStatus(List.of(tenantId), MarketplaceListingStatus.LIVE))
                .thenReturn(List.of(liveListing()));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(activeTenant(cityId)));
        FeedPost post = approvedPost();
        post.setDiscountLabel("20% off");
        post.setValidUntil(LocalDate.now().plusDays(3));
        when(feedPostRepository.findPublicFeedPage(
                        eq(List.of(tenantId)), eq(FeedPostStatus.APPROVED), any(), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(post)));

        PublicFeedPageResponse result = publicMarketplaceService.listPublicFeed(cityId, null, null, null, FeedSort.NEWEST, 0, 20);

        assertThat(result.items().get(0).discountLabel()).isEqualTo("20% off");
        assertThat(result.items().get(0).validUntil()).isEqualTo(LocalDate.now().plusDays(3));
    }

    @Test
    void listPublicFeed_endingSoon_asksTheRepositoryForEndDateOrder() {
        UUID cityId = UUID.randomUUID();
        when(tenantRepository.findAllByPlatformFalseAndActiveTrueAndCityId(cityId)).thenReturn(List.of(activeTenant(cityId)));
        when(listingRepository.findAllByTenantIdInAndStatus(List.of(tenantId), MarketplaceListingStatus.LIVE))
                .thenReturn(List.of(liveListing()));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(activeTenant(cityId)));
        when(feedPostRepository.findPublicFeedPage(
                        eq(List.of(tenantId)),
                        eq(FeedPostStatus.APPROVED),
                        any(),
                        any(LocalDate.class),
                        eq(PageRequest.of(0, 20, FeedSort.ENDING_SOON.sort()))))
                .thenReturn(new SliceImpl<>(List.of(approvedPost())));

        PublicFeedPageResponse result = publicMarketplaceService.listPublicFeed(cityId, null, null, null, FeedSort.ENDING_SOON, 0, 20);

        assertThat(result.items()).hasSize(1);
    }

    @Test
    void feedSort_newestOrdersByCreatedAtDescending() {
        assertThat(FeedSort.NEWEST.sort().toList())
                .extracting(org.springframework.data.domain.Sort.Order::getProperty)
                .containsExactly("createdAt");
        assertThat(FeedSort.NEWEST.sort().getOrderFor("createdAt").isDescending()).isTrue();
    }

    @Test
    void feedSort_endingSoonPutsEndDatesFirstSoonestFirst_thenNewestForTheRest() {
        var orders = FeedSort.ENDING_SOON.sort().toList();

        assertThat(orders).extracting(org.springframework.data.domain.Sort.Order::getProperty)
                .containsExactly("validUntil", "createdAt");
        assertThat(orders.get(0).isAscending()).isTrue();
        assertThat(orders.get(0).getNullHandling()).isEqualTo(org.springframework.data.domain.Sort.NullHandling.NULLS_LAST);
        assertThat(orders.get(1).isDescending()).isTrue();
    }
}
