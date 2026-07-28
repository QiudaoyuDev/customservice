package com.hardwareai.support.knowledge;

import com.hardwareai.support.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;

/**
 * Calls the local OCR adapter using its {@code file} multipart contract.
 *
 * <p>The adapter is allowed to extract text only; it must not be used to derive a hardware
 * diagnosis. Logs deliberately contain only media type and byte count, never recognised text
 * or the image itself.</p>
 */
@Component
class OcrClient {

    private static final Logger log = LoggerFactory.getLogger(OcrClient.class);
    private final RestClient client;

    OcrClient(AppProperties properties) {
        this.client = RestClient.builder().baseUrl(properties.ocrUrl()).build();
    }

    String extract(String contentType, InputStream source) {
        byte[] bytes;
        try (source) {
            bytes = source.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read image for OCR", e);
        }
        if (bytes.length == 0) {
            throw new IllegalArgumentException("Cannot OCR an empty image");
        }

        var body = new MultipartBodyBuilder();
        body.part("file", new NamedImageResource(bytes, filenameFor(contentType)))
            .contentType(MediaType.parseMediaType(contentType));
        try {
            var response = client
                .post()
                .uri("/v1/ocr")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body.build())
                .retrieve()
                .body(OcrResponse.class);
            if (response == null) {
                throw new IllegalStateException("OCR adapter returned an empty response");
            }
            var text = response.text() == null ? "" : response.text();
            log.info("OCR completed for {} image ({} bytes, {} characters)", contentType, bytes.length, text.length());
            return text;
        } catch (Exception e) {
            log.warn("OCR adapter request failed for {} image ({} bytes): {}", contentType, bytes.length, e.getMessage());
            throw new IllegalStateException("OCR adapter request failed", e);
        }
    }

    private String filenameFor(String contentType) {
        return MediaType.IMAGE_JPEG_VALUE.equals(contentType) ? "source.jpg" : "source.png";
    }

    private record OcrResponse(String text) {}

    /** Supplies a filename because FastAPI's UploadFile uses it to choose a safe suffix. */
    private static final class NamedImageResource extends ByteArrayResource {
        private final String filename;

        private NamedImageResource(byte[] bytes, String filename) {
            super(bytes);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
