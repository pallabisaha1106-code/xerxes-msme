package com.xerxes.msme.storefront;

import com.xerxes.msme.security.AuthContext;
import com.xerxes.msme.storefront.dto.RejectOrderRequest;
import com.xerxes.msme.storefront.dto.StorefrontOrderResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The shop owner's authenticated inbox for reviewing guest storefront orders. */
@RestController
@RequestMapping("/api/storefront/orders")
public class StorefrontOrderController {

    private final StorefrontOrderService orderService;
    private final AuthContext authContext;

    public StorefrontOrderController(StorefrontOrderService orderService, AuthContext authContext) {
        this.orderService = orderService;
        this.authContext = authContext;
    }

    @GetMapping
    public List<StorefrontOrderResponse> list(@RequestParam(required = false) StorefrontOrderStatus status) {
        return orderService.list(authContext.current().tenantId(), status);
    }

    @GetMapping("/{id}")
    public StorefrontOrderResponse get(@PathVariable UUID id) {
        return orderService.get(authContext.current().tenantId(), id);
    }

    @PostMapping("/{id}/confirm")
    public StorefrontOrderResponse confirm(@PathVariable UUID id) {
        var principal = authContext.current();
        return orderService.confirm(principal.tenantId(), id, principal.userId());
    }

    @PostMapping("/{id}/reject")
    public StorefrontOrderResponse reject(@PathVariable UUID id, @RequestBody(required = false) RejectOrderRequest request) {
        String reason = request != null ? request.reason() : null;
        return orderService.reject(authContext.current().tenantId(), id, reason);
    }
}
