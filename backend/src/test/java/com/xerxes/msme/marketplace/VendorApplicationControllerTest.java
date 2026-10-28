package com.xerxes.msme.marketplace;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerxes.msme.marketplace.dto.CreateStarterProductsRequest;
import com.xerxes.msme.marketplace.dto.MarketplaceListingResponse;
import com.xerxes.msme.marketplace.dto.SelectMembershipPlanRequest;
import com.xerxes.msme.marketplace.dto.UpdateMarketplaceListingRequest;
import com.xerxes.msme.marketplace.dto.VendorApplicationDetailsResponse;
import com.xerxes.msme.marketplace.dto.VendorApplicationResponse;
import com.xerxes.msme.tenant.BusinessType;
import com.xerxes.msme.tenant.MembershipType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = VendorApplicationController.class)
@AutoConfigureMockMvc(addFilters = false)
class VendorApplicationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private VendorApplicationService vendorApplicationService;

    private final UUID tenantId = UUID.randomUUID();

    @Test
    void apply_multipartWithThreeDocuments_returns201WithCode() throws Exception {
        String applicationJson = objectMapper.writeValueAsString(java.util.Map.of(
                "businessName", "Veera Collections",
                "businessType", "RETAILER",
                "marketplaceCategoryId", UUID.randomUUID().toString(),
                "stateCode", "27",
                "cityId", UUID.randomUUID().toString(),
                "areaId", UUID.randomUUID().toString(),
                "ownerName", "Veera",
                "email", "veera@example.com",
                "password", "SecurePass123"));
        MockMultipartFile applicationPart =
                new MockMultipartFile("application", "", "application/json", applicationJson.getBytes());
        MockMultipartFile shopPhoto = new MockMultipartFile("shopFrontPhoto", "shop.jpg", "image/jpeg", "x".getBytes());
        MockMultipartFile gstCert = new MockMultipartFile("gstCertificate", "gst.jpg", "image/jpeg", "x".getBytes());
        MockMultipartFile ownerId = new MockMultipartFile("ownerIdDocument", "id.jpg", "image/jpeg", "x".getBytes());

        when(vendorApplicationService.apply(any(), any(), any(), any()))
                .thenReturn(new VendorApplicationResponse(tenantId, "ABCD1234", "Veera Collections", null));

        mockMvc.perform(multipart("/api/public/vendor-applications")
                        .file(applicationPart)
                        .file(shopPhoto)
                        .file(gstCert)
                        .file(ownerId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registrationCode").value("ABCD1234"));
    }

    @Test
    void apply_emailWithoutDomainExtension_returns400() throws Exception {
        String applicationJson = objectMapper.writeValueAsString(java.util.Map.of(
                "businessName", "Veera Collections",
                "businessType", "RETAILER",
                "marketplaceCategoryId", UUID.randomUUID().toString(),
                "stateCode", "27",
                "cityId", UUID.randomUUID().toString(),
                "areaId", UUID.randomUUID().toString(),
                "ownerName", "Veera",
                "email", "veera@test",
                "password", "SecurePass123"));
        MockMultipartFile applicationPart =
                new MockMultipartFile("application", "", "application/json", applicationJson.getBytes());
        MockMultipartFile shopPhoto = new MockMultipartFile("shopFrontPhoto", "shop.jpg", "image/jpeg", "x".getBytes());

        mockMvc.perform(multipart("/api/public/vendor-applications").file(applicationPart).file(shopPhoto))
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_knownCode_returnsDetails() throws Exception {
        when(vendorApplicationService.getByCode("ABCD1234"))
                .thenReturn(new VendorApplicationDetailsResponse(
                        tenantId, "Veera Collections", BusinessType.RETAILER, "27", null, null, null, null,
                        MembershipType.FREE, 0, 0, null, null));

        mockMvc.perform(get("/api/public/vendor-applications/ABCD1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessName").value("Veera Collections"));
    }

    @Test
    void updateListing_delegatesWithCodeAndRequest() throws Exception {
        UpdateMarketplaceListingRequest request = new UpdateMarketplaceListingRequest(
                null, "Gujar Nagar", null, null, null, null, null, null, null, null, MarketplaceOrderMode.WHATSAPP);
        when(vendorApplicationService.updateListing(eq("ABCD1234"), any())).thenReturn(null);

        mockMvc.perform(post("/api/public/vendor-applications/ABCD1234/listing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(vendorApplicationService).updateListing(eq("ABCD1234"), any());
    }

    @Test
    void uploadPhoto_delegatesWithCodeAndFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "shop.jpg", "image/jpeg", "x".getBytes());
        when(vendorApplicationService.uploadListingPhoto(eq("ABCD1234"), any())).thenReturn(null);

        mockMvc.perform(multipart("/api/public/vendor-applications/ABCD1234/photo").file(file))
                .andExpect(status().isOk());

        verify(vendorApplicationService).uploadListingPhoto(eq("ABCD1234"), any());
    }

    @Test
    void selectPlan_delegatesWithCodeAndPlan() throws Exception {
        when(vendorApplicationService.selectPlan("ABCD1234", MembershipType.PLUS))
                .thenReturn(new VendorApplicationDetailsResponse(
                        tenantId, "Veera Collections", BusinessType.RETAILER, "27", null, null, null, null,
                        MembershipType.PLUS, 0, 100, null, null));

        mockMvc.perform(post("/api/public/vendor-applications/ABCD1234/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SelectMembershipPlanRequest(MembershipType.PLUS))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionPlan").value("PLUS"));
    }

    @Test
    void createStarterProducts_delegatesWithCodeAndIds() throws Exception {
        UUID defaultProductId = UUID.randomUUID();

        mockMvc.perform(post("/api/public/vendor-applications/ABCD1234/starter-products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateStarterProductsRequest(List.of(defaultProductId)))))
                .andExpect(status().isCreated());

        verify(vendorApplicationService).createStarterProducts(eq("ABCD1234"), eq(List.of(defaultProductId)));
    }

    @Test
    void submit_delegatesWithCode() throws Exception {
        when(vendorApplicationService.submit("ABCD1234")).thenReturn((MarketplaceListingResponse) null);

        mockMvc.perform(post("/api/public/vendor-applications/ABCD1234/submit"))
                .andExpect(status().isOk());

        verify(vendorApplicationService).submit("ABCD1234");
    }

    @Test
    void apply_withoutGstCertificateOrOwnerIdDocument_stillAccepted() throws Exception {
        String applicationJson = objectMapper.writeValueAsString(java.util.Map.of(
                "businessName", "Veera Collections",
                "businessType", "RETAILER",
                "marketplaceCategoryId", UUID.randomUUID().toString(),
                "stateCode", "27",
                "cityId", UUID.randomUUID().toString(),
                "areaId", UUID.randomUUID().toString(),
                "ownerName", "Veera",
                "email", "veera@example.com",
                "password", "SecurePass123"));
        MockMultipartFile applicationPart =
                new MockMultipartFile("application", "", "application/json", applicationJson.getBytes());
        MockMultipartFile shopPhoto = new MockMultipartFile("shopFrontPhoto", "shop.jpg", "image/jpeg", "x".getBytes());

        when(vendorApplicationService.apply(any(), any(), eq(null), eq(null)))
                .thenReturn(new VendorApplicationResponse(tenantId, "ABCD1234", "Veera Collections", null));

        // No gstCertificate/ownerIdDocument parts at all - required=false on the controller means
        // this doesn't 400 at the binding level; VendorApplicationService decides if that's allowed.
        mockMvc.perform(multipart("/api/public/vendor-applications").file(applicationPart).file(shopPhoto))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registrationCode").value("ABCD1234"));
    }

    @Test
    void config_returnsWhetherKycDocumentsAreRequired() throws Exception {
        when(vendorApplicationService.isKycDocumentsRequired()).thenReturn(false);

        mockMvc.perform(get("/api/public/vendor-applications/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycDocumentsRequired").value(false));
    }
}
