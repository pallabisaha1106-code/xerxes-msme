package com.xerxes.msme.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.xerxes.msme.catalog.dto.ProductCapacityResponse;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.tenant.MembershipType;
import com.xerxes.msme.tenant.Tenant;
import com.xerxes.msme.tenant.TenantRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductCapacityServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private ProductRepository productRepository;

    private ProductCapacityService service;
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ProductCapacityService(tenantRepository, productRepository);
    }

    private void tenantOnPlan(MembershipType plan) {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setSubscriptionPlan(plan);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
    }

    @Test
    void capacity_reportsLimitUsedAndRemaining() {
        tenantOnPlan(MembershipType.PLUS);
        when(productRepository.countByTenantId(tenantId)).thenReturn(37L);

        ProductCapacityResponse response = service.capacity(tenantId);

        assertThat(response.limit()).isEqualTo(100);
        assertThat(response.used()).isEqualTo(37);
        assertThat(response.remaining()).isEqualTo(63);
    }

    @Test
    void capacity_atTheLimit_hasNothingRemaining() {
        tenantOnPlan(MembershipType.PLUS);
        when(productRepository.countByTenantId(tenantId)).thenReturn(100L);

        assertThat(service.capacity(tenantId).remaining()).isZero();
    }

    @Test
    void capacity_usesTheLimitOfTheShopsOwnPlan() {
        tenantOnPlan(MembershipType.FREE);
        when(productRepository.countByTenantId(tenantId)).thenReturn(4L);

        ProductCapacityResponse response = service.capacity(tenantId);

        assertThat(response.limit()).isEqualTo(MembershipType.FREE.productLimit());
        assertThat(response.remaining()).isEqualTo(MembershipType.FREE.productLimit() - 4);
    }

    @Test
    void capacity_overTheLimit_neverGoesNegative() {
        tenantOnPlan(MembershipType.PLUS);
        when(productRepository.countByTenantId(tenantId)).thenReturn(120L);

        assertThat(service.capacity(tenantId).remaining()).isZero();
    }

    @Test
    void capacity_unknownTenant_throwsResourceNotFound() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.capacity(tenantId));
    }
}
