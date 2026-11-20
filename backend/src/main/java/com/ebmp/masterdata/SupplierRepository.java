package com.ebmp.masterdata;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    long countByStatus(Integer status);

    @Query("select s from Supplier s where (:kw is null or lower(s.name) like lower(concat('%', :kw, '%')) " +
            "or lower(s.code) like lower(concat('%', :kw, '%'))) and (:category is null or s.category = :category) " +
            "order by s.id desc")
    Page<Supplier> search(@Param("kw") String kw, @Param("category") String category, Pageable pageable);

    List<Supplier> findAllByOrderByIdDesc();
}
