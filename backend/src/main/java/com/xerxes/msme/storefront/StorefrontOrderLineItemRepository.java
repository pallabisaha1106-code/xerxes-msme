package com.xerxes.msme.storefront;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorefrontOrderLineItemRepository extends JpaRepository<StorefrontOrderLineItem, UUID> {

    List<StorefrontOrderLineItem> findAllByOrderId(UUID orderId);
}
