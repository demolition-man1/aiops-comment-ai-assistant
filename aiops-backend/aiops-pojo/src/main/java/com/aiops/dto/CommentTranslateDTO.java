package com.aiops.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "评论翻译参数")
public class CommentTranslateDTO {
    @Schema(description = "目标语言", example = "zh-CN")
    private String language = "zh-CN";

    @Schema(description = "是否强制重新翻译", example = "false")
    private Boolean forceRefresh = false;
}
