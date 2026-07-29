package io.ituknown.crypto.aes;

/**
 * 对称加解密入口：按加密家族返回对应构建器，在编译期阻止非法组合。
 * 认证加密家族（GCM/CCM/OCB）构建器可配置认证标签长度、不接受填充；
 * 分组家族（CBC）构建器须选择填充、无认证标签；
 * 流式家族（CTR/CFB/OFB）两者皆不接受。调用方无法表达"认证加密配填充"
 * 或"分组配认证标签"等非法组合——此类调用无法通过编译。
 */
public final class Aes {

    private Aes() {
    }

    public static AeadAesBuilder gcm() {
        return new AeadAesBuilder(AesMode.GCM);
    }

    public static AeadAesBuilder ccm() {
        return new AeadAesBuilder(AesMode.CCM);
    }

    public static AeadAesBuilder ocb() {
        return new AeadAesBuilder(AesMode.OCB);
    }

    public static BlockAesBuilder cbc() {
        return new BlockAesBuilder(AesMode.CBC);
    }

    public static StreamAesBuilder ctr() {
        return new StreamAesBuilder(AesMode.CTR);
    }

    public static StreamAesBuilder cfb() {
        return new StreamAesBuilder(AesMode.CFB);
    }

    public static StreamAesBuilder ofb() {
        return new StreamAesBuilder(AesMode.OFB);
    }
}
