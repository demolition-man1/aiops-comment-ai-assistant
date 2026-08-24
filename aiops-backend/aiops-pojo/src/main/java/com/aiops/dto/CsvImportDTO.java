package com.aiops.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

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
}
