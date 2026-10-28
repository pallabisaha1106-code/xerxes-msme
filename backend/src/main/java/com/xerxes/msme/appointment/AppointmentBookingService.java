package com.xerxes.msme.appointment;

import com.xerxes.msme.appointment.dto.AppointmentRequestedResponse;
import com.xerxes.msme.appointment.dto.CreateAppointmentRequest;
import com.xerxes.msme.catalog.Product;
import com.xerxes.msme.catalog.ProductRepository;
import com.xerxes.msme.catalog.ProductVariant;
import com.xerxes.msme.catalog.ProductVariantRepository;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.marketplace.PublicMarketplaceService;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A shopper requesting a slot. Nothing is locked: the time just has to be free right now (the same availability a
 * shopper was shown), and other people may well ask for the same time - the business decides who gets it.
 */
@Service
public class AppointmentBookingService {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MAX_SERVICE_NAME = 200;

    private final PublicMarketplaceService publicMarketplaceService;
    private final AppointmentAvailabilityService availabilityService;
    private final AppointmentSettingsService settingsService;
    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final AppointmentRepository appointmentRepository;
    private final Clock clock;

    public AppointmentBookingService(
            PublicMarketplaceService publicMarketplaceService,
            AppointmentAvailabilityService availabilityService,
            AppointmentSettingsService settingsService,
            ProductVariantRepository variantRepository,
            ProductRepository productRepository,
            AppointmentRepository appointmentRepository,
            Clock clock) {
        this.publicMarketplaceService = publicMarketplaceService;
        this.availabilityService = availabilityService;
        this.settingsService = settingsService;
        this.variantRepository = variantRepository;
        this.productRepository = productRepository;
        this.appointmentRepository = appointmentRepository;
        this.clock = clock;
    }

    @Transactional
    public AppointmentRequestedResponse request(UUID tenantId, CreateAppointmentRequest request) {
        // getListing already refuses a shop that isn't live or is suspended.
        if (!publicMarketplaceService.getListing(tenantId).bookable()) {
            throw ResourceNotFoundException.of("MarketplaceListing", tenantId);
        }
        String phone = PhoneNumbers.normalizeIndianMobile(request.phone())
                .orElseThrow(() -> new BusinessRuleViolationException("Enter a valid 10-digit mobile number."));

        ProductVariant variant = variantRepository
                .findByIdAndTenantId(request.variantId(), tenantId)
                .filter(ProductVariant::isActive)
                .orElseThrow(() -> new BusinessRuleViolationException("That service is not available."));
        Product product = productRepository
                .findByIdAndTenantId(variant.getProductId(), tenantId)
                .orElseThrow(() -> new BusinessRuleViolationException("That service is not available."));

        if (!availabilityService.freeTimes(tenantId, request.date()).contains(request.time().format(TIME))) {
            throw new BusinessRuleViolationException("That time is no longer available. Please pick another.");
        }

        int slotMinutes = settingsService.get(tenantId).slotMinutes();
        OffsetDateTime startsAt = request.date().atTime(request.time()).atZone(AppointmentRules.ZONE).toOffsetDateTime();
        OffsetDateTime endsAt = startsAt.plusMinutes(slotMinutes);

        if (appointmentRepository.countLiveOverlappingForPhone(tenantId, phone, startsAt, endsAt) > 0) {
            throw new BusinessRuleViolationException("You have already requested this time.");
        }
        long waiting = appointmentRepository.countByTenantIdAndCustomerPhoneAndStatusAndStartsAtGreaterThan(
                tenantId, phone, AppointmentStatus.PENDING, OffsetDateTime.now(clock));
        if (waiting >= AppointmentRules.MAX_PENDING_PER_PHONE) {
            throw new BusinessRuleViolationException("You already have " + AppointmentRules.MAX_PENDING_PER_PHONE
                    + " requests waiting with this business. Please wait to hear back before requesting more.");
        }

        Appointment appointment = new Appointment();
        appointment.setTenantId(tenantId);
        appointment.setVariantId(variant.getId());
        appointment.setServiceName(serviceName(product, variant));
        appointment.setPrice(variant.getSellingPrice());
        appointment.setCustomerName(request.name().trim());
        appointment.setCustomerPhone(phone);
        appointment.setNote(request.note() == null || request.note().isBlank() ? null : request.note().trim());
        appointment.setStartsAt(startsAt);
        appointment.setEndsAt(endsAt);
        appointmentRepository.save(appointment);

        return new AppointmentRequestedResponse(appointment.getServiceName(), request.date(), request.time().format(TIME), slotMinutes);
    }

    /** "Haircut (Men)": the product name, then the variant's attribute values if it has any. */
    private static String serviceName(Product product, ProductVariant variant) {
        String label = variant.getAttributes().values().stream()
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(", "));
        String name = label.isEmpty() ? product.getName() : product.getName() + " (" + label + ")";
        return name.length() > MAX_SERVICE_NAME ? name.substring(0, MAX_SERVICE_NAME) : name;
    }
}
