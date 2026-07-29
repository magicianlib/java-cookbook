package io.ituknown.crypto.aes;

/** 分组家族构建器（CBC）：需选择填充模式，无认证标签。 */
public final class BlockAesBuilder extends AesBuilder<BlockAesBuilder> {

    BlockAesBuilder(AesMode mode) {
        super(mode);
    }

    public BlockAesBuilder padding(Padding padding) {
        this.padding = padding;
        return this;
    }
}
