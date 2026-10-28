package com.xerxes.msme.admin;

import com.xerxes.msme.admin.dto.AdminMarketplaceListingResponse;
import com.xerxes.msme.admin.dto.ReviewMarketplaceListingRequest;
import com.xerxes.msme.marketplace.MarketplaceListingStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Platform-staff-only: review vendor marketplace listing submissions. See SecurityConfig for the role gate. */
@RestController
@RequestMapping("/api/admin/marketplace/listings")
public class AdminMarketplaceListingController {

    private final AdminMarketplaceListingService adminMarketplaceListingService;

    public AdminMarketplaceListingController(AdminMarketplaceListingService adminMarketplaceListingService) {
        this.adminMarketplaceListingService = adminMarketplaceListingService;
    }

    @GetMapping
    public List<AdminMarketplaceListingResponse> list(@RequestParam(required = false) MarketplaceListingStatus status) {
        return adminMarketplaceListingService.list(status);
    }

    @PostMapping("/{tenantId}/approve")
    public AdminMarketplaceListingResponse approve(@PathVariable UUID tenantId) {
        return adminMarketplaceListingService.approve(tenantId);
    }

    @PostMapping("/{tenantId}/reject")
    public AdminMarketplaceListingResponse reject(
            @PathVariable UUID tenantId, @RequestBody(required = false) ReviewMarketplaceListingRequest request) {
        return adminMarketplaceListingService.reject(tenantId, request != null ? request.reason() : null);
    }

    @PostMapping("/{tenantId}/unpublish")
    public AdminMarketplaceListingResponse unpublish(
            @PathVariable UUID tenantId, @RequestBody(required = false) ReviewMarketplaceListingRequest request) {
        return adminMarketplaceListingService.unpublish(tenantId, request != null ? request.reason() : null);
    }
}
