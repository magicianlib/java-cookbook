package io.ituknown.crypto;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECKey;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EcdsaKeysTest {

    @Test
    public void testBuildPublicKeyFromDer() throws Exception {
        java.security.PublicKey original = EcdsaKeys.generateKeyPair(EcdsaKeys.Curve.P256).getPublic();
        java.security.PublicKey rebuilt = EcdsaKeys.buildPublicKey(original.getEncoded());
        Assertions.assertEquals(original, rebuilt);
        Assertions.assertEquals("X.509", rebuilt.getFormat());
    }

    private static KeyPair newKeyPair() throws Exception {
        return EcdsaKeys.generateKeyPair(EcdsaKeys.Curve.P256);
    }

    @Test
    public void testLoadPrivateKeyPkcs8PemFromPath() throws Exception {
        KeyPair kp = newKeyPair();
        Path tmp = Files.createTempFile("ec-priv", ".pem");
        Files.writeString(tmp, toPem("PRIVATE KEY", kp.getPrivate().getEncoded()));
        PrivateKey key = EcdsaKeys.loadPrivateKey(tmp);
        Assertions.assertEquals(kp.getPrivate(), key);
    }

    @Test
    public void testLoadPrivateKeyPkcs8DerFromStream() throws Exception {
        KeyPair kp = newKeyPair();
        byte[] der = kp.getPrivate().getEncoded();
        PrivateKey key = EcdsaKeys.loadPrivateKey(new ByteArrayInputStream(der));
        Assertions.assertEquals(kp.getPrivate(), key);
    }

    @Test
    public void testLoadPublicKeyX509PemFromStream() throws Exception {
        KeyPair kp = newKeyPair();
        byte[] pem = toPem("PUBLIC KEY", kp.getPublic().getEncoded()).getBytes(StandardCharsets.US_ASCII);
        PublicKey key = EcdsaKeys.loadPublicKey(new ByteArrayInputStream(pem));
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
        EcdsaKeys.loadPublicKey(in);
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
        EcdsaKeys.loadPrivateKey(in);
        Assertions.assertTrue(closed[0], "loadPrivateKey should close the input stream");
    }

    @Test
    public void testLoadPublicKeyX509DerFromPath() throws Exception {
        KeyPair kp = newKeyPair();
        Path tmp = Files.createTempFile("ec-pub", ".der");
        Files.write(tmp, kp.getPublic().getEncoded());
        PublicKey key = EcdsaKeys.loadPublicKey(tmp);
        Assertions.assertEquals(kp.getPublic(), key);
    }

    @Test
    public void testParsePrivateKeyPemInline() throws Exception {
        KeyPair kp = newKeyPair();
        PrivateKey key = EcdsaKeys.parsePrivateKeyPem(toPem("PRIVATE KEY", kp.getPrivate().getEncoded()));
        Assertions.assertEquals(kp.getPrivate(), key);
    }

    @Test
    public void testParsePublicKeyBase64Inline() throws Exception {
        KeyPair kp = newKeyPair();
        String base64 = Base64.toString(kp.getPublic().getEncoded());
        PublicKey key = EcdsaKeys.parsePublicKeyBase64(base64);
        Assertions.assertEquals(kp.getPublic(), key);
    }

    /** 裸 Base64 允许含换行（如直接从 PEM body 复制、未带 ----- 头）。 */
    @Test
    public void testParsePublicKeyBase64AcceptsNewlines() throws Exception {
        KeyPair kp = newKeyPair();
        PublicKey key = EcdsaKeys.parsePublicKeyBase64(wrap(Base64.toString(kp.getPublic().getEncoded())));
        Assertions.assertEquals(kp.getPublic(), key);
    }

    @Test
    public void testParsePrivateKeyBase64AcceptsNewlines() throws Exception {
        KeyPair kp = newKeyPair();
        PrivateKey key = EcdsaKeys.parsePrivateKeyBase64(wrap(Base64.toString(kp.getPrivate().getEncoded())));
        Assertions.assertEquals(kp.getPrivate(), key);
    }

    @Test
    public void testGenerateKeyPairDefaultCurve() throws Exception {
        KeyPair kp = EcdsaKeys.generateKeyPair();
        Assertions.assertEquals(256, orderBits(kp), "默认曲线应为 P-256");
        Assertions.assertEquals(EcdsaKeys.DEFAULT_CURVE, EcdsaKeys.Curve.P256);
    }

    /** 四条曲线均可生成，且阶的比特长度与曲线匹配。 */
    @Test
    public void testGenerateKeyPairAllCurves() throws Exception {
        Assertions.assertEquals(256, orderBits(EcdsaKeys.generateKeyPair(EcdsaKeys.Curve.P256)));
        Assertions.assertEquals(384, orderBits(EcdsaKeys.generateKeyPair(EcdsaKeys.Curve.P384)));
        Assertions.assertEquals(521, orderBits(EcdsaKeys.generateKeyPair(EcdsaKeys.Curve.P521)));
        Assertions.assertEquals(256, orderBits(EcdsaKeys.generateKeyPair(EcdsaKeys.Curve.SECP256K1)));
    }

    /** secp256k1 生成的密钥可被完整往返（验证 BC 解析路径）。 */
    @Test
    public void testSecp256k1RoundTrip() throws Exception {
        KeyPair kp = EcdsaKeys.generateKeyPair(EcdsaKeys.Curve.SECP256K1);
        Assertions.assertEquals(kp.getPrivate(), EcdsaKeys.parsePrivateKeyBase64(Base64.toString(kp.getPrivate().getEncoded())));
        Assertions.assertEquals(kp.getPublic(), EcdsaKeys.parsePublicKeyBase64(Base64.toString(kp.getPublic().getEncoded())));
    }

    @Test
    public void testGenerateBase64KeyPairRoundTrip() throws Exception {
        EcdsaKeys.EcdsaKeyPair pair = EcdsaKeys.generateBase64KeyPair(EcdsaKeys.Curve.P256);
        Assertions.assertNotNull(pair.privateKeyBase64());
        Assertions.assertNotNull(pair.publicKeyBase64());
        Assertions.assertNotNull(EcdsaKeys.parsePrivateKeyBase64(pair.privateKeyBase64()));
        Assertions.assertNotNull(EcdsaKeys.parsePublicKeyBase64(pair.publicKeyBase64()));
    }

    @Test
    public void testPublicLoadersRejectNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> EcdsaKeys.loadPrivateKey((Path) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> EcdsaKeys.loadPrivateKey((java.io.InputStream) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> EcdsaKeys.loadPublicKey((Path) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> EcdsaKeys.loadPublicKey((java.io.InputStream) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> EcdsaKeys.parsePrivateKeyPem(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> EcdsaKeys.parsePublicKeyPem(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> EcdsaKeys.parsePrivateKeyBase64(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> EcdsaKeys.parsePublicKeyBase64(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> EcdsaKeys.generateKeyPair((EcdsaKeys.Curve) null));
    }

    private static int orderBits(KeyPair kp) {
        return ((ECKey) kp.getPublic()).getParams().getOrder().bitLength();
    }

    private static String wrap(String singleLine) {
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
}
