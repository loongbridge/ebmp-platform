package com.ebmp.masterdata;

import com.ebmp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "md_warehouse")
public class Warehouse extends BaseEntity {
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    private String location;

    private Long managerId;

    private Integer status = 1;

    private String remark;

    @Transient
    private String managerName;

    @Transient
    private java.math.BigDecimal totalQuantity;
}
