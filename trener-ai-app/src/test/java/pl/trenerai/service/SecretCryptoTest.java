package pl.trenerai.service;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SecretCryptoTest {

    private final SecretCrypto crypto = new SecretCrypto("test-secret-key-at-least-32-characters-long");

    @Test
    void encryptAndDecryptRoundTrip() {
        String original = "my-api-key-12345";
        String encrypted = crypto.encrypt(original);
        assertNotNull(encrypted);
        assertNotEquals(original, encrypted);
        assertEquals(original, crypto.decrypt(encrypted));
    }

    @Test
    void encryptProducesDifferentCiphertextEachTime() {
        String original = "same-input";
        String first = crypto.encrypt(original);
        String second = crypto.encrypt(original);
        assertNotEquals(first, second, "GCM random IV should produce different ciphertext");
        assertEquals(crypto.decrypt(first), crypto.decrypt(second));
    }

    @Test
    void encryptNullReturnsNull() {
        assertNull(crypto.encrypt(null));
    }

    @Test
    void encryptBlankReturnsNull() {
        assertNull(crypto.encrypt("   "));
    }

    @Test
    void decryptNullReturnsEmpty() {
        assertEquals("", crypto.decrypt(null));
    }

    @Test
    void decryptBlankReturnsEmpty() {
        assertEquals("", crypto.decrypt("  "));
    }

    @Test
    void decryptWithWrongKeyThrows() {
        String encrypted = crypto.encrypt("secret");
        SecretCrypto otherKey = new SecretCrypto("different-key-also-at-least-32-characters");
        assertThrows(IllegalStateException.class, () -> otherKey.decrypt(encrypted));
    }

    @Test
    void decryptGarbageThrows() {
        assertThrows(IllegalStateException.class, () -> crypto.decrypt("not-valid-base64-!!!"));
    }

    @Test
    void decryptTooShortThrows() {
        String tooShort = java.util.Base64.getEncoder().encodeToString(new byte[10]);
        assertThrows(IllegalStateException.class, () -> crypto.decrypt(tooShort));
    }
}
