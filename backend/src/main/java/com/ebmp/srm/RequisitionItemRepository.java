package com.ebmp.srm;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequisitionItemRepository extends JpaRepository<RequisitionItem, Long> {
    List<RequisitionItem> findByRequisitionId(Long requisitionId);

    void deleteByRequisitionId(Long requisitionId);
}
