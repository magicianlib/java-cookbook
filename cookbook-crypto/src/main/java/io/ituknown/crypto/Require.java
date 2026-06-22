package io.ituknown.crypto;

/**
 * 包级参数校验工具（仅 {@code io.ituknown.crypto} 包内使用，不对外暴露）。
 */
final class Require {

    private Require() {
    }

    /**
     * 校验 value 非 null，否则抛 {@link IllegalArgumentException}。
     *
     * @param value 待校验值
     * @param name  参数名（写入异常消息，便于定位）
     */
    static void requireNonNull(Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
    }
}
