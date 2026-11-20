package com.ebmp.system;

import com.ebmp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sys_permission")
public class Permission extends BaseEntity {
    private Long parentId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(unique = true, length = 80)
    private String code;

    private String type;

    private String path;

    private String icon;

    private Integer sort = 0;
}
