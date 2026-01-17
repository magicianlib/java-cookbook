package io.ituknown.crypto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RsaUtilsTest {

    static final String PLAINTEXT = "hello,world";
    static final String BASE64_CIPHERTEXT = "R4FCsRNxgH2fNf7xsFLMwuSJycwDLO0qBfsdNrrrqZWmLJjjNsHFXfx9iqr71vAFelBFVltwd7ze99cfFCQHxYxYTkiZiN4n+4hdoqk8STIE80eFWgt8dSDrxYqNncUWnCPWQkRayh2pWLkj3/MmbCVKrVD1XnR7ggvXPX5GBmgSEG0tNlHfvlS6GBgPMASCcP5HI42B6cBGaEZxp1+ukEqhQQ7HrcZDQOPvZAAoPEemiNDLRDAQGbDw4QK3SqFScU//qYnggixDRGGmdDDe2ipDgUD4TF1n3wTYNnTUM3ZKsZhOIvAKFn1qHi5AjfN3wNEoEie8oFR+9hUlfaof/g==";

    static final String BASE64_PRI_KEY = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCzew3wmb0kQTeamGzjxWnCNOPJ7ssxFwPx5WADZD8GDj0lP12THirZjhdVA39yQ/MH1/t4R7FIUVx1B3iNMVWtBSU8TtuZOB3dc2XIUFr0wNI2jIWM+aksJW05NiCoLOoeGVthijTYNIOezpYgkCLPoBk0dbuEufbpSz9guePB6KqSqYb5eNTxKZVq2J3s1YBPpyTPlyGIH0Hb3nm1rw5/++UTvpMrz5jWJkP/OBYCt+jNYyq2EiaM+00OmuXA1NtBa/wngO52cpLqfrf9/CixQPbhFH1yODHOX0W15Ilm54fORhHDfvKEVkpUr1FEJww1oaovEkgtQl0pO5f1lqsPAgMBAAECggEASQ4pmmm5p2w0aeTpTb0pUzecELH2W0D3BhtLfZXsYVaiZOrp99wrrOo9bQlDcwtdfS/Y2Bi2fK1BaRFlbHNMYGTbxzSCBlflMCKsp2Ct5TonVrTUF5H33lRNpR/3Iv1FPoMrwwQyhl65xIZqbU6+HK0aFVRmw5u+eS972K9RmbeePiPxKW/UsomLtgoxFBi74aEXlXe+jB8MALkloTCLw//dyL+h643AeQBZUO2L80zr4TAG4jKHjOW0xQw3qrBzQyK6rHqF1AhZmgmPbjIAzUN531cB7ZNWqeyEDJN+XV5p3K8XyWze7hM8YrlU2rIJ2S7mDrvv4PRsznqCEZc3YQKBgQC3nmiyETF46O6+CuV3Q/ernib6usYpfL/2gR+8y5JrACW+iYTICg5rBfz2JWea8D/7xdm/pcJGr1TZlVQRdlcvrQfL8K/limObOPZxLSjQZpdvJBt1N9/V/35J7+pJpxmHgzvYOzDoBiB7u8Qed9Vg7Lz228KfvS2NfIZL6NNNLQKBgQD6Ow403ErosjjwVH4KxN8PNc9OSy2YdpIUHlBG9/LTl62Y4zsI3YGmIOs19k7hGHLKOyEVNcOENEbQa2jfDLnNoew2hGJh9FQ9V5nETcKUX6gx6sxC4IIox78vM2wJdl0dF3pFRhTrGliPxzJLXYH0Q9uIjBDVHHjx4zlDy7lWqwKBgQCwP1MLKMmt3xfaPqdHNWwzxNhxbnnrNJ0lxYdrNpPSNRAFtgZH5K82N0c6FWk/JUClMKHz/O1f38e4GkfZgxfo4VNMhDiyQYWeZqzWsZwtfWv5+FSKzRkDVfwoiCsAi25LzsHQqfAlpkvjuLVk8W1VXad75DEKFxH0bwSRNGgt+QKBgGlS7CLhyoHxajf2SGs9/GmJi613xURUPB5NuBp3CPV49W/RzSppGcYUOwymlQL36HEovD8SNy5xVpEpdKXV1GsySZuU01hJoB+FvMo8tLcBIGmKW9mWaBLEKLu0WDgPYxf4ptV7rxhaKYazIH9KZ4Wp/kCWPAuHhXzC2HqpyGQrAoGAAYuA4DxO6HfiNCeHl8myC41PitOiojHWq7w2OD910mtxtavI0o978tHcsCtyc0uZofmx9V7BZiTRYrdvCdVjQM+52VR3UxoyYQx0Pc5tSIPH/cHvSe9cNz6f3mmrzLiwZjnMtXS6oNC3FKuO4M2IBI7jxrRxY0qAyK3XWBWCXMc=";
    static final String BASE64_PUB_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAs3sN8Jm9JEE3mphs48VpwjTjye7LMRcD8eVgA2Q/Bg49JT9dkx4q2Y4XVQN/ckPzB9f7eEexSFFcdQd4jTFVrQUlPE7bmTgd3XNlyFBa9MDSNoyFjPmpLCVtOTYgqCzqHhlbYYo02DSDns6WIJAiz6AZNHW7hLn26Us/YLnjweiqkqmG+XjU8SmVatid7NWAT6ckz5chiB9B2955ta8Of/vlE76TK8+Y1iZD/zgWArfozWMqthImjPtNDprlwNTbQWv8J4DudnKS6n63/fwosUD24RR9cjgxzl9FteSJZueHzkYRw37yhFZKVK9RRCcMNaGqLxJILUJdKTuX9ZarDwIDAQAB";

    @Test
    public void testGenerateBase64KeyPair() throws Exception {
        RsaUtils.Pair pair = RsaUtils.generateBase64KeyPair(2048);

        System.out.println("PrivateKey: " + pair.privateKey());
        System.out.println("PublicKey: " + pair.publicKey());
        Assertions.assertNotNull(pair);
    }

    @Test
    public void testEncrypt() throws Exception {
        String encrypted = RsaUtils.encryptToBase64String(PLAINTEXT, BASE64_PUB_KEY);
        System.out.println(encrypted);
        Assertions.assertNotNull(encrypted);
    }

    @Test
    public void testDecrypt() throws Exception {
        String plaintext = RsaUtils.decryptBase64(BASE64_CIPHERTEXT, BASE64_PRI_KEY, String::new);
        System.out.println(plaintext);
        Assertions.assertNotNull(plaintext);
    }
}