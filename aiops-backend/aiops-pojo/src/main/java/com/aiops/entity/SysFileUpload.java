package com.aiops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_file_upload")
public class SysFileUpload {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String originalName;
    private String fileName;
    private String objectKey;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private String businessType;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
