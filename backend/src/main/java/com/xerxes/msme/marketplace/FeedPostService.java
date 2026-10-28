package com.xerxes.msme.marketplace;

import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.marketplace.dto.CreateFeedPostRequest;
import com.xerxes.msme.marketplace.dto.FeedPostResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A vendor's own view of their feed posts: publish a new one (subject to the 1-per-7-days
 * cooldown) and see their own post history, including pending/rejected ones a shopper never
 * sees. Nothing here is visible on the public feed until a Super Admin approves it - see
 * FeedPostStatus and AdminFeedPostService (the review side). Active-tenant-only is already
 * enforced globally by TenantSubscriptionInterceptor on every non-public /api/** path, so
 * nothing further is checked here for that.
 */
@Service
public class FeedPostService {

    private static final int COOLDOWN_DAYS = 7;

    private final FeedPostRepository feedPostRepository;

    public FeedPostService(FeedPostRepository feedPostRepository) {
        this.feedPostRepository = feedPostRepository;
    }

    @Transactional
    public FeedPostResponse create(UUID tenantId, CreateFeedPostRequest request) {
        // The end date is the last day the offer is shown, so today is fine - only a past date can never show.
        // Checked first: cheap, and it needs no database read.
        if (request.validUntil() != null && request.validUntil().isBefore(LocalDate.now())) {
            throw new BusinessRuleViolationException("The end date can't be in the past.");
        }
        feedPostRepository.findFirstByTenantIdOrderByCreatedAtDesc(tenantId).ifPresent(last -> {
            OffsetDateTime cooldownEnds = last.getCreatedAt().plusDays(COOLDOWN_DAYS);
            if (cooldownEnds.isAfter(OffsetDateTime.now())) {
                long daysRemaining = Duration.between(OffsetDateTime.now(), cooldownEnds).toDays() + 1;
                throw new BusinessRuleViolationException(
                        "You can post again in " + daysRemaining + " day(s) - only one post every "
                                + COOLDOWN_DAYS + " days.");
            }
        });

        FeedPost post = new FeedPost();
        post.setTenantId(tenantId);
        post.setContent(request.content());
        post.setDiscountLabel(blankToNull(request.discountLabel()));
        post.setValidUntil(request.validUntil());
        post.setStatus(FeedPostStatus.PENDING);
        // saveAndFlush, not save - createdAt is Hibernate-generated (@CreationTimestamp on
        // BaseEntity) and only populated on the in-memory entity once the actual INSERT runs;
        // a plain save() defers that to transaction commit, so toResponse() below would read it
        // back as null otherwise (caught by manually verifying this exact response after
        // restarting the backend - not by any test, since no Mockito test exercises a real flush).
        feedPostRepository.saveAndFlush(post);
        return toResponse(post);
    }

    @Transactional(readOnly = true)
    public List<FeedPostResponse> listMine(UUID tenantId) {
        return feedPostRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    /** An empty or whitespace-only optional field means "not set". */
    private static String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private FeedPostResponse toResponse(FeedPost post) {
        return new FeedPostResponse(
                post.getId(),
                post.getContent(),
                post.getDiscountLabel(),
                post.getValidUntil(),
                post.getStatus(),
                post.getRejectionReason(),
                post.getCreatedAt());
    }
}
