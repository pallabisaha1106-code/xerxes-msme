package com.xerxes.msme.marketplace.dto;

import com.xerxes.msme.partner.dto.PartnerContactResponse;
import com.xerxes.msme.tenant.BusinessType;
import com.xerxes.msme.tenant.MembershipType;
import java.util.UUID;

/** What Step 2 loads after the applicant types in their registration code - see VendorApplicationController. */
public record VendorApplicationDetailsResponse(
        UUID tenantId,
        String businessName,
        BusinessType businessType,
        String stateCode,
        String addressLine1,
        String city,
        String pincode,
        String phone,
        MembershipType subscriptionPlan,
        long productCount,
        int productLimit,
        MarketplaceListingResponse listing,
        /** The applicant's local partner, or null when none was assigned (or theirs is switched off). */
        PartnerContactResponse partner) {}
