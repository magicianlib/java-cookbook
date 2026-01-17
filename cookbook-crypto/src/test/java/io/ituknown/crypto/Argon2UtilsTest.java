package io.ituknown.crypto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Argon2UtilsTest {
    @Test
    public void testRecommendedIterations() {
        int iterations = Argon2Utils.recommendedIterations();
        System.out.println("Optimal number of iterations: " + iterations);
        Assertions.assertTrue(iterations > 0);
    }

    @Test
    public void testEncrypt() {
        String pwd = Argon2Utils.encrypt("hello world", 10, Argon2Utils.DEFAULT_MEMORY, Argon2Utils.DEFAULT_PARALLELISM);
        System.out.println(pwd);
        Assertions.assertNotNull(pwd);
    }

    @Test
    public void testVerify() {
        boolean verification = Argon2Utils.verify("$argon2id$v=19$m=65536,t=186,p=32$/xUYiLE4HF9iTl6T2viyQg$97K5YJ6BweRacFTXNpLPk2q7su4zU9JpFM4CgXYmjBs", "hello world");
        Assertions.assertTrue(verification);
    }
}