package com.xerxes.msme.partner.dto;

import com.xerxes.msme.partner.PartnerStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** A partner as Super Admin sees them: who they are, where they work, how many shops they look after, and the decision. */
public record AdminPartnerResponse(
        UUID id,
        String name,
        String email,
        String whatsapp,
        PartnerStatus status,
        List<AdminPartnerAreaResponse> areas,
        long assignedShopCount,
        OffsetDateTime createdAt,
        OffsetDateTime decidedAt,
        String decisionReason) {}
