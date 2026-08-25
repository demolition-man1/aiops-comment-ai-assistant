package com.aiops.controller;

import com.aiops.dto.CrawlerImportDTO;
import com.aiops.dto.CsvImportDTO;
import com.aiops.dto.CsvImportPreflightDTO;
import com.aiops.result.Result;
import com.aiops.service.DataImportService;
import com.aiops.vo.CsvImportPreflightVO;
import com.aiops.vo.TaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data/import")
@RequiredArgsConstructor
@Tag(name = "数据导入", description = "CSV 导入、公开样例爬虫导入和导入任务查询")
public class DataImportController {

    private final DataImportService dataImportService;

    @PostMapping("/csv/preflight")
    @Operation(summary = "CSV 导入预检", description = "导入前检查预计行数、字段映射和重复导入风险")
    public Result<CsvImportPreflightVO> preflightCsv(@RequestBody CsvImportPreflightDTO preflightDTO) {
        return Result.success(dataImportService.preflightCsv(preflightDTO));
    }

    @PostMapping("/csv")
    @Operation(summary = "CSV 数据导入", description = "根据 OSS 文件或本地 Olist 数据目录创建导入任务")
    public Result<TaskVO> importCsv(@RequestBody CsvImportDTO csvImportDTO) {
        return Result.success(dataImportService.importCsv(csvImportDTO));
    }

    @PostMapping("/sample")
    @Operation(summary = "一键导入示例数据", description = "导入系统内置小型 Olist 评论样例，便于新用户快速体验")
    public Result<TaskVO> importSample() {
        return Result.success(dataImportService.importSample());
    }

    @PostMapping("/crawler")
    @Operation(summary = "爬虫导入", description = "创建低频公开样例爬虫导入任务，仅用于学习研究场景")
    public Result<TaskVO> importByCrawler(@RequestBody CrawlerImportDTO crawlerImportDTO) {
        return Result.success(dataImportService.importByCrawler(crawlerImportDTO));
    }

    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "查询导入任务", description = "查询 CSV 或爬虫导入任务状态和进度")
    public Result<TaskVO> getImportTask(@Parameter(description = "任务 ID") @PathVariable Long taskId,
                                        @Parameter(description = "导入类型，可选 csv/crawler")
                                        @RequestParam(required = false) String importType) {
        return Result.success(dataImportService.getImportTask(taskId, importType));
    }
}
