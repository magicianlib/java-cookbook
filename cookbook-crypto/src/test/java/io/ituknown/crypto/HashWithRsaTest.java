package io.ituknown.crypto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

public class HashWithRsaTest {

    private static java.security.KeyPair newKeyPair() throws Exception {
        java.security.KeyPairGenerator g = java.security.KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        return g.generateKeyPair();
    }

    @Test
    public void testSignatureAndVerifyRoundTripBytes() throws Exception {
        java.security.KeyPair kp = newKeyPair();
        byte[] data = "hello,world".getBytes(StandardCharsets.UTF_8);
        byte[] sig = HashWithRsa.SHA256withRSA.signature(kp.getPrivate(), data);
        Assertions.assertTrue(HashWithRsa.SHA256withRSA.verify(kp.getPublic(), data, sig));
    }

    @Test
    public void testSignatureToBase64AndVerifyFromBase64RoundTrip() throws Exception {
        java.security.KeyPair kp = newKeyPair();
        String sig = HashWithRsa.SHA256withRSA.signatureToBase64(kp.getPrivate(), "hello,world");
        Assertions.assertTrue(HashWithRsa.SHA256withRSA.verifyFromBase64(kp.getPublic(), "hello,world", sig));
    }

    @Test
    public void testVerifyRejectsWrongKey() throws Exception {
        java.security.KeyPair signKp = newKeyPair();
        java.security.KeyPair otherKp = newKeyPair();
        byte[] sig = HashWithRsa.SHA256withRSA.signature(signKp.getPrivate(), "hello,world");
        Assertions.assertFalse(HashWithRsa.SHA256withRSA.verify(otherKp.getPublic(), "hello,world", sig));
    }

    @Test
    public void testVerifyRejectsTamperedPlaintext() throws Exception {
        java.security.KeyPair kp = newKeyPair();
        byte[] sig = HashWithRsa.SHA256withRSA.signature(kp.getPrivate(), "hello,world");
        Assertions.assertFalse(HashWithRsa.SHA256withRSA.verify(kp.getPublic(), "tampered", sig));
    }

    @Test
    public void testNullArgsRejected() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> HashWithRsa.SHA256withRSA.signature(null, (byte[]) null));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> HashWithRsa.SHA256withRSA.verify(null, (byte[]) null, null));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> HashWithRsa.SHA256withRSA.verifyFromBase64(null, null, null));
    }

    /** Base64 签名允许含换行（与 RsaKeys.parse*Base64 行为一致）。 */
    @Test
    public void testVerifyFromBase64AcceptsNewlines() throws Exception {
        java.security.KeyPair kp = newKeyPair();
        String sig = HashWithRsa.SHA256withRSA.signatureToBase64(kp.getPrivate(), "hello,world");
        StringBuilder multiLine = new StringBuilder();
        for (int i = 0; i < sig.length(); i += 64) {
            multiLine.append(sig, i, Math.min(i + 64, sig.length())).append('\n');
        }
        Assertions.assertTrue(
                HashWithRsa.SHA256withRSA.verifyFromBase64(kp.getPublic(), "hello,world", multiLine.toString()));
    }

    /** @Deprecated 算法仍可工作（兼容旧数据）。 */
    @SuppressWarnings("deprecation")
    @Test
    public void testDeprecatedAlgorithmsStillWork() throws Exception {
        java.security.KeyPair kp = newKeyPair();
        for (HashWithRsa alg : new HashWithRsa[]{HashWithRsa.MD5withRSA, HashWithRsa.SHA1withRSA}) {
            byte[] sig = alg.signature(kp.getPrivate(), "hello,world");
            Assertions.assertTrue(alg.verify(kp.getPublic(), "hello,world", sig), alg.name());
        }
    }

    /** of() 按枚举常量名解析（忽略大小写）；未匹配或入参为 null 时返回 null，不抛异常。 */
    @SuppressWarnings("deprecation")
    @Test
    public void testOf() {
        Assertions.assertEquals(HashWithRsa.SHA256withRSA, HashWithRsa.of("SHA256withRSA"));
        Assertions.assertEquals(HashWithRsa.SHA256withRSA, HashWithRsa.of("sha256withrsa"));
        Assertions.assertEquals(HashWithRsa.SHA512withRSA, HashWithRsa.of("SHA512withRSA"));
        Assertions.assertEquals(HashWithRsa.MD5withRSA, HashWithRsa.of("MD5withRSA"));
        Assertions.assertEquals(HashWithRsa.SHA1withRSA, HashWithRsa.of("SHA1withRSA"));
        // 不存在
        Assertions.assertNull(HashWithRsa.of("unknown"));
        // null 入参不抛异常
        Assertions.assertNull(HashWithRsa.of(null));
    }
}
