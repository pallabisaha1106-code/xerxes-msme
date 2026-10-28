package com.xerxes.msme.marketplace.dto;

import com.xerxes.msme.tenant.MembershipType;
import jakarta.validation.constraints.NotNull;

public record SelectMembershipPlanRequest(@NotNull MembershipType plan) {}
