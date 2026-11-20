package com.ebmp.masterdata;

import com.ebmp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "md_customer")
public class Customer extends BaseEntity {
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    private String shortName;

    private String industry;

    private String level = "B";

    private String source;

    private Integer status = 1;

    private Long ownerId;

    private String phone;

    private String email;

    private String address;

    private String taxNo;

    private BigDecimal creditLimit = BigDecimal.ZERO;

    private String remark;

    private LocalDateTime lastContactAt;

    @Transient
    private String ownerName;

    @Transient
    private Long opportunityCount;
}
