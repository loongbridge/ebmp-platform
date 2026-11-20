package com.ebmp.crm;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    List<Activity> findAllByOrderByIdDesc();

    List<Activity> findByBizTypeAndBizIdOrderByIdDesc(String bizType, Long bizId);

    long countByBizTypeAndBizId(String bizType, Long bizId);
}
