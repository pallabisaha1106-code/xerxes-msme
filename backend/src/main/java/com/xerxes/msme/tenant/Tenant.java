package com.xerxes.msme.tenant;

import com.xerxes.msme.common.MutableEntity;
import com.xerxes.msme.subscription.SubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tenants")
@Getter
@Setter
public class Tenant extends MutableEntity {

    @Column(name = "business_name", nullable = false)
    private String businessName;

    private String gstin;

    private String pan;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "state_code", nullable = false, length = 2)
    private String stateCode;

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    private String city;

    private String pincode;

    @Column(nullable = false)
    private String email;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_plan", nullable = false)
    private MembershipType subscriptionPlan = MembershipType.FREE;

    /** Set by an admin plan change; applied to subscriptionPlan (and cleared) when the next cycle's invoice is generated. */
    @Enumerated(EnumType.STRING)
    @Column(name = "pending_subscription_plan")
    private MembershipType pendingSubscriptionPlan;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /** The one sentinel tenant platform admins belong to - see V5 migration. Never a real business. */
    @Column(name = "is_platform", nullable = false)
    private boolean platform = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_status", nullable = false)
    private SubscriptionStatus subscriptionStatus = SubscriptionStatus.ACTIVE;

    @Column(name = "trial_ends_at")
    private OffsetDateTime trialEndsAt;

    @Column(name = "current_period_end", nullable = false)
    private OffsetDateTime currentPeriodEnd;

    @Column(name = "grace_period_ends_at")
    private OffsetDateTime gracePeriodEndsAt;

    /** Set at Step 1 of vendor registration (see VendorApplicationService.apply) - kept even after Super Admin approval, see AdminTenantService.updateStatus. */
    @Column(name = "registration_code", unique = true)
    private String registrationCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_type")
    private BusinessType businessType;

    @Column(name = "shop_front_photo_url")
    private String shopFrontPhotoUrl;

    @Column(name = "gst_certificate_url")
    private String gstCertificateUrl;

    @Column(name = "owner_id_document_url")
    private String ownerIdDocumentUrl;

    /**
     * Structured city/area the vendor picked from Super Admin's curated
     * ServiceCity/ServiceArea list (see V13__service_locations.sql) - set by
     * the vendor-application flow only, alongside the legacy free-text
     * city/pincode above (still derived/kept in sync for existing consumers
     * like invoices and the generated storefront address).
     */
    @Column(name = "city_id")
    private UUID cityId;

    @Column(name = "area_id")
    private UUID areaId;

    private String landmark;

    /** The local partner looking after this shop (see the partner package); empty until one is assigned. */
    @Column(name = "partner_id")
    private UUID partnerId;

    @Column(name = "partner_assigned_at")
    private OffsetDateTime partnerAssignedAt;
}
