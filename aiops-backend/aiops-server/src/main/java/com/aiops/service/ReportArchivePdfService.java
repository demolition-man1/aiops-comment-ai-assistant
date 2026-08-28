package com.aiops.service;

import com.aiops.pdf.ReportPdfDocument;

public interface ReportArchivePdfService {
    ReportPdfDocument exportPdf(Long archiveId, String language);
}
