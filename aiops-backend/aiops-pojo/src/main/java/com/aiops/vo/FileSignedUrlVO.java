package com.aiops.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileSignedUrlVO {
    private Long fileId;
    private String objectKey;
    private String signedUrl;
    private Integer expireSeconds;
}

