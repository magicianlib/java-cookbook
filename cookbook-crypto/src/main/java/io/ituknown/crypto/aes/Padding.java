package io.ituknown.crypto.aes;

/**
 * AES 填充模式：转换串片段与是否必须依赖 BouncyCastle。
 * 对 AES（16 字节分组）而言，PKCS5 与 PKCS7 完全等价。
 */
public enum Padding {

    NONE("NoPadding", false),
    PKCS5("PKCS5Padding", false),
    PKCS7("PKCS7Padding", true),
    ISO7816("ISO7816-4Padding", true),
    ANSI_X9_23("X9.23Padding", true),
    ISO10126("ISO10126-2Padding", true),
    ZERO("ZeroBytePadding", true);

    final String transformation;
    final boolean requiresBc;

    Padding(String transformation, boolean requiresBc) {
        this.transformation = transformation;
        this.requiresBc = requiresBc;
    }
}
