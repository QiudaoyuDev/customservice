package com.hardwareai.support.knowledge;

import com.hardwareai.support.product.ProductApplicationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Creates immutable revision records while serialising revision number allocation per document.
 */
@Service
class KnowledgeRevisionApplicationService {
    private final KnowledgeDocumentRepository documents;
    private final KnowledgeRevisionRepository revisions;
    private final KnowledgeRevisionApplicabilityRepository applicability;
    private final ProcessingJobRepository jobs;
    private final ProductApplicationService products;

    KnowledgeRevisionApplicationService(KnowledgeDocumentRepository documents, KnowledgeRevisionRepository revisions,
        KnowledgeRevisionApplicabilityRepository applicability, ProcessingJobRepository jobs,
        ProductApplicationService products) {
        this.documents = documents;
        this.revisions = revisions;
        this.applicability = applicability;
        this.jobs = jobs;
        this.products = products;
    }

    @Transactional
    KnowledgeRevision createFromExistingSource(UUID tenantId, UUID documentId, UUID productModelId, UUID productVariantId,
        String region, String hardwareRevision, String firmwareMin, String firmwareMax,
        Instant validFrom, Instant validTo) {
        documents.findOwnedForRevision(documentId, tenantId).orElseThrow(() -> new IllegalArgumentException("Document not found"));
        if (productVariantId != null) products.requireActiveVariant(tenantId, productModelId, productVariantId);
        int nextNo = revisions.findAllByDocumentIdOrderByRevisionNoDesc(documentId).stream()
            .mapToInt(KnowledgeRevision::revisionNo).max().orElse(0) + 1;
        var revision = revisions.save(new KnowledgeRevision(documentId, productModelId, region, nextNo));
        applicability.save(new KnowledgeRevisionApplicability(revision.id(), productModelId, productVariantId, region,
            hardwareRevision, firmwareMin, firmwareMax, validFrom, validTo));
        jobs.save(new ProcessingJob(revision.id(), ProcessingJob.Type.PARSE));
        return revision;
    }
}
