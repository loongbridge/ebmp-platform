package com.ebmp.oa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveRepository extends JpaRepository<Leave, Long> {
    List<Leave> findAllByOrderByIdDesc();

    List<Leave> findByUserIdOrderByIdDesc(Long userId);

    long countByStatus(String status);
}
