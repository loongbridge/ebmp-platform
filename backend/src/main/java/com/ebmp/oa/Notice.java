package com.ebmp.oa;

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
@Table(name = "oa_notice")
public class Notice extends BaseEntity {
    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 5000)
    private String content;

    private String category;

    private Long publisherId;

    private Integer status = 1;

    private LocalDateTime publishTime;

    private Integer views = 0;

    @Transient
    private String publisherName;
}
