package com.ebmp.oa;

import com.ebmp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "oa_meeting")
public class Meeting extends BaseEntity {
    @Column(nullable = false, length = 200)
    private String title;

    private String room;

    private Long organizerId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String status = "SCHEDULED";

    private String remark;

    @Transient
    private String organizerName;

    @Transient
    private List<String> attendeeNames;

    @Transient
    private List<Long> attendeeIds;
}
