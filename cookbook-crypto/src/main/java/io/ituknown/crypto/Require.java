package io.ituknown.crypto;

/**
 * 参数校验工具：非空校验并原样返回受检值，便于链式赋值。
 */
public final class Require {

    private Require() {
    }

    /**
     * 校验 value 非 null，否则抛 {@link IllegalArgumentException}，并原样返回 value。
     *
     * @param value 待校验值
     * @param name  参数名（写入异常消息，便于定位）
     * @return 受检值本身
     */
    public static <T> T requireNonNull(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return value;
    }
}
