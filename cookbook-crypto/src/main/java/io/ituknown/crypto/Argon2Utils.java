package io.ituknown.crypto;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import de.mkammerer.argon2.Argon2Helper;

/**
 * 密码工具类
 * <p>
 * 如果是 SpringBoot 项目推荐直接使用 spring-security-crypto
 *
 * @author magicianlib@gmail.com
 */
public enum Argon2Utils {
    ;
    private static final Argon2 ARGON2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);

    /**
     * 默认迭代次数
     * <p>
     * 可以使用 {@link #recommendedIterations()} 根据性能计算机器推荐迭代次数
     */
    public static final int DEFAULT_ITERATIONS = 10;
    /**
     * 最大使用内存
     * <p>
     * 64MB
     */
    public static final int DEFAULT_MEMORY = 64 * 1024;
    /**
     * 并行度
     * <p>
     * 机器核心数的 2 倍
     */
    public static final int DEFAULT_PARALLELISM = Runtime.getRuntime().availableProcessors() * 2;

    /**
     * 加密
     *
     * @param plaintext 明文密码
     * @return encrypt 密文
     */
    public static String encrypt(String plaintext) {
        return encrypt(plaintext, DEFAULT_ITERATIONS, DEFAULT_MEMORY, DEFAULT_PARALLELISM);
    }

    /**
     * 密码加密
     *
     * @param plaintext   明文密码
     * @param iterations  迭代次数
     * @param memory      最大使用内存（byte）
     * @param parallelism 并行度，推荐使用机器核心数的 2 倍
     * @return encrypt 密文
     */
    public static String encrypt(String plaintext, int iterations, int memory, int parallelism) {
        char[] passwordChars = plaintext.toCharArray();
        try {
            return ARGON2.hash(iterations, memory, parallelism, passwordChars);
        } finally {
            // 擦除内存中的明文密码
            ARGON2.wipeArray(passwordChars);
        }
    }

    /**
     * 密码验证
     *
     * @param ciphertext 密文
     * @param plaintext  明文
     */
    public static boolean verify(String ciphertext, String plaintext) {
        char[] passwordChars = plaintext.toCharArray();
        try {
            // Argon2 会自动从存储的 ciphertext 字符串中解析出盐值和参数进行比对
            return ARGON2.verify(ciphertext, passwordChars);
        } finally {
            // 擦除内存中的明文密码
            ARGON2.wipeArray(passwordChars);
        }
    }

    /**
     * 计算机器推荐迭代次数
     * <p>
     * 设定：encrypt 最大耗时 1000ms，最大使用 64MB 内存，并行度为机器核心数的 2 倍
     * </p>
     */
    public static int recommendedIterations() {
        return Argon2Helper.findIterations(ARGON2, 1000, DEFAULT_MEMORY, DEFAULT_PARALLELISM);
    }
}