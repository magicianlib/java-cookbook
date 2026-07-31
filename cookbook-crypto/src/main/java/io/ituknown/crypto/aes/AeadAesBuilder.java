package io.ituknown.crypto.aes;

/**
 * AEAD 家族构建器（GCM/CCM/OCB）：有认证标签长度配置，无填充。
 */
public final class AeadAesBuilder extends AesBuilder<AeadAesBuilder> {

    AeadAesBuilder(AesMode mode) {
        super(mode);
    }

    /**
     * 设置认证标签位数。认证标签是带认证加密模式在密文末尾附加的完整性校验值，解密时据此验证密文未被篡改且来源持有正确密钥；位数越长强度越高。默认 128 位；GCM 支持 96/104/112/120/128 位，CCM/OCB 常取 128 位。
     *
     * @param tagBits 标签位数
     * @return 当前构建器
     */
    public AeadAesBuilder tagBits(int tagBits) {
        this.tagBits = tagBits;
        return this;
    }
}