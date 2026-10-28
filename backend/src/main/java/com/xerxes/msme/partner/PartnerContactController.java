package com.xerxes.msme.partner;

import com.xerxes.msme.partner.dto.PartnerContactResponse;
import com.xerxes.msme.security.AuthContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** A logged-in shop owner (or staff member) asking who their local partner is. */
@RestController
@RequestMapping("/api/partners")
public class PartnerContactController {

    private final PartnerContactService partnerContactService;
    private final AuthContext authContext;

    public PartnerContactController(PartnerContactService partnerContactService, AuthContext authContext) {
        this.partnerContactService = partnerContactService;
        this.authContext = authContext;
    }

    /** The shop's partner, or 204 No Content when it has none (or theirs is switched off). */
    @GetMapping("/mine")
    public ResponseEntity<PartnerContactResponse> mine() {
        return partnerContactService
                .contactFor(authContext.current().tenantId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
