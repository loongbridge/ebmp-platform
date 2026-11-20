package com.ebmp.workflow;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalNodeRepository extends JpaRepository<ApprovalNode, Long> {
    List<ApprovalNode> findByApprovalIdOrderByStepNoAsc(Long approvalId);

    List<ApprovalNode> findByApproverIdAndStatusOrderByIdDesc(Long approverId, String status);

    @org.springframework.data.jpa.repository.Query(
            "select n from ApprovalNode n where n.approverId = :approverId and n.status = 'PENDING' order by n.id desc")
    Page<ApprovalNode> findPendingByApprover(@org.springframework.data.repository.query.Param("approverId") Long approverId,
                                            Pageable pageable);

    long countByApproverIdAndStatus(Long approverId, String status);

    void deleteByApprovalId(Long approvalId);
}
