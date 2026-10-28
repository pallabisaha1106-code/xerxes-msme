package com.xerxes.msme.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * content is the offer's headline. discountLabel (badge text) and validUntil (last day the offer is shown) are
 * both optional - see V21 and FeedPostService for the end-date rule.
 */
public record CreateFeedPostRequest(
        @NotBlank @Size(max = 140) String content, @Size(max = 30) String discountLabel, LocalDate validUntil) {}
