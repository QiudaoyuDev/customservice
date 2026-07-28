package com.hardwareai.support.knowledge; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument,UUID>{List<KnowledgeDocument> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);Optional<KnowledgeDocument> findByIdAndTenantId(UUID id,UUID tenantId);}
interface KnowledgeRevisionRepository extends JpaRepository<KnowledgeRevision,UUID>{Optional<KnowledgeRevision> findByIdAndDocumentId(UUID id,UUID documentId);}
