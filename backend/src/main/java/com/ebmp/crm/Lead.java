package com.ebmp.crm;

import com.ebmp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "crm_lead")
public class Lead extends BaseEntity {
    @Column(nullable = false, unique = true, length = 50)
    private String leadNo;

    @Column(nullable = false, length = 100)
    private String name;

    private String company;

    private String contactName;

    private String phone;

    private String email;

    private String source;

    /** NEW / FOLLOWING / QUALIFIED / CONVERTED / INVALID */
    private String status = "NEW";

    private Long ownerId;

    private LocalDateTime nextFollowAt;

    private Long customerId;

    private Long opportunityId;

    private String remark;

    @Transient
    private String ownerName;
}
