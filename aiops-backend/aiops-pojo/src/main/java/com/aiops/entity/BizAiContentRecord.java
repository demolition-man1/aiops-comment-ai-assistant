package com.aiops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_ai_content_record")
public class BizAiContentRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String targetType;
    private String targetId;
    private String contentType;
    private String styleType;
    private String prompt;
    private String generatedContent;
    private String modelName;
    private Integer tokenUsage;
    private LocalDateTime createTime;
}
