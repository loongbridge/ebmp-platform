package com.ebmp.masterdata;

import com.ebmp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "md_supplier")
public class Supplier extends BaseEntity {
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    private String shortName;

    private String category;

    private String contactName;

    private String phone;

    private String email;

    private String address;

    private String taxNo;

    private String bankName;

    private String bankAccount;

    private String level = "B";

    private BigDecimal score = BigDecimal.ZERO;

    private Integer status = 1;

    private LocalDate cooperationStart;

    private String remark;

    @Transient
    private Long purchaseOrderCount;
}
