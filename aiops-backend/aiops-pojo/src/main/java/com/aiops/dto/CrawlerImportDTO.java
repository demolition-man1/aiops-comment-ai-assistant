package com.aiops.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "爬虫导入参数")
public class CrawlerImportDTO {
    @Schema(description = "平台名称", example = "demo")
    private String platform;
    @Schema(description = "目标公开样例 URL")
    private String targetUrl;
    @Schema(description = "采集目标类型", example = "product_comment")
    private String targetType = "product_comment";
    @Schema(description = "最大采集条数", example = "100")
    private Integer maxCount = 100;
    @Schema(description = "请求间隔秒数", example = "3")
    private Integer delaySeconds = 3;
    @Schema(description = "备注")
    private String remark;
}
