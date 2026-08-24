package com.aiops.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "差评回复生成参数")
public class NegativeReplyGenerateDTO {
    @Schema(description = "评论主键 ID", example = "22")
    private Long commentId;
    @Schema(description = "回复语气", example = "sincere")
    private String toneType = "sincere";
    @Schema(description = "输出语言", example = "zh-CN")
    private String language = "zh-CN";
}
