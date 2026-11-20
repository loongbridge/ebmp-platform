package com.ebmp.system;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findByDeptId(Long deptId);

    long countByStatus(Integer status);

    @Query("select u from User u where (:kw is null or lower(u.realName) like lower(concat('%', :kw, '%')) " +
            "or lower(u.username) like lower(concat('%', :kw, '%'))) " +
            "and (:deptId is null or u.deptId = :deptId) order by u.id desc")
    Page<User> search(@Param("kw") String kw, @Param("deptId") Long deptId, Pageable pageable);
}
