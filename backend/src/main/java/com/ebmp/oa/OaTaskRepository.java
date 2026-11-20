package com.ebmp.oa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OaTaskRepository extends JpaRepository<OaTask, Long> {
    List<OaTask> findByAssigneeIdOrderByIdDesc(Long assigneeId);

    List<OaTask> findAllByOrderByIdDesc();

    long countByAssigneeIdAndStatusNot(Long assigneeId, String status);
}
