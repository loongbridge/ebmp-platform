package com.ebmp.oa;

import com.ebmp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "oa_schedule")
public class Schedule extends BaseEntity {
    @Column(nullable = false, length = 200)
    private String title;

    private String description;

    private Long ownerId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String type = "WORK";

    private String location;

    private Integer remindMinutes = 15;

    private String status = "PLAN";

    @Transient
    private String ownerName;
}
