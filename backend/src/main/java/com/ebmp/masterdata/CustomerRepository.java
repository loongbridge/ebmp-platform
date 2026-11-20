package com.ebmp.masterdata;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    long countByStatus(Integer status);

    @Query("select c from Customer c where (:kw is null or lower(c.name) like lower(concat('%', :kw, '%')) " +
            "or lower(c.code) like lower(concat('%', :kw, '%'))) and (:level is null or c.level = :level) " +
            "order by c.id desc")
    Page<Customer> search(@Param("kw") String kw, @Param("level") String level, Pageable pageable);

    List<Customer> findAllByOrderByIdDesc();
}
