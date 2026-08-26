package com.aiops.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "自定义标签查询参数")
public class CustomTagQueryDTO {
    @Schema(description = "页码")
    private Integer pageNum = 1;
    @Schema(description = "每页条数")
    private Integer pageSize = 10;
    @Schema(description = "关键词，匹配标签名称或说明")
    private String keyword;
    @Schema(description = "标签分组")
    private String tagGroup;
    @Schema(description = "启用状态，1 启用 0 停用")
    private Integer enabled;
}
