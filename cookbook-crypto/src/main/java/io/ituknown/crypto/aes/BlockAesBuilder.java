package io.ituknown.crypto.aes;

/** 分组家族构建器（CBC）：需选择填充模式，无认证标签。 */
public final class BlockAesBuilder extends AesBuilder<BlockAesBuilder> {

    BlockAesBuilder(AesMode mode) {
        super(mode);
    }

    /**
     * 设置分组填充模式。分组加密要求明文为整块倍数，不足时按所选模式补齐；默认无填充，非块对齐明文会直接失败，故通常需显式指定。
     *
     * @param padding 填充模式
     * @return 当前构建器
     */
    public BlockAesBuilder padding(Padding padding) {
        this.padding = padding;
        return this;
    }
}
