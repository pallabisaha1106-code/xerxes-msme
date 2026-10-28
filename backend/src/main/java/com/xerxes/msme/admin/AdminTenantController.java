package com.xerxes.msme.admin;

import com.xerxes.msme.admin.dto.AdminTenantResponse;
import com.xerxes.msme.admin.dto.AssignTenantPartnerRequest;
import com.xerxes.msme.admin.dto.UpdateTenantPlanRequest;
import com.xerxes.msme.admin.dto.UpdateTenantStatusRequest;
import com.xerxes.msme.subscription.dto.AdminSubscriptionUpdateRequest;
import com.xerxes.msme.subscription.dto.PendingInvoiceResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Platform-staff-only: the vendor directory and per-vendor management actions. See SecurityConfig for the role gate. */
@RestController
@RequestMapping("/api/admin/tenants")
public class AdminTenantController {

    private final AdminTenantService adminTenantService;

    public AdminTenantController(AdminTenantService adminTenantService) {
        this.adminTenantService = adminTenantService;
    }

    @GetMapping
    public List<AdminTenantResponse> list() {
        return adminTenantService.listTenants();
    }

    @GetMapping("/{tenantId}")
    public AdminTenantResponse get(@PathVariable UUID tenantId) {
        return adminTenantService.getTenant(tenantId);
    }

    @PutMapping("/{tenantId}/status")
    public AdminTenantResponse updateStatus(@PathVariable UUID tenantId, @Valid @RequestBody UpdateTenantStatusRequest request) {
        return adminTenantService.updateStatus(tenantId, request.active());
    }

    /** Super Admin only (the partner allow-list in SecurityConfig stops at status): moves a shop to another partner. */
    @PutMapping("/{tenantId}/partner")
    public AdminTenantResponse assignPartner(@PathVariable UUID tenantId, @RequestBody AssignTenantPartnerRequest request) {
        return adminTenantService.assignPartner(tenantId, request.partnerId());
    }

    @PutMapping("/{tenantId}/plan")
    public AdminTenantResponse updatePlan(@PathVariable UUID tenantId, @Valid @RequestBody UpdateTenantPlanRequest request) {
        return adminTenantService.updatePlan(tenantId, request.subscriptionPlan());
    }

    @PutMapping("/{tenantId}/subscription")
    public AdminTenantResponse updateSubscription(
            @PathVariable UUID tenantId, @Valid @RequestBody AdminSubscriptionUpdateRequest request) {
        return adminTenantService.updateSubscription(tenantId, request.status(), request.currentPeriodEnd());
    }

    /** Manually creates (or returns the existing) pending invoice + Razorpay payment link, without waiting for the nightly job. */
    @PostMapping("/{tenantId}/subscription/invoice")
    public PendingInvoiceResponse generateSubscriptionInvoice(@PathVariable UUID tenantId) {
        return adminTenantService.generateSubscriptionInvoice(tenantId);
    }
}
