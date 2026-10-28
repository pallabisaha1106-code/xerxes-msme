package com.xerxes.msme.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminFeedPostServiceTest {

    @Mock private FeedPostRepository feedPostRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private PartnerScope partnerScope;

    private AdminFeedPostService adminFeedPostService;
    private UUID tenantId;
    private UUID postId;

    @BeforeEach
    void setUp() {
        adminFeedPostService = new AdminFeedPostService(feedPostRepository, tenantRepository, partnerScope);
        tenantId = UUID.randomUUID();
        postId = UUID.randomUUID();

        lenient().when(feedPostRepository.save(any(FeedPost.class))).thenAnswer(inv -> inv.getArgument(0));
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setBusinessName("Veera Collections");
        lenient().when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
    }

    private FeedPost pendingPost() {
        FeedPost post = new FeedPost();
        post.setId(postId);
        post.setTenantId(tenantId);
        post.setContent("50% off today!");
        post.setStatus(FeedPostStatus.PENDING);
        return post;
    }

    private FeedPost approvedPost() {
        FeedPost post = pendingPost();
        post.setStatus(FeedPostStatus.APPROVED);
        return post;
    }

    @Test
    void list_asAPartner_returnsOnlyThePostsOfTheirShops() {
        when(partnerScope.ownedTenantIds()).thenReturn(Optional.of(List.of(tenantId)));
        when(feedPostRepository.findAllByTenantIdInOrderByCreatedAtDesc(List.of(tenantId))).thenReturn(List.of(pendingPost()));

        assertThat(adminFeedPostService.list(null)).hasSize(1);

        verify(feedPostRepository, org.mockito.Mockito.never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void approve_aPostOfAShopThePartnerDoesNotLookAfter_isNotFoundAndNothingChanges() {
        when(feedPostRepository.findById(postId)).thenReturn(Optional.of(pendingPost()));
        org.mockito.Mockito.doThrow(ResourceNotFoundException.of("Tenant", tenantId)).when(partnerScope).requireAccess(tenantId);

        assertThrows(ResourceNotFoundException.class, () -> adminFeedPostService.approve(postId));

        verify(feedPostRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void approveAndReject_areLoggedWithThePostAsTheSubject() {
        when(feedPostRepository.findById(postId)).thenReturn(Optional.of(pendingPost()), Optional.of(pendingPost()));

        adminFeedPostService.approve(postId);
        adminFeedPostService.reject(postId, "misleading");

        verify(partnerScope).record(tenantId, ReviewActionType.FEED_POST_APPROVED, postId, null);
        verify(partnerScope).record(tenantId, ReviewActionType.FEED_POST_REJECTED, postId, "misleading");
    }

    @Test
    void list_noStatusFilter_returnsAllPostsWithBusinessNameResolved() {
        when(feedPostRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(pendingPost()));

        List<AdminFeedPostResponse> result = adminFeedPostService.list(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).businessName()).isEqualTo("Veera Collections");
        assertThat(result.get(0).tenantId()).isEqualTo(tenantId);
    }

    @Test
    void list_includesTheDiscountLabelAndEndDateWhenSet_andNullWhenNot() {
        FeedPost withOffer = pendingPost();
        withOffer.setDiscountLabel("Buy 1 Get 1");
        withOffer.setValidUntil(java.time.LocalDate.of(2030, 3, 9));
        when(feedPostRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(withOffer, pendingPost()));

        List<AdminFeedPostResponse> result = adminFeedPostService.list(null);

        assertThat(result.get(0).discountLabel()).isEqualTo("Buy 1 Get 1");
        assertThat(result.get(0).validUntil()).isEqualTo(java.time.LocalDate.of(2030, 3, 9));
        assertThat(result.get(1).discountLabel()).isNull();
        assertThat(result.get(1).validUntil()).isNull();
    }

    @Test
    void list_withStatusFilter_delegatesToFilteredQuery() {
        when(feedPostRepository.findAllByStatusOrderByCreatedAtDesc(FeedPostStatus.PENDING))
                .thenReturn(List.of(pendingPost()));

        List<AdminFeedPostResponse> result = adminFeedPostService.list(FeedPostStatus.PENDING);

        assertThat(result).hasSize(1);
        verify(feedPostRepository, org.mockito.Mockito.never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void approve_pendingPost_movesToApprovedAndStampsReviewedAt() {
        FeedPost post = pendingPost();
        when(feedPostRepository.findById(postId)).thenReturn(Optional.of(post));

        AdminFeedPostResponse response = adminFeedPostService.approve(postId);

        assertThat(response.status()).isEqualTo(FeedPostStatus.APPROVED);
        assertThat(post.getReviewedAt()).isNotNull();
    }

    @Test
    void approve_notPending_throwsBusinessRuleViolation() {
        when(feedPostRepository.findById(postId)).thenReturn(Optional.of(approvedPost()));

        assertThrows(BusinessRuleViolationException.class, () -> adminFeedPostService.approve(postId));
    }

    @Test
    void approve_unknownPost_throwsResourceNotFound() {
        when(feedPostRepository.findById(postId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adminFeedPostService.approve(postId));
    }

    @Test
    void reject_pendingPost_movesToRejectedWithReason() {
        FeedPost post = pendingPost();
        when(feedPostRepository.findById(postId)).thenReturn(Optional.of(post));

        AdminFeedPostResponse response = adminFeedPostService.reject(postId, "Prohibited item.");

        assertThat(response.status()).isEqualTo(FeedPostStatus.REJECTED);
        assertThat(response.rejectionReason()).isEqualTo("Prohibited item.");
        assertThat(post.getReviewedAt()).isNotNull();
    }

    @Test
    void reject_notPending_throwsBusinessRuleViolation() {
        when(feedPostRepository.findById(postId)).thenReturn(Optional.of(approvedPost()));

        assertThrows(BusinessRuleViolationException.class, () -> adminFeedPostService.reject(postId, "reason"));
    }
}
