package br.gov.mandaguari.saude.profissional.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Encrypts {@code SAU_PRO.ProCertificadoSenha} (the digital-certificate password) at rest using
 * AES-256-GCM. The legacy GeneXus app stored this column in PLAINTEXT and even echoed/logged it
 * (rule R31, SLICE-SPEC SAU_PRO) — that defect is deliberately NOT ported.
 *
 * <p>Key source: env {@code CERT_ENC_KEY} (Spring property {@code security.cert.enc-key}). It must be
 * a Base64-encoded 16/24/32-byte AES key. <b>Fail-closed:</b> if the key is blank, encryption (write of
 * a non-null value) throws; writing {@code null} is always allowed, and decryption of stored ciphertext
 * needs the key. This keeps the field out of any plaintext path even when the env is misconfigured.
 *
 * <p>Stored format: {@code v1:<base64(iv)>:<base64(ciphertext+tag)>}. Values that don't carry the
 * {@code v1:} prefix are treated as legacy plaintext on read and returned as-is (migration tolerance).
 *
 * <p>NOTE: in v1 the API surface neither accepts nor returns this field (see {@link Profissional}),
 * so in practice the converter only round-trips data already present in the live DB.
 */
@Component
@Converter
public class CertificadoSenhaCryptoConverter implements AttributeConverter<String, String> {

    private static final String PREFIX = "v1:";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final byte[] key;
    private final SecureRandom random = new SecureRandom();

    public CertificadoSenhaCryptoConverter(@Value("${security.cert.enc-key:}") String base64Key) {
        this.key = (base64Key == null || base64Key.isBlank())
                ? null
                : Base64.getDecoder().decode(base64Key.trim());
    }

    @Override
    public String convertToDatabaseColumn(String plaintext) {
        if (plaintext == null) return null; // null always allowed
        if (key == null) {
            // Fail-closed: never persist the password in the clear.
            throw new IllegalStateException(
                    "CERT_ENC_KEY (security.cert.enc-key) is not configured; refusing to store certificadoSenha in cleartext");
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return PREFIX + Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(ct);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt certificadoSenha", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String stored) {
        if (stored == null) return null;
        if (!stored.startsWith(PREFIX)) {
            // Legacy plaintext row (not yet re-encrypted) — return as-is. Never logged.
            return stored;
        }
        if (key == null) {
            throw new IllegalStateException(
                    "CERT_ENC_KEY (security.cert.enc-key) is not configured; cannot decrypt certificadoSenha");
        }
        try {
            String[] parts = stored.substring(PREFIX.length()).split(":", 2);
            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] ct = Base64.getDecoder().decode(parts[1]);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | RuntimeException e) {
            throw new IllegalStateException("Failed to decrypt certificadoSenha", e);
        }
    }
}
