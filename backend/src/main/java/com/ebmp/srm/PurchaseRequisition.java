package com.ebmp.srm;

import com.ebmp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "srm_requisition")
public class PurchaseRequisition extends BaseEntity {
    @Column(nullable = false, unique = true, length = 50)
    private String reqNo;

    @Column(nullable = false, length = 200)
    private String title;

    private Long deptId;

    private Long applicantId;

    private LocalDate needDate;

    private BigDecimal totalAmount = BigDecimal.ZERO;

    /** DRAFT / PENDING / APPROVED / REJECTED / CONVERTED */
    private String status = "DRAFT";

    private Long approvalId;

    private String remark;

    @Transient
    private String applicantName;

    @Transient
    private String deptName;

    @Transient
    private List<RequisitionItem> items;

    @Transient
    private Integer itemCount;
}
