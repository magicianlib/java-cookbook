package io.ituknown.result;

import io.ituknown.jackson.JacksonUtils;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 请求基类，提供通用的请求追踪字段。
 *
 * @author magicianlib@gmail.com
 */
@Getter
@Setter
public abstract class AbstractRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = -4848830183604183658L;

    /** 客户端应用ID */
    private String appId;

    /** 请求ID */
    private String tranceId;

    /** 是否需要打印请求日志 */
    protected boolean needLog = true;

    @Override
    public String toString() {
        return JacksonUtils.toJson(this);
    }
}
