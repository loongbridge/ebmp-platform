package com.ebmp.srm;

import com.ebmp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "srm_supplier_evaluation")
public class SupplierEvaluation extends BaseEntity {
    @Column(nullable = false)
    private Long supplierId;

    @Column(nullable = false, length = 20)
    private String period;

    private BigDecimal qualityScore = BigDecimal.ZERO;

    private BigDecimal deliveryScore = BigDecimal.ZERO;

    private BigDecimal serviceScore = BigDecimal.ZERO;

    private BigDecimal priceScore = BigDecimal.ZERO;

    private BigDecimal totalScore = BigDecimal.ZERO;

    private Long evaluatorId;

    private String comment;

    @Transient
    private String supplierName;

    @Transient
    private String evaluatorName;
}
