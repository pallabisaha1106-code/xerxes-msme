package com.xerxes.msme.partner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.security.AuthContext;
import com.xerxes.msme.security.AuthenticatedPrincipal;
import com.xerxes.msme.tenant.Tenant;
import com.xerxes.msme.tenant.TenantRepository;
import com.xerxes.msme.user.Role;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PartnerScopeTest {

    @Mock private AuthContext authContext;
    @Mock private PartnerContext partnerContext;
    @Mock private TenantRepository tenantRepository;
    @Mock private ReviewActionService reviewActionService;

    private final UUID userId = UUID.randomUUID();
    private final UUID partnerId = UUID.randomUUID();
    private PartnerScope scope;

    @BeforeEach
    void setUp() {
        scope = new PartnerScope(authContext, partnerContext, tenantRepository, reviewActionService);
    }

    private void signInAs(Role role) {
        when(authContext.current()).thenReturn(new AuthenticatedPrincipal(userId, UUID.randomUUID(), "a@example.com", role, UUID.randomUUID()));
    }

    private void partnerIsActive() {
        Partner partner = new Partner();
        partner.setId(partnerId);
        when(partnerContext.current()).thenReturn(partner);
    }

    private Tenant shop(UUID ownerPartnerId) {
        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setPartnerId(ownerPartnerId);
        return tenant;
    }

    @Test
    void superAdmin_isNotNarrowed() {
        signInAs(Role.SUPER_ADMIN);

        assertThat(scope.isPartner()).isFalse();
        assertThat(scope.partnerId()).isEmpty();
        assertThat(scope.ownedTenantIds()).isEmpty();
        assertThatCode(() -> scope.requireAccess(shop(null))).doesNotThrowAnyException();
    }

    @Test
    void aPartner_isLimitedToTheShopsAssignedToThem() {
        signInAs(Role.PARTNER);
        partnerIsActive();
        Tenant mine = shop(partnerId);
        when(tenantRepository.findAllByPartnerIdOrderByCreatedAtDesc(partnerId)).thenReturn(List.of(mine));

        assertThat(scope.partnerId()).contains(partnerId);
        assertThat(scope.ownedTenantIds()).contains(List.of(mine.getId()));
        assertThatCode(() -> scope.requireAccess(mine)).doesNotThrowAnyException();
    }

    @Test
    void aPartner_cannotReachAnotherPartnersOrAnUnassignedShop() {
        signInAs(Role.PARTNER);
        partnerIsActive();

        assertThatThrownBy(() -> scope.requireAccess(shop(UUID.randomUUID()))).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> scope.requireAccess(shop(null))).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void requireAccessById_aMissingShopLooksTheSameAsAnotherPartnersShop() {
        signInAs(Role.PARTNER);
        partnerIsActive();
        UUID missing = UUID.randomUUID();
        when(tenantRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scope.requireAccess(missing)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void record_logsTheCallerAndTheirPartnerRecord() {
        signInAs(Role.PARTNER);
        partnerIsActive();
        UUID tenantId = UUID.randomUUID();

        scope.record(tenantId, ReviewActionType.LISTING_APPROVED, null, null);

        verify(reviewActionService).record(tenantId, userId, partnerId, ReviewActionType.LISTING_APPROVED, null, null);
    }

    @Test
    void record_forSuperAdminHasNoPartner() {
        signInAs(Role.SUPER_ADMIN);
        UUID tenantId = UUID.randomUUID();

        scope.record(tenantId, ReviewActionType.ACCOUNT_SUSPENDED, null, "unpaid");

        verify(reviewActionService).record(tenantId, userId, null, ReviewActionType.ACCOUNT_SUSPENDED, null, "unpaid");
    }
}
