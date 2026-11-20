package com.ebmp.system;

import com.ebmp.common.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "sys_user")
public class User extends BaseEntity {
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @JsonIgnore
    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 50)
    private String realName;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    private String avatar;

    private Long deptId;

    private String position;

    private Integer status = 1;

    private LocalDateTime lastLoginAt;

    @Transient
    private String deptName;

    @Transient
    private List<Long> roleIds;

    @Transient
    private List<String> roleNames;
}
