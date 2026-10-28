package com.xerxes.msme.partner;

import com.xerxes.msme.partner.dto.AdminPartnerResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** A partner's own profile: name, status, areas and how many shops they look after. See SecurityConfig for the role gate. */
@RestController
@RequestMapping("/api/partner")
public class PartnerMeController {

    private final PartnerContext partnerContext;
    private final AdminPartnerService adminPartnerService;

    public PartnerMeController(PartnerContext partnerContext, AdminPartnerService adminPartnerService) {
        this.partnerContext = partnerContext;
        this.adminPartnerService = adminPartnerService;
    }

    @GetMapping("/me")
    public AdminPartnerResponse me() {
        return adminPartnerService.getPartner(partnerContext.current().getId());
    }
}
