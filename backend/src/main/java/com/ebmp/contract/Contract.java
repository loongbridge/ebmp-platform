package com.ebmp.contract;

import com.ebmp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 统一合同中心：客户合同（CRM）与供应商合同（SRM）共用，共享审批与归档能力。
 */
@Getter
@Setter
@Entity
@Table(name = "biz_contract")
public class Contract extends BaseEntity {
    @Column(nullable = false, unique = true, length = 50)
    private String contractNo;

    /** CUSTOMER / SUPPLIER */
    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false)
    private Long partyId;

    private Long opportunityId;

    private Long salesOrderId;

    @Column(nullable = false, length = 200)
    private String title;

    private BigDecimal amount = BigDecimal.ZERO;

    private LocalDate startDate;

    private LocalDate endDate;

    /** DRAFT / PENDING / ACTIVE / REJECTED / EXPIRED */
    private String status = "DRAFT";

    private Long approvalId;

    private Long ownerId;

    private LocalDateTime signedAt;

    private String remark;

    @Transient
    private String partyName;

    @Transient
    private String ownerName;
}
