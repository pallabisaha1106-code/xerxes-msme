package com.xerxes.msme.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xerxes.msme.appointment.dto.AppointmentRequestedResponse;
import com.xerxes.msme.appointment.dto.AppointmentSettingsResponse;
import com.xerxes.msme.appointment.dto.CreateAppointmentRequest;
import com.xerxes.msme.catalog.Product;
import com.xerxes.msme.catalog.ProductRepository;
import com.xerxes.msme.catalog.ProductVariant;
import com.xerxes.msme.catalog.ProductVariantRepository;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.marketplace.ImageProfile;
import com.xerxes.msme.marketplace.MarketplaceOrderMode;
import com.xerxes.msme.marketplace.PublicMarketplaceService;
import com.xerxes.msme.marketplace.dto.PublicMarketplaceListingResponse;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentBookingServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 21);
    private static final Instant NOW = Instant.parse("2026-10-18T19:00:00Z");

    @Mock private PublicMarketplaceService publicMarketplaceService;
    @Mock private AppointmentAvailabilityService availabilityService;
    @Mock private AppointmentSettingsService settingsService;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private ProductRepository productRepository;
    @Mock private AppointmentRepository appointmentRepository;

    private AppointmentBookingService service;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID variantId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AppointmentBookingService(publicMarketplaceService, availabilityService, settingsService, variantRepository,
                productRepository, appointmentRepository, Clock.fixed(NOW, ZoneOffset.UTC));

        lenient().when(publicMarketplaceService.getListing(tenantId)).thenReturn(listing(true));
        lenient().when(availabilityService.freeTimes(tenantId, DAY)).thenReturn(List.of("10:00", "10:30", "11:00"));
        lenient().when(settingsService.get(tenantId)).thenReturn(new AppointmentSettingsResponse(30, AppointmentRules.SLOT_LENGTHS));
        lenient().when(variantRepository.findByIdAndTenantId(variantId, tenantId)).thenReturn(Optional.of(variant(true, Map.of("Type", "Men"))));
        Product product = new Product();
        product.setName("Haircut");
        lenient().when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));
    }

    private PublicMarketplaceListingResponse listing(boolean bookable) {
        return new PublicMarketplaceListingResponse(tenantId, "Glow Salon", UUID.randomUUID(), "Salon & Spa", "Kothrud", null, null,
                null, null, null, null, null, null, "9876543210", MarketplaceOrderMode.WHATSAPP, ImageProfile.COMPACT, bookable);
    }

    private ProductVariant variant(boolean active, Map<String, String> attributes) {
        ProductVariant variant = new ProductVariant();
        variant.setId(variantId);
        variant.setProductId(productId);
        variant.setActive(active);
        variant.setSellingPrice(new BigDecimal("300.00"));
        variant.setAttributes(new LinkedHashMap<>(attributes));
        return variant;
    }

    private static CreateAppointmentRequest request(String time, String phone, String note) {
        return new CreateAppointmentRequest(null, DAY, LocalTime.parse(time), "  Asha  ", phone, note);
    }

    private CreateAppointmentRequest valid() {
        return new CreateAppointmentRequest(variantId, DAY, LocalTime.of(10, 30), "  Asha  ", "98765 43210", "  first visit  ");
    }

    private void assertRefused(CreateAppointmentRequest request, String messagePart) {
        BusinessRuleViolationException error = assertThrows(BusinessRuleViolationException.class, () -> service.request(tenantId, request));
        assertThat(error.getMessage()).contains(messagePart);
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void aValidRequest_isSavedPendingWithCopiesOfTheServiceAndNormalisedDetails() {
        AppointmentRequestedResponse response = service.request(tenantId, valid());

        ArgumentCaptor<Appointment> saved = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(saved.capture());
        Appointment appointment = saved.getValue();
        assertThat(appointment.getTenantId()).isEqualTo(tenantId);
        assertThat(appointment.getVariantId()).isEqualTo(variantId);
        assertThat(appointment.getServiceName()).isEqualTo("Haircut (Men)");
        assertThat(appointment.getPrice()).isEqualByComparingTo("300.00");
        assertThat(appointment.getCustomerName()).isEqualTo("Asha");
        assertThat(appointment.getCustomerPhone()).isEqualTo("9876543210");
        assertThat(appointment.getNote()).isEqualTo("first visit");
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        // 10:30 India is 05:00 UTC; the appointment runs one slot (30 minutes).
        assertThat(appointment.getStartsAt().toInstant()).isEqualTo(Instant.parse("2026-10-21T05:00:00Z"));
        assertThat(appointment.getEndsAt().toInstant()).isEqualTo(Instant.parse("2026-10-21T05:30:00Z"));
        assertThat(response).isEqualTo(new AppointmentRequestedResponse("Haircut (Men)", DAY, "10:30", 30));
    }

    @Test
    void theAppointmentLengthComesFromTheBusinessesSettings() {
        when(settingsService.get(tenantId)).thenReturn(new AppointmentSettingsResponse(45, AppointmentRules.SLOT_LENGTHS));

        service.request(tenantId, valid());

        ArgumentCaptor<Appointment> saved = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(saved.capture());
        assertThat(saved.getValue().getEndsAt()).isEqualTo(saved.getValue().getStartsAt().plusMinutes(45));
    }

    @Test
    void aBlankNoteIsStoredAsNothing_andAVariantWithoutAttributesKeepsJustTheProductName() {
        when(variantRepository.findByIdAndTenantId(variantId, tenantId)).thenReturn(Optional.of(variant(true, Map.of())));

        service.request(tenantId, new CreateAppointmentRequest(variantId, DAY, LocalTime.of(10, 0), "Asha", "9876543210", "   "));

        ArgumentCaptor<Appointment> saved = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(saved.capture());
        assertThat(saved.getValue().getNote()).isNull();
        assertThat(saved.getValue().getServiceName()).isEqualTo("Haircut");
    }

    @Test
    void severalAttributesAreJoinedInTheServiceName() {
        when(variantRepository.findByIdAndTenantId(variantId, tenantId))
                .thenReturn(Optional.of(variant(true, new LinkedHashMap<>(Map.of("Type", "Women")) {{ put("Length", "Long"); }})));

        AppointmentRequestedResponse response = service.request(tenantId, valid());

        assertThat(response.serviceName()).isEqualTo("Haircut (Women, Long)");
    }

    @Test
    void aShopThatIsNotBookable_isNotFound() {
        when(publicMarketplaceService.getListing(tenantId)).thenReturn(listing(false));

        assertThrows(ResourceNotFoundException.class, () -> service.request(tenantId, valid()));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void aShopThatIsNotLiveOrIsSuspended_isNotFound() {
        when(publicMarketplaceService.getListing(tenantId)).thenThrow(ResourceNotFoundException.of("MarketplaceListing", tenantId));

        assertThrows(ResourceNotFoundException.class, () -> service.request(tenantId, valid()));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void aPhoneThatIsNotAnIndianMobile_isRefused() {
        assertRefused(new CreateAppointmentRequest(variantId, DAY, LocalTime.of(10, 30), "Asha", "12345", null), "valid 10-digit mobile number");
    }

    @Test
    void aServiceThatDoesNotExistForThisShopOrIsInactive_isRefused() {
        UUID other = UUID.randomUUID();
        when(variantRepository.findByIdAndTenantId(other, tenantId)).thenReturn(Optional.empty());
        assertRefused(new CreateAppointmentRequest(other, DAY, LocalTime.of(10, 30), "Asha", "9876543210", null), "not available");

        when(variantRepository.findByIdAndTenantId(variantId, tenantId)).thenReturn(Optional.of(variant(false, Map.of())));
        assertRefused(valid(), "not available");
    }

    @Test
    void aTimeThatIsNotCurrentlyFree_isRefused() {
        assertRefused(new CreateAppointmentRequest(variantId, DAY, LocalTime.of(12, 0), "Asha", "9876543210", null), "no longer available");
    }

    @Test
    void aDateThatIsNotPublished_isRefused() {
        when(availabilityService.freeTimes(tenantId, DAY)).thenReturn(List.of());

        assertRefused(valid(), "no longer available");
    }

    @Test
    void thePhoneAlreadyHavingARequestOverlappingThatTime_isRefused() {
        when(appointmentRepository.countLiveOverlappingForPhone(any(), anyString(), any(), any())).thenReturn(1L);

        assertRefused(valid(), "already requested this time");
    }

    @Test
    void aPhoneWithThreeRequestsWaiting_isRefused_butTwoIsFine() {
        when(appointmentRepository.countByTenantIdAndCustomerPhoneAndStatusAndStartsAtGreaterThan(
                tenantId, "9876543210", AppointmentStatus.PENDING, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC))).thenReturn(3L);
        assertRefused(valid(), "already have 3 requests waiting");

        when(appointmentRepository.countByTenantIdAndCustomerPhoneAndStatusAndStartsAtGreaterThan(
                tenantId, "9876543210", AppointmentStatus.PENDING, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC))).thenReturn(2L);
        service.request(tenantId, valid());
        verify(appointmentRepository).save(any());
    }

    @Test
    void otherPeoplesRequestsForTheSameTimeNeverBlockARequest() {
        service.request(tenantId, valid());

        // Only this phone's own requests are consulted; nobody else's pending request (or approval lookup) is.
        verify(appointmentRepository, never()).findOverlapping(any(), any(), any(), any());
        verify(appointmentRepository).save(any());
    }
}
