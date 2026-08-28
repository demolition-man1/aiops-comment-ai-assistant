package com.aiops.service.impl;

import com.aiops.pdf.ReportPdfDocument;
import com.aiops.pdf.ReportPdfRenderer;
import com.aiops.service.ReportArchiveService;
import com.aiops.vo.ReportArchiveVO;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportArchivePdfServiceImplTest {

    @Test
    void exportsTheStableArchiveSnapshotAsPdf() {
        ReportArchiveService archiveService = mock(ReportArchiveService.class);
        ReportArchiveVO archive = new ReportArchiveVO();
        archive.setArchiveId(101L);
        archive.setTargetType("product");
        archive.setTargetId("product-a");
        archive.setReportTitle("Stable archived report");
        archive.setFullReport("Snapshot content");
        archive.setArchiveStatus("archived");
        when(archiveService.getArchive(101L)).thenReturn(archive);

        ReportArchivePdfServiceImpl service = new ReportArchivePdfServiceImpl(
                archiveService, new ReportPdfRenderer(""));

        ReportPdfDocument result = service.exportPdf(101L, "en-US");

        assertThat(result.filename()).isEqualTo("operations-report-product-a-101.pdf");
        assertThat(new String(result.content(), 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        assertThat(result.content().length).isGreaterThan(500);
    }
}
