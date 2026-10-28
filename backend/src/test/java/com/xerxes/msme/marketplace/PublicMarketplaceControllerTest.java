package com.xerxes.msme.marketplace;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.xerxes.msme.tenant.BusinessType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PublicMarketplaceController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicMarketplaceControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private PublicMarketplaceService publicMarketplaceService;

    private final UUID tenantId = UUID.randomUUID();

    private PublicMarketplaceListingResponse response() {
        return new PublicMarketplaceListingResponse(
                tenantId, "Veera Collections", null, "Ladies Wear", "Gujar Nagar", null, null, null, null, null, null,
                null, null, "919876543210", MarketplaceOrderMode.WHATSAPP);
    }

    @Test
    void categories_returnsCategoriesFromService() throws Exception {
        when(publicMarketplaceService.listCategories())
                .thenReturn(List.of(new MarketplaceCategoryResponse(UUID.randomUUID(), "Ladies Wear", "icon")));

        mockMvc.perform(get("/api/public/marketplace/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Ladies Wear"));
    }

    @Test
    void listings_noParams_defaultsToFirstPageOfTwenty() throws Exception {
        when(publicMarketplaceService.listListingsPage(null, null, null, null, 0, 20))
                .thenReturn(new PublicMarketplaceListingPageResponse(List.of(response()), false));

        mockMvc.perform(get("/api/public/marketplace/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].businessName").value("Veera Collections"))
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    void listings_withBusinessType_passesItThrough() throws Exception {
        when(publicMarketplaceService.listListingsPage(null, null, null, BusinessType.WHOLESALER, 0, 20))
                .thenReturn(new PublicMarketplaceListingPageResponse(List.of(response()), false));

        mockMvc.perform(get("/api/public/marketplace/listings").param("businessType", "WHOLESALER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].businessName").value("Veera Collections"));
    }

    @Test
    void listings_withUnknownBusinessType_returns400() throws Exception {
        mockMvc.perform(get("/api/public/marketplace/listings").param("businessType", "SUPPLIER"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listings_withCityAreaCategoryAndPagingParams_passesThemAllThrough() throws Exception {
        UUID categoryId = UUID.randomUUID();
        UUID cityId = UUID.randomUUID();
        UUID areaId = UUID.randomUUID();
        when(publicMarketplaceService.listListingsPage(categoryId, cityId, areaId, null, 2, 10))
                .thenReturn(new PublicMarketplaceListingPageResponse(List.of(), true));

        mockMvc.perform(get("/api/public/marketplace/listings")
                        .param("categoryId", categoryId.toString())
                        .param("cityId", cityId.toString())
                        .param("areaId", areaId.toString())
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasMore").value(true));
    }

    @Test
    void listings_sizeAboveMax_isCappedAtFifty() throws Exception {
        when(publicMarketplaceService.listListingsPage(null, null, null, null, 0, 50))
                .thenReturn(new PublicMarketplaceListingPageResponse(List.of(), false));

        mockMvc.perform(get("/api/public/marketplace/listings").param("size", "500")).andExpect(status().isOk());
    }

    @Test
    void listing_delegatesToServiceByTenantId() throws Exception {
        when(publicMarketplaceService.getListing(tenantId)).thenReturn(response());

        mockMvc.perform(get("/api/public/marketplace/listings/" + tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(tenantId.toString()));
    }

    @Test
    void defaultCatalog_delegatesToServiceByMarketplaceCategoryId() throws Exception {
        UUID marketplaceCategoryId = UUID.randomUUID();
        DefaultProductCatalogEntryResponse entry = new DefaultProductCatalogEntryResponse(
                UUID.randomUUID(),
                "Rice",
                List.of(new DefaultProductOptionResponse(UUID.randomUUID(), "Kohinoor Basmati Rice", null)));
        when(publicMarketplaceService.getDefaultCatalog(marketplaceCategoryId)).thenReturn(List.of(entry));

        mockMvc.perform(get("/api/public/marketplace/default-catalog").param("marketplaceCategoryId", marketplaceCategoryId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Rice"))
                .andExpect(jsonPath("$[0].products[0].name").value("Kohinoor Basmati Rice"));
    }

    @Test
    void advertisements_delegatesToService() throws Exception {
        when(publicMarketplaceService.listAdvertisements())
                .thenReturn(new PublicAdvertisementsResponse(
                        new AdvertisementResponse("/media/images/advertisements/top.jpg", null, "Top banner", null, null),
                        null,
                        null));

        mockMvc.perform(get("/api/public/marketplace/advertisements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topCarousel.imageUrl").value("/media/images/advertisements/top.jpg"))
                .andExpect(jsonPath("$.leftRail").doesNotExist())
                .andExpect(jsonPath("$.rightRail").doesNotExist());
    }

    @Test
    void serviceStates_delegatesToService() throws Exception {
        when(publicMarketplaceService.listServiceStates())
                .thenReturn(List.of(new ServiceStateResponse("27", "Maharashtra")));

        mockMvc.perform(get("/api/public/marketplace/service-states"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("27"))
                .andExpect(jsonPath("$[0].name").value("Maharashtra"));
    }

    @Test
    void serviceCities_delegatesToServiceByStateCode() throws Exception {
        when(publicMarketplaceService.listServiceCities("27"))
                .thenReturn(List.of(new ServiceCityResponse(UUID.randomUUID(), "Pune")));

        mockMvc.perform(get("/api/public/marketplace/service-cities").param("stateCode", "27"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Pune"));
    }

    @Test
    void serviceCities_withNoStateCode_delegatesToServiceForEveryState() throws Exception {
        when(publicMarketplaceService.listServiceCities(null))
                .thenReturn(List.of(new ServiceCityResponse(UUID.randomUUID(), "Pune")));

        mockMvc.perform(get("/api/public/marketplace/service-cities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Pune"));
    }

    @Test
    void serviceAreas_delegatesToServiceByCityId() throws Exception {
        UUID cityId = UUID.randomUUID();
        when(publicMarketplaceService.listServiceAreas(cityId))
                .thenReturn(List.of(new ServiceAreaResponse(UUID.randomUUID(), "Nigdi", "411044")));

        mockMvc.perform(get("/api/public/marketplace/service-areas").param("cityId", cityId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Nigdi"))
                .andExpect(jsonPath("$[0].pincode").value("411044"));
    }

    @Test
    void feed_withBusinessType_passesItThrough() throws Exception {
        UUID cityId = UUID.randomUUID();
        when(publicMarketplaceService.listPublicFeed(cityId, null, null, BusinessType.WHOLESALER, FeedSort.NEWEST, 0, 20))
                .thenReturn(new PublicFeedPageResponse(List.of(), false));

        mockMvc.perform(get("/api/public/marketplace/feed")
                        .param("cityId", cityId.toString())
                        .param("businessType", "WHOLESALER"))
                .andExpect(status().isOk());
    }

    @Test
    void feed_withUnknownBusinessType_returns400() throws Exception {
        mockMvc.perform(get("/api/public/marketplace/feed")
                        .param("cityId", UUID.randomUUID().toString())
                        .param("businessType", "SUPPLIER"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void feed_delegatesToServiceWithCityAndArea() throws Exception {
        UUID cityId = UUID.randomUUID();
        UUID areaId = UUID.randomUUID();
        when(publicMarketplaceService.listPublicFeed(cityId, areaId, null, null, FeedSort.NEWEST, 0, 20))
                .thenReturn(new PublicFeedPageResponse(
                        List.of(new PublicFeedPostResponse(
                                UUID.randomUUID(), "Veera Collections", null, "Nigdi", "Ladies Wear", BusinessType.WHOLESALER, "919876543210",
                                "50% off!", "20% off", java.time.LocalDate.of(2030, 1, 15),
                                java.time.OffsetDateTime.now())),
                        true));

        mockMvc.perform(get("/api/public/marketplace/feed")
                        .param("cityId", cityId.toString())
                        .param("areaId", areaId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].businessName").value("Veera Collections"))
                .andExpect(jsonPath("$.items[0].content").value("50% off!"))
                .andExpect(jsonPath("$.items[0].categoryName").value("Ladies Wear"))
                .andExpect(jsonPath("$.items[0].businessType").value("WHOLESALER"))
                .andExpect(jsonPath("$.items[0].enquiryWhatsapp").value("919876543210"))
                .andExpect(jsonPath("$.items[0].discountLabel").value("20% off"))
                .andExpect(jsonPath("$.items[0].validUntil").value("2030-01-15"))
                .andExpect(jsonPath("$.hasMore").value(true));
    }

    @Test
    void feed_withoutAreaId_stillWorks() throws Exception {
        UUID cityId = UUID.randomUUID();
        when(publicMarketplaceService.listPublicFeed(cityId, null, null, null, FeedSort.NEWEST, 0, 20))
                .thenReturn(new PublicFeedPageResponse(List.of(), false));

        mockMvc.perform(get("/api/public/marketplace/feed").param("cityId", cityId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    void feed_withCategoryAndPagingParams_passesThemThrough() throws Exception {
        UUID cityId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        when(publicMarketplaceService.listPublicFeed(cityId, null, categoryId, null, FeedSort.NEWEST, 3, 10))
                .thenReturn(new PublicFeedPageResponse(List.of(), true));

        mockMvc.perform(get("/api/public/marketplace/feed")
                        .param("cityId", cityId.toString())
                        .param("categoryId", categoryId.toString())
                        .param("page", "3")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasMore").value(true));
    }

    @Test
    void feed_sizeAboveMax_isCappedAtFifty_andNegativePageFloorsToZero() throws Exception {
        UUID cityId = UUID.randomUUID();
        when(publicMarketplaceService.listPublicFeed(cityId, null, null, null, FeedSort.NEWEST, 0, 50))
                .thenReturn(new PublicFeedPageResponse(List.of(), false));

        mockMvc.perform(get("/api/public/marketplace/feed")
                        .param("cityId", cityId.toString())
                        .param("size", "500")
                        .param("page", "-4"))
                .andExpect(status().isOk());
    }

    @Test
    void feed_missingCityId_isRejected() throws Exception {
        mockMvc.perform(get("/api/public/marketplace/feed")).andExpect(status().isBadRequest());
    }

    @Test
    void feed_sortEndingSoon_isPassedThrough() throws Exception {
        UUID cityId = UUID.randomUUID();
        when(publicMarketplaceService.listPublicFeed(cityId, null, null, null, FeedSort.ENDING_SOON, 0, 20))
                .thenReturn(new PublicFeedPageResponse(List.of(), false));

        mockMvc.perform(get("/api/public/marketplace/feed")
                        .param("cityId", cityId.toString())
                        .param("sort", "ENDING_SOON"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    void feed_unknownSort_isRejected() throws Exception {
        mockMvc.perform(get("/api/public/marketplace/feed")
                        .param("cityId", UUID.randomUUID().toString())
                        .param("sort", "cheapest"))
                .andExpect(status().isBadRequest());
    }
}
