package com.hardwareai.support.knowledge;

import com.hardwareai.support.config.AppProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Private-object-storage adapter. Logs object identifiers, never document content.
 */
@Service
public class ObjectStorage {

    private static final Logger log = LoggerFactory.getLogger(ObjectStorage.class);
    private final MinioClient client;
    private final AppProperties properties;

    ObjectStorage(MinioClient c, AppProperties p) {
        client = c;
        properties = p;
    }

    public void put(String key, MultipartFile file) {
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
    public java.io.InputStream get(String key) {
        try {
            return client.getObject(
                GetObjectArgs.builder().bucket(properties.storage().bucket()).object(key).build()
            );
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read knowledge source object", e);
        }
    }

    /**
     * Deletes an object that has reached its configured retention boundary.
     * The caller must have already established that no retained record references it.
     */
    public void delete(String key) {
        try {
            client.removeObject(
                RemoveObjectArgs.builder().bucket(properties.storage().bucket()).object(key).build()
            );
            log.info("Deleted retained object {}", key);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to delete retained object", e);
        }
    }
}
