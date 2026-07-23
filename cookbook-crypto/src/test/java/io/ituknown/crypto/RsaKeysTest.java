package io.ituknown.crypto;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RsaKeysTest {

    @Test
    public void testBuildPublicKeyFromDer() throws Exception {
        java.security.KeyPairGenerator g = java.security.KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        java.security.PublicKey original = g.generateKeyPair().getPublic();
        java.security.PublicKey rebuilt = RsaKeys.buildPublicKey(original.getEncoded());
        Assertions.assertEquals(original, rebuilt);
        Assertions.assertEquals("X.509", rebuilt.getFormat());
    }

    private static KeyPair newKeyPair() throws Exception {
        java.security.KeyPairGenerator g = java.security.KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        return g.generateKeyPair();
    }

    @Test
    public void testLoadPrivateKeyPkcs8PemFromPath() throws Exception {
        KeyPair kp = newKeyPair();
        Path tmp = Files.createTempFile("rsa-priv", ".pem");
        Files.writeString(tmp, toPem("PRIVATE KEY", kp.getPrivate().getEncoded()));
        PrivateKey key = RsaKeys.loadPrivateKey(tmp);
        Assertions.assertEquals(kp.getPrivate(), key);
    }

    @Test
    public void testLoadPrivateKeyPkcs8DerFromStream() throws Exception {
        KeyPair kp = newKeyPair();
        byte[] der = kp.getPrivate().getEncoded();
        PrivateKey key = RsaKeys.loadPrivateKey(new ByteArrayInputStream(der));
        Assertions.assertEquals(kp.getPrivate(), key);
    }

    @Test
    public void testLoadPublicKeyX509PemFromStream() throws Exception {
        KeyPair kp = newKeyPair();
        byte[] pem = toPem("PUBLIC KEY", kp.getPublic().getEncoded()).getBytes(StandardCharsets.US_ASCII);
        PublicKey key = RsaKeys.loadPublicKey(new ByteArrayInputStream(pem));
        Assertions.assertEquals(kp.getPublic(), key);
    }

    /** InputStream 重载应在读取后关闭流，调用方无需自行 close。 */
    @Test
    public void testLoadPublicKeyClosesInputStream() throws Exception {
        KeyPair kp = newKeyPair();
        byte[] pem = toPem("PUBLIC KEY", kp.getPublic().getEncoded()).getBytes(StandardCharsets.US_ASCII);
        boolean[] closed = {false};
        java.io.InputStream in = new java.io.FilterInputStream(new ByteArrayInputStream(pem)) {
            @Override
            public void close() throws java.io.IOException {
                closed[0] = true;
                super.close();
            }
        };
        RsaKeys.loadPublicKey(in);
        Assertions.assertTrue(closed[0], "loadPublicKey should close the input stream");
    }

    @Test
    public void testLoadPrivateKeyClosesInputStream() throws Exception {
        KeyPair kp = newKeyPair();
        byte[] der = kp.getPrivate().getEncoded();
        boolean[] closed = {false};
        java.io.InputStream in = new java.io.FilterInputStream(new ByteArrayInputStream(der)) {
            @Override
            public void close() throws java.io.IOException {
                closed[0] = true;
                super.close();
            }
        };
        RsaKeys.loadPrivateKey(in);
        Assertions.assertTrue(closed[0], "loadPrivateKey should close the input stream");
    }

    @Test
    public void testLoadPublicKeyX509DerFromPath() throws Exception {
        KeyPair kp = newKeyPair();
        Path tmp = Files.createTempFile("rsa-pub", ".der");
        Files.write(tmp, kp.getPublic().getEncoded());
        PublicKey key = RsaKeys.loadPublicKey(tmp);
        Assertions.assertEquals(kp.getPublic(), key);
    }

    @Test
    public void testParsePrivateKeyPemInline() throws Exception {
        KeyPair kp = newKeyPair();
        PrivateKey key = RsaKeys.parsePrivateKeyPem(toPem("PRIVATE KEY", kp.getPrivate().getEncoded()));
        Assertions.assertEquals(kp.getPrivate(), key);
    }

    @Test
    public void testParsePublicKeyBase64Inline() throws Exception {
        KeyPair kp = newKeyPair();
        String base64 = Base64.toString(kp.getPublic().getEncoded());
        PublicKey key = RsaKeys.parsePublicKeyBase64(base64);
        Assertions.assertEquals(kp.getPublic(), key);
    }

    /** 裸 Base64 允许含换行（如直接从 PEM body 复制、未带 ----- 头）。 */
    @Test
    public void testParsePublicKeyBase64AcceptsNewlines() throws Exception {
        KeyPair kp = newKeyPair();
        String singleLine = Base64.toString(kp.getPublic().getEncoded());
        String multiLine = wrapBase64WithNewlines(singleLine);
        PublicKey key = RsaKeys.parsePublicKeyBase64(multiLine);
        Assertions.assertEquals(kp.getPublic(), key);
    }

    @Test
    public void testParsePrivateKeyBase64AcceptsNewlines() throws Exception {
        KeyPair kp = newKeyPair();
        String singleLine = Base64.toString(kp.getPrivate().getEncoded());
        String multiLine = wrapBase64WithNewlines(singleLine);
        PrivateKey key = RsaKeys.parsePrivateKeyBase64(multiLine);
        Assertions.assertEquals(kp.getPrivate(), key);
    }

    /** 把单行 Base64 每 64 字符断行，模拟从 PEM body 复制的多行形态。 */
    private static String wrapBase64WithNewlines(String singleLine) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < singleLine.length(); i += 64) {
            sb.append(singleLine, i, Math.min(i + 64, singleLine.length())).append('\n');
        }
        return sb.toString();
    }

    /** 把 DER 字节包装成带换行的 PEM 文本。 */
    private static String toPem(String type, byte[] der) {
        String base64 = Base64.toString(der);
        StringBuilder sb = new StringBuilder("-----BEGIN ").append(type).append("-----\n");
        for (int i = 0; i < base64.length(); i += 64) {
            sb.append(base64, i, Math.min(i + 64, base64.length())).append('\n');
        }
        return sb.append("-----END ").append(type).append("-----\n").toString();
    }

    @Test
    public void testGenerateKeyPairDefaultSize() throws Exception {
        java.security.KeyPair kp = RsaKeys.generateKeyPair();
        int bits = ((java.security.interfaces.RSAKey) kp.getPublic()).getModulus().bitLength();
        Assertions.assertEquals(2048, bits);
    }

    @Test
    public void testGenerateKeyPairRejectsWeakSize() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> RsaKeys.generateKeyPair(1024));
    }

    @Test
    public void testGenerateBase64KeyPairRoundTrip() throws Exception {
        RsaKeys.RsaKeyPair pair = RsaKeys.generateBase64KeyPair(2048);
        Assertions.assertNotNull(pair.privateKeyBase64());
        Assertions.assertNotNull(pair.publicKeyBase64());
        // 生成的 Base64 可被 parse*Base64 还原
        Assertions.assertNotNull(RsaKeys.parsePrivateKeyBase64(pair.privateKeyBase64()));
        Assertions.assertNotNull(RsaKeys.parsePublicKeyBase64(pair.publicKeyBase64()));
    }

    @Test
    public void testPublicLoadersRejectNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> RsaKeys.loadPrivateKey((java.nio.file.Path) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> RsaKeys.loadPrivateKey((java.io.InputStream) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> RsaKeys.loadPublicKey((java.nio.file.Path) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> RsaKeys.loadPublicKey((java.io.InputStream) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> RsaKeys.parsePrivateKeyPem(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> RsaKeys.parsePublicKeyPem(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> RsaKeys.parsePrivateKeyBase64(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> RsaKeys.parsePublicKeyBase64(null));
    }
}
