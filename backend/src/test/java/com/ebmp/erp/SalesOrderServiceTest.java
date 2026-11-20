package com.ebmp.erp;

import com.ebmp.common.AuthUser;
import com.ebmp.common.BizException;
import com.ebmp.masterdata.Customer;
import com.ebmp.masterdata.CustomerRepository;
import com.ebmp.masterdata.Material;
import com.ebmp.masterdata.MaterialRepository;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SalesOrderServiceTest {

    @Mock
    private SalesOrderRepository orderRepository;
    @Mock
    private SalesOrderItemRepository itemRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StockService stockService;
    @Mock
    private ApprovalService approvalService;

    private SalesOrderService salesOrderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(orderRepository.save(any(SalesOrder.class))).thenAnswer(inv -> {
            SalesOrder order = inv.getArgument(0);
            if (order.getId() == null) {
                order.setId(10L);
            }
            return order;
        });
        when(itemRepository.save(any(SalesOrderItem.class))).thenAnswer(inv -> {
            SalesOrderItem item = inv.getArgument(0);
            if (item.getId() == null) {
                item.setId(100L);
            }
            return item;
        });
        salesOrderService = new SalesOrderService(orderRepository, itemRepository, customerRepository,
                materialRepository, userRepository, stockService, approvalService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void login(Long userId) {
        AuthUser user = new AuthUser();
        user.setId(userId);
        user.setUsername("u" + userId);
        user.setRoles(List.of("SALES"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    @Test
    void createComputesItemAmountsAndTotal() {
        login(8L);
        Material material = new Material();
        material.setId(1L);
        material.setSalePrice(new BigDecimal("100"));
        when(materialRepository.findById(1L)).thenReturn(Optional.of(material));

        SalesOrder body = new SalesOrder();
        SalesOrderItem item = new SalesOrderItem();
        item.setMaterialId(1L);
        item.setQuantity(new BigDecimal("3"));
        body.setItems(List.of(item));

        SalesOrder created = salesOrderService.create(body);

        assertEquals("DRAFT", created.getStatus());
        assertEquals(8L, created.getCreatorId());
        assertEquals(new BigDecimal("300"), created.getTotalAmount());
        assertEquals(new BigDecimal("300"), item.getAmount());
        assertEquals(BigDecimal.ZERO, item.getShippedQty());
        assertNotNull(created.getOrderDate());
        verify(itemRepository).save(item);
    }

    @Test
    void submitRejectsNonDraftOrder() {
        SalesOrder order = new SalesOrder();
        order.setId(11L);
        order.setStatus("APPROVED");
        when(orderRepository.findById(11L)).thenReturn(Optional.of(order));

        BizException ex = assertThrows(BizException.class, () -> salesOrderService.submit(11L));
        assertEquals("当前状态不可提交审批", ex.getMessage());
    }

    @Test
    void submitCreatesApprovalAndMarksPending() {
        SalesOrder order = new SalesOrder();
        order.setId(12L);
        order.setOrderNo("SO2026TEST");
        order.setCustomerId(1L);
        order.setCreatorId(8L);
        order.setStatus("DRAFT");
        order.setRemark("测试");
        when(orderRepository.findById(12L)).thenReturn(Optional.of(order));
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setName("华东建设集团");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        Approval approval = new Approval();
        approval.setId(50L);
        when(approvalService.submit(eq("ERP"), eq("SALES_ORDER"), eq(12L), eq("SO2026TEST"), any(),
                any(BigDecimal.class), eq(8L), eq("测试"))).thenReturn(approval);

        SalesOrder submitted = salesOrderService.submit(12L);

        assertEquals("PENDING", submitted.getStatus());
        assertEquals(50L, submitted.getApprovalId());
        verify(approvalService).submit(eq("ERP"), eq("SALES_ORDER"), eq(12L), eq("SO2026TEST"), any(),
                any(BigDecimal.class), eq(8L), eq("测试"));
    }

    @Test
    void outboundRequiresApprovedOrder() {
        SalesOrder order = new SalesOrder();
        order.setId(13L);
        order.setStatus("PENDING");
        when(orderRepository.findById(13L)).thenReturn(Optional.of(order));

        BizException ex = assertThrows(BizException.class, () -> salesOrderService.outbound(13L, 1L));
        assertEquals("订单审批通过后方可出库", ex.getMessage());
        verify(stockService, never()).outbound(anyLong(), anyLong(), any(), any(), any(), anyLong(), any());
    }

    @Test
    void outboundWritesOffStockForEachItem() {
        SalesOrder order = new SalesOrder();
        order.setId(14L);
        order.setOrderNo("SO2026OUT");
        order.setStatus("APPROVED");
        when(orderRepository.findById(14L)).thenReturn(Optional.of(order));
        SalesOrderItem item1 = new SalesOrderItem();
        item1.setOrderId(14L);
        item1.setMaterialId(1L);
        item1.setQuantity(new BigDecimal("10"));
        item1.setShippedQty(BigDecimal.ZERO);
        SalesOrderItem item2 = new SalesOrderItem();
        item2.setOrderId(14L);
        item2.setMaterialId(2L);
        item2.setQuantity(new BigDecimal("4"));
        item2.setShippedQty(BigDecimal.ZERO);
        when(itemRepository.findByOrderId(14L)).thenReturn(List.of(item1, item2));
        login(8L);

        SalesOrder result = salesOrderService.outbound(14L, 1L);

        assertEquals("OUTBOUND", result.getStatus());
        assertEquals(new BigDecimal("10"), item1.getShippedQty());
        assertEquals(new BigDecimal("4"), item2.getShippedQty());
        verify(stockService, times(2)).outbound(anyLong(), eq(1L), any(BigDecimal.class), eq("SALES_OUT"),
                eq("SO2026OUT"), eq(8L), eq("销售出库"));
    }

    @Test
    void outboundRejectsOrderWithoutItems() {
        SalesOrder order = new SalesOrder();
        order.setId(15L);
        order.setStatus("APPROVED");
        when(orderRepository.findById(15L)).thenReturn(Optional.of(order));
        when(itemRepository.findByOrderId(15L)).thenReturn(List.of());

        BizException ex = assertThrows(BizException.class, () -> salesOrderService.outbound(15L, 1L));
        assertEquals("订单无商品明细", ex.getMessage());
    }

    @Test
    void approvalCallbacksSyncOrderStatus() {
        SalesOrder order = new SalesOrder();
        order.setId(16L);
        order.setStatus("PENDING");
        when(orderRepository.findById(16L)).thenReturn(Optional.of(order));
        Approval approval = new Approval();
        approval.setBizId(16L);

        salesOrderService.onApproved(approval);
        assertEquals("APPROVED", order.getStatus());

        Approval rejected = new Approval();
        rejected.setBizId(16L);
        salesOrderService.onRejected(rejected);
        assertEquals("REJECTED", order.getStatus());
        assertEquals("SALES_ORDER", salesOrderService.bizType());
    }
}
