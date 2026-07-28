package com.hardwareai.support.knowledge;

import com.hardwareai.support.common.CurrentUser;
import com.hardwareai.support.product.ProductRepository;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Knowledge source upload, review and publish endpoints.
 */
@RestController
@RequestMapping("/api")
public class KnowledgeController {

    private final KnowledgeDocumentRepository documents;
    private final KnowledgeRevisionRepository revisions;
    private final ProcessingJobRepository jobs;
    private final ProductRepository products;
    private final ObjectStorage storage;
    private final CurrentUser current;
    private final KnowledgeChunkRepository chunks;
    private final VectorIndex vectorIndex;

    KnowledgeController(
        KnowledgeDocumentRepository d,
        KnowledgeRevisionRepository r,
        ProcessingJobRepository j,
        ProductRepository p,
        ObjectStorage s,
        CurrentUser c, KnowledgeChunkRepository chunks, VectorIndex vectorIndex
    ) {
        documents = d;
        revisions = r;
        jobs = j;
        products = p;
        storage = s;
        current = c;
        this.chunks = chunks;
        this.vectorIndex = vectorIndex;
    }

    @GetMapping("/documents")
    public List<DocumentView> list() {
        return documents
            .findAllByTenantIdOrderByCreatedAtDesc(current.tenantId())
            .stream()
            .map(d -> revisions.findAllByDocumentIdOrderByRevisionNoDesc(d.id()).stream().findFirst().map(r -> DocumentView.of(d, r)).orElse(new DocumentView(d.id(), null, d.title(), d.locale(), "UPLOADED")))
            .toList();
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public DocumentView upload(
        @RequestPart @NotBlank String title,
        @RequestPart @NotBlank String locale,
        @RequestPart UUID productModelId,
        @RequestPart @NotBlank String region,
        @RequestPart MultipartFile file
    ) {
        if (file.isEmpty() || file.getSize() > 20 * 1024 * 1024) throw new IllegalArgumentException(
            "A file up to 20 MiB is required"
        );
        if (
            !Set.of(
                "application/pdf",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "image/png",
                "image/jpeg"
            ).contains(file.getContentType())
        ) throw new IllegalArgumentException("Only PDF, DOCX, PNG and JPEG are supported");
        products
            .findByIdAndTenantId(productModelId, current.tenantId())
            .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        String key = current.tenantId() + "/documents/" + UUID.randomUUID();
        storage.put(key, file);
        var doc = documents.save(
            new KnowledgeDocument(
                current.tenantId(),
                title,
                locale,
                key,
                file.getContentType(),
                current.userId()
            )
        );
        var revision = revisions.save(new KnowledgeRevision(doc.id(), productModelId, region));
        jobs.save(new ProcessingJob(revision.id(), ProcessingJob.Type.PARSE));
        return DocumentView.of(doc, revision);
    }

    @PostMapping("/knowledge-revisions/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMIN','KNOWLEDGE_REVIEWER')")
    public void submit(@PathVariable UUID id) {
        var r = revisions
            .findOwned(id, current.tenantId())
            .orElseThrow(() -> new IllegalArgumentException("Revision not found"));
        r.submit();
        revisions.save(r);
    }

    @PostMapping("/knowledge-revisions/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN','KNOWLEDGE_REVIEWER')")
    public void publish(@PathVariable UUID id) {
        var r = revisions
            .findOwned(id, current.tenantId())
            .orElseThrow(() -> new IllegalArgumentException("Revision not found"));
        r.publish(current.userId());
        revisions.save(r);
        jobs.save(new ProcessingJob(r.id(), ProcessingJob.Type.INDEX));
    }

    @PostMapping("/knowledge-revisions/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','KNOWLEDGE_REVIEWER')")
    public void approve(@PathVariable UUID id) {
        var revision = revisions.findOwned(id, current.tenantId()).orElseThrow(() -> new IllegalArgumentException("Revision not found"));
        revision.approve(current.userId());
        revisions.save(revision);
    }

    @PostMapping("/knowledge-revisions/{id}/deprecate")
    @PreAuthorize("hasAnyRole('ADMIN','KNOWLEDGE_REVIEWER')")
    public void deprecate(@PathVariable UUID id) {
        var revision = revisions.findOwned(id, current.tenantId()).orElseThrow(() -> new IllegalArgumentException("Revision not found"));
        revision.deprecate();
        revisions.save(revision);
        vectorIndex.removeRevision(revision.id());
    }

    @PostMapping("/knowledge-revisions/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN','KNOWLEDGE_REVIEWER')")
    public void archive(@PathVariable UUID id) {
        var revision = revisions.findOwned(id, current.tenantId()).orElseThrow(() -> new IllegalArgumentException("Revision not found"));
        revision.archive(); revisions.save(revision); vectorIndex.removeRevision(revision.id());
    }

    @PostMapping("/knowledge-revisions/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN','KNOWLEDGE_REVIEWER')")
    public void restore(@PathVariable UUID id) {
        var revision = revisions.findOwned(id, current.tenantId()).orElseThrow(() -> new IllegalArgumentException("Revision not found"));
        revisions.findAllByDocumentIdOrderByRevisionNoDesc(revision.documentId()).stream()
            .filter(candidate -> candidate.status() == KnowledgeRevision.Status.PUBLISHED)
            .forEach(candidate -> { candidate.archive(); revisions.save(candidate); vectorIndex.removeRevision(candidate.id()); });
        revision.restore(current.userId());
        revisions.save(revision);
        jobs.save(new ProcessingJob(revision.id(), ProcessingJob.Type.INDEX));
    }

    @GetMapping("/documents/{id}/preview")
    public Preview preview(@PathVariable UUID id) {
        var document = documents.findByIdAndTenantId(id, current.tenantId()).orElseThrow(() -> new IllegalArgumentException("Document not found"));
        var revision = revisions.findAllByDocumentIdOrderByRevisionNoDesc(id).stream().findFirst().orElseThrow(() -> new IllegalStateException("Document has no revision"));
        return new Preview(document.title(), revision.status().name(), revision.extractedText(), chunks.findAllByRevisionIdOrderByChunkNo(revision.id()).stream().map(c -> new ChunkView(c.chunkNo(), c.sourceLabel(), c.content())).toList());
    }

    record DocumentView(UUID id, UUID revisionId, String title, String locale, String status) {
        static DocumentView of (KnowledgeDocument d, KnowledgeRevision revision){
            return new DocumentView(d.id(), revision.id(), d.title(), d.locale(), revision.status().name());
        }
    }
    record Preview(String title, String status, String text, List<ChunkView> chunks) {}
    record ChunkView(int chunkNo, String source, String text) {}
}
