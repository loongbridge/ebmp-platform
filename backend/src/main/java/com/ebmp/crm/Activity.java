package com.ebmp.crm;

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
@Table(name = "crm_activity")
public class Activity extends BaseEntity {
    /** LEAD / CUSTOMER / OPPORTUNITY */
    @Column(nullable = false, length = 20)
    private String bizType;

    @Column(nullable = false)
    private Long bizId;

    /** CALL / VISIT / EMAIL / MEETING / OTHER */
    private String type = "CALL";

    @Column(length = 1000)
    private String content;

    private String nextPlan;

    private LocalDateTime activityTime;

    private Long ownerId;

    @Transient
    private String ownerName;

    @Transient
    private String bizName;
}
