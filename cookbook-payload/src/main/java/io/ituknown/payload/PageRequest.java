package io.ituknown.payload;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * 偏移量分页请求参数，对应 {@link Page} 响应。
 *
 * @author magicianlib@gmail.com
 */
@Getter
@Setter
public class PageRequest extends AbstractRequest {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 当前页码（从 1 开始） */
    @Min(1)
    private int current = 1;

    /** 每页数量 */
    @Min(1)
    private int pageSize = 10;
}