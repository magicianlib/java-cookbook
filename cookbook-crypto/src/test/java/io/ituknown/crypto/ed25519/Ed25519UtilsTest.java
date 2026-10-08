package io.ituknown.crypto.ed25519;

import io.ituknown.crypto.Base64;
import io.ituknown.crypto.Hex;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Ed25519UtilsTest {

    /** RFC 8032 第 7.1 节测试向量 1 的私钥（PKCS#8 DER）。 */
    private static final String RFC8032_PKCS8 =
            "302e020100300506032b6570042204209d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60";

    /** RFC 8032 第 7.1 节测试向量 1 的公钥（X.509 SubjectPublicKeyInfo DER）。 */
    private static final String RFC8032_SPKI =
            "302a300506032b6570032100d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a";

    /** RFC 8032 第 7.1 节测试向量 1：空消息的签名。 */
    private static final String RFC8032_SIG_EMPTY =
            "e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e065224901555fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b";

    /** RFC 8032 第 7.1 节测试向量 3 的私钥（PKCS#8 DER）。 */
    private static final String RFC8032_PKCS8_3 =
            "302e020100300506032b657004220420c5aa8df43f9f837bedb7442f31dcb7b166d38535076f094b85ce3a2e0b4458f7";

    /** RFC 8032 第 7.1 节测试向量 3 的公钥（X.509 SubjectPublicKeyInfo DER）。 */
    private static final String RFC8032_SPKI_3 =
            "302a300506032b6570032100fc51cd8e6218a1a38da47ed00230f0580816ed13ba3303ac5deb911548908025";

    /** RFC 8032 第 7.1 节测试向量 3：双字节消息（十六进制 af82）的签名。 */
    private static final String RFC8032_SIG_TWO_BYTES =
            "6291d657deec24024827e69c3abe01a30ce548a284743a445e3680d7db5ac3ac18ff9b538d16f290ae67f760984dc6594a7c15e9716ed28dc027beceea1ec40a";

    private static KeyPair newKeyPair() throws Exception {
        return Ed25519Keys.generateKeyPair();
    }

    @Test
    public void testSignatureAndVerifyRoundTripBytes() throws Exception {
        KeyPair kp = newKeyPair();
        byte[] data = "hello,world".getBytes(StandardCharsets.UTF_8);
        byte[] sig = Ed25519Utils.signature(kp.getPrivate(), data);
        Assertions.assertTrue(Ed25519Utils.verify(kp.getPublic(), data, sig));
    }

    /** Ed25519 签名恒为 64 字节（无 DER/RAW 编码维度之分）。 */
    @Test
    public void testSignatureIsFixed64Bytes() throws Exception {
        KeyPair kp = newKeyPair();
        Assertions.assertEquals(64, Ed25519Utils.signature(kp.getPrivate(), "hello,world").length);
        Assertions.assertEquals(64, Ed25519Utils.signature(kp.getPrivate(), new byte[0]).length);
    }

    @Test
    public void testSignatureAndVerifyRoundTripString() throws Exception {
        KeyPair kp = newKeyPair();
        byte[] sig = Ed25519Utils.signature(kp.getPrivate(), "hello,world");
        Assertions.assertTrue(Ed25519Utils.verify(kp.getPublic(), "hello,world", sig));
    }

    @Test
    public void testSignatureToBase64AndVerifyFromBase64RoundTrip() throws Exception {
        KeyPair kp = newKeyPair();
        String sig = Ed25519Utils.signatureToBase64(kp.getPrivate(), "hello,world");
        Assertions.assertTrue(Ed25519Utils.verifyFromBase64(kp.getPublic(), "hello,world", sig));
    }

    /** Base64 签名允许含换行（与 Ed25519Keys.parse*Base64 行为一致）。 */
    @Test
    public void testVerifyFromBase64AcceptsNewlines() throws Exception {
        KeyPair kp = newKeyPair();
        String sig = Ed25519Utils.signatureToBase64(kp.getPrivate(), "hello,world");
        Assertions.assertTrue(Ed25519Utils.verifyFromBase64(kp.getPublic(), "hello,world", wrap(sig)));
    }

    /** RFC 8032 官方测试向量：已知密钥对已知消息的签名验签必须通过，篡改签名任一字节必须失败。 */
    @Test
    public void testVerifyRfc8032TestVectorSignatures() throws Exception {
        PrivateKey pri = Ed25519Keys.parsePrivateKeyBase64(Base64.toString(Hex.toByteArray(RFC8032_PKCS8)));
        PublicKey pub = Ed25519Keys.parsePublicKeyBase64(Base64.toString(Hex.toByteArray(RFC8032_SPKI)));
        byte[] sig = Hex.toByteArray(RFC8032_SIG_EMPTY);
        Assertions.assertTrue(Ed25519Utils.verify(pub, new byte[0], sig), "向量 1：空消息");

        PrivateKey pri2 = Ed25519Keys.parsePrivateKeyBase64(Base64.toString(Hex.toByteArray(RFC8032_PKCS8_3)));
        PublicKey pub2 = Ed25519Keys.parsePublicKeyBase64(Base64.toString(Hex.toByteArray(RFC8032_SPKI_3)));
        byte[] msg2 = Hex.toByteArray("af82");
        byte[] sig2 = Hex.toByteArray(RFC8032_SIG_TWO_BYTES);
        Assertions.assertTrue(Ed25519Utils.verify(pub2, msg2, sig2), "向量 3：双字节消息");
        Assertions.assertArrayEquals(sig2, Ed25519Utils.signature(pri2, msg2), "向量 3：确定性签名一致");

        /** 篡改点选在编码仍合法的区间（篡改标量高位会令编码非法、验签引擎按异常处理而非返回 false）。 */
        byte[] tampered = sig.clone();
        tampered[0] ^= 0x01;
        Assertions.assertFalse(Ed25519Utils.verify(pub, new byte[0], tampered), "篡改 R 段首字节");
        tampered = sig.clone();
        tampered[40] ^= 0x01;
        Assertions.assertFalse(Ed25519Utils.verify(pub, new byte[0], tampered), "篡改 S 段中部字节");
        Assertions.assertArrayEquals(sig, Ed25519Utils.signature(pri, new byte[0]), "向量 1：确定性签名一致");
    }

    /** 与 BouncyCastle 跨实现互验：本类签的名 BC 可验、BC 签的名本类可验。 */
    @Test
    public void testSignWithJdkVerifyWithBcAndViceVersa() throws Exception {
        KeyPair bcKp = KeyPairGenerator.getInstance("Ed25519", new BouncyCastleProvider()).generateKeyPair();
        PrivateKey pri = Ed25519Keys.parsePrivateKeyBase64(Base64.toString(bcKp.getPrivate().getEncoded()));
        PublicKey pub = Ed25519Keys.parsePublicKeyBase64(Base64.toString(bcKp.getPublic().getEncoded()));
        byte[] data = "hello,world".getBytes(StandardCharsets.UTF_8);

        byte[] jdkSig = Ed25519Utils.signature(pri, data);
        Signature bcVerify = Signature.getInstance("Ed25519", new BouncyCastleProvider());
        bcVerify.initVerify(bcKp.getPublic());
        bcVerify.update(data);
        Assertions.assertTrue(bcVerify.verify(jdkSig), "JDK 签 → BC 验");

        Signature bcSign = Signature.getInstance("Ed25519", new BouncyCastleProvider());
        bcSign.initSign(bcKp.getPrivate());
        bcSign.update(data);
        Assertions.assertTrue(Ed25519Utils.verify(pub, data, bcSign.sign()), "BC 签 → JDK 验");
    }

    @Test
    public void testVerifyRejectsWrongKey() throws Exception {
        KeyPair signKp = newKeyPair();
        KeyPair otherKp = newKeyPair();
        byte[] sig = Ed25519Utils.signature(signKp.getPrivate(), "hello,world");
        Assertions.assertFalse(Ed25519Utils.verify(otherKp.getPublic(), "hello,world", sig));
    }

    @Test
    public void testVerifyRejectsTamperedPlaintext() throws Exception {
        KeyPair kp = newKeyPair();
        byte[] sig = Ed25519Utils.signature(kp.getPrivate(), "hello,world");
        Assertions.assertFalse(Ed25519Utils.verify(kp.getPublic(), "tampered", sig));
    }

    @Test
    public void testNullArgsRejected() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> Ed25519Utils.signature(null, (byte[]) null));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> Ed25519Utils.signature(null, (String) null));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> Ed25519Utils.signatureToBase64(null, null));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> Ed25519Utils.verify(null, (byte[]) null, null));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> Ed25519Utils.verify(null, (String) null, null));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> Ed25519Utils.verifyFromBase64(null, null, null));
    }

    private static String wrap(String singleLine) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < singleLine.length(); i += 64) {
            sb.append(singleLine, i, Math.min(i + 64, singleLine.length())).append('\n');
        }
        return sb.toString();
    }
}
