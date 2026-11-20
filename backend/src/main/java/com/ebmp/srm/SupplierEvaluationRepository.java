package com.ebmp.srm;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierEvaluationRepository extends JpaRepository<SupplierEvaluation, Long> {
    List<SupplierEvaluation> findAllByOrderByIdDesc();
}
