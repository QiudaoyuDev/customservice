package com.hardwareai.support.knowledge;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KnowledgeRevisionTest {
    @Test
    void enforcesReviewedPublishAndRollbackLifecycle() {
        var revision = new KnowledgeRevision(UUID.randomUUID(), UUID.randomUUID(), "US");
        assertEquals(KnowledgeRevision.Status.UPLOADED, revision.status());
        revision.beginParsing();
        revision.setExtractedText("installation guide");
        revision.submit();
        revision.approve(UUID.randomUUID());
        revision.publish(UUID.randomUUID());
        revision.deprecate();
        revision.restore(UUID.randomUUID());
        assertEquals(KnowledgeRevision.Status.PUBLISHED, revision.status());
    }

    @Test
    void rejectsPublishBeforeApproval() {
        var revision = new KnowledgeRevision(UUID.randomUUID(), UUID.randomUUID(), "US");
        revision.beginParsing();
        revision.setExtractedText("guide");
        revision.submit();
        assertThrows(IllegalStateException.class, () -> revision.publish(UUID.randomUUID()));
    }
}
