package com.xerxes.msme.marketplace;

import com.xerxes.msme.marketplace.dto.CreateFeedPostRequest;
import com.xerxes.msme.marketplace.dto.FeedPostResponse;
import com.xerxes.msme.security.AuthContext;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** A vendor's own feed posts - see AdminFeedPostController for the Super Admin review side. */
@RestController
@RequestMapping("/api/feed-posts")
public class FeedPostController {

    private final FeedPostService feedPostService;
    private final AuthContext authContext;

    public FeedPostController(FeedPostService feedPostService, AuthContext authContext) {
        this.feedPostService = feedPostService;
        this.authContext = authContext;
    }

    @PostMapping
    public ResponseEntity<FeedPostResponse> create(@Valid @RequestBody CreateFeedPostRequest request) {
        FeedPostResponse response = feedPostService.create(authContext.current().tenantId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/mine")
    public List<FeedPostResponse> mine() {
        return feedPostService.listMine(authContext.current().tenantId());
    }
}
