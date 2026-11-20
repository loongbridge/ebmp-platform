package com.ebmp.srm;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findAllByOrderByIdDesc();

    long countByStatus(String status);

    long countByStatusIn(List<String> statuses);
}
