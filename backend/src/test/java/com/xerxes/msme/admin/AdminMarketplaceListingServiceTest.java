package com.xerxes.msme.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xerxes.msme.admin.dto.AdminMarketplaceListingResponse;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.marketplace.MarketplaceCategoryRepository;
import com.xerxes.msme.marketplace.MarketplaceListing;
import com.xerxes.msme.marketplace.MarketplaceListingRepository;
import com.xerxes.msme.marketplace.MarketplaceListingStatus;
import com.xerxes.msme.marketplace.MarketplaceOrderMode;
import com.xerxes.msme.partner.PartnerScope;
import com.xerxes.msme.partner.ReviewActionType;
import com.xerxes.msme.storage.ImageStorageService;
import com.xerxes.msme.tenant.Tenant;
import com.xerxes.msme.tenant.TenantRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminMarketplaceListingServiceTest {

    @Mock private MarketplaceListingRepository listingRepository;
    @Mock private MarketplaceCategoryRepository categoryRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private ImageStorageService imageStorageService;
    @Mock private PartnerScope partnerScope;

    private AdminMarketplaceListingService adminMarketplaceListingService;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        adminMarketplaceListingService = new AdminMarketplaceListingService(
                listingRepository, categoryRepository, tenantRepository, imageStorageService, partnerScope);
        tenantId = UUID.randomUUID();

        lenient().when(listingRepository.save(any(MarketplaceListing.class))).thenAnswer(inv -> inv.getArgument(0));
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setBusinessName("Veera Collections");
        lenient().when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
    }

    private MarketplaceListing pendingListing() {
        MarketplaceListing listing = new MarketplaceListing();
        listing.setId(UUID.randomUUID());
        listing.setTenantId(tenantId);
        listing.setStatus(MarketplaceListingStatus.PENDING);
        listing.setOrderMode(MarketplaceOrderMode.WHATSAPP);
        return listing;
    }

    private MarketplaceListing liveListing() {
        MarketplaceListing listing = pendingListing();
        listing.setStatus(MarketplaceListingStatus.LIVE);
        return listing;
    }

    @Test
    void list_asAPartner_returnsOnlyTheListingsOfTheirShops() {
        when(partnerScope.ownedTenantIds()).thenReturn(Optional.of(List.of(tenantId)));
        when(listingRepository.findAllByTenantIdInAndStatusOrderByCreatedAtDesc(List.of(tenantId), MarketplaceListingStatus.PENDING))
                .thenReturn(List.of(pendingListing()));

        assertThat(adminMarketplaceListingService.list(MarketplaceListingStatus.PENDING)).hasSize(1);

        verify(listingRepository, org.mockito.Mockito.never()).findAllByStatusOrderByCreatedAtDesc(any());
    }

    @Test
    void approve_aShopThePartnerDoesNotLookAfter_isNotFoundAndNothingChanges() {
        org.mockito.Mockito.doThrow(ResourceNotFoundException.of("Tenant", tenantId)).when(partnerScope).requireAccess(tenantId);

        assertThrows(ResourceNotFoundException.class, () -> adminMarketplaceListingService.approve(tenantId));

        verify(listingRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void approveRejectAndUnpublish_areLogged() {
        when(listingRepository.findByTenantId(tenantId))
                .thenReturn(Optional.of(pendingListing()), Optional.of(pendingListing()), Optional.of(liveListing()));

        adminMarketplaceListingService.approve(tenantId);
        adminMarketplaceListingService.reject(tenantId, "blurry photo");
        adminMarketplaceListingService.unpublish(tenantId, "closed down");

        verify(partnerScope).record(tenantId, ReviewActionType.LISTING_APPROVED, null, null);
        verify(partnerScope).record(tenantId, ReviewActionType.LISTING_REJECTED, null, "blurry photo");
        verify(partnerScope).record(tenantId, ReviewActionType.LISTING_UNPUBLISHED, null, "closed down");
    }

    @Test
    void list_noStatusFilter_returnsAllListingsWithBusinessNameResolved() {
        when(listingRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(pendingListing()));

        List<AdminMarketplaceListingResponse> result = adminMarketplaceListingService.list(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).businessName()).isEqualTo("Veera Collections");
        assertThat(result.get(0).tenantId()).isEqualTo(tenantId);
    }

    @Test
    void list_withStatusFilter_delegatesToFilteredQuery() {
        when(listingRepository.findAllByStatusOrderByCreatedAtDesc(MarketplaceListingStatus.PENDING))
                .thenReturn(List.of(pendingListing()));

        List<AdminMarketplaceListingResponse> result = adminMarketplaceListingService.list(MarketplaceListingStatus.PENDING);

        assertThat(result).hasSize(1);
        verify(listingRepository, org.mockito.Mockito.never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void approve_pendingListing_movesToLiveAndStampsReviewedAt() {
        MarketplaceListing listing = pendingListing();
        when(listingRepository.findByTenantId(tenantId)).thenReturn(Optional.of(listing));

        AdminMarketplaceListingResponse response = adminMarketplaceListingService.approve(tenantId);

        assertThat(response.status()).isEqualTo(MarketplaceListingStatus.LIVE);
        assertThat(listing.getReviewedAt()).isNotNull();
    }

    @Test
    void approve_notPending_throwsBusinessRuleViolation() {
        MarketplaceListing listing = liveListing();
        when(listingRepository.findByTenantId(tenantId)).thenReturn(Optional.of(listing));

        assertThrows(BusinessRuleViolationException.class, () -> adminMarketplaceListingService.approve(tenantId));
    }

    @Test
    void approve_unknownTenant_throwsResourceNotFound() {
        when(listingRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adminMarketplaceListingService.approve(tenantId));
    }

    @Test
    void reject_pendingListing_movesToDraftWithReason() {
        MarketplaceListing listing = pendingListing();
        when(listingRepository.findByTenantId(tenantId)).thenReturn(Optional.of(listing));

        AdminMarketplaceListingResponse response = adminMarketplaceListingService.reject(tenantId, "Please add a clearer photo.");

        assertThat(response.status()).isEqualTo(MarketplaceListingStatus.DRAFT);
        assertThat(response.rejectionReason()).isEqualTo("Please add a clearer photo.");
        assertThat(listing.getReviewedAt()).isNotNull();
    }

    @Test
    void reject_notPending_throwsBusinessRuleViolation() {
        MarketplaceListing listing = liveListing();
        when(listingRepository.findByTenantId(tenantId)).thenReturn(Optional.of(listing));

        assertThrows(BusinessRuleViolationException.class, () -> adminMarketplaceListingService.reject(tenantId, "reason"));
    }

    @Test
    void unpublish_liveListing_movesToDraftWithReason() {
        MarketplaceListing listing = liveListing();
        when(listingRepository.findByTenantId(tenantId)).thenReturn(Optional.of(listing));

        AdminMarketplaceListingResponse response =
                adminMarketplaceListingService.unpublish(tenantId, "Reported by another vendor.");

        assertThat(response.status()).isEqualTo(MarketplaceListingStatus.DRAFT);
        assertThat(response.rejectionReason()).isEqualTo("Reported by another vendor.");
    }

    @Test
    void unpublish_notLive_throwsBusinessRuleViolation() {
        MarketplaceListing listing = pendingListing();
        when(listingRepository.findByTenantId(tenantId)).thenReturn(Optional.of(listing));

        assertThrows(BusinessRuleViolationException.class, () -> adminMarketplaceListingService.unpublish(tenantId, null));
    }
}
