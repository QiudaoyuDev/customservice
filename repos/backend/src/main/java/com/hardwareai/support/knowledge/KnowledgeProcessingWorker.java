package com.hardwareai.support.knowledge;

import org.slf4j.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Polls one durable job at a time; failures are retried twice and remain inspectable.
 */
@Component
class KnowledgeProcessingWorker {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeProcessingWorker.class);
    private final ProcessingJobRepository jobs;
    private final KnowledgeRevisionRepository revisions;
    private final KnowledgeDocumentRepository documents;
    private final ObjectStorage storage;
    private final DocumentTextExtractor extractor;
    private final VectorIndex vectorIndex;

    KnowledgeProcessingWorker(
        ProcessingJobRepository jobs,
        KnowledgeRevisionRepository revisions,
        KnowledgeDocumentRepository documents,
        ObjectStorage storage,
        DocumentTextExtractor extractor,
        VectorIndex vectorIndex
    ) {
        this.jobs = jobs;
        this.revisions = revisions;
        this.documents = documents;
        this.storage = storage;
        this.extractor = extractor;
        this.vectorIndex = vectorIndex;
    }

    @Scheduled(fixedDelayString = "${app.worker-delay-ms:2000}")
    @Transactional
    public void processNext() {
        jobs.findFirstByStatusOrderByCreatedAtAsc(ProcessingJob.Status.PENDING).ifPresent((job) -> {
            job.start();
            jobs.save(job);
            try {
                var revision = revisions
                    .findById(job.revisionId())
                    .orElseThrow(() -> new IllegalStateException("Revision not found"));
                if (job.jobType() == ProcessingJob.Type.PARSE) {
                    var document = documents
                        .findById(revision.documentId())
                        .orElseThrow(() -> new IllegalStateException("Document not found"));
                    revision.setExtractedText(
                        extractor.extract(document.contentType(), storage.get(document.objectKey())).strip()
                    );
                    revisions.save(revision);
                } else vectorIndex.upsert(revision);
                job.complete();
                jobs.save(job);
                log.info("Completed {} job {} for revision {}", job.jobType(), job.id(), revision.id());
            } catch (Exception e) {
                job.fail(e);
                jobs.save(job);
                log.warn(
                    "Knowledge job {} failed on attempt {}: {}",
                    job.id(),
                    job.jobType(),
                    e.getMessage()
                );
            }
        });
    }
}
