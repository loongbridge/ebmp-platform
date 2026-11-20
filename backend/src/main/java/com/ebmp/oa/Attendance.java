package com.ebmp.oa;

import com.ebmp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "oa_attendance")
public class Attendance extends BaseEntity {
    private Long userId;

    @Column(nullable = false)
    private LocalDate workDate;

    private LocalDateTime checkInTime;

    private LocalDateTime checkOutTime;

    /** NORMAL / LATE / EARLY / ABSENT */
    private String status = "NORMAL";

    private String remark;

    @Transient
    private String userName;
}
