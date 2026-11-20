package com.ebmp.masterdata;

import com.ebmp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "md_contact")
public class Contact extends BaseEntity {
    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false, length = 50)
    private String name;

    private String position;

    private String phone;

    private String email;

    private Integer isPrimary = 0;

    private String remark;
}
