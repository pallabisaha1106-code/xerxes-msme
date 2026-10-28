package com.xerxes.msme.marketplace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.marketplace.dto.CreateFeedPostRequest;
import com.xerxes.msme.marketplace.dto.FeedPostResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeedPostServiceTest {

    @Mock private FeedPostRepository feedPostRepository;

    private FeedPostService feedPostService;
    private final UUID tenantId = UUID.randomUUID();

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        feedPostService = new FeedPostService(feedPostRepository);
    }

    private FeedPost existingPost(OffsetDateTime createdAt) {
        FeedPost post = new FeedPost();
        post.setId(UUID.randomUUID());
        post.setTenantId(tenantId);
        post.setContent("Earlier post");
        post.setStatus(FeedPostStatus.APPROVED);
        post.setCreatedAt(createdAt);
        return post;
    }

    @Test
    void create_noPriorPost_createsAPendingPost() {
        when(feedPostRepository.findFirstByTenantIdOrderByCreatedAtDesc(tenantId)).thenReturn(Optional.empty());

        FeedPostResponse response = feedPostService.create(tenantId, new CreateFeedPostRequest("50% off today only!", null, null));

        assertThat(response.status()).isEqualTo(FeedPostStatus.PENDING);
        assertThat(response.content()).isEqualTo("50% off today only!");
        ArgumentCaptor<FeedPost> captor = ArgumentCaptor.forClass(FeedPost.class);
        verify(feedPostRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(tenantId);
        assertThat(captor.getValue().getStatus()).isEqualTo(FeedPostStatus.PENDING);
    }

    @Test
    void create_lastPostWithinSevenDays_throwsBusinessRuleViolation() {
        when(feedPostRepository.findFirstByTenantIdOrderByCreatedAtDesc(tenantId))
                .thenReturn(Optional.of(existingPost(OffsetDateTime.now().minusDays(2))));

        assertThrows(
                BusinessRuleViolationException.class,
                () -> feedPostService.create(tenantId, new CreateFeedPostRequest("Another post", null, null)));

        verify(feedPostRepository, org.mockito.Mockito.never()).saveAndFlush(any());
    }

    @Test
    void create_lastPostExactlySevenDaysAgo_allowsANewPost() {
        when(feedPostRepository.findFirstByTenantIdOrderByCreatedAtDesc(tenantId))
                .thenReturn(Optional.of(existingPost(OffsetDateTime.now().minusDays(8))));

        FeedPostResponse response = feedPostService.create(tenantId, new CreateFeedPostRequest("New week, new offer", null, null));

        assertThat(response.status()).isEqualTo(FeedPostStatus.PENDING);
        verify(feedPostRepository).saveAndFlush(any());
    }

    @Test
    void listMine_returnsTheVendorsOwnPostsAsReturnedByTheRepository() {
        FeedPost pending = existingPost(OffsetDateTime.now());
        pending.setStatus(FeedPostStatus.PENDING);
        when(feedPostRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId)).thenReturn(List.of(pending));

        List<FeedPostResponse> responses = feedPostService.listMine(tenantId);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).status()).isEqualTo(FeedPostStatus.PENDING);
    }

    @Test
    void create_withDiscountLabelAndEndDate_storesBoth() {
        when(feedPostRepository.findFirstByTenantIdOrderByCreatedAtDesc(tenantId)).thenReturn(Optional.empty());
        java.time.LocalDate endDate = java.time.LocalDate.now().plusDays(20);

        FeedPostResponse response = feedPostService.create(
                tenantId, new CreateFeedPostRequest("Winter sale", "  20% off ", endDate));

        ArgumentCaptor<FeedPost> captor = ArgumentCaptor.forClass(FeedPost.class);
        verify(feedPostRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getDiscountLabel()).isEqualTo("20% off");
        assertThat(captor.getValue().getValidUntil()).isEqualTo(endDate);
        assertThat(response.discountLabel()).isEqualTo("20% off");
        assertThat(response.validUntil()).isEqualTo(endDate);
    }

    @Test
    void create_blankDiscountLabel_isStoredAsNotSet() {
        when(feedPostRepository.findFirstByTenantIdOrderByCreatedAtDesc(tenantId)).thenReturn(Optional.empty());

        feedPostService.create(tenantId, new CreateFeedPostRequest("Fresh stock", "   ", null));

        ArgumentCaptor<FeedPost> captor = ArgumentCaptor.forClass(FeedPost.class);
        verify(feedPostRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getDiscountLabel()).isNull();
        assertThat(captor.getValue().getValidUntil()).isNull();
    }

    @Test
    void create_endDateToday_isAllowed() {
        when(feedPostRepository.findFirstByTenantIdOrderByCreatedAtDesc(tenantId)).thenReturn(Optional.empty());

        FeedPostResponse response = feedPostService.create(
                tenantId, new CreateFeedPostRequest("Today only", "Flash sale", java.time.LocalDate.now()));

        assertThat(response.validUntil()).isEqualTo(java.time.LocalDate.now());
        verify(feedPostRepository).saveAndFlush(any());
    }

    @Test
    void create_endDateInThePast_isRejectedBeforeAnythingIsReadOrSaved() {
        assertThrows(
                BusinessRuleViolationException.class,
                () -> feedPostService.create(
                        tenantId,
                        new CreateFeedPostRequest("Old offer", null, java.time.LocalDate.now().minusDays(1))));

        verify(feedPostRepository, org.mockito.Mockito.never()).findFirstByTenantIdOrderByCreatedAtDesc(any());
        verify(feedPostRepository, org.mockito.Mockito.never()).saveAndFlush(any());
    }
}
