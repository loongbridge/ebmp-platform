package com.ebmp.crm;

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

@Getter
@Setter
@Entity
@Table(name = "crm_opportunity")
public class Opportunity extends BaseEntity {
    @Column(nullable = false, unique = true, length = 50)
    private String oppNo;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private Long customerId;

    private Long leadId;

    /** INITIAL / NEEDS / SOLUTION / QUOTE / NEGOTIATION / WON / LOST */
    private String stage = "INITIAL";

    private BigDecimal amount = BigDecimal.ZERO;

    private Integer probability = 20;

    private LocalDate expectedCloseDate;

    private Long ownerId;

    /** OPEN / WON / LOST */
    private String status = "OPEN";

    private LocalDateTime wonAt;

    private String remark;

    @Transient
    private String customerName;

    @Transient
    private String ownerName;

    @Transient
    private Long activityCount;

    @Transient
    private Long orderCount;
}
