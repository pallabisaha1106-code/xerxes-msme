package com.xerxes.msme.admin;

import com.xerxes.msme.admin.dto.AdminFeedPostResponse;
import com.xerxes.msme.admin.dto.ReviewFeedPostRequest;
import com.xerxes.msme.marketplace.FeedPostStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Platform-staff-only: review vendor feed-post submissions. See SecurityConfig for the role gate. */
@RestController
@RequestMapping("/api/admin/feed-posts")
public class AdminFeedPostController {

    private final AdminFeedPostService adminFeedPostService;

    public AdminFeedPostController(AdminFeedPostService adminFeedPostService) {
        this.adminFeedPostService = adminFeedPostService;
    }

    @GetMapping
    public List<AdminFeedPostResponse> list(@RequestParam(required = false) FeedPostStatus status) {
        return adminFeedPostService.list(status);
    }

    @PostMapping("/{id}/approve")
    public AdminFeedPostResponse approve(@PathVariable UUID id) {
        return adminFeedPostService.approve(id);
    }

    @PostMapping("/{id}/reject")
    public AdminFeedPostResponse reject(@PathVariable UUID id, @RequestBody(required = false) ReviewFeedPostRequest request) {
        return adminFeedPostService.reject(id, request != null ? request.reason() : null);
    }
}
