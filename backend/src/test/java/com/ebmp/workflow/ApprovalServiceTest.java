package com.ebmp.workflow;

import com.ebmp.common.AuthUser;
import com.ebmp.common.BizException;
import com.ebmp.system.Dept;
import com.ebmp.system.DeptRepository;
import com.ebmp.system.Role;
import com.ebmp.system.RoleRepository;
import com.ebmp.system.User;
import com.ebmp.system.UserRepository;
import com.ebmp.system.UserRole;
import com.ebmp.system.UserRoleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApprovalServiceTest {

    @Mock
    private ApprovalRepository approvalRepository;
    @Mock
    private ApprovalNodeRepository nodeRepository;
    @Mock
    private DeptRepository deptRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private ObjectProvider<ApprovalHandler> handlers;

    private ApprovalService approvalService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(handlers.orderedStream()).thenReturn(Stream.empty());
        when(approvalRepository.save(any(Approval.class))).thenAnswer(inv -> {
            Approval a = inv.getArgument(0);
            if (a.getId() == null) {
                a.setId(1L);
            }
            return a;
        });
        when(approvalRepository.findByBizTypeAndBizId(any(), any())).thenReturn(List.of());
        approvalService = new ApprovalService(approvalRepository, nodeRepository, deptRepository,
                userRepository, userRoleRepository, roleRepository, handlers);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void login(Long userId, String... roles) {
        AuthUser user = new AuthUser();
        user.setId(userId);
        user.setUsername("u" + userId);
        user.setRoles(List.of(roles));
        user.setPermissions(List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private User user(Long id, String name, Long deptId) {
        User u = new User();
        u.setId(id);
        u.setRealName(name);
        u.setDeptId(deptId);
        u.setStatus(1);
        return u;
    }

    private void mockGeneralManager() {
        Role gmRole = new Role();
        gmRole.setId(10L);
        gmRole.setCode("GM");
        when(roleRepository.findByCode("GM")).thenReturn(Optional.of(gmRole));
        UserRole ur = new UserRole();
        ur.setUserId(300L);
        when(userRoleRepository.findByRoleId(10L)).thenReturn(List.of(ur));
        when(userRepository.findById(300L)).thenReturn(Optional.of(user(300L, "张明远", null)));
    }

    @Test
    void submitBuildsDepartmentAndGeneralManagerChainForHighAmount() {
        login(100L, "SALES");
        when(userRepository.findById(100L)).thenReturn(Optional.of(user(100L, "李静", 2L)));
        Dept dept = new Dept();
        dept.setId(2L);
        dept.setLeaderId(200L);
        when(deptRepository.findById(2L)).thenReturn(Optional.of(dept));
        when(userRepository.findById(200L)).thenReturn(Optional.of(user(200L, "王强", 2L)));
        mockGeneralManager();

        Approval approval = approvalService.submit("ERP", "SALES_ORDER", 7L, "SO2026TEST", "销售订单审批",
                new BigDecimal("80000"), 100L, "测试");

        assertEquals(2, approval.getTotalSteps());
        assertEquals(1, approval.getCurrentStep());
        assertEquals("PENDING", approval.getStatus());
        assertEquals("王强", approval.getCurrentNodeName());
        assertEquals("李静", approval.getApplicantName());

        ArgumentCaptor<ApprovalNode> captor = ArgumentCaptor.forClass(ApprovalNode.class);
        verify(nodeRepository, times(2)).save(captor.capture());
        List<ApprovalNode> nodes = captor.getAllValues();
        assertEquals(1, nodes.get(0).getStepNo());
        assertEquals(200L, nodes.get(0).getApproverId());
        assertEquals("部门负责人审批", nodes.get(0).getNodeName());
        assertEquals(2, nodes.get(1).getStepNo());
        assertEquals(300L, nodes.get(1).getApproverId());
        assertEquals("总经理审批", nodes.get(1).getNodeName());
    }

    @Test
    void submitFallsBackToGeneralManagerWhenApplicantIsDepartmentLeader() {
        login(300L, "GM");
        when(userRepository.findById(300L)).thenReturn(Optional.of(user(300L, "张明远", null)));
        mockGeneralManager();

        Approval approval = approvalService.submit("OA", "LEAVE", 8L, "LV2026TEST", "请假申请",
                BigDecimal.ZERO, 300L, "年假");

        assertEquals(1, approval.getTotalSteps());
        verify(nodeRepository, times(1)).save(any(ApprovalNode.class));
        assertEquals("张明远", approval.getCurrentNodeName());
    }

    @Test
    void submitRejectsDuplicatedPendingApproval() {
        Approval pending = new Approval();
        pending.setId(1L);
        pending.setStatus("PENDING");
        when(approvalRepository.findByBizTypeAndBizId("LEAVE", 8L)).thenReturn(List.of(pending));

        BizException ex = assertThrows(BizException.class,
                () -> approvalService.submit("OA", "LEAVE", 8L, "LV2026TEST", "请假", BigDecimal.ZERO, 100L, ""));
        assertEquals("该单据已提交审批，请勿重复提交", ex.getMessage());
    }

    @Test
    void actRejectsUserWhoIsNotCurrentApprover() {
        login(999L, "SALES");
        Approval approval = pendingApproval(1L, 1, 2);
        ApprovalNode step1 = node(1L, 1, 200L);
        ApprovalNode step2 = node(1L, 2, 300L);
        when(approvalRepository.findById(1L)).thenReturn(Optional.of(approval));
        when(nodeRepository.findByApprovalIdOrderByStepNoAsc(1L)).thenReturn(List.of(step1, step2));

        BizException ex = assertThrows(BizException.class, () -> approvalService.act(1L, true, "同意"));
        assertEquals(403, ex.getCode());
    }

    @Test
    void actAdvancesStepAndFinishesWithCallback() {
        Approval approval = pendingApproval(1L, 1, 2);
        ApprovalNode step1 = node(1L, 1, 200L);
        ApprovalNode step2 = node(1L, 2, 300L);
        when(approvalRepository.findById(1L)).thenReturn(Optional.of(approval));
        when(nodeRepository.findByApprovalIdOrderByStepNoAsc(1L)).thenReturn(List.of(step1, step2));
        ApprovalHandler handler = mock(ApprovalHandler.class);
        when(handler.bizType()).thenReturn("LEAVE");
        when(handlers.orderedStream()).thenReturn(Stream.of(handler));

        login(200L, "DEPT_MANAGER");
        Approval afterFirst = approvalService.act(1L, true, "部门同意");
        assertEquals(2, afterFirst.getCurrentStep());
        assertEquals("PENDING", afterFirst.getStatus());
        assertEquals("总经理审批", afterFirst.getCurrentNodeName());
        verify(handler, never()).onApproved(any());

        login(300L, "GM");
        Approval afterSecond = approvalService.act(1L, true, "总经理同意");
        assertEquals("APPROVED", afterSecond.getStatus());
        assertEquals("已通过", afterSecond.getCurrentNodeName());
        assertNotNull(afterSecond.getFinishedAt());
        verify(handler, times(1)).onApproved(afterSecond);
    }

    @Test
    void actRejectFinishesAndNotifiesHandler() {
        Approval approval = pendingApproval(1L, 1, 1);
        ApprovalNode step1 = node(1L, 1, 200L);
        when(approvalRepository.findById(1L)).thenReturn(Optional.of(approval));
        when(nodeRepository.findByApprovalIdOrderByStepNoAsc(1L)).thenReturn(List.of(step1));
        ApprovalHandler handler = mock(ApprovalHandler.class);
        when(handler.bizType()).thenReturn("LEAVE");
        when(handlers.orderedStream()).thenReturn(Stream.of(handler));

        login(200L, "DEPT_MANAGER");
        Approval result = approvalService.act(1L, false, "不同意");
        assertEquals("REJECTED", result.getStatus());
        verify(handler, times(1)).onRejected(result);
    }

    @Test
    void actOnFinishedApprovalIsRejected() {
        Approval approval = pendingApproval(1L, 1, 1);
        approval.setStatus("APPROVED");
        when(approvalRepository.findById(1L)).thenReturn(Optional.of(approval));
        login(1L, "ADMIN");

        BizException ex = assertThrows(BizException.class, () -> approvalService.act(1L, true, ""));
        assertEquals("该审批单已处理完成", ex.getMessage());
    }

    @Test
    void pendingCountOnlyIncludesCurrentStepNodes() {
        ApprovalNode mine = node(1L, 1, 200L);
        ApprovalNode later = node(2L, 2, 200L);
        when(nodeRepository.findByApproverIdAndStatusOrderByIdDesc(200L, "PENDING"))
                .thenReturn(List.of(mine, later));
        Approval first = pendingApproval(1L, 1, 2);
        Approval second = pendingApproval(2L, 1, 2);
        when(approvalRepository.findById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return Optional.of(id == 1L ? first : second);
        });

        assertEquals(1, approvalService.pendingCount(200L));
        assertEquals(1, approvalService.todo(200L).size());
    }

    private Approval pendingApproval(Long id, int currentStep, int totalSteps) {
        Approval a = new Approval();
        a.setId(id);
        a.setModule("OA");
        a.setBizType("LEAVE");
        a.setBizId(id);
        a.setStatus("PENDING");
        a.setCurrentStep(currentStep);
        a.setTotalSteps(totalSteps);
        return a;
    }

    private ApprovalNode node(Long approvalId, int stepNo, Long approverId) {
        ApprovalNode n = new ApprovalNode();
        n.setId((long) (stepNo + 10));
        n.setApprovalId(approvalId);
        n.setStepNo(stepNo);
        n.setApproverId(approverId);
        n.setNodeName(stepNo == 1 ? "部门负责人审批" : "总经理审批");
        n.setStatus("PENDING");
        return n;
    }
}
