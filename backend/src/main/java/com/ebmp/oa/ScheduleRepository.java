package com.ebmp.oa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByOwnerIdOrderByStartTimeAsc(Long ownerId);

    List<Schedule> findByStartTimeBetweenOrderByStartTimeAsc(LocalDateTime start, LocalDateTime end);
}
