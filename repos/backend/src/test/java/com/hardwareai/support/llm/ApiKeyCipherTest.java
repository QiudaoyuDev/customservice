package com.hardwareai.support.llm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApiKeyCipherTest {
    @Test
    void encryptsWithIndependentNonceAndRejectsWrongKeyVersion() {
        var cipher = new ApiKeyCipher("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=", "v1");
        var first = cipher.encrypt("sk-secret");
        var second = cipher.encrypt("sk-secret");
        assertNotEquals(first.ciphertext(), second.ciphertext());
        assertEquals("sk-secret", cipher.decrypt(first.ciphertext(), first.nonce(), "v1"));
        assertThrows(IllegalStateException.class, () -> cipher.decrypt(first.ciphertext(), first.nonce(), "v2"));
    }
}
