package com.hardwareai.support.knowledge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
    private final KnowledgeChunkRepository chunks;
    private final KnowledgeChunker chunker;

    KnowledgeProcessingWorker(
            ProcessingJobRepository jobs,
            KnowledgeRevisionRepository revisions,
            KnowledgeDocumentRepository documents,
            ObjectStorage storage,
            DocumentTextExtractor extractor,
            VectorIndex vectorIndex, KnowledgeChunkRepository chunks, KnowledgeChunker chunker
    ) {
        this.jobs = jobs;
        this.revisions = revisions;
        this.documents = documents;
        this.storage = storage;
        this.extractor = extractor;
        this.vectorIndex = vectorIndex;
        this.chunks = chunks;
        this.chunker = chunker;
    }

    @Scheduled(fixedDelayString = "${app.worker-delay-ms:2000}")
    public void processNext() {
        jobs.claimNext().ifPresent((job) -> {
            try {
                var revision = revisions
                        .findById(job.revisionId())
                        .orElseThrow(() -> new IllegalStateException("Revision not found"));
                if (job.jobType() == ProcessingJob.Type.PARSE) {
                    revision.beginParsing();
                    revisions.save(revision);
                    var document = documents
                            .findById(revision.documentId())
                            .orElseThrow(() -> new IllegalStateException("Document not found"));
                    // One extractor entry point keeps PDF/DOCX/OCR parsing inside the same durable retry flow.
                    revision.setExtractedText(
                            extractor.extract(document.contentType(), storage.get(document.objectKey())).strip()
                    );
                    revisions.save(revision);
                    chunks.deleteAllByRevisionId(revision.id());
                    chunks.saveAll(chunker.split(revision, document));
                } else {
                    var document = documents.findById(revision.documentId()).orElseThrow(() -> new IllegalStateException("Document not found"));
                    vectorIndex.upsert(revision, document, chunks.findAllByRevisionIdOrderByChunkNo(revision.id()));
                }
                job.complete();
                jobs.save(job);
                log.info("Completed knowledge job type={} jobId={} revisionId={}", job.jobType(), job.id(), revision.id());
            } catch (Exception e) {
                job.fail(e);
                jobs.save(job);
                log.warn("Knowledge job failed jobId={} type={} errorType={}", job.id(), job.jobType(),
                        e.getClass().getSimpleName());
            }
        });
    }
}
