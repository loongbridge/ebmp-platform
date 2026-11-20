package com.ebmp.oa;

import com.ebmp.common.BizException;
import com.ebmp.common.SecurityUtils;
import com.ebmp.system.UserRepository;
import com.ebmp.workflow.Approval;
import com.ebmp.workflow.ApprovalHandler;
import com.ebmp.workflow.ApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class LeaveService implements ApprovalHandler {

    private final LeaveRepository leaveRepository;
    private final UserRepository userRepository;
    private final ApprovalService approvalService;

    @Transactional
    public Leave create(Leave body) {
        body.setId(null);
        body.setUserId(SecurityUtils.currentUserId());
        body.setLeaveNo("LV" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        body.setStatus("DRAFT");
        if (body.getDays() == null) {
            body.setDays(BigDecimal.ONE);
        }
        return leaveRepository.save(body);
    }

    @Transactional
    public Leave submit(Long id) {
        Leave leave = leaveRepository.findById(id).orElseThrow(() -> new BizException("请假单不存在"));
        if (!"DRAFT".equals(leave.getStatus()) && !"REJECTED".equals(leave.getStatus())) {
            throw new BizException("当前状态不可提交");
        }
        leave.setStatus("PENDING");
        leave = leaveRepository.save(leave);
        String name = userRepository.findById(leave.getUserId()).map(u -> u.getRealName()).orElse("员工");
        String type = leave.getType();
        Approval approval = approvalService.submit("OA", "LEAVE", leave.getId(), leave.getLeaveNo(),
                name + "的" + typeName(type) + "申请", BigDecimal.ZERO, leave.getUserId(), leave.getReason());
        leave.setApprovalId(approval.getId());
        return leaveRepository.save(leave);
    }

    @Override
    public String bizType() {
        return "LEAVE";
    }

    @Override
    public void onApproved(Approval approval) {
        leaveRepository.findById(approval.getBizId()).ifPresent(leave -> {
            leave.setStatus("APPROVED");
            leaveRepository.save(leave);
        });
    }

    @Override
    public void onRejected(Approval approval) {
        leaveRepository.findById(approval.getBizId()).ifPresent(leave -> {
            leave.setStatus("REJECTED");
            leaveRepository.save(leave);
        });
    }

    private String typeName(String type) {
        if (type == null) {
            return "请假";
        }
        return switch (type) {
            case "ANNUAL" -> "年假";
            case "SICK" -> "病假";
            case "PERSONAL" -> "事假";
            case "MARRIAGE" -> "婚假";
            default -> "请假";
        };
    }
}
