package io.ituknown.crypto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class AesUtilsTest {
    final AesUtils.Key KEY = new AesUtils.Key(
            "E430CC9B2E50C128DB01D5B3DDCB909AA64C494992120CF35150274D09EFECE1",
            "5DDMmy5QwSjbAdWz3cuQmqZMSUmSEgzzUVAnTQnv7OE="
    );

    final String plaintext = "hello, world";
    final String ciphertext = "6jnoRfDK2wy6VWrvL/kFrsFYEcQrBZ3AY34ECa6jRy84vGUoGwkElg==";

    @Test
    public void testGenerateEncodedKey() {
        System.out.println("testGenerateEncodedKey:");
        System.out.println("hexString: " + KEY.hexString());
        System.out.println("base64String: " + KEY.base64String());
        Assertions.assertTrue(StringUtils.isNotBlank(KEY.base64String()));
    }

    @Test
    public void testEncrypt() throws Exception {
        String ciphertext = AesUtils.encryptToBase64String(plaintext, KEY.fromBase64());
        Assertions.assertTrue(StringUtils.isNotBlank(ciphertext));
    }

    @Test
    public void testDecrypt() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        String plaintext = AesUtils.decryptFromBase64String(ciphertext, KEY.fromBase64());
        Assertions.assertEquals(plaintext, this.plaintext);
    }
}