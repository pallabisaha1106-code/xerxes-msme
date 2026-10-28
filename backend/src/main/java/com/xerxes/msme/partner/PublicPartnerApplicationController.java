package com.xerxes.msme.partner;

import com.xerxes.msme.partner.dto.PartnerApplicationRequest;
import com.xerxes.msme.partner.dto.PartnerApplicationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** No authentication - the public "Become a local partner" form. Rate-limited like the rest of /api/public. */
@RestController
@RequestMapping("/api/public/partner-applications")
public class PublicPartnerApplicationController {

    private final PartnerApplicationService partnerApplicationService;

    public PublicPartnerApplicationController(PartnerApplicationService partnerApplicationService) {
        this.partnerApplicationService = partnerApplicationService;
    }

    @PostMapping
    public ResponseEntity<PartnerApplicationResponse> apply(@Valid @RequestBody PartnerApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(partnerApplicationService.apply(request));
    }
}
