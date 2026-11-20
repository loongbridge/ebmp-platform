package com.ebmp.oa;

import com.ebmp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "oa_meeting_attendee")
public class MeetingAttendee extends BaseEntity {
    @Column(nullable = false)
    private Long meetingId;

    @Column(nullable = false)
    private Long userId;
}
