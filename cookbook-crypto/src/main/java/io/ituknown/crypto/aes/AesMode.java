package io.ituknown.crypto.aes;

/**
 * AES 工作模式元数据：转换串片段、所属家族、推荐初始化向量长度、是否必须依赖 BouncyCastle。
 */
enum AesMode {

    GCM("GCM", Family.AEAD, 12, false),
    CCM("CCM", Family.AEAD, 12, true),
    OCB("OCB", Family.AEAD, 12, true),
    CBC("CBC", Family.BLOCK, 16, false),
    CTR("CTR", Family.STREAM, 16, false),
    CFB("CFB", Family.STREAM, 16, false),
    OFB("OFB", Family.STREAM, 16, false);

    enum Family { AEAD, BLOCK, STREAM }

    final String transformation;
    final Family family;
    final int ivLength;
    final boolean requiresBc;

    AesMode(String transformation, Family family, int ivLength, boolean requiresBc) {
        this.transformation = transformation;
        this.family = family;
        this.ivLength = ivLength;
        this.requiresBc = requiresBc;
    }
}
