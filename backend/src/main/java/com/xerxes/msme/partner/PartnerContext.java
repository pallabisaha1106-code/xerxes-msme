package com.xerxes.msme.partner;

import com.xerxes.msme.common.exception.AccountSuspendedException;
import com.xerxes.msme.security.AuthContext;
import org.springframework.stereotype.Component;

/**
 * The local partner behind the current request, for the endpoints a partner uses. Anyone who is not an ACTIVE partner
 * is refused - including a partner switched off after their token was issued - so every partner endpoint can rely on
 * getting a real, active partner.
 */
@Component
public class PartnerContext {

    private final AuthContext authContext;
    private final PartnerRepository partnerRepository;

    public PartnerContext(AuthContext authContext, PartnerRepository partnerRepository) {
        this.authContext = authContext;
        this.partnerRepository = partnerRepository;
    }

    public Partner current() {
        return partnerRepository
                .findByUserId(authContext.current().userId())
                .filter(partner -> partner.getStatus() == PartnerStatus.ACTIVE)
                .orElseThrow(() -> new AccountSuspendedException("Your partner account is not active."));
    }
}
