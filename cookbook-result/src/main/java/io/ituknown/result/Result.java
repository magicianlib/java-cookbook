package io.ituknown.result;

import io.ituknown.jackson.JacksonUtils;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一响应结果封装
 * <p>
 * JSON 结构：
 * <pre>{@code
 * {
 *   "code": 200,
 *   "msg": "success",
 *   "data": {}
 * }
 * }</pre>
 *
 * @param <T> 数据类型
 * @author magicianlib@gmail.com
 */
@Getter
@Setter
public class Result<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 6820965203253182123L;

    /** 状态码 */
    private int code;
    /** 响应信息 */
    private String msg;
    /** 响应数据 */
    private T data;

    @Override
    public String toString() {
        try {
            return JacksonUtils.toJson(this);
        } catch (Exception e) {
            return "Result{code=" + code + ", msg='" + msg + "', data=" + data + '}';
        }
    }
}
