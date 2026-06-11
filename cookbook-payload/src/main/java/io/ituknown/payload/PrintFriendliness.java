package io.ituknown.payload;

import io.ituknown.jackson.JacksonUtils;

import java.io.Serial;
import java.io.Serializable;

/**
 * 提供可读性更好的 {@link #toString()} 输出，格式为 {@code ClassName: json}。
 */
public class PrintFriendliness implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ": " + JacksonUtils.toJson(this);
    }
}
