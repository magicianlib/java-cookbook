package io.ituknown.crypto.aes;

/** AEAD 家族构建器（GCM/CCM/OCB）：有认证标签长度配置，无填充。 */
public final class AeadAesBuilder extends AesBuilder<AeadAesBuilder> {

    AeadAesBuilder(AesMode mode) {
        super(mode);
    }

    public AeadAesBuilder tagBits(int tagBits) {
        this.tagBits = tagBits;
        return this;
    }
}
