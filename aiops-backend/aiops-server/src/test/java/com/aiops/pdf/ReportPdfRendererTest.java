package com.aiops.pdf;

import com.aiops.vo.ReportArchiveVO;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportPdfRendererTest {

    private final ReportPdfRenderer renderer = new ReportPdfRenderer(latinTestFont());

    @Test
    void rendersReadablePdfWithEnglishReportSections() throws Exception {
        ReportPdfDocument result = renderer.render(reportArchive(), "en-US");

        assertThat(new String(result.content(), 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        assertThat(result.filename()).isEqualTo("operations-report-product-a-101.pdf");

        PdfReader reader = new PdfReader(result.content());
        try {
            assertThat(reader.getNumberOfPages()).isGreaterThanOrEqualTo(1);
            String firstPage = new PdfTextExtractor(reader).getTextFromPage(1);
            assertThat(firstPage).contains("AI Operations Report", "Consumer Pain Points", "Slow delivery");
        } finally {
            reader.close();
        }
    }

    @Test
    void rendersReadablePdfWithPortugueseSectionLabels() throws Exception {
        ReportPdfDocument portuguese = renderer.render(reportArchive(), "pt-BR");

        assertThat(extractAllText(portuguese.content())).contains("Relatório de Operações com IA",
                "Pontos de Dor do Consumidor", "Recomendações Operacionais");
    }

    @Test
    void selectsChineseLabelsWithoutRequiringACjkFontInTheTestRuntime() {
        ReportPdfRenderer.PdfLabels labels = ReportPdfRenderer.labelsForLanguage("zh-CN");

        assertThat(labels.title()).isEqualTo("AI 运营报告");
        assertThat(labels.painPoints()).isEqualTo("消费者痛点");
        assertThat(labels.operationSuggestions()).isEqualTo("运营建议");
    }

    private String extractAllText(byte[] content) throws Exception {
        PdfReader reader = new PdfReader(content);
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(extractor.getTextFromPage(page));
            }
            return text.toString();
        } finally {
            reader.close();
        }
    }

    private static String latinTestFont() {
        return List.of(
                        "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                        "C:/Windows/Fonts/arial.ttf")
                .stream()
                .filter(path -> Files.isRegularFile(Path.of(path)))
                .findFirst()
                .orElse("");
    }

    private ReportArchiveVO reportArchive() {
        ReportArchiveVO archive = new ReportArchiveVO();
        archive.setArchiveId(101L);
        archive.setSourceReportId(41L);
        archive.setTargetType("product");
        archive.setTargetId("product-a");
        archive.setReportTitle("August operations report");
        archive.setConsumerPainPoints("Slow delivery");
        archive.setProductAdvantages("Reliable quality");
        archive.setProductDisadvantages("Limited packaging");
        archive.setOperationSuggestions("Improve logistics");
        archive.setCopywritingSuggestions("Highlight product quality");
        archive.setServiceSuggestions("Reply within 24 hours");
        archive.setRiskTips("Monitor late deliveries");
        archive.setFullReport("Prioritize logistics and packaging improvements.");
        archive.setModelName("deepseek-chat");
        archive.setArchiveStatus("archived");
        archive.setReportCreateTime(LocalDateTime.of(2026, 8, 20, 10, 30));
        archive.setArchiveTime(LocalDateTime.of(2026, 8, 21, 9, 0));
        return archive;
    }
}
