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
@Table(name = "sys_user_role")
public class UserRole extends BaseEntity {
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long roleId;
}
