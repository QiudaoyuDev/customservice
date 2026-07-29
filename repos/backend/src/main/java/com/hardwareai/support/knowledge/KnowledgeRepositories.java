package com.hardwareai.support.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, UUID> {
    List<KnowledgeDocument> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    Optional<KnowledgeDocument> findByIdAndTenantId(UUID id, UUID tenantId);

    List<KnowledgeDocument> findAllByTenantIdAndSourceChecksum(UUID tenantId, String sourceChecksum);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from KnowledgeDocument d where d.id = :id and d.tenantId = :tenantId")
    Optional<KnowledgeDocument> findOwnedForRevision(@Param("id") UUID id, @Param("tenantId") UUID tenantId);
}

interface KnowledgeRevisionRepository extends JpaRepository<KnowledgeRevision, UUID> {
    Optional<KnowledgeRevision> findByIdAndDocumentId(UUID id, UUID documentId);

    @Query(
        "select r from KnowledgeRevision r, KnowledgeDocument d where r.id = :id and r.documentId = d.id and d.tenantId = :tenantId")
    Optional<KnowledgeRevision> findOwned(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    List<KnowledgeRevision> findAllByDocumentIdOrderByRevisionNoDesc(UUID documentId);
}

interface KnowledgeRevisionApplicabilityRepository extends JpaRepository<KnowledgeRevisionApplicability, UUID> {
    List<KnowledgeRevisionApplicability> findAllByRevisionId(UUID revisionId);
}

interface KnowledgeOcrResultRepository extends JpaRepository<KnowledgeOcrResult, UUID> {
}

interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, UUID> {
    List<KnowledgeChunk> findAllByRevisionIdOrderByChunkNo(UUID revisionId);

    void deleteAllByRevisionId(UUID revisionId);

    @Query(value = """
        select c.* from knowledge_chunks c
          join knowledge_revisions r on c.revision_id = r.id
          join knowledge_documents d on r.document_id = d.id
          join knowledge_revision_applicability a on a.revision_id = r.id
        where d.tenant_id = :tenantId
          and a.product_model_id = :productModelId and a.region = :region and d.locale = :locale
          and r.status = 'PUBLISHED' and r.index_status = 'READY'
          and (cast(:productVariantId as uuid) is null or a.product_variant_id is null or a.product_variant_id = cast(:productVariantId as uuid))
          and (:hardwareRevision is null or a.hardware_revision is null or a.hardware_revision = :hardwareRevision)
          and (:firmwareVersion is null or (a.firmware_min is null or a.firmware_min <= :firmwareVersion)
               and (a.firmware_max is null or a.firmware_max >= :firmwareVersion))
          and (a.valid_from is null or a.valid_from <= now()) and (a.valid_to is null or a.valid_to > now())
          and c.search_vector @@ websearch_to_tsquery('simple', :query)
        order by case when :errorCode is not null and lower(c.content) like concat('%', lower(:errorCode), '%') then 1 else 0 end desc,
          ts_rank_cd(c.search_vector, websearch_to_tsquery('simple', :query)) desc, c.chunk_no
        """, nativeQuery = true)
    List<KnowledgeChunk> keywordSearch(@Param("tenantId") UUID tenantId, @Param("productModelId") UUID productModelId,
        @Param("productVariantId") UUID productVariantId, @Param("region") String region,
        @Param("hardwareRevision") String hardwareRevision, @Param("firmwareVersion") String firmwareVersion,
        @Param("locale") String locale, @Param("query") String query, @Param("errorCode") String errorCode,
        org.springframework.data.domain.Pageable pageable);
}
