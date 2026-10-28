package com.xerxes.msme.appointment;

import com.xerxes.msme.appointment.dto.AvailabilityResponse;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.marketplace.PublicMarketplaceService;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * What shoppers may see of a business's appointments: only for a shop that is live on the marketplace, not suspended
 * and in a bookable category. Anything else is "not found", the same as an unknown shop, so nothing is revealed.
 */
@Service
public class PublicAppointmentService {

    private final PublicMarketplaceService publicMarketplaceService;
    private final AppointmentAvailabilityService availabilityService;

    public PublicAppointmentService(
            PublicMarketplaceService publicMarketplaceService, AppointmentAvailabilityService availabilityService) {
        this.publicMarketplaceService = publicMarketplaceService;
        this.availabilityService = availabilityService;
    }

    public AvailabilityResponse availability(UUID tenantId) {
        // getListing already refuses a shop that isn't live or is suspended.
        if (!publicMarketplaceService.getListing(tenantId).bookable()) {
            throw ResourceNotFoundException.of("MarketplaceListing", tenantId);
        }
        return availabilityService.availability(tenantId);
    }
}
