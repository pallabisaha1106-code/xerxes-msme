package com.xerxes.msme.partner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.xerxes.msme.partner.dto.PartnerContactResponse;
import com.xerxes.msme.tenant.Tenant;
import com.xerxes.msme.tenant.TenantRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PartnerContactServiceTest {

    @Mock private PartnerRepository partnerRepository;
    @Mock private TenantRepository tenantRepository;
    @InjectMocks private PartnerContactService service;

    private final UUID partnerId = UUID.randomUUID();

    private Partner partner(PartnerStatus status) {
        Partner partner = new Partner();
        partner.setId(partnerId);
        partner.setName("Meera");
        partner.setWhatsapp("919876543210");
        partner.setStatus(status);
        return partner;
    }

    private Tenant shop(UUID partner) {
        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setPartnerId(partner);
        return tenant;
    }

    @Test
    void contactFor_anActivePartner_showsNameAndWhatsapp() {
        when(partnerRepository.findById(partnerId)).thenReturn(Optional.of(partner(PartnerStatus.ACTIVE)));

        assertThat(service.contactFor(shop(partnerId)))
                .contains(new PartnerContactResponse("Meera", "919876543210"));
    }

    @Test
    void contactFor_aSwitchedOffPartnerOrNoPartner_showsNothing() {
        when(partnerRepository.findById(partnerId)).thenReturn(Optional.of(partner(PartnerStatus.INACTIVE)));

        assertThat(service.contactFor(shop(partnerId))).isEmpty();
        assertThat(service.contactFor(shop(null))).isEmpty();
    }

    @Test
    void contactFor_byTenantId_looksTheShopUp() {
        Tenant tenant = shop(partnerId);
        when(tenantRepository.findById(tenant.getId())).thenReturn(Optional.of(tenant));
        when(partnerRepository.findById(partnerId)).thenReturn(Optional.of(partner(PartnerStatus.ACTIVE)));

        assertThat(service.contactFor(tenant.getId())).isPresent();
        assertThat(service.contactFor(UUID.randomUUID())).isEmpty();
    }
}
