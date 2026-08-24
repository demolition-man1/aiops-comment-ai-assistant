package com.aiops.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "同步配置响应")
public class SyncConfigVO {
    @Schema(description = "配置 ID")
    private Long id;
    @Schema(description = "同步名称")
    private String syncName;
    @Schema(description = "来源类型")
    private String sourceType;
    @Schema(description = "数据来源")
    private String dataSource;
    @Schema(description = "导入模式")
    private String importMode;
    @Schema(description = "本地 Olist 数据目录")
    private String dataPath;
    @Schema(description = "上传文件 ID")
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
    @Schema(description = "请求延时秒数")
    private Integer delaySeconds;
    @Schema(description = "Quartz Cron 表达式")
    private String cronExpression;
    @Schema(description = "是否导入后自动分析")
    private Integer autoAnalysis;
    @Schema(description = "启用状态")
    private Integer enabled;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "最近运行时间")
    private LocalDateTime lastRunTime;
    @Schema(description = "下次运行时间")
    private LocalDateTime nextRunTime;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
