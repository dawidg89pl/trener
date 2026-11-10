package pl.trenerai.service;

import java.nio.charset.StandardCharsets; import java.security.*; import java.util.*; import javax.crypto.*; import javax.crypto.spec.GCMParameterSpec;
import org.springframework.beans.factory.annotation.Value; import org.springframework.stereotype.Service;

@Service
public class SecretCrypto {
    private final SecretKey key;
    public SecretCrypto(@Value("${app.crypto-key}") String value) {
        try { key = new javax.crypto.spec.SecretKeySpec(Arrays.copyOf(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)), 32), "AES"); }
        catch (GeneralSecurityException e) { throw new IllegalStateException(e); }
    }
    public String encrypt(String text) { if (text == null || text.isBlank()) return null; try { byte[] iv=new byte[12]; new SecureRandom().nextBytes(iv); Cipher c=Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE,key,new GCMParameterSpec(128,iv)); byte[] payload=c.doFinal(text.getBytes(StandardCharsets.UTF_8)); byte[] both=new byte[iv.length+payload.length]; System.arraycopy(iv,0,both,0,iv.length); System.arraycopy(payload,0,both,iv.length,payload.length); return Base64.getEncoder().encodeToString(both); } catch(GeneralSecurityException e){throw new IllegalStateException(e);} }
    public String decrypt(String text) {
        if (text == null || text.isBlank()) return "";
        try {
            byte[] both=Base64.getDecoder().decode(text);
            if (both.length <= 12) throw new IllegalArgumentException("Za krótka wartość");
            byte[] iv=Arrays.copyOfRange(both,0,12); Cipher c=Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,iv));
            return new String(c.doFinal(Arrays.copyOfRange(both,12,both.length)),StandardCharsets.UTF_8);
        } catch(GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Zapisany sekret jest nieprawidłowy lub został zaszyfrowany innym APP_CRYPTO_KEY. W ustawieniach wpisz i zapisz klucze ponownie.", e);
        }
    }
}
