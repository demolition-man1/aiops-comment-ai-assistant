package com.aiops.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadVO {
    private Long fileId;
    private String originalName;
    private String objectKey;
    private String fileUrl;
    private Long fileSize;
}

