package com.aiops.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "问题解决方案响应")
public class ProblemSolutionVO {
    @Schema(description = "方案 ID")
    private Long id;
    @Schema(description = "问题类型")
    private String problemType;
    @Schema(description = "英文类目")
    private String categoryNameEn;
    @Schema(description = "方案标题")
    private String solutionTitle;
    @Schema(description = "方案内容")
    private String solutionContent;
    @Schema(description = "关键词")
    private String keywords;
    @Schema(description = "来源类型")
    private String sourceType;
    @Schema(description = "优先级")
    private Integer priority;
    @Schema(description = "使用次数")
    private Integer useCount;
    @Schema(description = "启用状态")
    private Integer enabled;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
