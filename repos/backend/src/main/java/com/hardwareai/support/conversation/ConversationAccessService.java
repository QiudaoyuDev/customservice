package com.hardwareai.support.conversation;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Exposes only the tenant boundary required by downstream domain services.
 */
@Service
public class ConversationAccessService {
    private final ConversationRepository conversations;

    public ConversationAccessService(ConversationRepository conversations) {
        this.conversations = conversations;
    }

    public UUID tenantId(UUID conversationId) {
        return conversations.findById(conversationId).orElseThrow(() -> new IllegalArgumentException("Conversation is unavailable"))
            .tenantId();
    }

    public UUID authorize(UUID conversationId, String token) {
        if (token == null || token.isBlank()) throw new IllegalArgumentException("Conversation access token is required");
        var conversation =
            conversations.findById(conversationId).orElseThrow(() -> new IllegalArgumentException("Conversation is unavailable"));
        String hash;
        try {
            hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        if (!conversation.authorizes(hash)) throw new IllegalArgumentException("Conversation access is invalid");
        return conversation.tenantId();
    }
}
