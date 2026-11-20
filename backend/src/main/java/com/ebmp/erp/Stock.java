package com.ebmp.erp;

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
@Table(name = "erp_stock", uniqueConstraints = @jakarta.persistence.UniqueConstraint(columnNames = {"materialId", "warehouseId"}))
public class Stock extends BaseEntity {
    @Column(nullable = false)
    private Long materialId;

    @Column(nullable = false)
    private Long warehouseId;

    private BigDecimal quantity = BigDecimal.ZERO;

    private BigDecimal lockedQuantity = BigDecimal.ZERO;

    @Transient
    private String materialName;

    @Transient
    private String materialCode;

    @Transient
    private String spec;

    @Transient
    private String unit;

    @Transient
    private BigDecimal safeStock;

    @Transient
    private String warehouseName;

    @Transient
    private BigDecimal costPrice;

    @Transient
    private Boolean alert;
}
