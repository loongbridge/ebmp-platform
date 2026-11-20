package com.ebmp.oa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeetingAttendeeRepository extends JpaRepository<MeetingAttendee, Long> {
    List<MeetingAttendee> findByMeetingId(Long meetingId);

    void deleteByMeetingId(Long meetingId);
}
