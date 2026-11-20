package com.ebmp.srm;

import com.ebmp.common.BizException;
import com.ebmp.common.SecurityUtils;
import com.ebmp.erp.StockService;
import com.ebmp.masterdata.MaterialRepository;
import com.ebmp.masterdata.SupplierRepository;
import com.ebmp.system.UserRepository;
import com.ebmp.workflow.Approval;
import com.ebmp.workflow.ApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SrmService {

    private final PurchaseRequisitionRepository requisitionRepository;
    private final RequisitionItemRepository requisitionItemRepository;
    private final PurchaseOrderRepository orderRepository;
    private final PurchaseOrderItemRepository orderItemRepository;
    private final SupplierRepository supplierRepository;
    private final MaterialRepository materialRepository;
    private final UserRepository userRepository;
    private final StockService stockService;
    private final ApprovalService approvalService;

    @Transactional
    public PurchaseRequisition createRequisition(PurchaseRequisition body) {
        body.setId(null);
        body.setReqNo("PR" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        body.setApplicantId(SecurityUtils.currentUserId());
        body.setStatus("DRAFT");
        body.setTotalAmount(BigDecimal.ZERO);
        PurchaseRequisition req = requisitionRepository.save(body);
        BigDecimal total = BigDecimal.ZERO;
        if (body.getItems() != null) {
            for (RequisitionItem item : body.getItems()) {
                item.setId(null);
                item.setRequisitionId(req.getId());
                // 预估单价未填写时（实体默认值为 0）回退到物料成本价，避免零金额采购需求
                if (item.getEstPrice() == null || item.getEstPrice().compareTo(BigDecimal.ZERO) == 0) {
                    item.setEstPrice(materialRepository.findById(item.getMaterialId())
                            .map(m -> m.getCostPrice()).orElse(BigDecimal.ZERO));
                }
                if (item.getQuantity() == null) {
                    item.setQuantity(BigDecimal.ZERO);
                }
                item.setAmount(item.getEstPrice().multiply(item.getQuantity()));
                requisitionItemRepository.save(item);
                total = total.add(item.getAmount());
            }
        }
        req.setTotalAmount(total);
        return requisitionRepository.save(req);
    }

    @Transactional
    public PurchaseRequisition submitRequisition(Long id) {
        PurchaseRequisition req = requisitionRepository.findById(id)
                .orElseThrow(() -> new BizException("采购需求不存在"));
        if (!"DRAFT".equals(req.getStatus()) && !"REJECTED".equals(req.getStatus())) {
            throw new BizException("当前状态不可提交审批");
        }
        req.setStatus("PENDING");
        requisitionRepository.save(req);
        Approval approval = approvalService.submit("SRM", "PURCHASE_REQ", req.getId(), req.getReqNo(),
                "采购需求审批 - " + req.getTitle(), req.getTotalAmount(), req.getApplicantId(), req.getRemark());
        req.setApprovalId(approval.getId());
        return requisitionRepository.save(req);
    }

    @Transactional
    public PurchaseOrder createOrder(PurchaseOrder body) {
        body.setId(null);
        body.setOrderNo("PO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        body.setCreatorId(SecurityUtils.currentUserId());
        body.setStatus("DRAFT");
        body.setTotalAmount(BigDecimal.ZERO);
        PurchaseOrder order = orderRepository.save(body);
        BigDecimal total = BigDecimal.ZERO;
        if (body.getItems() != null) {
            for (PurchaseOrderItem item : body.getItems()) {
                item.setId(null);
                item.setOrderId(order.getId());
                // 采购单价未填写时（实体默认值为 0）回退到物料成本价，避免生成零金额采购订单
                if (item.getPrice() == null || item.getPrice().compareTo(BigDecimal.ZERO) == 0) {
                    item.setPrice(materialRepository.findById(item.getMaterialId())
                            .map(m -> m.getCostPrice()).orElse(BigDecimal.ZERO));
                }
                if (item.getQuantity() == null) {
                    item.setQuantity(BigDecimal.ZERO);
                }
                item.setAmount(item.getPrice().multiply(item.getQuantity()));
                item.setReceivedQty(BigDecimal.ZERO);
                orderItemRepository.save(item);
                total = total.add(item.getAmount());
            }
        }
        order.setTotalAmount(total);
        return orderRepository.save(order);
    }

    @Transactional
    public PurchaseOrder submitOrder(Long id) {
        PurchaseOrder order = orderRepository.findById(id).orElseThrow(() -> new BizException("采购订单不存在"));
        if (!"DRAFT".equals(order.getStatus()) && !"REJECTED".equals(order.getStatus())) {
            throw new BizException("当前状态不可提交审批");
        }
        String supplierName = supplierRepository.findById(order.getSupplierId())
                .map(s -> s.getName()).orElse("供应商");
        order.setStatus("PENDING");
        orderRepository.save(order);
        Approval approval = approvalService.submit("SRM", "PURCHASE_ORDER", order.getId(), order.getOrderNo(),
                "采购订单审批 - " + supplierName, order.getTotalAmount(), order.getCreatorId(), order.getRemark());
        order.setApprovalId(approval.getId());
        return orderRepository.save(order);
    }

    @Transactional
    public PurchaseOrder receive(Long id, Long warehouseId) {
        PurchaseOrder order = orderRepository.findById(id).orElseThrow(() -> new BizException("采购订单不存在"));
        if (!"APPROVED".equals(order.getStatus()) && !"PARTIALLY_RECEIVED".equals(order.getStatus())) {
            throw new BizException("订单审批通过后方可入库");
        }
        List<PurchaseOrderItem> items = orderItemRepository.findByOrderId(id);
        boolean allReceived = true;
        for (PurchaseOrderItem item : items) {
            BigDecimal received = item.getReceivedQty() == null ? BigDecimal.ZERO : item.getReceivedQty();
            BigDecimal remain = item.getQuantity().subtract(received);
            if (remain.compareTo(BigDecimal.ZERO) > 0) {
                stockService.inbound(item.getMaterialId(), warehouseId, remain, "PURCHASE_IN",
                        order.getOrderNo(), SecurityUtils.currentUserId(), "采购入库");
                item.setReceivedQty(item.getQuantity());
                orderItemRepository.save(item);
            }
            if (item.getReceivedQty().compareTo(item.getQuantity()) < 0) {
                allReceived = false;
            }
        }
        order.setStatus(allReceived ? "RECEIVED" : "PARTIALLY_RECEIVED");
        if (order.getRequisitionId() != null) {
            requisitionRepository.findById(order.getRequisitionId()).ifPresent(req -> {
                req.setStatus("CONVERTED");
                requisitionRepository.save(req);
            });
        }
        return orderRepository.save(order);
    }

    public void updateRequisitionStatus(Approval approval, boolean approved) {
        requisitionRepository.findById(approval.getBizId()).ifPresent(req -> {
            req.setStatus(approved ? "APPROVED" : "REJECTED");
            requisitionRepository.save(req);
        });
    }

    public void updateOrderStatus(Approval approval, boolean approved) {
        orderRepository.findById(approval.getBizId()).ifPresent(order -> {
            order.setStatus(approved ? "APPROVED" : "REJECTED");
            orderRepository.save(order);
        });
    }
}
