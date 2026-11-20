package com.ebmp.erp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockTxnRepository extends JpaRepository<StockTxn, Long> {
    List<StockTxn> findTop100ByOrderByIdDesc();
}
