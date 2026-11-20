package com.ebmp.erp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {
    List<SalesOrder> findAllByOrderByIdDesc();

    long countByStatusIn(List<String> statuses);

    long countByOpportunityId(Long opportunityId);
}
