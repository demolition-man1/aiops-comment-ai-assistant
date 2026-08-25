package com.aiops.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "CSV 导入预检参数")
public class CsvImportPreflightDTO {
    @Schema(description = "CSV 原始文件名", example = "reviews.csv")
    private String fileName;
    @Schema(description = "CSV 文件大小，单位字节", example = "20480")
    private Long fileSize;
    @Schema(description = "浏览器计算的文件 SHA-256，用于重复导入检测")
    private String fileHash;
    @Schema(description = "本地 Olist 数据目录")
    private String dataPath;
    @Schema(description = "预计可导入数据行数", example = "100")
    private Long estimatedRows;
    @Schema(description = "数据来源", example = "platform_csv")
    private String dataSource = "platform_csv";
    @Schema(description = "导入模式", example = "incremental")
    private String importMode = "incremental";
    @Schema(description = "字段映射，key 为标准字段，value 为 CSV 原始列名")
    private Map<String, String> columnMapping;
}
