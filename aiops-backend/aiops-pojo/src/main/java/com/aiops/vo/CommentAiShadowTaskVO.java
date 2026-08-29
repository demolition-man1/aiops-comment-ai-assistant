package com.aiops.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "评论 AI Shadow 任务状态")
public class CommentAiShadowTaskVO {
    private Long taskId;
    private Long runId;
    private String taskStatus;
    private Integer progress;
    private Integer actualSampleSize;
    private String errorMessage;
}
