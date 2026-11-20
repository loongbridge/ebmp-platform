package com.ebmp.erp;

import com.ebmp.common.BizException;
import com.ebmp.masterdata.Material;
import com.ebmp.masterdata.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;
    private final StockTxnRepository txnRepository;
    private final MaterialRepository materialRepository;

    @Transactional
    public void inbound(Long materialId, Long warehouseId, BigDecimal quantity,
                        String bizType, String bizNo, Long operatorId, String remark) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("入库数量必须大于 0");
        }
        Stock stock = stockRepository.findByMaterialIdAndWarehouseId(materialId, warehouseId)
                .orElseGet(() -> {
                    Stock s = new Stock();
                    s.setMaterialId(materialId);
                    s.setWarehouseId(warehouseId);
                    s.setQuantity(BigDecimal.ZERO);
                    return s;
                });
        BigDecimal before = stock.getQuantity() == null ? BigDecimal.ZERO : stock.getQuantity();
        stock.setQuantity(before.add(quantity));
        stockRepository.save(stock);
        recordTxn("IN", materialId, warehouseId, quantity, before, stock.getQuantity(),
                bizType, bizNo, operatorId, remark);
    }

    @Transactional
    public void outbound(Long materialId, Long warehouseId, BigDecimal quantity,
                         String bizType, String bizNo, Long operatorId, String remark) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("出库数量必须大于 0");
        }
        Stock stock = stockRepository.findByMaterialIdAndWarehouseId(materialId, warehouseId)
                .orElseThrow(() -> new BizException("该仓库无此物料库存"));
        BigDecimal before = stock.getQuantity() == null ? BigDecimal.ZERO : stock.getQuantity();
        if (before.compareTo(quantity) < 0) {
            Material material = materialRepository.findById(materialId).orElse(null);
            throw new BizException((material == null ? "物料" : material.getName()) + " 库存不足，当前可用 " + before);
        }
        stock.setQuantity(before.subtract(quantity));
        stockRepository.save(stock);
        recordTxn("OUT", materialId, warehouseId, quantity, before, stock.getQuantity(),
                bizType, bizNo, operatorId, remark);
    }

    public BigDecimal available(Long materialId) {
        return stockRepository.findAll().stream()
                .filter(s -> s.getMaterialId().equals(materialId))
                .map(Stock::getQuantity)
                .filter(q -> q != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<Stock> allWithMaterial() {
        List<Stock> list = stockRepository.findAllByOrderByIdAsc();
        list.forEach(s -> materialRepository.findById(s.getMaterialId()).ifPresent(m -> {
            s.setMaterialName(m.getName());
            s.setMaterialCode(m.getCode());
            s.setSpec(m.getSpec());
            s.setUnit(m.getUnit());
            s.setSafeStock(m.getSafeStock());
            s.setCostPrice(m.getCostPrice());
            BigDecimal qty = s.getQuantity() == null ? BigDecimal.ZERO : s.getQuantity();
            s.setAlert(m.getSafeStock() != null && qty.compareTo(m.getSafeStock()) < 0);
        }));
        return list;
    }

    private void recordTxn(String type, Long materialId, Long warehouseId, BigDecimal quantity,
                           BigDecimal before, BigDecimal after, String bizType, String bizNo,
                           Long operatorId, String remark) {
        StockTxn txn = new StockTxn();
        txn.setTxnNo("ST" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")));
        txn.setType(type);
        txn.setMaterialId(materialId);
        txn.setWarehouseId(warehouseId);
        txn.setQuantity(quantity);
        txn.setBeforeQuantity(before);
        txn.setAfterQuantity(after);
        txn.setBizType(bizType);
        txn.setBizNo(bizNo);
        txn.setOperatorId(operatorId);
        txn.setRemark(remark);
        txnRepository.save(txn);
    }
}
