package com.ebmp.workflow;

import com.ebmp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 统一审批单：OA 请假、SRM 采购需求、ERP 采购/销售订单、CRM 合同均共用此模型。
 */
@Getter
@Setter
@Entity
@Table(name = "wf_approval")
public class Approval extends BaseEntity {
    /** 所属模块 OA / SRM / ERP / CRM */
    @Column(nullable = false, length = 20)
    private String module;

    /** 业务类型，如 LEAVE / PURCHASE_REQ / PURCHASE_ORDER / SALES_ORDER / CONTRACT */
    @Column(nullable = false, length = 40)
    private String bizType;

    private Long bizId;

    @Column(length = 50)
    private String bizNo;

    @Column(nullable = false, length = 200)
    private String title;

    private Long applicantId;

    @Column(length = 50)
    private String applicantName;

    private BigDecimal amount = BigDecimal.ZERO;

    /** 当前审批节点序号，从 1 开始 */
    private Integer currentStep = 1;

    private Integer totalSteps = 1;

    @Column(length = 50)
    private String currentNodeName;

    /** PENDING 审批中 / APPROVED 已通过 / REJECTED 已驳回 / CANCELLED 已撤销 */
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    private String remark;

    private LocalDateTime finishedAt;

    @Transient
    private List<ApprovalNode> nodes;
}
