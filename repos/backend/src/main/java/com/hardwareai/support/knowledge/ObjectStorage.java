package com.hardwareai.support.knowledge;

import com.hardwareai.support.config.AppProperties;
import io.minio.*;
import org.slf4j.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Private-object-storage adapter. Logs object identifiers, never document content.
 */
@Service
class ObjectStorage {

    private static final Logger log = LoggerFactory.getLogger(ObjectStorage.class);
    private final MinioClient client;
    private final AppProperties properties;

    ObjectStorage(MinioClient c, AppProperties p) {
        client = c;
        properties = p;
    }

    void put(String key, MultipartFile file) {
        try {
            if (
                !client.bucketExists(
                    BucketExistsArgs.builder().bucket(properties.storage().bucket()).build()
                )
            ) client.makeBucket(MakeBucketArgs.builder().bucket(properties.storage().bucket()).build());
            client.putObject(
                PutObjectArgs.builder()
                    .bucket(properties.storage().bucket())
                    .object(key)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );
            log.info("Stored knowledge source object {} ({} bytes)", key, file.getSize());
        } catch (Exception e) {
            log.error("Failed to store knowledge source object {}", key, e);
            throw new IllegalStateException("Unable to store uploaded file", e);
        }
    }

    /**
     * Caller owns and closes the private stream.
     */
    java.io.InputStream get(String key) {
        try {
            return client.getObject(
                GetObjectArgs.builder().bucket(properties.storage().bucket()).object(key).build()
            );
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read knowledge source object", e);
        }
    }
}
