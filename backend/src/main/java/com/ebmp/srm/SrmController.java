package com.ebmp.srm;

import com.ebmp.common.ApiResponse;
import com.ebmp.common.BizException;
import com.ebmp.common.SecurityUtils;
import com.ebmp.masterdata.MaterialRepository;
import com.ebmp.masterdata.SupplierRepository;
import com.ebmp.system.DeptRepository;
import com.ebmp.system.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/srm")
@RequiredArgsConstructor
public class SrmController {

    private final SrmService srmService;
    private final PurchaseRequisitionRepository requisitionRepository;
    private final RequisitionItemRepository requisitionItemRepository;
    private final PurchaseOrderRepository orderRepository;
    private final PurchaseOrderItemRepository orderItemRepository;
    private final SupplierEvaluationRepository evaluationRepository;
    private final SupplierRepository supplierRepository;
    private final MaterialRepository materialRepository;
    private final UserRepository userRepository;
    private final DeptRepository deptRepository;

    public record ReceiveRequest(Long warehouseId) {
    }

    @GetMapping("/requisitions")
    public ApiResponse<List<PurchaseRequisition>> requisitions() {
        List<PurchaseRequisition> list = requisitionRepository.findAllByOrderByIdDesc();
        list.forEach(this::enrichRequisition);
        return ApiResponse.ok(list);
    }

    @GetMapping("/requisitions/{id}")
    public ApiResponse<PurchaseRequisition> requisition(@PathVariable Long id) {
        PurchaseRequisition req = requisitionRepository.findById(id).orElseThrow(() -> new BizException("采购需求不存在"));
        enrichRequisition(req);
        return ApiResponse.ok(req);
    }

    @PostMapping("/requisitions")
    public ApiResponse<PurchaseRequisition> createRequisition(@RequestBody PurchaseRequisition body) {
        PurchaseRequisition req = srmService.createRequisition(body);
        enrichRequisition(req);
        return ApiResponse.ok(req);
    }

    @PostMapping("/requisitions/{id}/submit")
    public ApiResponse<PurchaseRequisition> submitRequisition(@PathVariable Long id) {
        return ApiResponse.ok(srmService.submitRequisition(id));
    }

    @GetMapping("/orders")
    public ApiResponse<List<PurchaseOrder>> orders() {
        List<PurchaseOrder> list = orderRepository.findAllByOrderByIdDesc();
        list.forEach(this::enrichOrder);
        return ApiResponse.ok(list);
    }

    @GetMapping("/orders/{id}")
    public ApiResponse<PurchaseOrder> order(@PathVariable Long id) {
        PurchaseOrder order = orderRepository.findById(id).orElseThrow(() -> new BizException("采购订单不存在"));
        enrichOrder(order);
        return ApiResponse.ok(order);
    }

    @PostMapping("/orders")
    public ApiResponse<PurchaseOrder> createOrder(@RequestBody PurchaseOrder body) {
        PurchaseOrder order = srmService.createOrder(body);
        enrichOrder(order);
        return ApiResponse.ok(order);
    }

    @PostMapping("/orders/{id}/submit")
    public ApiResponse<PurchaseOrder> submitOrder(@PathVariable Long id) {
        return ApiResponse.ok(srmService.submitOrder(id));
    }

    @PostMapping("/orders/{id}/receive")
    public ApiResponse<PurchaseOrder> receive(@PathVariable Long id, @RequestBody ReceiveRequest request) {
        if (request.warehouseId() == null) {
            throw new BizException("请选择入库仓库");
        }
        return ApiResponse.ok(srmService.receive(id, request.warehouseId()));
    }

    @GetMapping("/evaluations")
    public ApiResponse<List<SupplierEvaluation>> evaluations() {
        List<SupplierEvaluation> list = evaluationRepository.findAllByOrderByIdDesc();
        list.forEach(e -> {
            supplierRepository.findById(e.getSupplierId()).ifPresent(s -> e.setSupplierName(s.getName()));
            if (e.getEvaluatorId() != null) {
                userRepository.findById(e.getEvaluatorId()).ifPresent(u -> e.setEvaluatorName(u.getRealName()));
            }
        });
        return ApiResponse.ok(list);
    }

    @PostMapping("/evaluations")
    public ApiResponse<SupplierEvaluation> createEvaluation(@RequestBody SupplierEvaluation body) {
        body.setId(null);
        body.setEvaluatorId(SecurityUtils.currentUserId());
        BigDecimal total = body.getQualityScore().add(body.getDeliveryScore())
                .add(body.getServiceScore()).add(body.getPriceScore());
        body.setTotalScore(total.divide(BigDecimal.valueOf(4), 1, RoundingMode.HALF_UP));
        SupplierEvaluation saved = evaluationRepository.save(body);
        supplierRepository.findById(body.getSupplierId()).ifPresent(s -> {
            s.setScore(saved.getTotalScore());
            s.setLevel(total.divide(BigDecimal.valueOf(4), 1, RoundingMode.HALF_UP)
                    .compareTo(BigDecimal.valueOf(90)) >= 0 ? "A" : (saved.getTotalScore().compareTo(BigDecimal.valueOf(75)) >= 0 ? "B" : "C"));
            supplierRepository.save(s);
        });
        return ApiResponse.ok(saved);
    }

    @GetMapping("/performance")
    public ApiResponse<List<Map<String, Object>>> performance() {
        List<Map<String, Object>> result = orderRepository.findAll().stream()
                .collect(java.util.stream.Collectors.groupingBy(PurchaseOrder::getSupplierId))
                .entrySet().stream().map(entry -> {
                    BigDecimal total = entry.getValue().stream().map(PurchaseOrder::getTotalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    var supplier = supplierRepository.findById(entry.getKey()).orElse(null);
                    return Map.<String, Object>of(
                            "supplierId", entry.getKey(),
                            "supplierName", supplier == null ? "-" : supplier.getName(),
                            "orderCount", entry.getValue().size(),
                            "totalAmount", total,
                            "score", supplier == null || supplier.getScore() == null ? BigDecimal.ZERO : supplier.getScore());
                }).toList();
        return ApiResponse.ok(result);
    }

    private void enrichRequisition(PurchaseRequisition req) {
        if (req.getApplicantId() != null) {
            userRepository.findById(req.getApplicantId()).ifPresent(u -> {
                req.setApplicantName(u.getRealName());
                if (u.getDeptId() != null) {
                    deptRepository.findById(u.getDeptId()).ifPresent(d -> req.setDeptName(d.getName()));
                }
            });
        }
        List<RequisitionItem> items = requisitionItemRepository.findByRequisitionId(req.getId());
        items.forEach(i -> materialRepository.findById(i.getMaterialId()).ifPresent(m -> {
            i.setMaterialName(m.getName());
            i.setSpec(m.getSpec());
            i.setUnit(m.getUnit());
        }));
        req.setItems(items);
        req.setItemCount(items.size());
    }

    private void enrichOrder(PurchaseOrder order) {
        supplierRepository.findById(order.getSupplierId()).ifPresent(s -> order.setSupplierName(s.getName()));
        if (order.getCreatorId() != null) {
            userRepository.findById(order.getCreatorId()).ifPresent(u -> order.setCreatorName(u.getRealName()));
        }
        List<PurchaseOrderItem> items = orderItemRepository.findByOrderId(order.getId());
        items.forEach(i -> materialRepository.findById(i.getMaterialId()).ifPresent(m -> {
            i.setMaterialName(m.getName());
            i.setSpec(m.getSpec());
            i.setUnit(m.getUnit());
        }));
        order.setItems(items);
        order.setItemCount(items.size());
        BigDecimal totalQty = items.stream().map(PurchaseOrderItem::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal received = items.stream().map(i -> i.getReceivedQty() == null ? BigDecimal.ZERO : i.getReceivedQty())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setReceivedPercent(totalQty.compareTo(BigDecimal.ZERO) == 0 ? 0
                : received.multiply(BigDecimal.valueOf(100)).divide(totalQty, 0, RoundingMode.HALF_UP).intValue());
    }
}
