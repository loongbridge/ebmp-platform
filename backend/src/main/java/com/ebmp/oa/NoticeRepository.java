package com.ebmp.oa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findTop5ByStatusOrderByIdDesc(Integer status);

    Page<Notice> findAllByOrderByIdDesc(Pageable pageable);
}
