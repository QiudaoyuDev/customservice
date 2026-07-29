package com.hardwareai.support.knowledge;

import org.apache.pdfbox.Loader;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Validates the declared type and bounded structure before a source reaches object storage.
 */
@Component
class UploadedKnowledgeFileValidator {
    private static final long MAX_SIZE = 20L * 1024 * 1024;
    private static final long MAX_DOCX_UNCOMPRESSED_SIZE = 100L * 1024 * 1024;
    private static final long MAX_IMAGE_PIXELS = 40_000_000L;
    private static final int MAX_PDF_PAGES = 500;
    private static final Map<String, String> TYPE_BY_EXTENSION = Map.of(
        "pdf", "application/pdf",
        "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "png", "image/png",
        "jpg", "image/jpeg",
        "jpeg", "image/jpeg"
    );

    void validate(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("A file up to 20 MiB is required");
        }
        String extension = extension(file.getOriginalFilename());
        String expectedType = TYPE_BY_EXTENSION.get(extension);
        if (expectedType == null || !expectedType.equals(file.getContentType())) {
            throw new IllegalArgumentException("File extension and MIME type must match a supported knowledge source");
        }
        try {
            byte[] content = file.getBytes();
            verifyMagicBytes(expectedType, content);
            switch (expectedType) {
            case "application/pdf" -> verifyPdf(content);
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> verifyDocx(content);
            default -> verifyImage(content);
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read uploaded knowledge source", exception);
        }
    }

    private static void verifyPdf(byte[] content) throws IOException {
        try (var pdf = Loader.loadPDF(content)) {
            if (pdf.getNumberOfPages() > MAX_PDF_PAGES) throw new IllegalArgumentException("PDF has too many pages");
        }
    }

    private static void verifyDocx(byte[] content) throws IOException {
        long total = 0;
        boolean documentXml = false;
        try (var zip = new ZipInputStream(new ByteArrayInputStream(content))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zip.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) documentXml = true;
                for (int read; (read = zip.read(buffer)) >= 0; ) {
                    total += read;
                    if (total > MAX_DOCX_UNCOMPRESSED_SIZE) throw new IllegalArgumentException("DOCX expands beyond allowed size");
                }
            }
        }
        if (!documentXml) throw new IllegalArgumentException("Invalid DOCX source");
    }

    private static void verifyImage(byte[] content) throws IOException {
        var image = ImageIO.read(new ByteArrayInputStream(content));
        if (image == null || (long)image.getWidth() * image.getHeight() > MAX_IMAGE_PIXELS) {
            throw new IllegalArgumentException("Invalid or oversized image source");
        }
    }

    private static void verifyMagicBytes(String type, byte[] content) {
        boolean valid = switch (type) {
            case "application/pdf" -> startsWith(content, "%PDF-".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ->
                startsWith(content, new byte[] { 'P', 'K', 3, 4 });
            case "image/png" -> startsWith(content, new byte[] { (byte)0x89, 'P', 'N', 'G', 13, 10, 26, 10 });
            case "image/jpeg" ->
                content.length >= 3 && content[0] == (byte)0xff && content[1] == (byte)0xd8 && content[2] == (byte)0xff;
            default -> false;
        };
        if (!valid) throw new IllegalArgumentException("File content does not match its declared type");
    }

    private static boolean startsWith(byte[] actual, byte[] prefix) {
        if (actual.length < prefix.length) return false;
        for (int index = 0; index < prefix.length; index++) if (actual[index] != prefix[index]) return false;
        return true;
    }

    private static String extension(String filename) {
        if (filename == null) return "";
        int separator = filename.lastIndexOf('.');
        return separator < 0 ? "" : filename.substring(separator + 1).toLowerCase(Locale.ROOT);
    }
}
