package com.aiops.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "同步配置参数")
public class SyncConfigDTO {
    @Schema(description = "同步名称", example = "Olist 每日同步")
    private String syncName;
    @Schema(description = "来源类型", example = "olist_directory", allowableValues = {"olist_directory", "csv_file", "crawler"})
    private String sourceType;
    @Schema(description = "数据来源", example = "olist")
    private String dataSource = "olist";
    @Schema(description = "导入模式", example = "incremental", allowableValues = {"full", "incremental"})
    private String importMode = "incremental";
    @Schema(description = "本地 Olist 数据目录")
    private String dataPath;
    @Schema(description = "已上传 CSV 文件 ID")
    private Long fileId;
    @Schema(description = "OSS 对象 Key")
    private String objectKey;
    @Schema(description = "CSV 文件 URL")
    private String fileUrl;
    @Schema(description = "爬虫平台")
    private String platform;
    @Schema(description = "爬虫目标 URL")
    private String targetUrl;
    @Schema(description = "爬虫目标类型")
    private String targetType;
    @Schema(description = "最大采集数量")
    private Integer maxCount;
    @Schema(description = "采集延时秒数")
    private Integer delaySeconds;
    @Schema(description = "Quartz Cron 表达式", example = "0 0 2 * * ?")
    private String cronExpression;
    @Schema(description = "是否导入后自动分析，0 否 1 是")
    private Integer autoAnalysis = 0;
    @Schema(description = "是否启用，0 停用 1 启用")
    private Integer enabled = 1;
    @Schema(description = "备注")
    private String remark;
}
