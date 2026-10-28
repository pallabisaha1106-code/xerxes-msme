package com.xerxes.msme.purchase;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseLineItemRepository extends JpaRepository<PurchaseLineItem, UUID> {

    List<PurchaseLineItem> findAllByPurchaseEntryId(UUID purchaseEntryId);
}
