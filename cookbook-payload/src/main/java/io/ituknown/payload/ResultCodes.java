package io.ituknown.payload;

/**
 * 通用响应码，提供开箱即用的成功 / 失败响应码。
 * <p>
 * 业务模块如需自定义响应码，请实现 {@link ResultCode} 接口。
 *
 * @author magicianlib@gmail.com
 * @see ResultCode
 */
public enum ResultCodes implements ResultCode {
    /**
     * 请求成功。
     * <p>用于接口正常返回的场景。
     */
    SUCCESS("000000", "success", true),

    /**
     * 参数错误 / 请求不合法。
     * <p>用于请求参数缺失、格式错误、值越界、类型不匹配等入参校验失败的场景。
     *
     * @see io.ituknown.payload.exception.BizArgumentException
     */
    BAD_REQUEST("000400", "bad request"),

    /**
     * 未认证（未登录）。
     * <p>用于未携带凭证、Token 过期、签名无效等身份校验失败的场景。
     *
     * @see io.ituknown.payload.exception.BizUnauthorizedException
     */
    UNAUTHORIZED("000401", "unauthorized"),

    /**
     * 无权限（已认证但权限不足）。
     * <p>用于已登录用户访问超出其角色 / 权限范围的资源的场景。
     *
     * @see io.ituknown.payload.exception.BizForbiddenException
     */
    FORBIDDEN("000403", "forbidden"),

    /**
     * 资源未找到。
     * <p>用于根据标识查询数据不存在、接口路径不存在等场景。
     *
     * @see io.ituknown.payload.exception.BizNotFoundException
     */
    NOT_FOUND("000404", "not found"),

    /**
     * 资源冲突。
     * <p>用于唯一约束冲突、并发修改版本不匹配、重复创建等场景。
     *
     * @see io.ituknown.payload.exception.BizConflictException
     */
    CONFLICT("000409", "conflict"),

    /**
     * 通用业务失败。
     * <p>用于不属于上述分类的通用业务逻辑不满足的场景，
     * 如业务前置条件不成立、状态流转不合法等。
     *
     * @see io.ituknown.payload.exception.BizException
     */
    FAILURE("000500", "failure"),
    ;

    private final String code;
    private final String message;
    private final boolean success;

    ResultCodes(String code, String message) {
        this(code, message, false);
    }

    ResultCodes(String code, String message, boolean success) {
        this.code = code;
        this.message = message;
        this.success = success;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public boolean success() {
        return success;
    }
}
