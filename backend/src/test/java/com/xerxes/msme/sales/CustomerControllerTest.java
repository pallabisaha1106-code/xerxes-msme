package com.xerxes.msme.sales;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerxes.msme.common.BulkImportResult;
import com.xerxes.msme.common.BulkImportSupport;
import com.xerxes.msme.sales.dto.CustomerRequest;
import com.xerxes.msme.sales.dto.CustomerResponse;
import com.xerxes.msme.security.AuthContext;
import com.xerxes.msme.security.AuthenticatedPrincipal;
import com.xerxes.msme.user.Role;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CustomerController.class)
@AutoConfigureMockMvc(addFilters = false)
class CustomerControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private CustomerService customerService;
    @MockBean private AuthContext authContext;
    @MockBean private BulkImportSupport bulkImportSupport;

    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void stubCurrentUser() {
        when(authContext.current())
                .thenReturn(new AuthenticatedPrincipal(UUID.randomUUID(), tenantId, "a@b.com", Role.OWNER, UUID.randomUUID()));
    }

    private CustomerRequest validRequest() {
        return new CustomerRequest("Retail Buyer Ltd", null, "27", null, null, null, null, null, null, null);
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        CustomerResponse response = new CustomerResponse(
                UUID.randomUUID(), "Retail Buyer Ltd", null, "27", null, null, null, null, null, null, null);
        when(customerService.create(eq(tenantId), any())).thenReturn(response);

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Retail Buyer Ltd"));
    }

    @Test
    void create_blankName_returns400() throws Exception {
        CustomerRequest request = new CustomerRequest("", null, "27", null, null, null, null, null, null, null);

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_returnsCustomersForCurrentTenant() throws Exception {
        when(customerService.list(tenantId)).thenReturn(List.of());

        mockMvc.perform(get("/api/customers")).andExpect(status().isOk());
    }

    @Test
    void get_returnsCustomerById() throws Exception {
        UUID customerId = UUID.randomUUID();
        when(customerService.get(tenantId, customerId))
                .thenReturn(new CustomerResponse(
                        customerId, "Customer A", null, null, null, null, null, null, null, null, null));

        mockMvc.perform(get("/api/customers/" + customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Customer A"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void bulkCreate_delegatesToBulkImportSupportWithTenantScopedCreator() throws Exception {
        BulkImportResult result = new BulkImportResult(1, 0, List.of());
        when(bulkImportSupport.run(any(), any())).thenReturn(result);

        mockMvc.perform(post("/api/customers/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(validRequest()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successCount").value(1));

        ArgumentCaptor<Consumer<CustomerRequest>> creatorCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(bulkImportSupport).run(any(), creatorCaptor.capture());

        CustomerRequest request = validRequest();
        creatorCaptor.getValue().accept(request);
        verify(customerService).create(tenantId, request);
    }
}
