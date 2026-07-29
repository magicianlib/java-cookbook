package io.ituknown.crypto.aes;

/** 流式家族构建器（CTR/CFB/OFB）：既无填充也无认证标签。 */
public final class StreamAesBuilder extends AesBuilder<StreamAesBuilder> {

    StreamAesBuilder(AesMode mode) {
        super(mode);
    }
}
