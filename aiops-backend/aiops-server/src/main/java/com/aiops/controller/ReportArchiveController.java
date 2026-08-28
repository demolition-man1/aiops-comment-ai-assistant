package com.aiops.controller;

import com.aiops.dto.ReportArchiveCreateDTO;
import com.aiops.dto.ReportArchiveQueryDTO;
import com.aiops.dto.ReportArchiveStatusDTO;
import com.aiops.pdf.ReportPdfDocument;
import com.aiops.result.PageResult;
import com.aiops.result.Result;
import com.aiops.service.ReportArchivePdfService;
import com.aiops.service.ReportArchiveService;
import com.aiops.vo.ReportArchiveVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/report-archives")
@RequiredArgsConstructor
@Tag(name = "报告归档", description = "归档 AI 运营报告并按目标、状态和时间检索历史快照")
public class ReportArchiveController {

    private final ReportArchiveService reportArchiveService;
    private final ReportArchivePdfService reportArchivePdfService;

    @GetMapping
    @Operation(summary = "分页查询报告归档", description = "支持目标类型、目标 ID、关键词、状态和归档时间范围组合筛选")
    public Result<PageResult<ReportArchiveVO>> pageArchives(@ModelAttribute ReportArchiveQueryDTO queryDTO) {
        return Result.success(reportArchiveService.pageArchives(queryDTO));
    }

    @PostMapping("/{reportId}")
    @Operation(summary = "归档运营报告", description = "将已生成的 AI 运营报告保存为稳定快照；重复归档同一报告时幂等返回")
    public Result<ReportArchiveVO> archiveReport(
            @Parameter(description = "源报告 ID") @PathVariable Long reportId,
            @RequestBody(required = false) ReportArchiveCreateDTO createDTO) {
        return Result.success(reportArchiveService.archiveReport(reportId, createDTO));
    }

    @GetMapping("/{archiveId}")
    @Operation(summary = "查询归档详情", description = "根据归档 ID 回看完整报告快照")
    public Result<ReportArchiveVO> getArchive(
            @Parameter(description = "归档 ID") @PathVariable Long archiveId) {
        return Result.success(reportArchiveService.getArchive(archiveId));
    }

    @GetMapping("/{archiveId}/export/pdf")
    @Operation(summary = "导出归档 PDF", description = "将稳定的报告归档快照导出为中、英或葡语版 PDF")
    public ResponseEntity<byte[]> exportPdf(
            @Parameter(description = "归档 ID") @PathVariable Long archiveId,
            @Parameter(description = "标签语言：zh-CN/en-US/pt-BR")
            @RequestParam(defaultValue = "zh-CN") String language) {
        ReportPdfDocument document = reportArchivePdfService.exportPdf(archiveId, language);
        String disposition = ContentDisposition.attachment()
                .filename(document.filename(), StandardCharsets.UTF_8)
                .build()
                .toString();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentLength(document.content().length)
                .body(document.content());
    }

    @PutMapping("/{archiveId}/status")
    @Operation(summary = "修改归档状态", description = "支持 archived 和 restored 两种状态")
    public Result<ReportArchiveVO> updateStatus(
            @Parameter(description = "归档 ID") @PathVariable Long archiveId,
            @RequestBody ReportArchiveStatusDTO statusDTO) {
        return Result.success(reportArchiveService.updateStatus(archiveId, statusDTO));
    }
}
