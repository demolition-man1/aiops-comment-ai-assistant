package com.aiops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_comment_ai_decision")
public class BizCommentAiDecision {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long commentId;
    private Long shadowResultId;
    private String acceptedProblemType;
    private BigDecimal confidence;
    private String gateVersion;
    private Integer active;
    private Long activatedBy;
    private LocalDateTime activatedAt;
}
