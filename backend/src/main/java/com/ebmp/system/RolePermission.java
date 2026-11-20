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
@Table(name = "sys_role_permission")
public class RolePermission extends BaseEntity {
    @Column(nullable = false)
    private Long roleId;

    @Column(nullable = false)
    private Long permissionId;
}
