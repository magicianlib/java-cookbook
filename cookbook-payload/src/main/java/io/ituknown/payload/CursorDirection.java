package io.ituknown.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 游标分页方向
 *
 * @author magicianlib@gmail.com
 */
public enum CursorDirection {
    /**
     * 向后翻页（下一页）
     */
    FORWARD,
    /**
     * 向前翻页（上一页）
     */
    BACKWARD;

    @JsonValue
    public String toValue() {
        return this.name().toLowerCase();
    }

    @JsonCreator
    public static CursorDirection fromValue(String value) {
        return CursorDirection.valueOf(value.toUpperCase());
    }
}