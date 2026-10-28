package com.xerxes.msme.partner;

import com.xerxes.msme.partner.dto.AdminPartnerResponse;
import com.xerxes.msme.partner.dto.RejectPartnerRequest;
import com.xerxes.msme.partner.dto.UpdatePartnerAreasRequest;
import com.xerxes.msme.security.AuthContext;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Platform-staff-only: review partner applications and manage partners. See SecurityConfig for the role gate. */
@RestController
@RequestMapping("/api/admin/partners")
public class AdminPartnerController {

    private final AdminPartnerService adminPartnerService;
    private final AuthContext authContext;

    public AdminPartnerController(AdminPartnerService adminPartnerService, AuthContext authContext) {
        this.adminPartnerService = adminPartnerService;
        this.authContext = authContext;
    }

    @GetMapping
    public List<AdminPartnerResponse> list(@RequestParam(required = false) PartnerStatus status) {
        return adminPartnerService.listPartners(status);
    }

    @GetMapping("/{partnerId}")
    public AdminPartnerResponse get(@PathVariable UUID partnerId) {
        return adminPartnerService.getPartner(partnerId);
    }

    @PostMapping("/{partnerId}/approve")
    public AdminPartnerResponse approve(@PathVariable UUID partnerId) {
        return adminPartnerService.approve(partnerId, authContext.current().userId());
    }

    @PostMapping("/{partnerId}/reject")
    public AdminPartnerResponse reject(@PathVariable UUID partnerId, @Valid @RequestBody RejectPartnerRequest request) {
        return adminPartnerService.reject(partnerId, request.reason(), authContext.current().userId());
    }

    @PostMapping("/{partnerId}/deactivate")
    public AdminPartnerResponse deactivate(@PathVariable UUID partnerId) {
        return adminPartnerService.deactivate(partnerId, authContext.current().userId());
    }

    @PostMapping("/{partnerId}/reactivate")
    public AdminPartnerResponse reactivate(@PathVariable UUID partnerId) {
        return adminPartnerService.reactivate(partnerId, authContext.current().userId());
    }

    @PutMapping("/{partnerId}/areas")
    public AdminPartnerResponse replaceAreas(@PathVariable UUID partnerId, @Valid @RequestBody UpdatePartnerAreasRequest request) {
        return adminPartnerService.replaceAreas(partnerId, request.areaIds());
    }
}
