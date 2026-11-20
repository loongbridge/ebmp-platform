package com.ebmp.system;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeptRepository extends JpaRepository<Dept, Long> {
    List<Dept> findAllByOrderBySortAsc();
}
