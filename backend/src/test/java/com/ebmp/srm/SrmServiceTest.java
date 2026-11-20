package com.ebmp.srm;

import com.ebmp.common.AuthUser;
import com.ebmp.common.BizException;
import com.ebmp.erp.StockService;
import com.ebmp.masterdata.Material;
import com.ebmp.masterdata.MaterialRepository;
import com.ebmp.masterdata.SupplierRepository;
import com.ebmp.system.UserRepository;
import com.ebmp.workflow.Approval;
import com.ebmp.workflow.ApprovalService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SrmServiceTest {

    @Mock
    private PurchaseRequisitionRepository requisitionRepository;
    @Mock
    private RequisitionItemRepository requisitionItemRepository;
    @Mock
    private PurchaseOrderRepository orderRepository;
    @Mock
    private PurchaseOrderItemRepository orderItemRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StockService stockService;
    @Mock
    private ApprovalService approvalService;

    private SrmService srmService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(requisitionRepository.save(any(PurchaseRequisition.class))).thenAnswer(inv -> {
            PurchaseRequisition req = inv.getArgument(0);
            if (req.getId() == null) {
                req.setId(10L);
            }
            return req;
        });
        when(orderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> {
            PurchaseOrder order = inv.getArgument(0);
            if (order.getId() == null) {
                order.setId(20L);
            }
            return order;
        });
        when(requisitionItemRepository.save(any(RequisitionItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.save(any(PurchaseOrderItem.class))).thenAnswer(inv -> inv.getArgument(0));
        srmService = new SrmService(requisitionRepository, requisitionItemRepository, orderRepository,
                orderItemRepository, supplierRepository, materialRepository, userRepository,
                stockService, approvalService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void login(Long userId) {
        AuthUser user = new AuthUser();
        user.setId(userId);
        user.setUsername("u" + userId);
        user.setRoles(List.of("BUYER"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private Material material(Long id, String costPrice) {
        Material material = new Material();
        material.setId(id);
        material.setCostPrice(new BigDecimal(costPrice));
        return material;
    }

    @Test
    void createOrderFallsBackToMaterialCostWhenPriceOmitted() {
        login(4L);
        when(materialRepository.findById(1L)).thenReturn(Optional.of(material(1L, "2000")));
        PurchaseOrder body = new PurchaseOrder();
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setMaterialId(1L);
        item.setQuantity(new BigDecimal("5"));
        body.setItems(List.of(item));

        PurchaseOrder created = srmService.createOrder(body);

        assertEquals("DRAFT", created.getStatus());
        assertEquals(4L, created.getCreatorId());
        assertEquals(new BigDecimal("10000"), created.getTotalAmount());
        assertEquals(new BigDecimal("2000"), item.getPrice());
        assertEquals(new BigDecimal("10000"), item.getAmount());
        assertEquals(BigDecimal.ZERO, item.getReceivedQty());
    }

    @Test
    void createRequisitionComputesAmountFromEstimatedPrice() {
        login(4L);
        when(materialRepository.findById(1L)).thenReturn(Optional.of(material(1L, "1500")));
        PurchaseRequisition body = new PurchaseRequisition();
        body.setTitle("测试采购需求");
        RequisitionItem item = new RequisitionItem();
        item.setMaterialId(1L);
        item.setQuantity(new BigDecimal("4"));
        body.setItems(List.of(item));

        PurchaseRequisition created = srmService.createRequisition(body);

        assertEquals("DRAFT", created.getStatus());
        assertEquals(4L, created.getApplicantId());
        assertEquals(new BigDecimal("6000"), created.getTotalAmount());
        assertEquals(new BigDecimal("1500"), item.getEstPrice());
    }

    @Test
    void submitOrderRejectsNonDraftOrder() {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(21L);
        order.setStatus("APPROVED");
        when(orderRepository.findById(21L)).thenReturn(Optional.of(order));

        BizException ex = assertThrows(BizException.class, () -> srmService.submitOrder(21L));
        assertEquals("当前状态不可提交审批", ex.getMessage());
        verify(approvalService, never()).submit(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void submitOrderCreatesApproval() {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(22L);
        order.setOrderNo("PO2026TEST");
        order.setSupplierId(1L);
        order.setCreatorId(4L);
        order.setStatus("DRAFT");
        order.setTotalAmount(new BigDecimal("3000"));
        when(orderRepository.findById(22L)).thenReturn(Optional.of(order));
        com.ebmp.masterdata.Supplier supplier = new com.ebmp.masterdata.Supplier();
        supplier.setId(1L);
        supplier.setName("宝钢物资");
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        Approval approval = new Approval();
        approval.setId(60L);
        when(approvalService.submit(eq("SRM"), eq("PURCHASE_ORDER"), eq(22L), eq("PO2026TEST"), any(),
                any(BigDecimal.class), eq(4L), any())).thenReturn(approval);

        PurchaseOrder submitted = srmService.submitOrder(22L);

        assertEquals("PENDING", submitted.getStatus());
        assertEquals(60L, submitted.getApprovalId());
    }

    @Test
    void receiveRequiresApprovedOrder() {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(23L);
        order.setStatus("DRAFT");
        when(orderRepository.findById(23L)).thenReturn(Optional.of(order));

        BizException ex = assertThrows(BizException.class, () -> srmService.receive(23L, 1L));
        assertEquals("订单审批通过后方可入库", ex.getMessage());
        verify(stockService, never()).inbound(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void receiveInboundsRemainingQuantityAndClosesOrder() {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(24L);
        order.setOrderNo("PO2026RECV");
        order.setStatus("APPROVED");
        when(orderRepository.findById(24L)).thenReturn(Optional.of(order));
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setOrderId(24L);
        item.setMaterialId(1L);
        item.setQuantity(new BigDecimal("10"));
        item.setReceivedQty(BigDecimal.ZERO);
        when(orderItemRepository.findByOrderId(24L)).thenReturn(List.of(item));
        login(4L);

        PurchaseOrder result = srmService.receive(24L, 1L);

        assertEquals("RECEIVED", result.getStatus());
        assertEquals(new BigDecimal("10"), item.getReceivedQty());
        verify(stockService).inbound(eq(1L), eq(1L), eq(new BigDecimal("10")), eq("PURCHASE_IN"),
                eq("PO2026RECV"), eq(4L), eq("采购入库"));
    }
}
