package com.ebmp.system;

import com.ebmp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "sys_dept")
public class Dept extends BaseEntity {
    @Column(nullable = false, length = 50)
    private String name;

    private Long parentId = 0L;

    private Long leaderId;

    private Integer sort = 0;

    private Integer status = 1;

    @Transient
    private String leaderName;

    @Transient
    private List<Dept> children = new ArrayList<>();
}
