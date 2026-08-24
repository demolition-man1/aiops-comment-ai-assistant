package com.aiops.controller;

import com.aiops.result.Result;
import com.aiops.service.FileService;
import com.aiops.vo.FileSignedUrlVO;
import com.aiops.vo.FileUploadVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "文件上传", description = "CSV 文件上传和 OSS 临时访问地址")
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    @Operation(summary = "上传文件", description = "上传 CSV 文件到阿里云 OSS，并返回文件记录 ID")
    public Result<FileUploadVO> upload(@Parameter(description = "上传文件") @RequestParam("file") MultipartFile file,
                                       @Parameter(description = "业务类型")
                                       @RequestParam(value = "businessType", defaultValue = "csv_import") String businessType) {
        return Result.success(fileService.upload(file, businessType));
    }

    @GetMapping("/{fileId}/url")
    @Operation(summary = "获取文件临时地址", description = "根据文件 ID 生成 OSS 签名访问 URL")
    public Result<FileSignedUrlVO> signedUrl(@Parameter(description = "文件记录 ID") @PathVariable Long fileId) {
        return Result.success(fileService.signedUrl(fileId));
    }
}
