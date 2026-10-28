package com.xerxes.msme.admin;

import com.xerxes.msme.admin.dto.AdminFeedPostResponse;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.marketplace.FeedPost;
import com.xerxes.msme.marketplace.FeedPostRepository;
import com.xerxes.msme.marketplace.FeedPostStatus;
import com.xerxes.msme.partner.PartnerScope;
import com.xerxes.msme.partner.ReviewActionType;
import com.xerxes.msme.tenant.Tenant;
import com.xerxes.msme.tenant.TenantRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Platform-staff review of vendor feed-post submissions: approve a PENDING post (visible in the
 * public feed until it falls out of the 14-day window - see PublicMarketplaceService) or reject
 * it with a reason. Unlike a marketplace listing, a rejected post is terminal - the vendor posts
 * a new one next week rather than editing and resubmitting this one (see FeedPostStatus).
 */
@Service
public class AdminFeedPostService {

    private final FeedPostRepository feedPostRepository;
    private final TenantRepository tenantRepository;
    private final PartnerScope partnerScope;

    public AdminFeedPostService(
            FeedPostRepository feedPostRepository, TenantRepository tenantRepository, PartnerScope partnerScope) {
        this.feedPostRepository = feedPostRepository;
        this.tenantRepository = tenantRepository;
        this.partnerScope = partnerScope;
    }

    @Transactional(readOnly = true)
    public List<AdminFeedPostResponse> list(FeedPostStatus status) {
        List<FeedPost> posts = partnerScope
                .ownedTenantIds()
                .map(ids -> status != null
                        ? feedPostRepository.findAllByTenantIdInAndStatusOrderByCreatedAtDesc(ids, status)
                        : feedPostRepository.findAllByTenantIdInOrderByCreatedAtDesc(ids))
                .orElseGet(() -> status != null
                        ? feedPostRepository.findAllByStatusOrderByCreatedAtDesc(status)
                        : feedPostRepository.findAllByOrderByCreatedAtDesc());
        return posts.stream().map(this::toResponse).toList();
    }

    @Transactional
    public AdminFeedPostResponse approve(UUID id) {
        FeedPost post = findPost(id);
        if (post.getStatus() != FeedPostStatus.PENDING) {
            throw new BusinessRuleViolationException("Only a pending post can be approved.");
        }
        post.setStatus(FeedPostStatus.APPROVED);
        post.setRejectionReason(null);
        post.setReviewedAt(OffsetDateTime.now());
        feedPostRepository.save(post);
        partnerScope.record(post.getTenantId(), ReviewActionType.FEED_POST_APPROVED, id, null);
        return toResponse(post);
    }

    @Transactional
    public AdminFeedPostResponse reject(UUID id, String reason) {
        FeedPost post = findPost(id);
        if (post.getStatus() != FeedPostStatus.PENDING) {
            throw new BusinessRuleViolationException("Only a pending post can be rejected.");
        }
        post.setStatus(FeedPostStatus.REJECTED);
        post.setRejectionReason(reason);
        post.setReviewedAt(OffsetDateTime.now());
        feedPostRepository.save(post);
        partnerScope.record(post.getTenantId(), ReviewActionType.FEED_POST_REJECTED, id, reason);
        return toResponse(post);
    }

    private FeedPost findPost(UUID id) {
        FeedPost post = feedPostRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("FeedPost", id));
        partnerScope.requireAccess(post.getTenantId());
        return post;
    }

    private AdminFeedPostResponse toResponse(FeedPost post) {
        String businessName =
                tenantRepository.findById(post.getTenantId()).map(Tenant::getBusinessName).orElse(null);
        return new AdminFeedPostResponse(
                post.getId(),
                post.getTenantId(),
                businessName,
                post.getContent(),
                post.getDiscountLabel(),
                post.getValidUntil(),
                post.getStatus(),
                post.getRejectionReason(),
                post.getCreatedAt(),
                post.getReviewedAt());
    }
}
