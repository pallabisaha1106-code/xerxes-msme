package com.xerxes.msme.admin;

import com.xerxes.msme.admin.dto.AdminMarketplaceListingResponse;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.marketplace.MarketplaceCategory;
import com.xerxes.msme.marketplace.MarketplaceCategoryRepository;
import com.xerxes.msme.marketplace.MarketplaceListing;
import com.xerxes.msme.marketplace.MarketplaceListingRepository;
import com.xerxes.msme.marketplace.MarketplaceListingStatus;
import com.xerxes.msme.partner.PartnerScope;
import com.xerxes.msme.partner.ReviewActionType;
import com.xerxes.msme.storage.ImageStorageService;
import com.xerxes.msme.tenant.Tenant;
import com.xerxes.msme.tenant.TenantRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Platform-staff curation of vendor marketplace listings: review a
 * PENDING submission (approve -> LIVE, or reject -> DRAFT with a reason),
 * or pull an already-LIVE listing back down (unpublish -> DRAFT). Either
 * way, sending a listing back to DRAFT leaves the vendor's own rejection
 * reason in place so they see why on their own listing page (see
 * MarketplaceListingService, the vendor-facing counterpart to this).
 */
@Service
public class AdminMarketplaceListingService {

    private final MarketplaceListingRepository listingRepository;
    private final MarketplaceCategoryRepository categoryRepository;
    private final TenantRepository tenantRepository;
    private final ImageStorageService imageStorageService;
    private final PartnerScope partnerScope;

    public AdminMarketplaceListingService(
            MarketplaceListingRepository listingRepository,
            MarketplaceCategoryRepository categoryRepository,
            TenantRepository tenantRepository,
            ImageStorageService imageStorageService,
            PartnerScope partnerScope) {
        this.listingRepository = listingRepository;
        this.categoryRepository = categoryRepository;
        this.tenantRepository = tenantRepository;
        this.imageStorageService = imageStorageService;
        this.partnerScope = partnerScope;
    }

    @Transactional(readOnly = true)
    public List<AdminMarketplaceListingResponse> list(MarketplaceListingStatus status) {
        List<MarketplaceListing> listings = partnerScope
                .ownedTenantIds()
                .map(ids -> status != null
                        ? listingRepository.findAllByTenantIdInAndStatusOrderByCreatedAtDesc(ids, status)
                        : listingRepository.findAllByTenantIdInOrderByCreatedAtDesc(ids))
                .orElseGet(() -> status != null
                        ? listingRepository.findAllByStatusOrderByCreatedAtDesc(status)
                        : listingRepository.findAllByOrderByCreatedAtDesc());
        return listings.stream().map(this::toResponse).toList();
    }

    @Transactional
    public AdminMarketplaceListingResponse approve(UUID tenantId) {
        MarketplaceListing listing = findListing(tenantId);
        if (listing.getStatus() != MarketplaceListingStatus.PENDING) {
            throw new BusinessRuleViolationException("Only a pending listing can be approved.");
        }
        listing.setStatus(MarketplaceListingStatus.LIVE);
        listing.setRejectionReason(null);
        listing.setReviewedAt(OffsetDateTime.now());
        listingRepository.save(listing);
        partnerScope.record(tenantId, ReviewActionType.LISTING_APPROVED, null, null);
        return toResponse(listing);
    }

    @Transactional
    public AdminMarketplaceListingResponse reject(UUID tenantId, String reason) {
        MarketplaceListing listing = findListing(tenantId);
        if (listing.getStatus() != MarketplaceListingStatus.PENDING) {
            throw new BusinessRuleViolationException("Only a pending listing can be rejected.");
        }
        partnerScope.record(tenantId, ReviewActionType.LISTING_REJECTED, null, reason);
        return sendBackToDraft(listing, reason);
    }

    @Transactional
    public AdminMarketplaceListingResponse unpublish(UUID tenantId, String reason) {
        MarketplaceListing listing = findListing(tenantId);
        if (listing.getStatus() != MarketplaceListingStatus.LIVE) {
            throw new BusinessRuleViolationException("Only a live listing can be unpublished.");
        }
        partnerScope.record(tenantId, ReviewActionType.LISTING_UNPUBLISHED, null, reason);
        return sendBackToDraft(listing, reason);
    }

    private AdminMarketplaceListingResponse sendBackToDraft(MarketplaceListing listing, String reason) {
        listing.setStatus(MarketplaceListingStatus.DRAFT);
        listing.setRejectionReason(reason);
        listing.setReviewedAt(OffsetDateTime.now());
        listingRepository.save(listing);
        return toResponse(listing);
    }

    private MarketplaceListing findListing(UUID tenantId) {
        partnerScope.requireAccess(tenantId);
        return listingRepository
                .findByTenantId(tenantId)
                .orElseThrow(() -> ResourceNotFoundException.of("MarketplaceListing", tenantId));
    }

    private AdminMarketplaceListingResponse toResponse(MarketplaceListing listing) {
        String businessName = tenantRepository.findById(listing.getTenantId()).map(Tenant::getBusinessName).orElse(null);
        String categoryName = listing.getCategoryId() != null
                ? categoryRepository.findById(listing.getCategoryId()).map(MarketplaceCategory::getName).orElse(null)
                : null;
        String shopPhotoUrl = listing.getShopPhotoFileName() != null
                ? imageStorageService.mediaUrl(listing.getTenantId() + "/marketplace/" + listing.getShopPhotoFileName())
                : null;
        return new AdminMarketplaceListingResponse(
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
                listing.getStatus(),
                listing.getRejectionReason(),
                listing.getSubmittedAt(),
                listing.getReviewedAt());
    }
}
