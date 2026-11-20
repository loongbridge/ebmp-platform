package com.ebmp.erp;

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
@Table(name = "erp_sales_order")
public class SalesOrder extends BaseEntity {
    @Column(nullable = false, unique = true, length = 50)
    private String orderNo;

    @Column(nullable = false)
    private Long customerId;

    private Long opportunityId;

    private Long contractId;

    private BigDecimal totalAmount = BigDecimal.ZERO;

    /** DRAFT / PENDING / APPROVED / REJECTED / OUTBOUND / DONE / CANCELLED */
    private String status = "DRAFT";

    private LocalDate orderDate;

    private Long creatorId;

    private Long approvalId;

    private String remark;

    @Transient
    private String customerName;

    @Transient
    private String creatorName;

    @Transient
    private List<SalesOrderItem> items;

    @Transient
    private Integer itemCount;

    @Transient
    private BigDecimal shippedPercent;
}
