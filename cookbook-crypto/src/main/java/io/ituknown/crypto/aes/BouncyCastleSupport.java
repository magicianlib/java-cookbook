package io.ituknown.crypto.aes;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

/**
 * BouncyCastle provider 幂等懒注册：仅当尚未注册时加锁添加，避免重复与并发重复注入。
 */
final class BouncyCastleSupport {

    private BouncyCastleSupport() {
    }

    static void ensureRegistered() {
        if (Security.getProvider("BC") == null) {
            synchronized (BouncyCastleSupport.class) {
                if (Security.getProvider("BC") == null) {
                    Security.addProvider(new BouncyCastleProvider());
                }
            }
        }
    }
}
