package com.ebmp.srm;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {
    List<PurchaseOrderItem> findByOrderId(Long orderId);

    void deleteByOrderId(Long orderId);
}
