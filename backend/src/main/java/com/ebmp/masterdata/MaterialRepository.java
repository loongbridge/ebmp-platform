package com.ebmp.masterdata;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    long countByStatus(Integer status);

    @Query("select m from Material m where (:kw is null or lower(m.name) like lower(concat('%', :kw, '%')) " +
            "or lower(m.code) like lower(concat('%', :kw, '%'))) and (:category is null or m.category = :category) " +
            "order by m.id desc")
    Page<Material> search(@Param("kw") String kw, @Param("category") String category, Pageable pageable);

    List<Material> findAllByOrderByIdAsc();
}
