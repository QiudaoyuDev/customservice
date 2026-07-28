package com.hardwareai.support.knowledge;

import java.io.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

/** Converts supported office sources into text; images are delegated to the OCR adapter. */
@Component
class DocumentTextExtractor {
  String extract(String contentType, InputStream source) {
    try (source) {
      if ("application/pdf".equals(contentType)) { try (var pdf=Loader.loadPDF(source.readAllBytes())) { return new PDFTextStripper().getText(pdf); } }
      if ("application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType)) { try (var doc=new XWPFDocument(source); var extractor=new XWPFWordExtractor(doc)) { return extractor.getText(); } }
      throw new IllegalArgumentException("Image OCR must be handled by the OCR adapter");
    } catch (IOException e) { throw new IllegalStateException("Unable to extract document text",e); }
  }
}
