package com.aiops.service;

import com.aiops.vo.FileSignedUrlVO;
import com.aiops.vo.FileUploadVO;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    FileUploadVO upload(MultipartFile file, String businessType);

    FileSignedUrlVO signedUrl(Long fileId);
}

