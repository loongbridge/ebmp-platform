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
@Table(name = "srm_requisition_item")
public class RequisitionItem extends BaseEntity {
    @Column(nullable = false)
    private Long requisitionId;

    @Column(nullable = false)
    private Long materialId;

    private BigDecimal quantity = BigDecimal.ZERO;

    private BigDecimal estPrice = BigDecimal.ZERO;

    private BigDecimal amount = BigDecimal.ZERO;

    private String remark;

    @Transient
    private String materialName;

    @Transient
    private String spec;

    @Transient
    private String unit;
}
