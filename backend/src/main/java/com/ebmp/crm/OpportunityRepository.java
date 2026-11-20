package com.ebmp.crm;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OpportunityRepository extends JpaRepository<Opportunity, Long> {
    List<Opportunity> findAllByOrderByIdDesc();

    List<Opportunity> findByCustomerIdOrderByIdDesc(Long customerId);

    long countByStage(String stage);

    long countByStatus(String status);
}
