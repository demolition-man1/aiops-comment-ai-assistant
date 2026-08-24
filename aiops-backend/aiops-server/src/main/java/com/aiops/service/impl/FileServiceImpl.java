package com.aiops.service.impl;

import com.aiops.entity.SysFileUpload;
import com.aiops.exception.BusinessException;
import com.aiops.mapper.SysFileUploadMapper;
import com.aiops.properties.AliyunOssProperties;
import com.aiops.service.FileService;
import com.aiops.vo.FileSignedUrlVO;
import com.aiops.vo.FileUploadVO;
import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final SysFileUploadMapper fileUploadMapper;
    private final AliyunOssProperties ossProperties;

    @Override
    public FileUploadVO upload(MultipartFile file, String businessType) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".csv")) {
            throw new BusinessException("仅支持上传 CSV 文件");
        }
        String fileName = UUID.randomUUID() + "-" + originalName;
        String objectKey = ossProperties.getUploadDir() + "/" + fileName;
        OSS ossClient = new OSSClientBuilder().build(
                ossProperties.getEndpoint(),
                ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeySecret());
        try {
            ossClient.putObject(ossProperties.getBucketName(), objectKey, file.getInputStream());
        } catch (Exception exception) {
            throw new BusinessException(500, "上传阿里云 OSS 失败：" + exception.getMessage());
        } finally {
            ossClient.shutdown();
        }

        SysFileUpload record = new SysFileUpload();
        record.setOriginalName(originalName);
        record.setFileName(fileName);
        record.setObjectKey(objectKey);
        record.setFileUrl(buildPublicUrl(objectKey));
        record.setFileType("csv");
        record.setFileSize(file.getSize());
        record.setBusinessType(businessType);
        record.setStatus(1);
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        fileUploadMapper.insert(record);
        return new FileUploadVO(record.getId(), originalName, objectKey, record.getFileUrl(), file.getSize());
    }

    @Override
    public FileSignedUrlVO signedUrl(Long fileId) {
        SysFileUpload file = fileUploadMapper.selectById(fileId);
        if (file == null) {
            throw new BusinessException(404, "文件不存在");
        }
        OSS ossClient = new OSSClientBuilder().build(
                ossProperties.getEndpoint(),
                ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeySecret());
        try {
            Date expiration = new Date(System.currentTimeMillis() + ossProperties.getSignedUrlExpireSeconds() * 1000);
            URL signedUrl = ossClient.generatePresignedUrl(
                    ossProperties.getBucketName(), file.getObjectKey(), expiration, HttpMethod.GET);
            return new FileSignedUrlVO(fileId, file.getObjectKey(), signedUrl.toString(),
                    ossProperties.getSignedUrlExpireSeconds().intValue());
        } finally {
            ossClient.shutdown();
        }
    }

    private String buildPublicUrl(String objectKey) {
        String endpoint = ossProperties.getEndpoint().replace("https://", "").replace("http://", "");
        return "https://" + ossProperties.getBucketName() + "." + endpoint + "/" + objectKey;
    }
}

