package com.xerxes.msme.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MembershipTypeTest {

    @Test
    void productLimit_matchesEachPlanTier() {
        assertThat(MembershipType.FREE.productLimit()).isEqualTo(10);
        assertThat(MembershipType.PLUS.productLimit()).isEqualTo(100);
        assertThat(MembershipType.PREMIUM.productLimit()).isEqualTo(500);
    }

    @Test
    void monthlyAmountPaise_matchesEachPlanTier() {
        assertThat(MembershipType.FREE.monthlyAmountPaise()).isEqualTo(0);
        assertThat(MembershipType.PLUS.monthlyAmountPaise()).isEqualTo(19_900);
        assertThat(MembershipType.PREMIUM.monthlyAmountPaise()).isEqualTo(99_900);
    }
}
