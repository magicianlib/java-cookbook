package io.ituknown.payload;

import io.ituknown.validator.ValidatorUtils;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * 请求基类，提供通用的请求追踪字段。
 *
 * @author magicianlib@gmail.com
 */
@Getter
@Setter
public abstract class AbstractRequest extends PrintFriendliness {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 客户端应用ID
     */
    private String appId;

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 是否需要打印请求日志
     */
    protected boolean needLog = true;

    public void validate() {
        ValidatorUtils.validate(this);
    }
}