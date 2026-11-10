package pl.trenerai.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SecretCrypto {

    private final SecretKey key;

    public SecretCrypto(@Value("${app.crypto-key}") String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            key = new SecretKeySpec(Arrays.copyOf(hash, 32), "AES");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public String encrypt(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] payload = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + payload.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(payload, 0, combined, iv.length, payload.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public String decrypt(String text) {
        if (text == null || text.isBlank()) return "";
        try {
            byte[] combined = Base64.getDecoder().decode(text);
            if (combined.length <= 12) throw new IllegalArgumentException("Za krótka wartość");

            byte[] iv = Arrays.copyOfRange(combined, 0, 12);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));

            return new String(
                    cipher.doFinal(Arrays.copyOfRange(combined, 12, combined.length)),
                    StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Zapisany sekret jest nieprawidłowy lub został zaszyfrowany innym APP_CRYPTO_KEY. "
                            + "W ustawieniach wpisz i zapisz klucze ponownie.", e);
        }
    }
}
