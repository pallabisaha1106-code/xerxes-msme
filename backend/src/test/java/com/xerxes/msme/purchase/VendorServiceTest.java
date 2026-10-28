package com.xerxes.msme.purchase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.purchase.dto.VendorRequest;
import com.xerxes.msme.purchase.dto.VendorResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VendorServiceTest {

    @Mock private VendorRepository vendorRepository;

    private VendorService vendorService;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        vendorService = new VendorService(vendorRepository);
        tenantId = UUID.randomUUID();
    }

    private VendorRequest request() {
        return new VendorRequest(
                "Fabric Supplier Co", "27AAAPF5678D1Z3", "27", "Contact", "9999999999", "v@example.com", "Addr1",
                null, "Mumbai", "400001");
    }

    @Test
    void create_savesAndReturnsResponse() {
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(inv -> {
            Vendor v = inv.getArgument(0);
            v.setId(UUID.randomUUID());
            return v;
        });

        VendorResponse response = vendorService.create(tenantId, request());

        assertThat(response.name()).isEqualTo("Fabric Supplier Co");
        assertThat(response.gstin()).isEqualTo("27AAAPF5678D1Z3");
    }

    @Test
    void list_returnsAllVendorsForTenant() {
        Vendor vendor = new Vendor();
        vendor.setId(UUID.randomUUID());
        vendor.setName("Vendor A");
        when(vendorRepository.findAllByTenantId(tenantId)).thenReturn(List.of(vendor));

        List<VendorResponse> responses = vendorService.list(tenantId);

        assertThat(responses).extracting("name").containsExactly("Vendor A");
    }

    @Test
    void get_unknownId_throwsResourceNotFound() {
        UUID vendorId = UUID.randomUUID();
        when(vendorRepository.findByIdAndTenantId(vendorId, tenantId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> vendorService.get(tenantId, vendorId));
    }

    @Test
    void get_found_returnsResponse() {
        UUID vendorId = UUID.randomUUID();
        Vendor vendor = new Vendor();
        vendor.setId(vendorId);
        vendor.setName("Vendor B");
        when(vendorRepository.findByIdAndTenantId(vendorId, tenantId)).thenReturn(Optional.of(vendor));

        VendorResponse response = vendorService.get(tenantId, vendorId);

        assertThat(response.name()).isEqualTo("Vendor B");
    }
}
