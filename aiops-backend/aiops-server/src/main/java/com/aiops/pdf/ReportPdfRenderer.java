package com.aiops.pdf;

import com.aiops.exception.BusinessException;
import com.aiops.vo.ReportArchiveVO;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class ReportPdfRenderer {

    private static final Color BRAND_COLOR = new Color(37, 99, 235);
    private static final Color HEADING_COLOR = new Color(15, 23, 42);
    private static final Color MUTED_COLOR = new Color(71, 85, 105);
    private static final Color LIGHT_BACKGROUND = new Color(241, 245, 249);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String fontPath;

    public ReportPdfRenderer(@Value("${aiops.pdf.font-path:}") String fontPath) {
        this.fontPath = fontPath == null ? "" : fontPath.trim();
    }

    public ReportPdfDocument render(ReportArchiveVO archive, String language) {
        if (archive == null || archive.getArchiveId() == null) {
            throw new BusinessException(400, "报告归档不能为空");
        }

        PdfLabels labels = labelsForLanguage(language);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            BaseFont baseFont = createBaseFont(language);
            Font titleFont = new Font(baseFont, 20, Font.BOLD, HEADING_COLOR);
            Font subtitleFont = new Font(baseFont, 10, Font.NORMAL, MUTED_COLOR);
            Font headingFont = new Font(baseFont, 13, Font.BOLD, BRAND_COLOR);
            Font bodyFont = new Font(baseFont, 10, Font.NORMAL, HEADING_COLOR);
            Font labelFont = new Font(baseFont, 9, Font.BOLD, MUTED_COLOR);
            Font valueFont = new Font(baseFont, 9, Font.NORMAL, HEADING_COLOR);

            Document document = new Document(org.openpdf.text.PageSize.A4, 48, 48, 54, 52);
            PdfWriter writer = PdfWriter.getInstance(document, output);
            writer.setPageEvent(new PageFooter(labels.page(), baseFont));
            document.addTitle(valueOrDefault(archive.getReportTitle(), labels.title()));
            document.addAuthor("AIOps Comment AI Assistant");
            document.addSubject(labels.title());
            document.open();

            Paragraph title = new Paragraph(labels.title(), titleFont);
            title.setSpacingAfter(4);
            document.add(title);

            Paragraph reportTitle = new Paragraph(valueOrDefault(archive.getReportTitle(), labels.untitled()), subtitleFont);
            reportTitle.setSpacingAfter(18);
            document.add(reportTitle);

            document.add(createMetadataTable(archive, labels, labelFont, valueFont));

            List<PdfSection> sections = List.of(
                    new PdfSection(labels.painPoints(), archive.getConsumerPainPoints()),
                    new PdfSection(labels.advantages(), archive.getProductAdvantages()),
                    new PdfSection(labels.disadvantages(), archive.getProductDisadvantages()),
                    new PdfSection(labels.operationSuggestions(), archive.getOperationSuggestions()),
                    new PdfSection(labels.copywritingSuggestions(), archive.getCopywritingSuggestions()),
                    new PdfSection(labels.serviceSuggestions(), archive.getServiceSuggestions()),
                    new PdfSection(labels.riskTips(), archive.getRiskTips()),
                    new PdfSection(labels.fullReport(), archive.getFullReport())
            );
            for (PdfSection section : sections) {
                addSection(document, section, headingFont, bodyFont);
            }

            document.close();
            return new ReportPdfDocument(buildFilename(archive), output.toByteArray());
        } catch (DocumentException | IOException exception) {
            throw new BusinessException(500, "生成 PDF 运营报告失败：" + exception.getMessage());
        }
    }

    private BaseFont createBaseFont(String language) throws DocumentException, IOException {
        List<String> candidates = new ArrayList<>();
        if (!fontPath.isBlank()) {
            candidates.add(fontPath);
        }
        candidates.add("C:/Windows/Fonts/msyh.ttc,0");
        candidates.add("C:/Windows/Fonts/simhei.ttf");
        candidates.add("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc,0");
        candidates.add("/usr/share/fonts/opentype/noto/NotoSansCJKsc-Regular.otf");
        if (isLatinLanguage(language)) {
            candidates.add("C:/Windows/Fonts/arial.ttf");
            candidates.add("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf");
        }

        for (String candidate : candidates) {
            if (!fontFileExists(candidate)) {
                continue;
            }
            try {
                return BaseFont.createFont(candidate, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } catch (DocumentException | IOException ignored) {
                // Try the next installed CJK font before using the built-in fallback.
            }
        }
        return BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
    }

    private boolean isLatinLanguage(String language) {
        String normalized = language == null ? "" : language.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("en") || normalized.startsWith("pt");
    }

    private boolean fontFileExists(String candidate) {
        String filePath = candidate.replaceFirst(",\\d+$", "");
        try {
            return Files.isRegularFile(Path.of(filePath));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private PdfPTable createMetadataTable(ReportArchiveVO archive, PdfLabels labels,
                                           Font labelFont, Font valueFont) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{1.1f, 2.4f, 1.1f, 2.4f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(18);
        addMetadataRow(table, labels.archiveId(), String.valueOf(archive.getArchiveId()),
                labels.status(), valueOrDefault(archive.getArchiveStatus(), "-"), labelFont, valueFont);
        addMetadataRow(table, labels.target(), buildTarget(archive),
                labels.model(), valueOrDefault(archive.getModelName(), "-"), labelFont, valueFont);
        addMetadataRow(table, labels.reportTime(), formatDateTime(archive.getReportCreateTime()),
                labels.archiveTime(), formatDateTime(archive.getArchiveTime()), labelFont, valueFont);
        return table;
    }

    private void addMetadataRow(PdfPTable table, String leftLabel, String leftValue,
                                String rightLabel, String rightValue, Font labelFont, Font valueFont) {
        table.addCell(metadataCell(leftLabel, labelFont, LIGHT_BACKGROUND));
        table.addCell(metadataCell(leftValue, valueFont, Color.WHITE));
        table.addCell(metadataCell(rightLabel, labelFont, LIGHT_BACKGROUND));
        table.addCell(metadataCell(rightValue, valueFont, Color.WHITE));
    }

    private PdfPCell metadataCell(String text, Font font, Color background) {
        PdfPCell cell = new PdfPCell(new Phrase(valueOrDefault(text, "-"), font));
        cell.setBackgroundColor(background);
        cell.setBorderColor(new Color(203, 213, 225));
        cell.setPadding(7);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private void addSection(Document document, PdfSection section, Font headingFont, Font bodyFont)
            throws DocumentException {
        if (section.content() == null || section.content().isBlank()) {
            return;
        }
        Paragraph heading = new Paragraph(section.title(), headingFont);
        heading.setSpacingBefore(8);
        heading.setSpacingAfter(6);
        document.add(heading);

        Paragraph body = new Paragraph(section.content().trim(), bodyFont);
        body.setLeading(16);
        body.setSpacingAfter(8);
        document.add(body);
    }

    private String buildTarget(ReportArchiveVO archive) {
        String targetType = valueOrDefault(archive.getTargetType(), "-");
        String targetId = valueOrDefault(archive.getTargetId(), "-");
        return targetType + " / " + targetId;
    }

    private String buildFilename(ReportArchiveVO archive) {
        String target = valueOrDefault(archive.getTargetId(), "archive");
        String safeTarget = target.replaceAll("[^A-Za-z0-9._-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");
        if (safeTarget.isBlank()) {
            safeTarget = "archive";
        }
        if (safeTarget.length() > 60) {
            safeTarget = safeTarget.substring(0, 60);
        }
        return "operations-report-" + safeTarget + "-" + archive.getArchiveId() + ".pdf";
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : DATE_TIME_FORMATTER.format(value);
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private record PdfSection(String title, String content) {
    }

    static PdfLabels labelsForLanguage(String language) {
        return PdfLabels.forLanguage(language);
    }

    static record PdfLabels(String title, String untitled, String archiveId, String status,
                            String target, String model, String reportTime, String archiveTime,
                            String painPoints, String advantages, String disadvantages,
                            String operationSuggestions, String copywritingSuggestions,
                            String serviceSuggestions, String riskTips, String fullReport, String page) {

        private static PdfLabels forLanguage(String language) {
            String normalized = language == null ? "zh-cn" : language.trim().toLowerCase(Locale.ROOT);
            if (normalized.startsWith("en")) {
                return new PdfLabels("AI Operations Report", "Untitled Report", "Archive ID", "Status",
                        "Target", "Model", "Report Time", "Archive Time", "Consumer Pain Points",
                        "Product Advantages", "Product Disadvantages", "Operational Recommendations",
                        "Copywriting Recommendations", "Customer Service Recommendations", "Risk Alerts",
                        "Full Report", "Page");
            }
            if (normalized.startsWith("pt")) {
                return new PdfLabels("Relatório de Operações com IA", "Relatório sem título", "ID do Arquivo",
                        "Status", "Alvo", "Modelo", "Data do Relatório", "Data do Arquivo",
                        "Pontos de Dor do Consumidor", "Vantagens do Produto", "Desvantagens do Produto",
                        "Recomendações Operacionais", "Recomendações de Texto", "Recomendações de Atendimento",
                        "Alertas de Risco", "Relatório Completo", "Página");
            }
            return new PdfLabels("AI 运营报告", "未命名报告", "归档 ID", "状态", "分析对象", "模型",
                    "报告时间", "归档时间", "消费者痛点", "商品优势", "商品短板", "运营建议", "文案建议",
                    "客服建议", "风险提示", "完整报告", "第");
        }
    }

    private static class PageFooter extends PdfPageEventHelper {
        private final String pageLabel;
        private final Font footerFont;

        private PageFooter(String pageLabel, BaseFont baseFont) {
            this.pageLabel = pageLabel;
            this.footerFont = new Font(baseFont, 8, Font.NORMAL, MUTED_COLOR);
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            Rectangle pageSize = document.getPageSize();
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER,
                    new Phrase(pageLabel + " " + writer.getPageNumber(), footerFont),
                    (pageSize.getLeft() + pageSize.getRight()) / 2, pageSize.getBottom() + 22, 0);
        }
    }
}
