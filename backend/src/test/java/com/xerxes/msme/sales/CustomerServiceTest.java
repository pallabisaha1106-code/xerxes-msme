package com.xerxes.msme.sales;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.sales.dto.CustomerRequest;
import com.xerxes.msme.sales.dto.CustomerResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock private CustomerRepository customerRepository;

    private CustomerService customerService;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        customerService = new CustomerService(customerRepository);
        tenantId = UUID.randomUUID();
    }

    private CustomerRequest request() {
        return new CustomerRequest(
                "Retail Buyer Ltd", null, "27", "Contact", "9999999999", "c@example.com", "Addr1", null, "Mumbai",
                "400001");
    }

    @Test
    void create_savesAndReturnsResponse() {
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        CustomerResponse response = customerService.create(tenantId, request());

        assertThat(response.name()).isEqualTo("Retail Buyer Ltd");
        assertThat(response.stateCode()).isEqualTo("27");
    }

    @Test
    void list_returnsAllCustomersForTenant() {
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setName("Customer A");
        when(customerRepository.findAllByTenantId(tenantId)).thenReturn(List.of(customer));

        List<CustomerResponse> responses = customerService.list(tenantId);

        assertThat(responses).extracting("name").containsExactly("Customer A");
    }

    @Test
    void get_unknownId_throwsResourceNotFound() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findByIdAndTenantId(customerId, tenantId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> customerService.get(tenantId, customerId));
    }

    @Test
    void get_found_returnsResponse() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setName("Customer B");
        when(customerRepository.findByIdAndTenantId(customerId, tenantId)).thenReturn(Optional.of(customer));

        CustomerResponse response = customerService.get(tenantId, customerId);

        assertThat(response.name()).isEqualTo("Customer B");
    }
}
