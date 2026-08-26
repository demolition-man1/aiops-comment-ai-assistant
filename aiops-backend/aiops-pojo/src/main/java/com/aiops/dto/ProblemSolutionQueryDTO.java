package com.aiops.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "问题解决方案查询参数")
public class ProblemSolutionQueryDTO {
    @Schema(description = "页码")
    private Integer pageNum = 1;
    @Schema(description = "每页条数")
    private Integer pageSize = 10;
    @Schema(description = "问题类型")
    private String problemType;
    @Schema(description = "英文类目")
    private String categoryNameEn;
    @Schema(description = "关键词，匹配标题、内容或关键词字段")
    private String keyword;
    @Schema(description = "启用状态，1 启用 0 停用")
    private Integer enabled;
}
