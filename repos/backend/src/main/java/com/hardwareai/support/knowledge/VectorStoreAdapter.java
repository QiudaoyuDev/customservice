package com.hardwareai.support.knowledge;

import java.util.List;
import java.util.UUID;

/** A replaceable vector-store replica; PostgreSQL remains the source of truth. */
public interface VectorStoreAdapter {
    void upsert(KnowledgeRevision revision, KnowledgeDocument document, List<KnowledgeChunk> chunks);

    void removeRevision(UUID revisionId);
}
