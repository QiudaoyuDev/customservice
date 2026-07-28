package com.hardwareai.support.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, UUID> {
    List<KnowledgeDocument> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    Optional<KnowledgeDocument> findByIdAndTenantId(UUID id, UUID tenantId);
}

interface KnowledgeRevisionRepository extends JpaRepository<KnowledgeRevision, UUID> {
    Optional<KnowledgeRevision> findByIdAndDocumentId(UUID id, UUID documentId);

    @Query("select r from KnowledgeRevision r, KnowledgeDocument d where r.id = :id and r.documentId = d.id and d.tenantId = :tenantId")
    Optional<KnowledgeRevision> findOwned(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    List<KnowledgeRevision> findAllByDocumentIdOrderByRevisionNoDesc(UUID documentId);
}

interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, UUID> {
    List<KnowledgeChunk> findAllByRevisionIdOrderByChunkNo(UUID revisionId);

    void deleteAllByRevisionId(UUID revisionId);

    @Query("""
            select c from KnowledgeChunk c, KnowledgeRevision r, KnowledgeDocument d
            where c.revisionId = r.id and r.documentId = d.id and d.tenantId = :tenantId
              and r.productModelId = :productModelId and r.region = :region and d.locale = :locale
              and r.status = com.hardwareai.support.knowledge.KnowledgeRevision.Status.PUBLISHED
              and lower(c.content) like lower(concat('%', :query, '%'))
            order by c.chunkNo
            """)
    List<KnowledgeChunk> keywordSearch(@Param("tenantId") UUID tenantId, @Param("productModelId") UUID productModelId,
                                       @Param("region") String region, @Param("locale") String locale, @Param("query") String query,
                                       org.springframework.data.domain.Pageable pageable);
}
