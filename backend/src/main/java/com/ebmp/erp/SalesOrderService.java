package com.ebmp.erp;

import com.ebmp.common.BizException;
import com.ebmp.common.SecurityUtils;
import com.ebmp.masterdata.CustomerRepository;
import com.ebmp.masterdata.MaterialRepository;
import com.ebmp.system.UserRepository;
import com.ebmp.workflow.Approval;
import com.ebmp.workflow.ApprovalHandler;
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
public class SalesOrderService implements ApprovalHandler {

    private final SalesOrderRepository orderRepository;
    private final SalesOrderItemRepository itemRepository;
    private final CustomerRepository customerRepository;
    private final MaterialRepository materialRepository;
    private final UserRepository userRepository;
    private final StockService stockService;
    private final ApprovalService approvalService;

    @Transactional
    public SalesOrder create(SalesOrder body) {
        body.setId(null);
        body.setOrderNo("SO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        body.setCreatorId(SecurityUtils.currentUserId());
        body.setOrderDate(body.getOrderDate() == null ? LocalDate.now() : body.getOrderDate());
        body.setStatus("DRAFT");
        body.setTotalAmount(BigDecimal.ZERO);
        SalesOrder order = orderRepository.save(body);
        BigDecimal total = BigDecimal.ZERO;
        if (body.getItems() != null) {
            for (SalesOrderItem item : body.getItems()) {
                item.setId(null);
                item.setOrderId(order.getId());
                // 明细价格未填写时（实体默认值为 0）回退到物料销售价，避免生成零金额订单
                if (item.getPrice() == null || item.getPrice().compareTo(BigDecimal.ZERO) == 0) {
                    item.setPrice(materialRepository.findById(item.getMaterialId())
                            .map(m -> m.getSalePrice()).orElse(BigDecimal.ZERO));
                }
                if (item.getQuantity() == null) {
                    item.setQuantity(BigDecimal.ZERO);
                }
                BigDecimal amount = item.getPrice().multiply(item.getQuantity());
                item.setAmount(amount);
                item.setShippedQty(BigDecimal.ZERO);
                itemRepository.save(item);
                total = total.add(amount);
            }
        }
        order.setTotalAmount(total);
        return orderRepository.save(order);
    }

    @Transactional
    public SalesOrder submit(Long id) {
        SalesOrder order = orderRepository.findById(id).orElseThrow(() -> new BizException("销售订单不存在"));
        if (!"DRAFT".equals(order.getStatus()) && !"REJECTED".equals(order.getStatus())) {
            throw new BizException("当前状态不可提交审批");
        }
        String customerName = customerRepository.findById(order.getCustomerId())
                .map(c -> c.getName()).orElse("客户");
        order.setStatus("PENDING");
        order = orderRepository.save(order);
        Approval approval = approvalService.submit("ERP", "SALES_ORDER", order.getId(), order.getOrderNo(),
                "销售订单审批 - " + customerName, order.getTotalAmount(), order.getCreatorId(), order.getRemark());
        order.setApprovalId(approval.getId());
        return orderRepository.save(order);
    }

    @Transactional
    public SalesOrder outbound(Long id, Long warehouseId) {
        SalesOrder order = orderRepository.findById(id).orElseThrow(() -> new BizException("销售订单不存在"));
        if (!"APPROVED".equals(order.getStatus())) {
            throw new BizException("订单审批通过后方可出库");
        }
        List<SalesOrderItem> items = itemRepository.findByOrderId(id);
        if (items.isEmpty()) {
            throw new BizException("订单无商品明细");
        }
        for (SalesOrderItem item : items) {
            BigDecimal remain = item.getQuantity().subtract(item.getShippedQty() == null ? BigDecimal.ZERO : item.getShippedQty());
            if (remain.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            stockService.outbound(item.getMaterialId(), warehouseId, remain, "SALES_OUT",
                    order.getOrderNo(), SecurityUtils.currentUserId(), "销售出库");
            item.setShippedQty(item.getQuantity());
            itemRepository.save(item);
        }
        order.setStatus("OUTBOUND");
        return orderRepository.save(order);
    }

    @Override
    public String bizType() {
        return "SALES_ORDER";
    }

    @Override
    public void onApproved(Approval approval) {
        orderRepository.findById(approval.getBizId()).ifPresent(o -> {
            o.setStatus("APPROVED");
            orderRepository.save(o);
        });
    }

    @Override
    public void onRejected(Approval approval) {
        orderRepository.findById(approval.getBizId()).ifPresent(o -> {
            o.setStatus("REJECTED");
            orderRepository.save(o);
        });
    }
}
