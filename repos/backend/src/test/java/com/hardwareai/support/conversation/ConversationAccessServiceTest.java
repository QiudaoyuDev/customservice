package com.hardwareai.support.conversation;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ConversationAccessServiceTest {
    @Test
    void acceptsOnlyTheTokenHashBoundToTheRequestedConversation() {
        var repository = mock(ConversationRepository.class);
        var conversation = mock(Conversation.class);
        var id = UUID.randomUUID();
        var tenant = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(conversation));
        when(conversation.tenantId()).thenReturn(tenant);
        when(conversation.authorizes(QrHash.tokenHash("valid-token"))).thenReturn(true);
        var service = new ConversationAccessService(repository);

        assertEquals(tenant, service.authorize(id, "valid-token"));
        assertThrows(IllegalArgumentException.class, () -> service.authorize(id, "replayed-token"));
        assertThrows(IllegalArgumentException.class, () -> service.authorize(id, ""));
    }

    private static final class QrHash {
        private static String tokenHash(String token) {
            try {
                return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                        .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            } catch (java.security.NoSuchAlgorithmException exception) {
                throw new AssertionError(exception);
            }
        }
    }
}
