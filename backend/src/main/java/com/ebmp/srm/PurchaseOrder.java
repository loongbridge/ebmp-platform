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
@Table(name = "srm_purchase_order")
public class PurchaseOrder extends BaseEntity {
    @Column(nullable = false, unique = true, length = 50)
    private String orderNo;

    @Column(nullable = false)
    private Long supplierId;

    private Long requisitionId;

    private BigDecimal totalAmount = BigDecimal.ZERO;

    /** DRAFT / PENDING / APPROVED / REJECTED / PARTIALLY_RECEIVED / RECEIVED / CANCELLED */
    private String status = "DRAFT";

    private LocalDate expectedDate;

    private Long creatorId;

    private Long approvalId;

    private String remark;

    @Transient
    private String supplierName;

    @Transient
    private String creatorName;

    @Transient
    private List<PurchaseOrderItem> items;

    @Transient
    private Integer itemCount;

    @Transient
    private Integer receivedPercent;
}
