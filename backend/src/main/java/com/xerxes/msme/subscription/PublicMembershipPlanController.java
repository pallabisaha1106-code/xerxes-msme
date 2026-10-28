package com.xerxes.msme.subscription;

import com.xerxes.msme.subscription.dto.MembershipPlanResponse;
import com.xerxes.msme.tenant.MembershipType;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The plans and their prices, straight from MembershipType, so the public pricing page and the plan picker can never
 * disagree with what the backend enforces and bills. No authentication; rate-limited like the rest of /api/public.
 */
@RestController
@RequestMapping("/api/public/membership-plans")
public class PublicMembershipPlanController {

    @GetMapping
    public List<MembershipPlanResponse> plans() {
        return Arrays.stream(MembershipType.values())
                .map(plan -> new MembershipPlanResponse(plan, plan.monthlyAmountPaise(), plan.productLimit()))
                .toList();
    }
}
