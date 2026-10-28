package com.xerxes.msme.marketplace;

import com.xerxes.msme.marketplace.dto.MarketplaceListingResponse;
import com.xerxes.msme.marketplace.dto.UpdateMarketplaceListingRequest;
import com.xerxes.msme.security.AuthContext;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** The authenticated vendor's own view of their marketplace listing - draft, edit, upload photo, submit for review. */
@RestController
@RequestMapping("/api/marketplace/listing")
public class MarketplaceListingController {

    private final MarketplaceListingService listingService;
    private final AuthContext authContext;

    public MarketplaceListingController(MarketplaceListingService listingService, AuthContext authContext) {
        this.listingService = listingService;
        this.authContext = authContext;
    }

    @GetMapping
    public MarketplaceListingResponse get() {
        return listingService.getListing(authContext.current().tenantId());
    }

    @PutMapping
    public MarketplaceListingResponse update(@Valid @RequestBody UpdateMarketplaceListingRequest request) {
        return listingService.updateListing(authContext.current().tenantId(), request);
    }

    @PostMapping(path = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MarketplaceListingResponse uploadPhoto(@RequestParam("file") MultipartFile file) {
        return listingService.uploadPhoto(authContext.current().tenantId(), file);
    }

    @DeleteMapping("/photo")
    public MarketplaceListingResponse deletePhoto() {
        return listingService.deletePhoto(authContext.current().tenantId());
    }

    @PostMapping("/submit")
    public MarketplaceListingResponse submit() {
        return listingService.submit(authContext.current().tenantId());
    }
}
