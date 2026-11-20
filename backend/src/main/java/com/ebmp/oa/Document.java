package com.ebmp.oa;

import com.ebmp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "oa_document")
public class Document extends BaseEntity {
    @Column(nullable = false, length = 200)
    private String name;

    private String category;

    private String filePath;

    private String fileUrl;

    private Long fileSize;

    private Long uploaderId;

    private String version = "v1.0";

    private String remark;

    @Transient
    private String uploaderName;
}
