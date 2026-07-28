package com.hardwareai.support.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, UUID> {
    List<KnowledgeDocument> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    Optional<KnowledgeDocument> findByIdAndTenantId(UUID id, UUID tenantId);
}

interface KnowledgeRevisionRepository extends JpaRepository<KnowledgeRevision, UUID> {
    Optional<KnowledgeRevision> findByIdAndDocumentId(UUID id, UUID documentId);
}
