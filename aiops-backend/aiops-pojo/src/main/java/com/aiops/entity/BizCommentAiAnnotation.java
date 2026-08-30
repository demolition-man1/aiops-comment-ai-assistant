package com.aiops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_comment_ai_annotation")
public class BizCommentAiAnnotation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long commentId;
    private String manualSentiment;
    private String manualProblemTypes;
    private String annotationNote;
    private Long annotatedBy;
    private LocalDateTime annotationTime;
    private LocalDateTime updateTime;
}
