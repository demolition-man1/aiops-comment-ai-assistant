package com.aiops.service.impl;

import com.aiops.pdf.ReportPdfDocument;
import com.aiops.pdf.ReportPdfRenderer;
import com.aiops.service.ReportArchivePdfService;
import com.aiops.service.ReportArchiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportArchivePdfServiceImpl implements ReportArchivePdfService {

    private final ReportArchiveService reportArchiveService;
    private final ReportPdfRenderer reportPdfRenderer;

    @Override
    public ReportPdfDocument exportPdf(Long archiveId, String language) {
        return reportPdfRenderer.render(reportArchiveService.getArchive(archiveId), language);
    }
}
