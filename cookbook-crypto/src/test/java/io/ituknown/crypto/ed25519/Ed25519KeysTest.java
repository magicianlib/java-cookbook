package io.ituknown.crypto.ed25519;

import io.ituknown.crypto.Base64;
import io.ituknown.crypto.Hex;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Ed25519KeysTest {

    /** RFC 8032 第 7.1 节测试向量 1 的私钥（种子 32 字节包装为 PKCS#8 DER，48 字节）。 */
    private static final String RFC8032_PKCS8 =
            "302e020100300506032b6570042204209d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60";

    /** RFC 8032 第 7.1 节测试向量 1 的公钥（X.509 SubjectPublicKeyInfo DER，44 字节）。 */
    private static final String RFC8032_SPKI =
            "302a300506032b6570032100d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a";

    @Test
    public void testBuildPublicKeyFromDer() throws Exception {
        PublicKey original = Ed25519Keys.generateKeyPair().getPublic();
        PublicKey rebuilt = Ed25519Keys.buildPublicKey(original.getEncoded());
        Assertions.assertEquals(original, rebuilt);
        Assertions.assertEquals("X.509", rebuilt.getFormat());
    }

    /** 算法展示名是 provider 相关的（JDK SunEC 报 EdDSA、BouncyCastle 报 Ed25519），不作断言；
     *  真 Ed25519 语义由 RFC 8032 向量与定长断言保证。 */
    @Test
    public void testGenerateKeyPair() throws Exception {
        KeyPair kp = Ed25519Keys.generateKeyPair();
        Assertions.assertEquals("PKCS#8", kp.getPrivate().getFormat());
        Assertions.assertEquals("X.509", kp.getPublic().getFormat());
    }

    /** Ed25519 密钥尺寸恒定：私钥 PKCS#8 为 48 字节，公钥为 44 字节。 */
    @Test
    public void testKeyEncodingSizesAreFixed() throws Exception {
        KeyPair kp = Ed25519Keys.generateKeyPair();
        Assertions.assertEquals(48, kp.getPrivate().getEncoded().length);
        Assertions.assertEquals(44, kp.getPublic().getEncoded().length);
    }

    @Test
    public void testLoadPrivateKeyPkcs8PemFromPath() throws Exception {
        KeyPair kp = Ed25519Keys.generateKeyPair();
        Path tmp = Files.createTempFile("ed25519-priv", ".pem");
        Files.writeString(tmp, toPem("PRIVATE KEY", kp.getPrivate().getEncoded()));
        PrivateKey key = Ed25519Keys.loadPrivateKey(tmp);
        Assertions.assertEquals(kp.getPrivate(), key);
    }

    @Test
    public void testLoadPrivateKeyPkcs8DerFromStream() throws Exception {
        KeyPair kp = Ed25519Keys.generateKeyPair();
        byte[] der = kp.getPrivate().getEncoded();
        PrivateKey key = Ed25519Keys.loadPrivateKey(new ByteArrayInputStream(der));
        Assertions.assertEquals(kp.getPrivate(), key);
    }

    @Test
    public void testLoadPublicKeyX509PemFromStream() throws Exception {
        KeyPair kp = Ed25519Keys.generateKeyPair();
        byte[] pem = toPem("PUBLIC KEY", kp.getPublic().getEncoded()).getBytes(StandardCharsets.US_ASCII);
        PublicKey key = Ed25519Keys.loadPublicKey(new ByteArrayInputStream(pem));
        Assertions.assertEquals(kp.getPublic(), key);
    }

    /** InputStream 重载应在读取后关闭流，调用方无需自行 close。 */
    @Test
    public void testLoadPublicKeyClosesInputStream() throws Exception {
        KeyPair kp = Ed25519Keys.generateKeyPair();
        byte[] pem = toPem("PUBLIC KEY", kp.getPublic().getEncoded()).getBytes(StandardCharsets.US_ASCII);
        boolean[] closed = {false};
        java.io.InputStream in = new java.io.FilterInputStream(new ByteArrayInputStream(pem)) {
            @Override
            public void close() throws java.io.IOException {
                closed[0] = true;
                super.close();
            }
        };
        Ed25519Keys.loadPublicKey(in);
        Assertions.assertTrue(closed[0], "loadPublicKey should close the input stream");
    }

    @Test
    public void testLoadPrivateKeyClosesInputStream() throws Exception {
        KeyPair kp = Ed25519Keys.generateKeyPair();
        byte[] der = kp.getPrivate().getEncoded();
        boolean[] closed = {false};
        java.io.InputStream in = new java.io.FilterInputStream(new ByteArrayInputStream(der)) {
            @Override
            public void close() throws java.io.IOException {
                closed[0] = true;
                super.close();
            }
        };
        Ed25519Keys.loadPrivateKey(in);
        Assertions.assertTrue(closed[0], "loadPrivateKey should close the input stream");
    }

    @Test
    public void testLoadPublicKeyX509DerFromPath() throws Exception {
        KeyPair kp = Ed25519Keys.generateKeyPair();
        Path tmp = Files.createTempFile("ed25519-pub", ".der");
        Files.write(tmp, kp.getPublic().getEncoded());
        PublicKey key = Ed25519Keys.loadPublicKey(tmp);
        Assertions.assertEquals(kp.getPublic(), key);
    }

    @Test
    public void testParsePrivateKeyPemInline() throws Exception {
        KeyPair kp = Ed25519Keys.generateKeyPair();
        PrivateKey key = Ed25519Keys.parsePrivateKeyPem(toPem("PRIVATE KEY", kp.getPrivate().getEncoded()));
        Assertions.assertEquals(kp.getPrivate(), key);
    }

    @Test
    public void testParsePublicKeyBase64Inline() throws Exception {
        KeyPair kp = Ed25519Keys.generateKeyPair();
        String base64 = Base64.toString(kp.getPublic().getEncoded());
        PublicKey key = Ed25519Keys.parsePublicKeyBase64(base64);
        Assertions.assertEquals(kp.getPublic(), key);
    }

    /** 裸 Base64 允许含换行（如直接从 PEM body 复制、未带 ----- 头）。 */
    @Test
    public void testParsePublicKeyBase64AcceptsNewlines() throws Exception {
        KeyPair kp = Ed25519Keys.generateKeyPair();
        PublicKey key = Ed25519Keys.parsePublicKeyBase64(wrap(Base64.toString(kp.getPublic().getEncoded())));
        Assertions.assertEquals(kp.getPublic(), key);
    }

    @Test
    public void testParsePrivateKeyBase64AcceptsNewlines() throws Exception {
        KeyPair kp = Ed25519Keys.generateKeyPair();
        PrivateKey key = Ed25519Keys.parsePrivateKeyBase64(wrap(Base64.toString(kp.getPrivate().getEncoded())));
        Assertions.assertEquals(kp.getPrivate(), key);
    }

    @Test
    public void testGenerateBase64KeyPairRoundTrip() throws Exception {
        Ed25519Keys.Ed25519KeyPair pair = Ed25519Keys.generateBase64KeyPair();
        Assertions.assertNotNull(pair.privateKeyBase64());
        Assertions.assertNotNull(pair.publicKeyBase64());
        Assertions.assertNotNull(Ed25519Keys.parsePrivateKeyBase64(pair.privateKeyBase64()));
        Assertions.assertNotNull(Ed25519Keys.parsePublicKeyBase64(pair.publicKeyBase64()));
    }

    /** RFC 8032 官方测试向量：公私钥可正确解析，且编码往返后与向量字节一致。 */
    @Test
    public void testParseRfc8032TestVectorKeys() throws Exception {
        byte[] pkcs8 = Hex.toByteArray(RFC8032_PKCS8);
        byte[] spki = Hex.toByteArray(RFC8032_SPKI);
        PrivateKey pri = Ed25519Keys.parsePrivateKeyBase64(Base64.toString(pkcs8));
        PublicKey pub = Ed25519Keys.parsePublicKeyBase64(Base64.toString(spki));
        Assertions.assertArrayEquals(pkcs8, pri.getEncoded());
        Assertions.assertArrayEquals(spki, pub.getEncoded());
    }

    /** BouncyCastle 生成的密钥可被本类解析（跨实现互操作，密钥编码为标准格式）。 */
    @Test
    public void testParseBouncyCastleGeneratedKeys() throws Exception {
        KeyPair bcKp = KeyPairGenerator.getInstance("Ed25519", new BouncyCastleProvider()).generateKeyPair();
        PrivateKey pri = Ed25519Keys.parsePrivateKeyBase64(Base64.toString(bcKp.getPrivate().getEncoded()));
        PublicKey pub = Ed25519Keys.parsePublicKeyBase64(Base64.toString(bcKp.getPublic().getEncoded()));
        Assertions.assertArrayEquals(bcKp.getPublic().getEncoded(), pub.getEncoded());
        Assertions.assertEquals("PKCS#8", pri.getFormat());
    }

    @Test
    public void testPublicLoadersRejectNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Ed25519Keys.loadPrivateKey((Path) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Ed25519Keys.loadPrivateKey((java.io.InputStream) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Ed25519Keys.loadPublicKey((Path) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Ed25519Keys.loadPublicKey((java.io.InputStream) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Ed25519Keys.parsePrivateKeyPem(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Ed25519Keys.parsePublicKeyPem(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Ed25519Keys.parsePrivateKeyBase64(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Ed25519Keys.parsePublicKeyBase64(null));
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
