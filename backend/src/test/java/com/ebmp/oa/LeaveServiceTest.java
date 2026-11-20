package com.ebmp.oa;

import com.ebmp.common.AuthUser;
import com.ebmp.common.BizException;
import com.ebmp.system.User;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LeaveServiceTest {

    @Mock
    private LeaveRepository leaveRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ApprovalService approvalService;

    private LeaveService leaveService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(leaveRepository.save(any(Leave.class))).thenAnswer(inv -> {
            Leave l = inv.getArgument(0);
            if (l.getId() == null) {
                l.setId(1L);
            }
            return l;
        });
        leaveService = new LeaveService(leaveRepository, userRepository, approvalService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void login(Long userId) {
        AuthUser user = new AuthUser();
        user.setId(userId);
        user.setUsername("u" + userId);
        user.setRoles(List.of("EMPLOYEE"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    @Test
    void createInitializesDraftLeaveForCurrentUser() {
        login(100L);
        Leave body = new Leave();
        body.setType("SICK");
        body.setReason("感冒");

        Leave created = leaveService.create(body);

        assertEquals(100L, created.getUserId());
        assertEquals("DRAFT", created.getStatus());
        assertTrue(created.getLeaveNo().startsWith("LV"));
        assertEquals(BigDecimal.ONE, created.getDays());
    }

    @Test
    void submitMovesDraftToPendingAndCreatesApproval() {
        login(100L);
        Leave draft = new Leave();
        draft.setId(5L);
        draft.setUserId(100L);
        draft.setType("ANNUAL");
        draft.setLeaveNo("LV2026TEST");
        draft.setStatus("DRAFT");
        draft.setReason("休假");
        when(leaveRepository.findById(5L)).thenReturn(Optional.of(draft));
        User applicant = new User();
        applicant.setId(100L);
        applicant.setRealName("李静");
        when(userRepository.findById(100L)).thenReturn(Optional.of(applicant));
        Approval approval = new Approval();
        approval.setId(9L);
        when(approvalService.submit(eq("OA"), eq("LEAVE"), eq(5L), anyString(), anyString(),
                any(BigDecimal.class), eq(100L), anyString())).thenReturn(approval);

        Leave submitted = leaveService.submit(5L);

        assertEquals("PENDING", submitted.getStatus());
        assertEquals(9L, submitted.getApprovalId());
        verify(approvalService).submit(eq("OA"), eq("LEAVE"), eq(5L), anyString(),
                org.mockito.ArgumentMatchers.contains("年假"), any(BigDecimal.class), eq(100L), eq("休假"));
    }

    @Test
    void submitRejectsApprovedLeave() {
        Leave approved = new Leave();
        approved.setId(6L);
        approved.setStatus("APPROVED");
        when(leaveRepository.findById(6L)).thenReturn(Optional.of(approved));

        BizException ex = assertThrows(BizException.class, () -> leaveService.submit(6L));
        assertEquals("当前状态不可提交", ex.getMessage());
    }

    @Test
    void submitRejectsMissingLeave() {
        when(leaveRepository.findById(7L)).thenReturn(Optional.empty());
        BizException ex = assertThrows(BizException.class, () -> leaveService.submit(7L));
        assertEquals("请假单不存在", ex.getMessage());
    }

    @Test
    void approvalCallbacksSyncLeaveStatus() {
        Leave pending = new Leave();
        pending.setId(8L);
        pending.setStatus("PENDING");
        when(leaveRepository.findById(8L)).thenReturn(Optional.of(pending));
        Approval approval = new Approval();
        approval.setBizId(8L);

        leaveService.onApproved(approval);
        assertEquals("APPROVED", pending.getStatus());

        Approval rejected = new Approval();
        rejected.setBizId(8L);
        leaveService.onRejected(rejected);
        assertEquals("REJECTED", pending.getStatus());
        assertEquals("LEAVE", leaveService.bizType());
    }
}
