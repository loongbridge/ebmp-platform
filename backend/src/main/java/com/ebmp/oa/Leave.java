package com.ebmp.oa;

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
@Table(name = "oa_leave")
public class Leave extends BaseEntity {
    @Column(nullable = false, length = 50)
    private String leaveNo;

    private Long userId;

    private String type = "ANNUAL";

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private BigDecimal days;

    private String reason;

    /** DRAFT / PENDING / APPROVED / REJECTED */
    private String status = "DRAFT";

    private Long approvalId;

    @Transient
    private String userName;

    @Transient
    private String deptName;
}
