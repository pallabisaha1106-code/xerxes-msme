package com.xerxes.msme.marketplace;

import com.xerxes.msme.marketplace.dto.CreateStarterProductsRequest;
import com.xerxes.msme.marketplace.dto.VendorApplicationConfigResponse;
import com.xerxes.msme.marketplace.dto.MarketplaceListingResponse;
import com.xerxes.msme.marketplace.dto.SelectMembershipPlanRequest;
import com.xerxes.msme.marketplace.dto.UpdateMarketplaceListingRequest;
import com.xerxes.msme.marketplace.dto.VendorApplicationDetailsResponse;
import com.xerxes.msme.marketplace.dto.VendorApplicationRequest;
import com.xerxes.msme.marketplace.dto.VendorApplicationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Two-step vendor registration, both steps unauthenticated (permitAll under
 * /api/public/** - see SecurityConfig - and rate-limited by
 * PublicApiRateLimitInterceptor, same as every other /api/public/** path):
 * Step 1 (POST, no code yet) applies and hands back a registration code;
 * Step 2 (everything under /{code}) is reached later by typing that code
 * back in. See VendorApplicationService for why this is deliberately not a
 * JWT-based flow.
 */
@RestController
@RequestMapping("/api/public/vendor-applications")
public class VendorApplicationController {

    private final VendorApplicationService vendorApplicationService;

    public VendorApplicationController(VendorApplicationService vendorApplicationService) {
        this.vendorApplicationService = vendorApplicationService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VendorApplicationResponse> apply(
            @Valid @RequestPart("application") VendorApplicationRequest request,
            @RequestPart("shopFrontPhoto") MultipartFile shopFrontPhoto,
            // Optional at the HTTP binding level regardless of the kyc-documents-required flag -
            // VendorApplicationService.apply() is where "is this actually required right now" is
            // decided, so the flag only needs to live in one place.
            @RequestPart(value = "gstCertificate", required = false) MultipartFile gstCertificate,
            @RequestPart(value = "ownerIdDocument", required = false) MultipartFile ownerIdDocument) {
        VendorApplicationResponse response =
                vendorApplicationService.apply(request, shopFrontPhoto, gstCertificate, ownerIdDocument);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Lets the (unauthenticated) registration form know whether to require the GST/owner-ID uploads. */
    @GetMapping("/config")
    public VendorApplicationConfigResponse config() {
        return new VendorApplicationConfigResponse(vendorApplicationService.isKycDocumentsRequired());
    }

    @GetMapping("/{code}")
    public VendorApplicationDetailsResponse get(@PathVariable String code) {
        return vendorApplicationService.getByCode(code);
    }

    // POST, not PUT/PATCH: /api/public/** CORS (SecurityConfig) only allows
    // GET/POST for this deliberately narrow, unauthenticated bucket.
    @PostMapping("/{code}/listing")
    public MarketplaceListingResponse updateListing(
            @PathVariable String code, @Valid @RequestBody UpdateMarketplaceListingRequest request) {
        return vendorApplicationService.updateListing(code, request);
    }

    @PostMapping(path = "/{code}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MarketplaceListingResponse uploadPhoto(
            @PathVariable String code, @RequestPart("file") MultipartFile file) {
        return vendorApplicationService.uploadListingPhoto(code, file);
    }

    // POST, not PUT - same CORS constraint as updateListing above.
    @PostMapping("/{code}/plan")
    public VendorApplicationDetailsResponse selectPlan(
            @PathVariable String code, @Valid @RequestBody SelectMembershipPlanRequest request) {
        return vendorApplicationService.selectPlan(code, request.plan());
    }

    @PostMapping("/{code}/starter-products")
    public ResponseEntity<Void> createStarterProducts(
            @PathVariable String code, @Valid @RequestBody CreateStarterProductsRequest request) {
        vendorApplicationService.createStarterProducts(code, request.defaultProductIds());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/{code}/submit")
    public MarketplaceListingResponse submit(@PathVariable String code) {
        return vendorApplicationService.submit(code);
    }
}
