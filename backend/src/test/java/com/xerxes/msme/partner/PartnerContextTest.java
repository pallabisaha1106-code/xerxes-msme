package com.xerxes.msme.partner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.xerxes.msme.common.exception.AccountSuspendedException;
import com.xerxes.msme.security.AuthContext;
import com.xerxes.msme.security.AuthenticatedPrincipal;
import com.xerxes.msme.user.Role;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PartnerContextTest {

    @Mock private AuthContext authContext;
    @Mock private PartnerRepository partnerRepository;

    private final UUID userId = UUID.randomUUID();
    private PartnerContext context;

    @BeforeEach
    void setUp() {
        context = new PartnerContext(authContext, partnerRepository);
        when(authContext.current())
                .thenReturn(new AuthenticatedPrincipal(userId, UUID.randomUUID(), "meera@example.com", Role.PARTNER, UUID.randomUUID()));
    }

    private Partner partner(PartnerStatus status) {
        Partner partner = new Partner();
        partner.setStatus(status);
        return partner;
    }

    @Test
    void current_anActivePartner_isReturned() {
        Partner active = partner(PartnerStatus.ACTIVE);
        when(partnerRepository.findByUserId(userId)).thenReturn(Optional.of(active));

        assertThat(context.current()).isSameAs(active);
    }

    @Test
    void current_aPartnerSwitchedOffAfterSigningIn_isRefused() {
        when(partnerRepository.findByUserId(userId)).thenReturn(Optional.of(partner(PartnerStatus.INACTIVE)));

        assertThatThrownBy(() -> context.current()).isInstanceOf(AccountSuspendedException.class);
    }

    @Test
    void current_someoneWithNoPartnerRow_isRefused() {
        when(partnerRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> context.current()).isInstanceOf(AccountSuspendedException.class);
    }
}
