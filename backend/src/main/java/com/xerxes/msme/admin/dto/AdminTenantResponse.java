package com.xerxes.msme.admin.dto;

import com.xerxes.msme.subscription.SubscriptionStatus;
import com.xerxes.msme.tenant.BusinessType;
import com.xerxes.msme.tenant.MembershipType;
import java.time.OffsetDateTime;
import java.util.UUID;

/** A vendor as seen by platform staff - profile plus at-a-glance usage counts. */
public record AdminTenantResponse(
        UUID id,
        String businessName,
        String email,
        String phone,
        String gstin,
        String stateCode,
        String addressLine1,
        String addressLine2,
        String city,
        String pincode,
        MembershipType subscriptionPlan,
        /** Set once an admin has scheduled a plan change; applies (and clears) at currentPeriodEnd - see SubscriptionService. */
        MembershipType pendingSubscriptionPlan,
        boolean active,
        OffsetDateTime createdAt,
        long productCount,
        long customerCount,
        long invoiceCount,
        long storefrontOrderCount,
        SubscriptionStatus subscriptionStatus,
        OffsetDateTime trialEndsAt,
        OffsetDateTime currentPeriodEnd,
        OffsetDateTime gracePeriodEndsAt,
        String registrationCode,
        BusinessType businessType,
        String shopFrontPhotoUrl,
        String gstCertificateUrl,
        String ownerIdDocumentUrl,
        String landmark,
        /** The local partner looking after this shop, or null when it has none. */
        String partnerName,
        UUID partnerId,
        /** Whether that partner is still active; null when the shop has none. A shop of an inactive partner needs a new one. */
        Boolean partnerActive) {}
