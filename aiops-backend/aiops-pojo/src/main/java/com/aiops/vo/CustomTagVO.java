package com.aiops.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "自定义标签响应")
public class CustomTagVO {
    @Schema(description = "标签 ID")
    private Long id;
    @Schema(description = "标签名称")
    private String tagName;
    @Schema(description = "标签分组")
    private String tagGroup;
    @Schema(description = "标签颜色")
    private String color;
    @Schema(description = "标签说明")
    private String description;
    @Schema(description = "排序值")
    private Integer sortOrder;
    @Schema(description = "启用状态")
    private Integer enabled;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
