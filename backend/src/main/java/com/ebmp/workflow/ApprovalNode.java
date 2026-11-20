package com.ebmp.workflow;

import com.ebmp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "wf_approval_node")
public class ApprovalNode extends BaseEntity {
    @Column(nullable = false)
    private Long approvalId;

    @Column(nullable = false)
    private Integer stepNo;

    @Column(nullable = false, length = 50)
    private String nodeName;

    private Long approverId;

    @Column(length = 50)
    private String approverName;

    /** PENDING / APPROVED / REJECTED / SKIPPED */
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    private String comment;

    private LocalDateTime actedAt;
}
