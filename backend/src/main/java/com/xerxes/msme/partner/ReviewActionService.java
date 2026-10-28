package com.xerxes.msme.partner;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Writes (and reads) the log of decisions on shops - see ReviewAction. */
@Service
public class ReviewActionService {

    private final ReviewActionRepository reviewActionRepository;

    public ReviewActionService(ReviewActionRepository reviewActionRepository) {
        this.reviewActionRepository = reviewActionRepository;
    }

    /**
     * Records one decision. actorUserId is empty when the system acted by itself, partnerId when Super Admin acted,
     * subjectId when the thing acted on is not the shop itself (e.g. a feed post), reason when one was given.
     */
    @Transactional
    public ReviewAction record(
            UUID tenantId, UUID actorUserId, UUID partnerId, ReviewActionType action, UUID subjectId, String reason) {
        ReviewAction entry = new ReviewAction();
        entry.setTenantId(tenantId);
        entry.setActorUserId(actorUserId);
        entry.setPartnerId(partnerId);
        entry.setAction(action);
        entry.setSubjectId(subjectId);
        entry.setReason(reason);
        return reviewActionRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<ReviewAction> historyOf(UUID tenantId) {
        return reviewActionRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId);
    }
}
