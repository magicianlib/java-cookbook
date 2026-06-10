package io.ituknown.payload;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * 游标分页请求参数，对应 {@link CursorPage} 响应。
 *
 * @param <C> 游标类型（如 {@code String}、{@code Long} 等）
 * @author magicianlib@gmail.com
 */
@Getter
@Setter
public class CursorPageRequest<C> extends AbstractRequest {
    @Serial
    private static final long serialVersionUID = 681913112366327366L;

    /** 游标，首次请求传 null */
    private C cursor;

    /** 每页数量 */
    @Min(1)
    private int pageSize = 10;
}