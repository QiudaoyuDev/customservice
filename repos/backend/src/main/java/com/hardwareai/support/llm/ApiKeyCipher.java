package com.hardwareai.support.llm;

import com.hardwareai.support.config.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM envelope for provider credentials; callers receive no plaintext persistence API.
 */
@Component
class ApiKeyCipher {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final SecretKey key;
    private final String keyVersion;

    @Autowired
    ApiKeyCipher(AppProperties properties) {
        this(properties.modelKeyEncryption().masterKey(), properties.modelKeyEncryption().keyVersion());
    }

    ApiKeyCipher(String masterKey, String keyVersion) {
        byte[] bytes = Base64.getDecoder().decode(masterKey);
        if (bytes.length != 32) throw new IllegalArgumentException("Model key encryption key must be 32 bytes");
        key = new SecretKeySpec(bytes, "AES");
        this.keyVersion = keyVersion;
    }

    Encrypted encrypt(String value) {
        try {
            byte[] nonce = new byte[12];
            RANDOM.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, nonce));
            return new Encrypted(
                Base64.getEncoder().encodeToString(cipher.doFinal(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))),
                Base64.getEncoder().encodeToString(nonce), keyVersion);
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to encrypt provider key", exception);
        }
    }

    String decrypt(String ciphertext, String nonce, String version) {
        if (!keyVersion.equals(version)) throw new IllegalStateException("Provider key requires unavailable key version");
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, Base64.getDecoder().decode(nonce)));
            return new String(cipher.doFinal(Base64.getDecoder().decode(ciphertext)), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to decrypt provider key", exception);
        }
    }

    record Encrypted(String ciphertext, String nonce, String keyVersion) {
    }
}
