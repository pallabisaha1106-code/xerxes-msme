package com.xerxes.msme.marketplace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xerxes.msme.catalog.ProductRepository;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.common.exception.DuplicateResourceException;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.marketplace.dto.MarketplaceListingResponse;
import com.xerxes.msme.partner.Partner;
import com.xerxes.msme.partner.PartnerAssignmentService;
import com.xerxes.msme.partner.PartnerContactService;
import com.xerxes.msme.marketplace.dto.VendorApplicationDetailsResponse;
import com.xerxes.msme.marketplace.dto.VendorApplicationRequest;
import com.xerxes.msme.marketplace.dto.VendorApplicationResponse;
import com.xerxes.msme.storage.ImageStorageService;
import com.xerxes.msme.subscription.SubscriptionProperties;
import com.xerxes.msme.tenant.BusinessType;
import com.xerxes.msme.tenant.MembershipType;
import com.xerxes.msme.tenant.Tenant;
import com.xerxes.msme.tenant.TenantRepository;
import com.xerxes.msme.user.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class VendorApplicationServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ImageStorageService imageStorageService;
    @Mock private MarketplaceListingService marketplaceListingService;
    @Mock private MarketplaceCategoryRepository marketplaceCategoryRepository;
    @Mock private ServiceCityRepository serviceCityRepository;
    @Mock private ServiceAreaRepository serviceAreaRepository;
    @Mock private ServiceStateRepository serviceStateRepository;
    @Mock private StarterCatalogService starterCatalogService;
    @Mock private ProductRepository productRepository;
    @Mock private PartnerAssignmentService partnerAssignmentService;
    @Mock private PartnerContactService partnerContactService;

    private final SubscriptionProperties subscriptionProperties = new SubscriptionProperties(30, 7);
    private final UUID marketplaceCategoryId = UUID.randomUUID();
    private final UUID cityId = UUID.randomUUID();
    private final UUID areaId = UUID.randomUUID();

    private VendorApplicationService vendorApplicationService;

    @BeforeEach
    void setUp() {
        vendorApplicationService = new VendorApplicationService(
                tenantRepository,
                userRepository,
                passwordEncoder,
                subscriptionProperties,
                imageStorageService,
                marketplaceListingService,
                marketplaceCategoryRepository,
                serviceCityRepository,
                serviceAreaRepository,
                serviceStateRepository,
                starterCatalogService,
                productRepository,
                partnerAssignmentService,
                partnerContactService);
        lenient().when(marketplaceCategoryRepository.existsById(any())).thenReturn(true);

        ServiceState maharashtra = new ServiceState();
        maharashtra.setCode("27");
        maharashtra.setName("Maharashtra");
        maharashtra.setActive(true);
        lenient().when(serviceStateRepository.findById("27")).thenReturn(Optional.of(maharashtra));

        ServiceCity pune = new ServiceCity();
        pune.setId(cityId);
        pune.setStateCode("27");
        pune.setName("Pune");
        lenient().when(serviceCityRepository.findById(cityId)).thenReturn(Optional.of(pune));

        ServiceArea nigdi = new ServiceArea();
        nigdi.setId(areaId);
        nigdi.setCityId(cityId);
        nigdi.setName("Nigdi");
        nigdi.setPincode("411044");
        lenient().when(serviceAreaRepository.findById(areaId)).thenReturn(Optional.of(nigdi));
    }

    private VendorApplicationRequest applicationRequest() {
        return new VendorApplicationRequest(
                "Veera Collections", BusinessType.RETAILER, marketplaceCategoryId, "27AAAPL1234C1Z5", "27", "Addr 1",
                cityId, areaId, "Near XYZ Mall", "9999999999", "Veera", "veera@example.com", "SecurePass123");
    }

    private MockMultipartFile doc(String name) {
        return new MockMultipartFile(name, name + ".jpg", "image/jpeg", "bytes".getBytes());
    }

    @Test
    void apply_createsAnInactiveTenantWithAUniqueCodeAndUploadsAllThreeDocuments() {
        ReflectionTestUtils.setField(vendorApplicationService, "kycDocumentsRequired", true);
        when(tenantRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(tenantRepository.existsByRegistrationCode(anyString())).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            if (t.getId() == null) t.setId(UUID.randomUUID());
            return t;
        });
        when(imageStorageService.mediaUrl(any())).thenAnswer(inv -> "/media/images/" + inv.getArgument(0));

        VendorApplicationResponse response =
                vendorApplicationService.apply(applicationRequest(), doc("shop"), doc("gst"), doc("id"));

        assertThat(response.businessName()).isEqualTo("Veera Collections");
        assertThat(response.registrationCode()).hasSize(8);

        ArgumentCaptor<Tenant> captor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        Tenant saved = captor.getValue();
        assertThat(saved.isActive()).isFalse();
        assertThat(saved.getRegistrationCode()).isNotBlank();
        assertThat(saved.getShopFrontPhotoUrl()).isNotBlank();
        assertThat(saved.getGstCertificateUrl()).isNotBlank();
        assertThat(saved.getOwnerIdDocumentUrl()).isNotBlank();
        verify(imageStorageService, org.mockito.Mockito.times(3)).upload(any(), any(), eq("image/jpeg"));
        verify(userRepository).save(any());
    }

    @Test
    void apply_handsTheNewShopToAPartnerCoveringItsArea() {
        when(tenantRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(tenantRepository.existsByRegistrationCode(anyString())).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            if (t.getId() == null) t.setId(UUID.randomUUID());
            return t;
        });
        when(imageStorageService.mediaUrl(any())).thenAnswer(inv -> "/media/images/" + inv.getArgument(0));

        vendorApplicationService.apply(applicationRequest(), doc("shop"), null, null);

        ArgumentCaptor<Tenant> assigned = ArgumentCaptor.forClass(Tenant.class);
        verify(partnerAssignmentService).assign(assigned.capture());
        assertThat(assigned.getValue().getAreaId()).isEqualTo(areaId);
    }

    @Test
    void apply_showsTheAssignedPartnerInTheResponse_andNoneWhenThereIsNoPartner() {
        when(tenantRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(tenantRepository.existsByRegistrationCode(anyString())).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            if (t.getId() == null) t.setId(UUID.randomUUID());
            return t;
        });
        when(imageStorageService.mediaUrl(any())).thenAnswer(inv -> "/media/images/" + inv.getArgument(0));
        Partner meera = new Partner();
        meera.setName("Meera");
        meera.setWhatsapp("919876543210");
        when(partnerAssignmentService.assign(any(Tenant.class))).thenReturn(Optional.of(meera));

        VendorApplicationResponse withPartner = vendorApplicationService.apply(applicationRequest(), doc("shop"), null, null);
        assertThat(withPartner.partner().name()).isEqualTo("Meera");
        assertThat(withPartner.partner().whatsapp()).isEqualTo("919876543210");

        when(partnerAssignmentService.assign(any(Tenant.class))).thenReturn(Optional.empty());
        VendorApplicationResponse without = vendorApplicationService.apply(applicationRequest(), doc("shop"), null, null);
        assertThat(without.partner()).isNull();
    }

    @Test
    void apply_createsTheDraftListingWithTheChosenCategoryAndAreaAlreadySet() {
        when(tenantRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(tenantRepository.existsByRegistrationCode(anyString())).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            if (t.getId() == null) t.setId(UUID.randomUUID());
            return t;
        });
        when(imageStorageService.mediaUrl(any())).thenAnswer(inv -> "/media/images/" + inv.getArgument(0));

        vendorApplicationService.apply(applicationRequest(), doc("shop"), doc("gst"), doc("id"));

        ArgumentCaptor<com.xerxes.msme.marketplace.dto.UpdateMarketplaceListingRequest> listingCaptor =
                ArgumentCaptor.forClass(com.xerxes.msme.marketplace.dto.UpdateMarketplaceListingRequest.class);
        verify(marketplaceListingService).updateListing(any(), listingCaptor.capture());
        assertThat(listingCaptor.getValue().categoryId()).isEqualTo(marketplaceCategoryId);
        assertThat(listingCaptor.getValue().area()).isEqualTo("Nigdi");
        assertThat(listingCaptor.getValue().orderMode()).isEqualTo(MarketplaceOrderMode.WHATSAPP);
    }

    @Test
    void apply_seedsTheListingPhotoFromTheShopFrontKycPhotoWithoutGuidelines() {
        when(tenantRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(tenantRepository.existsByRegistrationCode(anyString())).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            if (t.getId() == null) t.setId(UUID.randomUUID());
            return t;
        });
        when(imageStorageService.mediaUrl(any())).thenAnswer(inv -> "/media/images/" + inv.getArgument(0));
        MockMultipartFile shopPhoto = doc("shop");

        vendorApplicationService.apply(applicationRequest(), shopPhoto, doc("gst"), doc("id"));

        verify(marketplaceListingService).adoptPhotoWithoutGuidelines(any(), eq(shopPhoto));
    }

    @Test
    void apply_duplicateEmail_throwsAndNeverPersistsOrUploads() {
        when(tenantRepository.existsByEmailIgnoreCase("veera@example.com")).thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> vendorApplicationService.apply(applicationRequest(), doc("shop"), doc("gst"), doc("id")));

        verify(tenantRepository, org.mockito.Mockito.never()).save(any());
        verify(imageStorageService, org.mockito.Mockito.never()).upload(any(), any(), any());
    }

    @Test
    void apply_unknownMarketplaceCategory_throwsAndNeverPersistsOrUploads() {
        when(tenantRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(marketplaceCategoryRepository.existsById(marketplaceCategoryId)).thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> vendorApplicationService.apply(applicationRequest(), doc("shop"), doc("gst"), doc("id")));

        verify(tenantRepository, org.mockito.Mockito.never()).save(any());
        verify(imageStorageService, org.mockito.Mockito.never()).upload(any(), any(), any());
    }

    @Test
    void apply_unknownState_throwsAndNeverPersistsOrUploads() {
        when(tenantRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(serviceStateRepository.findById("27")).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> vendorApplicationService.apply(applicationRequest(), doc("shop"), doc("gst"), doc("id")));

        verify(tenantRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void apply_inactiveState_throwsAsIfUnknown() {
        when(tenantRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        ServiceState disabledMaharashtra = new ServiceState();
        disabledMaharashtra.setCode("27");
        disabledMaharashtra.setName("Maharashtra");
        disabledMaharashtra.setActive(false);
        when(serviceStateRepository.findById("27")).thenReturn(Optional.of(disabledMaharashtra));

        assertThrows(
                ResourceNotFoundException.class,
                () -> vendorApplicationService.apply(applicationRequest(), doc("shop"), doc("gst"), doc("id")));

        verify(tenantRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void apply_unknownCity_throwsAndNeverPersistsOrUploads() {
        when(tenantRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(serviceCityRepository.findById(cityId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> vendorApplicationService.apply(applicationRequest(), doc("shop"), doc("gst"), doc("id")));

        verify(tenantRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void apply_cityBelongsToADifferentState_throwsAsIfUnknown() {
        when(tenantRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        ServiceCity wrongStateCity = new ServiceCity();
        wrongStateCity.setId(cityId);
        wrongStateCity.setStateCode("09"); // request uses stateCode "27"
        wrongStateCity.setName("Lucknow");
        when(serviceCityRepository.findById(cityId)).thenReturn(Optional.of(wrongStateCity));

        assertThrows(
                ResourceNotFoundException.class,
                () -> vendorApplicationService.apply(applicationRequest(), doc("shop"), doc("gst"), doc("id")));

        verify(tenantRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void apply_unknownArea_throwsAndNeverPersistsOrUploads() {
        when(tenantRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(serviceAreaRepository.findById(areaId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> vendorApplicationService.apply(applicationRequest(), doc("shop"), doc("gst"), doc("id")));

        verify(tenantRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void apply_areaBelongsToADifferentCity_throwsAsIfUnknown() {
        when(tenantRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        ServiceArea wrongCityArea = new ServiceArea();
        wrongCityArea.setId(areaId);
        wrongCityArea.setCityId(UUID.randomUUID()); // not this request's cityId
        wrongCityArea.setName("Somewhere Else");
        wrongCityArea.setPincode("000000");
        when(serviceAreaRepository.findById(areaId)).thenReturn(Optional.of(wrongCityArea));

        assertThrows(
                ResourceNotFoundException.class,
                () -> vendorApplicationService.apply(applicationRequest(), doc("shop"), doc("gst"), doc("id")));

        verify(tenantRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void apply_validCityAndArea_setsStructuredAndDerivedLegacyFieldsOnTheTenant() {
        when(tenantRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(tenantRepository.existsByRegistrationCode(anyString())).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            if (t.getId() == null) t.setId(UUID.randomUUID());
            return t;
        });
        when(imageStorageService.mediaUrl(any())).thenAnswer(inv -> "/media/images/" + inv.getArgument(0));

        vendorApplicationService.apply(applicationRequest(), doc("shop"), doc("gst"), doc("id"));

        ArgumentCaptor<Tenant> captor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        Tenant saved = captor.getValue();
        assertThat(saved.getCityId()).isEqualTo(cityId);
        assertThat(saved.getAreaId()).isEqualTo(areaId);
        assertThat(saved.getLandmark()).isEqualTo("Near XYZ Mall");
        assertThat(saved.getCity()).isEqualTo("Pune");
        assertThat(saved.getPincode()).isEqualTo("411044");
    }

    @Test
    void apply_registrationCodeCollision_retriesUntilUnique() {
        when(tenantRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(tenantRepository.existsByRegistrationCode(anyString())).thenReturn(true, true, false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            if (t.getId() == null) t.setId(UUID.randomUUID());
            return t;
        });
        when(imageStorageService.mediaUrl(any())).thenReturn("/media/images/bucket/x");

        vendorApplicationService.apply(applicationRequest(), doc("shop"), doc("gst"), doc("id"));

        verify(tenantRepository, org.mockito.Mockito.times(3)).existsByRegistrationCode(anyString());
    }

    private Tenant pendingTenant(UUID tenantId, String code) {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setBusinessName("Veera Collections");
        tenant.setBusinessType(BusinessType.RETAILER);
        tenant.setStateCode("27");
        tenant.setSubscriptionPlan(MembershipType.FREE);
        tenant.setRegistrationCode(code);
        tenant.setActive(false);
        return tenant;
    }

    @Test
    void getByCode_knownCode_returnsDetailsWithProductCountAndLimit() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = pendingTenant(tenantId, "ABCD1234");
        when(tenantRepository.findByRegistrationCode("ABCD1234")).thenReturn(Optional.of(tenant));
        when(productRepository.countByTenantId(tenantId)).thenReturn(3L);
        when(marketplaceListingService.getListing(tenantId))
                .thenReturn(new MarketplaceListingResponse(
                        null, null, null, null, null, null, null, null, null, null, null, null, null,
                        MarketplaceOrderMode.WHATSAPP, MarketplaceListingStatus.DRAFT, null, null));

        VendorApplicationDetailsResponse response = vendorApplicationService.getByCode("ABCD1234");

        assertThat(response.tenantId()).isEqualTo(tenantId);
        assertThat(response.productCount()).isEqualTo(3);
        assertThat(response.productLimit()).isEqualTo(10); // FREE
    }

    @Test
    void getByCode_unknownCode_throwsResourceNotFound() {
        when(tenantRepository.findByRegistrationCode("NOPE0000")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> vendorApplicationService.getByCode("NOPE0000"));
    }

    @Test
    void selectPlan_savesTheChosenPlanOnTheTenant() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = pendingTenant(tenantId, "ABCD1234");
        when(tenantRepository.findByRegistrationCode("ABCD1234")).thenReturn(Optional.of(tenant));
        when(productRepository.countByTenantId(tenantId)).thenReturn(0L);
        when(marketplaceListingService.getListing(tenantId))
                .thenReturn(new MarketplaceListingResponse(
                        null, null, null, null, null, null, null, null, null, null, null, null, null,
                        MarketplaceOrderMode.WHATSAPP, MarketplaceListingStatus.DRAFT, null, null));

        VendorApplicationDetailsResponse response = vendorApplicationService.selectPlan("ABCD1234", MembershipType.PLUS);

        assertThat(tenant.getSubscriptionPlan()).isEqualTo(MembershipType.PLUS);
        assertThat(response.subscriptionPlan()).isEqualTo(MembershipType.PLUS);
        verify(tenantRepository).save(tenant);
    }

    @Test
    void createStarterProducts_resolvesTenantIdFromCodeAndDelegates() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = pendingTenant(tenantId, "ABCD1234");
        when(tenantRepository.findByRegistrationCode("ABCD1234")).thenReturn(Optional.of(tenant));
        List<UUID> ids = List.of(UUID.randomUUID());

        vendorApplicationService.createStarterProducts("ABCD1234", ids);

        verify(starterCatalogService).createStarterProducts(tenantId, ids);
    }

    @Test
    void updateListing_unknownCode_throwsResourceNotFoundBeforeDelegating() {
        when(tenantRepository.findByRegistrationCode("NOPE0000")).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class, () -> vendorApplicationService.updateListing("NOPE0000", null));

        verify(marketplaceListingService, org.mockito.Mockito.never()).updateListing(any(), any());
    }

    @Test
    void apply_kycDocumentsNotRequired_acceptsTheApplicationWithoutGstOrOwnerIdDocuments() {
        ReflectionTestUtils.setField(vendorApplicationService, "kycDocumentsRequired", false);
        when(tenantRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(tenantRepository.existsByRegistrationCode(anyString())).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            if (t.getId() == null) t.setId(UUID.randomUUID());
            return t;
        });
        when(imageStorageService.mediaUrl(any())).thenAnswer(inv -> "/media/images/" + inv.getArgument(0));

        vendorApplicationService.apply(applicationRequest(), doc("shop"), null, null);

        ArgumentCaptor<Tenant> captor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        Tenant saved = captor.getValue();
        assertThat(saved.getShopFrontPhotoUrl()).isNotBlank();
        assertThat(saved.getGstCertificateUrl()).isNull();
        assertThat(saved.getOwnerIdDocumentUrl()).isNull();
        // Only the shop front photo is uploaded - not 3 calls like the all-documents test above.
        verify(imageStorageService, org.mockito.Mockito.times(1)).upload(any(), any(), any());
    }

    @Test
    void apply_kycDocumentsNotRequired_stillIgnoresThemEvenIfAClientSendsThemAnyway() {
        // The frontend hides these upload fields when the flag is off, but this proves the
        // backend doesn't rely on that - a raw API call providing both files must still never
        // store them, since they'd land in the same public-but-unlisted bucket as shop photos.
        ReflectionTestUtils.setField(vendorApplicationService, "kycDocumentsRequired", false);
        when(tenantRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(tenantRepository.existsByRegistrationCode(anyString())).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            if (t.getId() == null) t.setId(UUID.randomUUID());
            return t;
        });
        when(imageStorageService.mediaUrl(any())).thenAnswer(inv -> "/media/images/" + inv.getArgument(0));

        vendorApplicationService.apply(applicationRequest(), doc("shop"), doc("gst"), doc("id"));

        ArgumentCaptor<Tenant> captor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        Tenant saved = captor.getValue();
        assertThat(saved.getGstCertificateUrl()).isNull();
        assertThat(saved.getOwnerIdDocumentUrl()).isNull();
        // Only the shop front photo is uploaded - the gst/id MultipartFiles are never read.
        verify(imageStorageService, org.mockito.Mockito.times(1)).upload(any(), any(), any());
    }

    @Test
    void apply_kycDocumentsRequired_missingGstCertificate_throwsAndNeverPersistsOrUploads() {
        ReflectionTestUtils.setField(vendorApplicationService, "kycDocumentsRequired", true);
        when(tenantRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);

        assertThrows(
                BusinessRuleViolationException.class,
                () -> vendorApplicationService.apply(applicationRequest(), doc("shop"), null, doc("id")));

        verify(tenantRepository, org.mockito.Mockito.never()).save(any());
        verify(imageStorageService, org.mockito.Mockito.never()).upload(any(), any(), any());
    }

    @Test
    void apply_kycDocumentsRequired_missingOwnerIdDocument_throwsAndNeverPersistsOrUploads() {
        ReflectionTestUtils.setField(vendorApplicationService, "kycDocumentsRequired", true);
        when(tenantRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);

        assertThrows(
                BusinessRuleViolationException.class,
                () -> vendorApplicationService.apply(applicationRequest(), doc("shop"), doc("gst"), null));

        verify(tenantRepository, org.mockito.Mockito.never()).save(any());
        verify(imageStorageService, org.mockito.Mockito.never()).upload(any(), any(), any());
    }
}
