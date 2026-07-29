package com.hardwareai.support.knowledge;

import com.hardwareai.support.common.CurrentUser;
import com.hardwareai.support.product.ProductRepository;
import com.hardwareai.support.product.ProductApplicationService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
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
    private final ProductApplicationService productService;
    private final ObjectStorage storage;
    private final CurrentUser current;
    private final KnowledgeChunkRepository chunks;
    private final VectorIndex vectorIndex;
    private final KnowledgeRevisionApplicabilityRepository applicability;
    private final UploadedKnowledgeFileValidator fileValidator;
    private final KnowledgeRevisionApplicationService revisionService;

    KnowledgeController(
            KnowledgeDocumentRepository d,
            KnowledgeRevisionRepository r,
            ProcessingJobRepository j,
            ProductRepository p, ProductApplicationService productService,
            ObjectStorage s,
            CurrentUser c, KnowledgeChunkRepository chunks, VectorIndex vectorIndex, KnowledgeRevisionApplicabilityRepository applicability,
            UploadedKnowledgeFileValidator fileValidator, KnowledgeRevisionApplicationService revisionService
    ) {
        documents = d;
        revisions = r;
        jobs = j;
        products = p;
        this.productService = productService;
        storage = s;
        current = c;
        this.chunks = chunks;
        this.vectorIndex = vectorIndex;
        this.applicability = applicability;
        this.fileValidator = fileValidator;
        this.revisionService = revisionService;
    }

    @GetMapping("/documents")
    public List<DocumentView> list() {
        return documents
                .findAllByTenantIdOrderByCreatedAtDesc(current.tenantId())
                .stream()
                .map(d -> revisions.findAllByDocumentIdOrderByRevisionNoDesc(d.id()).stream().findFirst().map(r -> DocumentView.of(d, r)).orElse(new DocumentView(d.id(), null, d.title(), d.locale(), "UPLOADED", "NOT_INDEXED")))
                .toList();
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public DocumentView upload(
            @RequestPart @NotBlank String title,
            @RequestPart @NotBlank String locale,
            @RequestPart UUID productModelId,
            @RequestPart(required = false) UUID productVariantId,
            @RequestPart @NotBlank String region,
            @RequestPart(required = false) String hardwareRevision,
            @RequestPart(required = false) String firmwareMin,
            @RequestPart(required = false) String firmwareMax,
            @RequestPart(required = false) Boolean allowDuplicate,
            @RequestPart MultipartFile file
    ) {
        fileValidator.validate(file);
        String sourceChecksum = checksum(file);
        if (!Boolean.TRUE.equals(allowDuplicate) && !documents.findAllByTenantIdAndSourceChecksum(current.tenantId(), sourceChecksum).isEmpty()) {
            throw new IllegalStateException("An identical source already exists; set allowDuplicate to create an explicit new revision");
        }
        products
                .findByIdAndTenantId(productModelId, current.tenantId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        if (productVariantId != null) productService.requireActiveVariant(current.tenantId(), productModelId, productVariantId);
        String key = current.tenantId() + "/documents/" + UUID.randomUUID();
        storage.put(key, file);
        var doc = documents.save(
                new KnowledgeDocument(
                        current.tenantId(),
                        title,
                        locale,
                        key,
                        file.getContentType(),
                        current.userId(), sourceChecksum
                )
        );
        var revision = revisions.save(new KnowledgeRevision(doc.id(), productModelId, region));
        applicability.save(new KnowledgeRevisionApplicability(revision.id(), productModelId, productVariantId, region,
                hardwareRevision, firmwareMin, firmwareMax, null, null));
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

    @PostMapping("/documents/{id}/revisions")
    @PreAuthorize("hasRole('ADMIN')")
    public DocumentView createRevision(@PathVariable UUID id, @RequestBody NewRevisionRequest request) {
        products.findByIdAndTenantId(request.productModelId(), current.tenantId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        var revision = revisionService.createFromExistingSource(current.tenantId(), id, request.productModelId(),
                request.productVariantId(), request.region(), request.hardwareRevision(), request.firmwareMin(),
                request.firmwareMax(), request.validFrom(), request.validTo());
        var document = documents.findByIdAndTenantId(id, current.tenantId()).orElseThrow(() -> new IllegalArgumentException("Document not found"));
        return DocumentView.of(document, revision);
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
        revision.archive();
        revisions.save(revision);
        vectorIndex.removeRevision(revision.id());
    }

    @PostMapping("/knowledge-revisions/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN','KNOWLEDGE_REVIEWER')")
    public void restore(@PathVariable UUID id) {
        var revision = revisions.findOwned(id, current.tenantId()).orElseThrow(() -> new IllegalArgumentException("Revision not found"));
        revisions.findAllByDocumentIdOrderByRevisionNoDesc(revision.documentId()).stream()
                .filter(candidate -> candidate.status() == KnowledgeRevision.Status.PUBLISHED)
                .forEach(candidate -> {
                    candidate.archive();
                    revisions.save(candidate);
                    vectorIndex.removeRevision(candidate.id());
                });
        revision.restore(current.userId());
        revisions.save(revision);
        jobs.save(new ProcessingJob(revision.id(), ProcessingJob.Type.INDEX));
    }

    @GetMapping("/documents/{id}/preview")
    public Preview preview(@PathVariable UUID id) {
        var document = documents.findByIdAndTenantId(id, current.tenantId()).orElseThrow(() -> new IllegalArgumentException("Document not found"));
        var revision = revisions.findAllByDocumentIdOrderByRevisionNoDesc(id).stream().findFirst().orElseThrow(() -> new IllegalStateException("Document has no revision"));
        return new Preview(document.title(), revision.status().name(), revision.indexStatus().name(), revision.extractedText(), chunks.findAllByRevisionIdOrderByChunkNo(revision.id()).stream().map(c -> new ChunkView(c.chunkNo(), c.sourceLabel(), c.titlePath(), c.pageFrom(), c.content())).toList());
    }

    record DocumentView(UUID id, UUID revisionId, String title, String locale, String status, String indexStatus) {
        static DocumentView of(KnowledgeDocument d, KnowledgeRevision revision) {
            return new DocumentView(d.id(), revision.id(), d.title(), d.locale(), revision.status().name(), revision.indexStatus().name());
        }
    }

    record Preview(String title, String status, String indexStatus, String text, List<ChunkView> chunks) {
    }

    record ChunkView(int chunkNo, String source, String titlePath, Integer pageFrom, String text) {
    }

    record NewRevisionRequest(UUID productModelId, UUID productVariantId, @NotBlank String region, String hardwareRevision,
                              String firmwareMin, String firmwareMax, java.time.Instant validFrom, java.time.Instant validTo) { }

    private static String checksum(MultipartFile file) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(file.getBytes()));
        } catch (NoSuchAlgorithmException | java.io.IOException exception) {
            throw new IllegalArgumentException("Unable to checksum uploaded knowledge source", exception);
        }
    }
}
