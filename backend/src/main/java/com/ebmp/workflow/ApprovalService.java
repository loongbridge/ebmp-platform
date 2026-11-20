package com.ebmp.workflow;

import com.ebmp.common.BizException;
import com.ebmp.common.SecurityUtils;
import com.ebmp.system.DeptRepository;
import com.ebmp.system.Role;
import com.ebmp.system.RoleRepository;
import com.ebmp.system.User;
import com.ebmp.system.UserRepository;
import com.ebmp.system.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ApprovalService {

    private static final BigDecimal HIGH_AMOUNT = new BigDecimal("50000");

    private final ApprovalRepository approvalRepository;
    private final ApprovalNodeRepository nodeRepository;
    private final DeptRepository deptRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final ObjectProvider<ApprovalHandler> handlers;

    @Transactional
    public Approval submit(String module, String bizType, Long bizId, String bizNo, String title,
                           BigDecimal amount, Long applicantId, String remark) {
        List<Approval> existing = approvalRepository.findByBizTypeAndBizId(bizType, bizId);
        if (existing.stream().anyMatch(a -> "PENDING".equals(a.getStatus()))) {
            throw new BizException("该单据已提交审批，请勿重复提交");
        }
        User applicant = userRepository.findById(applicantId).orElseThrow(() -> new BizException("申请人不存在"));
        User deptApprover = applicant.getDeptId() == null ? null : deptRepository.findById(applicant.getDeptId())
                .map(d -> d.getLeaderId()).map(id -> userRepository.findById(id).orElse(null)).orElse(null);
        User gm = findGeneralManager();

        List<User> approvers = new ArrayList<>();
        if (deptApprover != null && !deptApprover.getId().equals(applicantId)) {
            approvers.add(deptApprover);
        }
        boolean highAmount = (amount != null && amount.compareTo(HIGH_AMOUNT) >= 0) || "CONTRACT".equals(bizType);
        if (highAmount && gm != null && approvers.stream().noneMatch(u -> u.getId().equals(gm.getId()))) {
            approvers.add(gm);
        }
        if (approvers.isEmpty() && gm != null) {
            approvers.add(gm);
        }

        Approval approval = new Approval();
        approval.setModule(module);
        approval.setBizType(bizType);
        approval.setBizId(bizId);
        approval.setBizNo(bizNo);
        approval.setTitle(title);
        approval.setApplicantId(applicantId);
        approval.setApplicantName(applicant.getRealName());
        approval.setAmount(amount == null ? BigDecimal.ZERO : amount);
        approval.setRemark(remark);
        approval.setStatus("PENDING");
        approval.setCurrentStep(1);
        approval.setTotalSteps(approvers.size());
        approval.setCurrentNodeName(approvers.isEmpty() ? "审批" : approvers.get(0).getRealName());
        approval = approvalRepository.save(approval);

        for (int i = 0; i < approvers.size(); i++) {
            User approver = approvers.get(i);
            ApprovalNode node = new ApprovalNode();
            node.setApprovalId(approval.getId());
            node.setStepNo(i + 1);
            node.setNodeName((i == approvers.size() - 1 && approvers.size() > 1)
                    ? "总经理审批" : (approver.getId().equals(gm == null ? null : gm.getId()) ? "总经理审批" : "部门负责人审批"));
            node.setApproverId(approver.getId());
            node.setApproverName(approver.getRealName());
            node.setStatus("PENDING");
            nodeRepository.save(node);
        }
        return approval;
    }

    @Transactional
    public Approval act(Long approvalId, boolean approve, String comment) {
        Approval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new BizException("审批单不存在"));
        if (!"PENDING".equals(approval.getStatus())) {
            throw new BizException("该审批单已处理完成");
        }
        List<ApprovalNode> nodes = nodeRepository.findByApprovalIdOrderByStepNoAsc(approvalId);
        ApprovalNode current = nodes.stream()
                .filter(n -> n.getStepNo().equals(approval.getCurrentStep()))
                .findFirst().orElseThrow(() -> new BizException("审批节点异常"));
        if (!SecurityUtils.isAdmin() && !current.getApproverId().equals(SecurityUtils.currentUserId())) {
            throw new BizException("当前节点不由您审批", 403);
        }
        current.setStatus(approve ? "APPROVED" : "REJECTED");
        current.setComment(comment);
        current.setActedAt(LocalDateTime.now());
        nodeRepository.save(current);

        if (!approve) {
            approval.setStatus("REJECTED");
            approval.setFinishedAt(LocalDateTime.now());
            approvalRepository.save(approval);
            callback(approval, false);
            return approval;
        }
        if (approval.getCurrentStep() < approval.getTotalSteps()) {
            approval.setCurrentStep(approval.getCurrentStep() + 1);
            ApprovalNode next = nodes.stream()
                    .filter(n -> n.getStepNo().equals(approval.getCurrentStep())).findFirst().orElse(null);
            approval.setCurrentNodeName(next == null ? "" : next.getNodeName());
        } else {
            approval.setStatus("APPROVED");
            approval.setFinishedAt(LocalDateTime.now());
            approval.setCurrentNodeName("已通过");
            callback(approval, true);
        }
        approvalRepository.save(approval);
        return approval;
    }

    public List<Map<String, Object>> todo(Long userId) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (ApprovalNode node : nodeRepository.findByApproverIdAndStatusOrderByIdDesc(userId, "PENDING")) {
            approvalRepository.findById(node.getApprovalId()).ifPresent(a -> {
                if (!"PENDING".equals(a.getStatus()) || !a.getCurrentStep().equals(node.getStepNo())) {
                    return;
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("approvalId", a.getId());
                item.put("module", a.getModule());
                item.put("bizType", a.getBizType());
                item.put("bizNo", a.getBizNo());
                item.put("title", a.getTitle());
                item.put("applicantName", a.getApplicantName());
                item.put("amount", a.getAmount());
                item.put("nodeName", node.getNodeName());
                item.put("createdAt", a.getCreatedAt());
                list.add(item);
            });
        }
        return list;
    }

    public long pendingCount(Long userId) {
        // 只统计当前审批步骤落在本人名下的节点，避免多级审批中后续节点被提前计入
        return todo(userId).size();
    }

    public Approval detail(Long id, Long userId) {
        Approval approval = approvalRepository.findById(id).orElseThrow(() -> new BizException("审批单不存在"));
        List<ApprovalNode> nodes = nodeRepository.findByApprovalIdOrderByStepNoAsc(id);
        approval.setNodes(nodes);
        return approval;
    }

    private void callback(Approval approval, boolean approved) {
        for (ApprovalHandler handler : handlers.orderedStream().toList()) {
            if (handler.bizType().equals(approval.getBizType())) {
                if (approved) {
                    handler.onApproved(approval);
                } else {
                    handler.onRejected(approval);
                }
                return;
            }
        }
    }

    private User findGeneralManager() {
        Role gmRole = roleRepository.findByCode("GM").orElse(null);
        if (gmRole != null) {
            for (var ur : userRoleRepository.findByRoleId(gmRole.getId())) {
                var user = userRepository.findById(ur.getUserId()).orElse(null);
                if (user != null && user.getStatus() != null && user.getStatus() == 1) {
                    return user;
                }
            }
        }
        return userRepository.findByUsername("gm")
                .orElseGet(() -> userRepository.findByUsername("admin").orElse(null));
    }
}
