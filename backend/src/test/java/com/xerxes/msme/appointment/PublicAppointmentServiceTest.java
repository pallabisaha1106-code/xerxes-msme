package com.xerxes.msme.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xerxes.msme.appointment.dto.AvailabilityResponse;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.marketplace.ImageProfile;
import com.xerxes.msme.marketplace.MarketplaceOrderMode;
import com.xerxes.msme.marketplace.PublicMarketplaceService;
import com.xerxes.msme.marketplace.dto.PublicMarketplaceListingResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublicAppointmentServiceTest {

    @Mock private PublicMarketplaceService publicMarketplaceService;
    @Mock private AppointmentAvailabilityService availabilityService;

    private PublicAppointmentService service;
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new PublicAppointmentService(publicMarketplaceService, availabilityService);
    }

    private PublicMarketplaceListingResponse listing(boolean bookable) {
        return new PublicMarketplaceListingResponse(tenantId, "Glow Salon", UUID.randomUUID(), "Salon & Spa", "Kothrud", null, null,
                null, null, null, null, null, null, "9876543210", MarketplaceOrderMode.WHATSAPP, ImageProfile.COMPACT, bookable);
    }

    @Test
    void aLiveBookableShop_getsItsAvailability() {
        AvailabilityResponse expected = new AvailabilityResponse(30, List.of());
        when(publicMarketplaceService.getListing(tenantId)).thenReturn(listing(true));
        when(availabilityService.availability(tenantId)).thenReturn(expected);

        assertThat(service.availability(tenantId)).isSameAs(expected);
    }

    @Test
    void aShopInANonBookableCategory_isNotFound_andNothingIsComputed() {
        when(publicMarketplaceService.getListing(tenantId)).thenReturn(listing(false));

        assertThrows(ResourceNotFoundException.class, () -> service.availability(tenantId));

        verify(availabilityService, never()).availability(tenantId);
    }

    @Test
    void aShopThatIsNotLiveOrIsSuspended_isNotFound() {
        when(publicMarketplaceService.getListing(tenantId)).thenThrow(ResourceNotFoundException.of("MarketplaceListing", tenantId));

        assertThrows(ResourceNotFoundException.class, () -> service.availability(tenantId));

        verify(availabilityService, never()).availability(tenantId);
    }
}
