package com.ebmp.contract;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractRepository extends JpaRepository<Contract, Long> {
    List<Contract> findAllByOrderByIdDesc();

    List<Contract> findByTypeOrderByIdDesc(String type);

    List<Contract> findByPartyIdOrderByIdDesc(Long partyId);

    long countByStatus(String status);
}
