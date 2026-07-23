package io.ituknown.crypto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Map;

public class HashWithEcdsaTest {

    private static KeyPair newKeyPair() throws Exception {
        return EcdsaKeys.generateKeyPair(EcdsaKeys.Curve.P256);
    }

    @Test
    public void testSignatureAndVerifyRoundTripBytes() throws Exception {
        KeyPair kp = newKeyPair();
        byte[] data = "hello,world".getBytes(StandardCharsets.UTF_8);
        byte[] sig = HashWithEcdsa.SHA256withECDSA.signature(kp.getPrivate(), data);
        Assertions.assertTrue(HashWithEcdsa.SHA256withECDSA.verify(kp.getPublic(), data, sig));
    }

    /** DER 是变长 ASN.1，长度应落在 P-256 签名典型区间 [8, 72]。 */
    @Test
    public void testDerSignatureIsVariableLength() throws Exception {
        KeyPair kp = newKeyPair();
        byte[] sig = HashWithEcdsa.SHA256withECDSA.signature(kp.getPrivate(), "hello,world");
        Assertions.assertTrue(sig.length >= 8 && sig.length <= 72, "DER len=" + sig.length);
    }

    /** RAW 是定长 r‖s，P-256 恰好 64 字节（JWT ES256 形态）。 */
    @Test
    public void testRawSignatureIsFixedLength() throws Exception {
        KeyPair kp = newKeyPair();
        byte[] sig = HashWithEcdsa.SHA256withECDSA.signature(kp.getPrivate(), "hello,world", HashWithEcdsa.Encoding.RAW);
        Assertions.assertEquals(64, sig.length);
    }

    @Test
    public void testRawRoundTripBytes() throws Exception {
        KeyPair kp = newKeyPair();
        byte[] data = "hello,world".getBytes(StandardCharsets.UTF_8);
        byte[] sig = HashWithEcdsa.SHA256withECDSA.signature(kp.getPrivate(), data, HashWithEcdsa.Encoding.RAW);
        Assertions.assertTrue(HashWithEcdsa.SHA256withECDSA.verify(kp.getPublic(), data, sig, HashWithEcdsa.Encoding.RAW));
    }

    @Test
    public void testSignatureToBase64AndVerifyFromBase64RoundTrip() throws Exception {
        KeyPair kp = newKeyPair();
        String sig = HashWithEcdsa.SHA256withECDSA.signatureToBase64(kp.getPrivate(), "hello,world");
        Assertions.assertTrue(HashWithEcdsa.SHA256withECDSA.verifyFromBase64(kp.getPublic(), "hello,world", sig));
    }

    @Test
    public void testRawBase64RoundTrip() throws Exception {
        KeyPair kp = newKeyPair();
        String sig = HashWithEcdsa.SHA256withECDSA.signatureToBase64(kp.getPrivate(), "hello,world", HashWithEcdsa.Encoding.RAW);
        Assertions.assertEquals(64, Base64.toByte(sig).length);
        Assertions.assertTrue(HashWithEcdsa.SHA256withECDSA.verifyFromBase64(kp.getPublic(), "hello,world", sig, HashWithEcdsa.Encoding.RAW));
    }

    /** DER 与 RAW 不可互换：把 DER 签名当作 RAW 验签，因字节长度不符（DER 非 2×域字节）而抛 SignatureException。 */
    @Test
    public void testDerAndRawNotInterchangeable() throws Exception {
        KeyPair kp = newKeyPair();
        byte[] data = "hello,world".getBytes(StandardCharsets.UTF_8);
        byte[] derSig = HashWithEcdsa.SHA256withECDSA.signature(kp.getPrivate(), data, HashWithEcdsa.Encoding.DER);
        Assertions.assertThrows(java.security.SignatureException.class,
                () -> HashWithEcdsa.SHA256withECDSA.verify(kp.getPublic(), data, derSig, HashWithEcdsa.Encoding.RAW));
    }

    @Test
    public void testVerifyRejectsWrongKey() throws Exception {
        KeyPair signKp = newKeyPair();
        KeyPair otherKp = newKeyPair();
        byte[] sig = HashWithEcdsa.SHA256withECDSA.signature(signKp.getPrivate(), "hello,world");
        Assertions.assertFalse(HashWithEcdsa.SHA256withECDSA.verify(otherKp.getPublic(), "hello,world", sig));
    }

    @Test
    public void testVerifyRejectsTamperedPlaintext() throws Exception {
        KeyPair kp = newKeyPair();
        byte[] sig = HashWithEcdsa.SHA256withECDSA.signature(kp.getPrivate(), "hello,world");
        Assertions.assertFalse(HashWithEcdsa.SHA256withECDSA.verify(kp.getPublic(), "tampered", sig));
    }

    @Test
    public void testNullArgsRejected() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> HashWithEcdsa.SHA256withECDSA.signature(null, (byte[]) null));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> HashWithEcdsa.SHA256withECDSA.signature(null, (byte[]) null, HashWithEcdsa.Encoding.DER));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> HashWithEcdsa.SHA256withECDSA.verify(null, (byte[]) null, null));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> HashWithEcdsa.SHA256withECDSA.verify(null, (byte[]) null, null, HashWithEcdsa.Encoding.DER));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> HashWithEcdsa.SHA256withECDSA.verifyFromBase64(null, null, null));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> HashWithEcdsa.SHA256withECDSA.verifyFromBase64(null, null, null, HashWithEcdsa.Encoding.RAW));
    }

    /** Base64 签名允许含换行（与 EcdsaKeys.parse*Base64 行为一致）。 */
    @Test
    public void testVerifyFromBase64AcceptsNewlines() throws Exception {
        KeyPair kp = newKeyPair();
        String sig = HashWithEcdsa.SHA256withECDSA.signatureToBase64(kp.getPrivate(), "hello,world");
        Assertions.assertTrue(HashWithEcdsa.SHA256withECDSA.verifyFromBase64(kp.getPublic(), "hello,world", wrap(sig)));
    }

    /** of() 按枚举常量名解析（忽略大小写）；未匹配或入参为 null 时返回 null，不抛异常。 */
    @Test
    public void testOf() {
        Assertions.assertEquals(HashWithEcdsa.SHA256withECDSA, HashWithEcdsa.of("SHA256withECDSA"));
        Assertions.assertEquals(HashWithEcdsa.SHA256withECDSA, HashWithEcdsa.of("sha256withecdsa"));
        Assertions.assertEquals(HashWithEcdsa.SHA512withECDSA, HashWithEcdsa.of("SHA512withECDSA"));
        Assertions.assertNull(HashWithEcdsa.of("unknown"));
        Assertions.assertNull(HashWithEcdsa.of(null));
    }

    /** 全部算法 × 全部曲线：DER 与 RAW 均可往返，且 RAW 长度 = 2 × 域字节。 */
    @Test
    public void testAllAlgorithmsAndCurvesRoundTrip() throws Exception {
        HashWithEcdsa[] algs = {HashWithEcdsa.SHA256withECDSA, HashWithEcdsa.SHA384withECDSA, HashWithEcdsa.SHA512withECDSA};
        Map<EcdsaKeys.Curve, Integer> fieldBytes = Map.of(
                EcdsaKeys.Curve.P256, 32,
                EcdsaKeys.Curve.P384, 48,
                EcdsaKeys.Curve.P521, 66,
                EcdsaKeys.Curve.SECP256K1, 32);
        for (EcdsaKeys.Curve curve : EcdsaKeys.Curve.values()) {
            KeyPair kp = EcdsaKeys.generateKeyPair(curve);
            int expectedRawLen = 2 * fieldBytes.get(curve);
            for (HashWithEcdsa alg : algs) {
                byte[] data = "hello,world".getBytes(StandardCharsets.UTF_8);
                byte[] der = alg.signature(kp.getPrivate(), data, HashWithEcdsa.Encoding.DER);
                Assertions.assertTrue(alg.verify(kp.getPublic(), data, der, HashWithEcdsa.Encoding.DER),
                        alg.name() + "/" + curve + " DER");
                byte[] raw = alg.signature(kp.getPrivate(), data, HashWithEcdsa.Encoding.RAW);
                Assertions.assertEquals(expectedRawLen, raw.length, alg.name() + "/" + curve + " RAW len");
                Assertions.assertTrue(alg.verify(kp.getPublic(), data, raw, HashWithEcdsa.Encoding.RAW),
                        alg.name() + "/" + curve + " RAW");
            }
        }
    }

    private static String wrap(String singleLine) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < singleLine.length(); i += 64) {
            sb.append(singleLine, i, Math.min(i + 64, singleLine.length())).append('\n');
        }
        return sb.toString();
    }
}
