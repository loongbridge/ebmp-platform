package com.ebmp.erp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByMaterialIdAndWarehouseId(Long materialId, Long warehouseId);

    List<Stock> findAllByOrderByIdAsc();

    List<Stock> findByWarehouseIdOrderByIdAsc(Long warehouseId);
}
