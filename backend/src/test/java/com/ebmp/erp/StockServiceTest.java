package com.ebmp.erp;

import com.ebmp.common.BizException;
import com.ebmp.masterdata.Material;
import com.ebmp.masterdata.MaterialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockServiceTest {

    @Mock
    private StockRepository stockRepository;
    @Mock
    private StockTxnRepository txnRepository;
    @Mock
    private MaterialRepository materialRepository;

    private StockService stockService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(stockRepository.save(any(Stock.class))).thenAnswer(inv -> inv.getArgument(0));
        when(txnRepository.save(any(StockTxn.class))).thenAnswer(inv -> inv.getArgument(0));
        stockService = new StockService(stockRepository, txnRepository, materialRepository);
    }

    private Stock stock(Long materialId, Long warehouseId, String quantity) {
        Stock stock = new Stock();
        stock.setMaterialId(materialId);
        stock.setWarehouseId(warehouseId);
        stock.setQuantity(new BigDecimal(quantity));
        return stock;
    }

    @Test
    void inboundCreatesStockAndRecordsTransaction() {
        when(stockRepository.findByMaterialIdAndWarehouseId(1L, 2L)).thenReturn(Optional.empty());

        stockService.inbound(1L, 2L, new BigDecimal("15"), "PURCHASE_IN", "PO2026TEST", 100L, "采购入库");

        ArgumentCaptor<Stock> stockCaptor = ArgumentCaptor.forClass(Stock.class);
        verify(stockRepository).save(stockCaptor.capture());
        assertEquals(new BigDecimal("15"), stockCaptor.getValue().getQuantity());

        ArgumentCaptor<StockTxn> txnCaptor = ArgumentCaptor.forClass(StockTxn.class);
        verify(txnRepository).save(txnCaptor.capture());
        assertEquals("IN", txnCaptor.getValue().getType());
        assertEquals(new BigDecimal("15"), txnCaptor.getValue().getQuantity());
        assertEquals(BigDecimal.ZERO, txnCaptor.getValue().getBeforeQuantity());
    }

    @Test
    void inboundRejectsNonPositiveQuantity() {
        BizException ex = assertThrows(BizException.class,
                () -> stockService.inbound(1L, 2L, BigDecimal.ZERO, "PURCHASE_IN", "PO1", 1L, ""));
        assertEquals("入库数量必须大于 0", ex.getMessage());
        verify(stockRepository, never()).save(any());
    }

    @Test
    void outboundRejectsInsufficientStockWithMaterialName() {
        when(stockRepository.findByMaterialIdAndWarehouseId(1L, 2L))
                .thenReturn(Optional.of(stock(1L, 2L, "5")));
        Material material = new Material();
        material.setName("螺纹钢筋");
        when(materialRepository.findById(1L)).thenReturn(Optional.of(material));

        BizException ex = assertThrows(BizException.class,
                () -> stockService.outbound(1L, 2L, new BigDecimal("8"), "SALES_OUT", "SO1", 1L, ""));
        assertTrue(ex.getMessage().contains("库存不足"));
        assertTrue(ex.getMessage().contains("螺纹钢筋"));
        verify(stockRepository, never()).save(any());
    }

    @Test
    void outboundDecreasesStockAndRecordsTransaction() {
        when(stockRepository.findByMaterialIdAndWarehouseId(1L, 2L))
                .thenReturn(Optional.of(stock(1L, 2L, "20")));

        stockService.outbound(1L, 2L, new BigDecimal("12"), "SALES_OUT", "SO2026TEST", 100L, "销售出库");

        ArgumentCaptor<Stock> captor = ArgumentCaptor.forClass(Stock.class);
        verify(stockRepository).save(captor.capture());
        assertEquals(new BigDecimal("8"), captor.getValue().getQuantity());

        ArgumentCaptor<StockTxn> txnCaptor = ArgumentCaptor.forClass(StockTxn.class);
        verify(txnRepository).save(txnCaptor.capture());
        assertEquals("OUT", txnCaptor.getValue().getType());
        assertEquals(new BigDecimal("20"), txnCaptor.getValue().getBeforeQuantity());
        assertEquals(new BigDecimal("8"), txnCaptor.getValue().getAfterQuantity());
    }

    @Test
    void outboundRejectsMissingWarehouseStock() {
        when(stockRepository.findByMaterialIdAndWarehouseId(anyLong(), anyLong())).thenReturn(Optional.empty());
        BizException ex = assertThrows(BizException.class,
                () -> stockService.outbound(1L, 2L, BigDecimal.ONE, "SALES_OUT", "SO1", 1L, ""));
        assertEquals("该仓库无此物料库存", ex.getMessage());
    }

    @Test
    void availableSumsAllWarehouses() {
        when(stockRepository.findAll()).thenReturn(List.of(
                stock(1L, 1L, "10"), stock(1L, 2L, "5"), stock(2L, 1L, "3")));
        assertEquals(new BigDecimal("15"), stockService.available(1L));
    }
}
