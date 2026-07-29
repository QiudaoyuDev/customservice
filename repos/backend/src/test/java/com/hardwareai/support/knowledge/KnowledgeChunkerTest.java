package com.hardwareai.support.knowledge;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeChunkerTest {
    @Test
    void retainsHeadingAndPageMetadataInsteadOfCharacterOnlyChunks() {
        var document = new KnowledgeDocument(UUID.randomUUID(), "Installation Manual", "en", "key", "application/pdf", UUID.randomUUID(), "checksum");
        var revision = new KnowledgeRevision(document.id(), UUID.randomUUID(), "EU");
        revision.setExtractedText("\fPAGE:2\n# Power reset\n\nDisconnect the power for ten seconds.");

        var chunks = new KnowledgeChunker().split(revision, document);

        assertEquals(1, chunks.size());
        assertEquals(2, chunks.getFirst().pageFrom());
        assertTrue(chunks.getFirst().titlePath().contains("Power reset"));
    }
}
