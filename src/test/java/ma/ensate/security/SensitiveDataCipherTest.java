package ma.ensate.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SensitiveDataCipherTest {

    @Test
    void shouldEncryptAndDecryptSensitiveValue() {
        String encrypted = SensitiveDataCipher.encrypt("jihane@example.com");

        assertNotEquals("jihane@example.com", encrypted);
        assertTrue(SensitiveDataCipher.isEncrypted(encrypted));
        assertEquals("jihane@example.com", SensitiveDataCipher.decrypt(encrypted));
    }

    @Test
    void shouldKeepLegacyPlaintextReadable() {
        assertEquals("ancienne_valeur", SensitiveDataCipher.decrypt("ancienne_valeur"));
    }
}
