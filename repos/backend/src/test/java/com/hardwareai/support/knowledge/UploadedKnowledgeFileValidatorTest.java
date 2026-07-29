package com.hardwareai.support.knowledge;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UploadedKnowledgeFileValidatorTest {
    private final UploadedKnowledgeFileValidator validator = new UploadedKnowledgeFileValidator();

    @Test
    void rejectsMismatchedExtensionMimeAndContent() {
        var file = new MockMultipartFile("file", "manual.pdf", "application/pdf", "not a pdf".getBytes());
        assertThrows(IllegalArgumentException.class, () -> validator.validate(file));
    }

    @Test
    void acceptsBoundedDocxPackageWithDocumentXml() throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write("<w:document/>".getBytes());
            zip.closeEntry();
        }
        var file = new MockMultipartFile("file", "manual.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", bytes.toByteArray());
        assertDoesNotThrow(() -> validator.validate(file));
    }
}
