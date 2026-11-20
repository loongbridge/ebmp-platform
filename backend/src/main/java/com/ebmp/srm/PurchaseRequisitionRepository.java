package com.ebmp.srm;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseRequisitionRepository extends JpaRepository<PurchaseRequisition, Long> {
    List<PurchaseRequisition> findAllByOrderByIdDesc();

    long countByStatus(String status);
}
