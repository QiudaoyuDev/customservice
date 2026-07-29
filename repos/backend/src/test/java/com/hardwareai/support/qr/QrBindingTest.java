package com.hardwareai.support.qr;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QrBindingTest {
    @Test
    void hashDoesNotExposeTheOpaquePublicCredential() {
        var token = "qr-public-token";
        assertNotEquals(token, QrApplicationService.hash(token));
        assertNotEquals(QrApplicationService.hash(token), QrApplicationService.hash("replayed-token"));
    }

    @Test
    void rejectsExpiredAndRevokedBindings() {
        var expired = binding(Instant.now().minusSeconds(1));
        var active = binding(Instant.now().plusSeconds(60));
        assertFalse(expired.valid());
        assertTrue(active.valid());
        active.revoke("compromised");
        assertFalse(active.valid());
    }

    private static QrBinding binding(Instant expiresAt) {
        return new QrBinding(UUID.randomUUID(), UUID.randomUUID(), null, null,
                QrApplicationService.hash("qr-token"), null, null, expiresAt);
    }
}
