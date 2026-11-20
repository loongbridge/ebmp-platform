package com.ebmp.system;

import com.ebmp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "sys_role")
public class Role extends BaseEntity {
    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    private String description;

    private Integer dataScope = 1;

    private Integer status = 1;

    @Transient
    private List<Long> permissionIds;
}
