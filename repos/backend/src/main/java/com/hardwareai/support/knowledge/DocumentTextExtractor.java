package com.hardwareai.support.knowledge;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * Converts office sources locally and routes supported image sources to the local OCR adapter.
 */
@Component
class DocumentTextExtractor {

    private final OcrClient ocr;

    DocumentTextExtractor(OcrClient ocr) {
        this.ocr = ocr;
    }

    ExtractedText extract(String contentType, InputStream source) {
        if ("image/png".equals(contentType) || "image/jpeg".equals(contentType)) {
            var result = ocr.extract(contentType, source);
            return new ExtractedText(result.text(), result);
        }
        try (source) {
            if ("application/pdf".equals(contentType)) {
                try (var pdf = Loader.loadPDF(source.readAllBytes())) {
                    var text = new StringBuilder();
                    for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
                        var stripper = new PDFTextStripper();
                        stripper.setStartPage(page);
                        stripper.setEndPage(page);
                        text.append("\fPAGE:").append(page).append('\n').append(stripper.getText(pdf)).append('\n');
                    }
                    return new ExtractedText(text.toString(), null);
                }
            }
            if (
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(
                            contentType
                    )
            ) {
                try (var doc = new XWPFDocument(source)) {
                    var text = new StringBuilder();
                    for (var paragraph : doc.getParagraphs()) {
                        String value = paragraph.getText().strip();
                        if (value.isEmpty()) continue;
                        String style = paragraph.getStyle() == null ? "" : paragraph.getStyle().toLowerCase(java.util.Locale.ROOT);
                        if (style.startsWith("heading")) text.append("\n# ").append(value).append("\n");
                        else text.append(value).append("\n\n");
                    }
                    for (var table : doc.getTables()) {
                        for (var row : table.getRows()) {
                            var cells = row.getTableCells().stream().map(cell -> cell.getText().strip().replace("\n", " ")).toList();
                            if (!cells.isEmpty()) text.append("| ").append(String.join(" | ", cells)).append(" |\n");
                        }
                        text.append('\n');
                    }
                    return new ExtractedText(text.toString(), null);
                }
            }
            throw new IllegalArgumentException("Unsupported knowledge document content type: " + contentType);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to extract document text", e);
        }
    }

    record ExtractedText(String text, OcrClient.OcrText ocr) { }
}
