package com.aiops.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "CSV 导入参数")
public class CsvImportDTO {
    @Schema(description = "已上传文件 ID，上传单个 CSV 时使用", example = "12")
    private Long fileId;
    @Schema(description = "OSS 对象 Key", example = "aiops/csv/olist_order_reviews_dataset.csv")
    private String objectKey;
    @Schema(description = "CSV 文件访问 URL")
    private String fileUrl;
    @Schema(description = "本地 Olist 数据目录", example = "D:\\666\\olist-brazilian-ecommerce")
    private String dataPath;
    @Schema(description = "数据来源", example = "olist")
    private String dataSource = "olist";
    @Schema(description = "导入模式", example = "full", allowableValues = {"full", "incremental"})
    private String importMode = "full";
    @Schema(description = "浏览器计算的文件 SHA-256，用于重复导入检测")
    private String fileHash;
    @Schema(description = "字段映射，key 为标准字段，value 为 CSV 原始列名")
    private Map<String, String> columnMapping;
    @Schema(description = "确认允许重复导入", example = "false")
    private Boolean allowDuplicate = false;
    @Schema(description = "是否使用系统内置示例数据", example = "false")
    private Boolean sampleData = false;
}
