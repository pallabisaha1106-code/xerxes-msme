package com.xerxes.msme.marketplace;

import com.xerxes.msme.catalog.ProductRepository;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.partner.PartnerAssignmentService;
import com.xerxes.msme.partner.PartnerContactService;
import com.xerxes.msme.partner.dto.PartnerContactResponse;
import com.xerxes.msme.common.exception.DuplicateResourceException;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.marketplace.dto.MarketplaceListingResponse;
import com.xerxes.msme.marketplace.dto.UpdateMarketplaceListingRequest;
import com.xerxes.msme.marketplace.dto.VendorApplicationDetailsResponse;
import com.xerxes.msme.marketplace.dto.VendorApplicationRequest;
import com.xerxes.msme.marketplace.dto.VendorApplicationResponse;
import com.xerxes.msme.storage.ImageStorageService;
import com.xerxes.msme.subscription.SubscriptionProperties;
import com.xerxes.msme.subscription.SubscriptionStatus;
import com.xerxes.msme.tenant.MembershipType;
import com.xerxes.msme.tenant.Tenant;
import com.xerxes.msme.tenant.TenantRepository;
import com.xerxes.msme.user.Role;
import com.xerxes.msme.user.User;
import com.xerxes.msme.user.UserRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Step 1 ("apply") creates a Tenant+User but INACTIVE, pending Super Admin
 * review of the 3 uploaded documents (see AdminTenantService.updateStatus).
 * It returns a registrationCode - an API-key-style credential stored on the
 * tenant, not a JWT - rather than logging the applicant in; AuthService.login()
 * already rejects inactive tenants, so this is a deliberately separate path
 * from normal login, not a variant of it. The code is intentionally kept even
 * after approval (not cleared), so it keeps working as a standing credential
 * for these endpoints going forward - see AdminTenantService.updateStatus.
 * apply() also picks the marketplace category (Grocery, Ladies Wear, etc.)
 * here rather than deferring it to Step 2 - it's set once on the draft
 * listing immediately, and Step 2's own Category field is pre-filled from
 * it and locked (not resubmittable) so it's never picked twice.
 *
 * <p>Step 2 is reached later by typing that code back in; every method here
 * resolves it to a tenantId and delegates to the same MarketplaceListingService/
 * StarterCatalogService methods the old single-page flow already used - no
 * business logic is duplicated, only the entry point (code instead of JWT)
 * differs. Endpoints live under /api/public/** (see VendorApplicationController)
 * so they're reachable without a token and already rate-limited.
 */
@Service
public class VendorApplicationService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"; // no 0/O/1/I/L - avoids transcription errors
    private static final int CODE_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SubscriptionProperties subscriptionProperties;
    private final ImageStorageService imageStorageService;
    private final MarketplaceListingService marketplaceListingService;
    private final MarketplaceCategoryRepository marketplaceCategoryRepository;
    private final ServiceCityRepository serviceCityRepository;
    private final ServiceAreaRepository serviceAreaRepository;
    private final ServiceStateRepository serviceStateRepository;
    private final StarterCatalogService starterCatalogService;
    private final ProductRepository productRepository;
    private final PartnerAssignmentService partnerAssignmentService;
    private final PartnerContactService partnerContactService;

    // Off by default - see application.yml for the full rationale. Read with getKycDocumentsRequired()
    // so VendorApplicationController can expose it to the (unauthenticated) frontend via /config.
    @Value("${app.vendor-application.kyc-documents-required:false}")
    private boolean kycDocumentsRequired;

    public VendorApplicationService(
            TenantRepository tenantRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            SubscriptionProperties subscriptionProperties,
            ImageStorageService imageStorageService,
            MarketplaceListingService marketplaceListingService,
            MarketplaceCategoryRepository marketplaceCategoryRepository,
            ServiceCityRepository serviceCityRepository,
            ServiceAreaRepository serviceAreaRepository,
            ServiceStateRepository serviceStateRepository,
            StarterCatalogService starterCatalogService,
            ProductRepository productRepository,
            PartnerAssignmentService partnerAssignmentService,
            PartnerContactService partnerContactService) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.subscriptionProperties = subscriptionProperties;
        this.imageStorageService = imageStorageService;
        this.marketplaceListingService = marketplaceListingService;
        this.marketplaceCategoryRepository = marketplaceCategoryRepository;
        this.serviceCityRepository = serviceCityRepository;
        this.serviceAreaRepository = serviceAreaRepository;
        this.serviceStateRepository = serviceStateRepository;
        this.starterCatalogService = starterCatalogService;
        this.productRepository = productRepository;
        this.partnerAssignmentService = partnerAssignmentService;
        this.partnerContactService = partnerContactService;
    }

    @Transactional
    public VendorApplicationResponse apply(
            VendorApplicationRequest request,
            MultipartFile shopFrontPhoto,
            MultipartFile gstCertificate,
            MultipartFile ownerIdDocument) {
        if (tenantRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("A business is already registered with email " + request.email());
        }
        if (!marketplaceCategoryRepository.existsById(request.marketplaceCategoryId())) {
            throw ResourceNotFoundException.of("MarketplaceCategory", request.marketplaceCategoryId());
        }
        if (kycDocumentsRequired) {
            if (isBlank(gstCertificate)) {
                throw new BusinessRuleViolationException("A GST certificate is required");
            }
            if (isBlank(ownerIdDocument)) {
                throw new BusinessRuleViolationException("An owner ID document (PAN or Aadhaar) is required");
            }
        }

        // Registration is gated on a real, Super Admin-curated state/city/area
        // - not free text - see AdminServiceLocationService/AdminServiceStateService.
        // City must belong to the chosen state, and area must belong to the
        // chosen city, so a client can't mix-and-match unrelated ids.
        serviceStateRepository
                .findById(request.stateCode())
                .filter(ServiceState::isActive)
                .orElseThrow(() -> ResourceNotFoundException.of("ServiceState", request.stateCode()));
        ServiceCity city = serviceCityRepository
                .findById(request.cityId())
                .filter(c -> c.getStateCode().equals(request.stateCode()))
                .orElseThrow(() -> ResourceNotFoundException.of("ServiceCity", request.cityId()));
        ServiceArea area = serviceAreaRepository
                .findById(request.areaId())
                .filter(a -> a.getCityId().equals(request.cityId()))
                .orElseThrow(() -> ResourceNotFoundException.of("ServiceArea", request.areaId()));

        Tenant tenant = new Tenant();
        tenant.setBusinessName(request.businessName());
        tenant.setBusinessType(request.businessType());
        tenant.setGstin(request.gstin());
        tenant.setStateCode(request.stateCode());
        tenant.setAddressLine1(request.addressLine1());
        tenant.setCityId(city.getId());
        tenant.setAreaId(area.getId());
        tenant.setLandmark(request.landmark());
        // Kept in sync for consumers that still read the legacy free-text
        // fields (invoices, generated storefront address, Admin's vendor
        // view) - see the migration comment for why these columns stay.
        tenant.setCity(city.getName());
        tenant.setPincode(area.getPincode());
        tenant.setPhone(request.phone());
        tenant.setEmail(request.email());
        tenant.setActive(false); // pending Super Admin review - see AdminTenantService.updateStatus

        OffsetDateTime trialEndsAt = OffsetDateTime.now().plusDays(subscriptionProperties.trialDays());
        tenant.setSubscriptionStatus(SubscriptionStatus.TRIALING);
        tenant.setTrialEndsAt(trialEndsAt);
        tenant.setCurrentPeriodEnd(trialEndsAt);

        tenant.setRegistrationCode(generateUniqueCode());
        tenant = tenantRepository.save(tenant);

        // Pre-creates the draft listing with its category and area already
        // set, so Step 2 (VendorCataloguePage) opens with both pre-filled -
        // the vendor picks them once, here, rather than twice across both
        // steps.
        marketplaceListingService.updateListing(
                tenant.getId(),
                new UpdateMarketplaceListingRequest(
                        request.marketplaceCategoryId(), area.getName(), null, null, null, null, null, null, null,
                        null, MarketplaceOrderMode.WHATSAPP));

        tenant.setShopFrontPhotoUrl(uploadDocument(tenant.getId(), "shop-front-photo", shopFrontPhoto));
        // Gated on the flag itself, not just on whether a file was sent - these documents are
        // stored in the same public-but-unlisted bucket as marketing photos (see
        // ImageStorageService/uploadDocument), so while KYC uploads are off, nothing ever lands
        // there via this path even if the frontend's upload fields were bypassed (devtools, a
        // raw API call, or the fields re-shown after someone flips the flag back off again).
        // Super Admin verifies the GSTIN/Aadhaar over a phone call before activating the shop
        // instead.
        if (kycDocumentsRequired && !isBlank(gstCertificate)) {
            tenant.setGstCertificateUrl(uploadDocument(tenant.getId(), "gst-certificate", gstCertificate));
        }
        if (kycDocumentsRequired && !isBlank(ownerIdDocument)) {
            tenant.setOwnerIdDocumentUrl(uploadDocument(tenant.getId(), "owner-id", ownerIdDocument));
        }
        tenant = tenantRepository.save(tenant);

        // Also seeds the listing's public shop photo from this same KYC
        // upload, so Step 2 doesn't need a second photo upload - see
        // MarketplaceListingService.adoptPhotoWithoutGuidelines for why this
        // skips the landscape/size check a normal listing-photo upload has.
        marketplaceListingService.adoptPhotoWithoutGuidelines(tenant.getId(), shopFrontPhoto);

        User owner = new User();
        owner.setTenantId(tenant.getId());
        owner.setName(request.ownerName());
        owner.setEmail(request.email());
        owner.setPasswordHash(passwordEncoder.encode(request.password()));
        owner.setRole(Role.OWNER);
        userRepository.save(owner);

        // Hands the shop to a local partner covering its area; if there is none it simply stays unassigned and
        // waits in Super Admin's own queue.
        PartnerContactResponse partner =
                partnerAssignmentService.assign(tenant).map(PartnerContactService::contactOf).orElse(null);

        return new VendorApplicationResponse(
                tenant.getId(), tenant.getRegistrationCode(), tenant.getBusinessName(), partner);
    }

    @Transactional(readOnly = true)
    public VendorApplicationDetailsResponse getByCode(String code) {
        Tenant tenant = resolveTenant(code);
        return toDetailsResponse(tenant);
    }

    @Transactional
    public MarketplaceListingResponse updateListing(String code, UpdateMarketplaceListingRequest request) {
        UUID tenantId = resolveTenant(code).getId();
        return marketplaceListingService.updateListing(tenantId, request);
    }

    @Transactional
    public MarketplaceListingResponse uploadListingPhoto(String code, MultipartFile file) {
        UUID tenantId = resolveTenant(code).getId();
        return marketplaceListingService.uploadPhoto(tenantId, file);
    }

    @Transactional
    public VendorApplicationDetailsResponse selectPlan(String code, MembershipType plan) {
        Tenant tenant = resolveTenant(code);
        tenant.setSubscriptionPlan(plan);
        tenantRepository.save(tenant);
        return toDetailsResponse(tenant);
    }

    @Transactional
    public void createStarterProducts(String code, List<UUID> defaultProductIds) {
        UUID tenantId = resolveTenant(code).getId();
        starterCatalogService.createStarterProducts(tenantId, defaultProductIds);
    }

    @Transactional
    public MarketplaceListingResponse submit(String code) {
        UUID tenantId = resolveTenant(code).getId();
        return marketplaceListingService.submit(tenantId);
    }

    private Tenant resolveTenant(String code) {
        return tenantRepository
                .findByRegistrationCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("No pending application found for that code."));
    }

    private VendorApplicationDetailsResponse toDetailsResponse(Tenant tenant) {
        return new VendorApplicationDetailsResponse(
                tenant.getId(),
                tenant.getBusinessName(),
                tenant.getBusinessType(),
                tenant.getStateCode(),
                tenant.getAddressLine1(),
                tenant.getCity(),
                tenant.getPincode(),
                tenant.getPhone(),
                tenant.getSubscriptionPlan(),
                productRepository.countByTenantId(tenant.getId()),
                tenant.getSubscriptionPlan().productLimit(),
                marketplaceListingService.getListing(tenant.getId()),
                partnerContactService.contactFor(tenant).orElse(null));
    }

    /** So VendorApplicationController can tell the (unauthenticated) frontend whether to require these uploads. */
    public boolean isKycDocumentsRequired() {
        return kycDocumentsRequired;
    }

    private static boolean isBlank(MultipartFile file) {
        return file == null || file.isEmpty();
    }

    private String uploadDocument(UUID tenantId, String label, MultipartFile file) {
        String objectPath = tenantId + "/kyc/" + label + "-" + UUID.randomUUID() + extensionOf(file);
        try {
            imageStorageService.upload(objectPath, file.getBytes(), file.getContentType());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read uploaded document: " + label, e);
        }
        return imageStorageService.mediaUrl(objectPath);
    }

    private static String extensionOf(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original == null) return "";
        int dot = original.lastIndexOf('.');
        return dot < 0 ? "" : original.substring(dot);
    }

    private String generateUniqueCode() {
        String candidate;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            candidate = sb.toString();
        } while (tenantRepository.existsByRegistrationCode(candidate));
        return candidate;
    }
}
