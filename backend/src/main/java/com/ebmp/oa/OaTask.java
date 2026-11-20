package com.ebmp.oa;

import com.ebmp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "oa_task")
public class OaTask extends BaseEntity {
    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String description;

    private Long assigneeId;

    private Long creatorId;

    private LocalDate dueDate;

    private String priority = "NORMAL";

    private String status = "TODO";

    private LocalDateTime completedAt;

    @Transient
    private String assigneeName;

    @Transient
    private String creatorName;
}
