package com.hardwareai.support.conversation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConversationStreamTest {
    @Test
    void splitsAnswerIntoOrderedDeltaFrames() {
        var chunks = ConversationController.streamChunks("abcdefghij", 4);

        assertEquals(java.util.List.of("abcd", "efgh", "ij"), chunks);
    }
}
