package com.ebmp.erp;

import com.ebmp.common.ApiResponse;
import com.ebmp.common.BizException;
import com.ebmp.common.SecurityUtils;
import com.ebmp.masterdata.CustomerRepository;
import com.ebmp.masterdata.Material;
import com.ebmp.masterdata.MaterialRepository;
import com.ebmp.masterdata.WarehouseRepository;
import com.ebmp.system.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/erp")
@RequiredArgsConstructor
public class ErpController {

    private final SalesOrderRepository orderRepository;
    private final SalesOrderItemRepository itemRepository;
    private final SalesOrderService salesOrderService;
    private final StockService stockService;
    private final StockTxnRepository txnRepository;
    private final CustomerRepository customerRepository;
    private final MaterialRepository materialRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;

    public record OutboundRequest(Long warehouseId) {
    }

    @GetMapping("/orders")
    public ApiResponse<List<SalesOrder>> orders() {
        List<SalesOrder> list = orderRepository.findAllByOrderByIdDesc();
        list.forEach(this::enrichOrder);
        return ApiResponse.ok(list);
    }

    @GetMapping("/orders/{id}")
    public ApiResponse<SalesOrder> order(@PathVariable Long id) {
        SalesOrder order = orderRepository.findById(id).orElseThrow(() -> new BizException("订单不存在"));
        enrichOrder(order);
        return ApiResponse.ok(order);
    }

    @PostMapping("/orders")
    public ApiResponse<SalesOrder> createOrder(@RequestBody SalesOrder body) {
        SalesOrder order = salesOrderService.create(body);
        enrichOrder(order);
        return ApiResponse.ok(order);
    }

    @PostMapping("/orders/{id}/submit")
    public ApiResponse<SalesOrder> submitOrder(@PathVariable Long id) {
        return ApiResponse.ok(salesOrderService.submit(id));
    }

    @PostMapping("/orders/{id}/outbound")
    public ApiResponse<SalesOrder> outbound(@PathVariable Long id, @RequestBody OutboundRequest request) {
        if (request.warehouseId() == null) {
            throw new BizException("请选择出库仓库");
        }
        return ApiResponse.ok(salesOrderService.outbound(id, request.warehouseId()));
    }

    @GetMapping("/stock")
    public ApiResponse<List<Stock>> stock(@RequestParam(required = false) Long warehouseId,
                                          @RequestParam(defaultValue = "false") boolean alertOnly) {
        List<Stock> list = stockService.allWithMaterial();
        if (warehouseId != null) {
            list = list.stream().filter(s -> warehouseId.equals(s.getWarehouseId())).toList();
        }
        if (alertOnly) {
            list = list.stream().filter(s -> Boolean.TRUE.equals(s.getAlert())).toList();
        }
        list.forEach(s -> warehouseRepository.findById(s.getWarehouseId())
                .ifPresent(w -> s.setWarehouseName(w.getName())));
        return ApiResponse.ok(list);
    }

    @GetMapping("/stock/alerts")
    public ApiResponse<List<Stock>> alerts() {
        List<Stock> list = stockService.allWithMaterial().stream()
                .filter(s -> Boolean.TRUE.equals(s.getAlert())).toList();
        list.forEach(s -> warehouseRepository.findById(s.getWarehouseId())
                .ifPresent(w -> s.setWarehouseName(w.getName())));
        return ApiResponse.ok(list);
    }

    @GetMapping("/stock/value")
    public ApiResponse<Map<String, Object>> stockValue() {
        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal totalQuantity = BigDecimal.ZERO;
        int alertCount = 0;
        for (Stock s : stockService.allWithMaterial()) {
            BigDecimal qty = s.getQuantity() == null ? BigDecimal.ZERO : s.getQuantity();
            BigDecimal cost = s.getCostPrice() == null ? BigDecimal.ZERO : s.getCostPrice();
            totalValue = totalValue.add(qty.multiply(cost));
            totalQuantity = totalQuantity.add(qty);
            if (Boolean.TRUE.equals(s.getAlert())) {
                alertCount++;
            }
        }
        return ApiResponse.ok(Map.of("value", totalValue, "quantity", totalQuantity, "alertCount", alertCount));
    }

    @GetMapping("/stock/ledger")
    public ApiResponse<List<StockTxn>> ledger() {
        List<StockTxn> list = txnRepository.findTop100ByOrderByIdDesc();
        list.forEach(t -> {
            materialRepository.findById(t.getMaterialId()).ifPresent(m -> {
                t.setMaterialName(m.getName());
                t.setMaterialCode(m.getCode());
                t.setUnit(m.getUnit());
            });
            warehouseRepository.findById(t.getWarehouseId()).ifPresent(w -> t.setWarehouseName(w.getName()));
            if (t.getOperatorId() != null) {
                userRepository.findById(t.getOperatorId()).ifPresent(u -> t.setOperatorName(u.getRealName()));
            }
        });
        return ApiResponse.ok(list);
    }

    @GetMapping("/stock/options")
    public ApiResponse<List<Stock>> stockOptions() {
        return ApiResponse.ok(stockService.allWithMaterial());
    }

    @PostMapping("/stock/adjust")
    public ApiResponse<Void> adjust(@RequestBody Map<String, Object> body) {
        Long materialId = body.get("materialId") == null ? null : Long.valueOf(body.get("materialId").toString());
        Long warehouseId = body.get("warehouseId") == null ? null : Long.valueOf(body.get("warehouseId").toString());
        BigDecimal quantity = new BigDecimal(body.get("quantity").toString());
        if (materialId == null || warehouseId == null) {
            throw new BizException("请选择物料与仓库");
        }
        if (quantity.compareTo(BigDecimal.ZERO) >= 0) {
            stockService.inbound(materialId, warehouseId, quantity, "ADJUST", "盘点调整",
                    SecurityUtils.currentUserId(), "库存调整");
        } else {
            stockService.outbound(materialId, warehouseId, quantity.abs(), "ADJUST", "盘点调整",
                    SecurityUtils.currentUserId(), "库存调整");
        }
        return ApiResponse.ok();
    }

    private void enrichOrder(SalesOrder order) {
        customerRepository.findById(order.getCustomerId()).ifPresent(c -> order.setCustomerName(c.getName()));
        if (order.getCreatorId() != null) {
            userRepository.findById(order.getCreatorId()).ifPresent(u -> order.setCreatorName(u.getRealName()));
        }
        List<SalesOrderItem> items = itemRepository.findByOrderId(order.getId());
        items.forEach(i -> materialRepository.findById(i.getMaterialId()).ifPresent(m -> {
            i.setMaterialName(m.getName());
            i.setSpec(m.getSpec());
            i.setUnit(m.getUnit());
        }));
        order.setItems(items);
        order.setItemCount(items.size());
        BigDecimal totalQty = items.stream().map(SalesOrderItem::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shipped = items.stream().map(i -> i.getShippedQty() == null ? BigDecimal.ZERO : i.getShippedQty())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setShippedPercent(totalQty.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                : shipped.multiply(BigDecimal.valueOf(100)).divide(totalQty, 0, RoundingMode.HALF_UP));
    }
}
