package com.ebmp.workflow;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {
    List<Approval> findByBizTypeAndBizId(String bizType, Long bizId);

    long countByStatus(String status);

    List<Approval> findTop10ByStatusOrderByIdDesc(String status);

    @Query("select a from Approval a where (:module is null or a.module = :module) " +
            "and (:status is null or a.status = :status) and (:applicantId is null or a.applicantId = :applicantId) " +
            "order by a.id desc")
    Page<Approval> search(@Param("module") String module, @Param("status") String status,
                          @Param("applicantId") Long applicantId, Pageable pageable);
}
