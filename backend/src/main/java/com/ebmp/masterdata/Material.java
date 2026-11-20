package com.ebmp.masterdata;

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
@Table(name = "md_material")
public class Material extends BaseEntity {
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    private String category;

    private String spec;

    @Column(nullable = false, length = 20)
    private String unit;

    private BigDecimal salePrice = BigDecimal.ZERO;

    private BigDecimal costPrice = BigDecimal.ZERO;

    private BigDecimal safeStock = BigDecimal.ZERO;

    private Integer status = 1;

    private String remark;

    @Transient
    private BigDecimal stockQuantity;
}
