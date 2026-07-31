package io.ituknown.payload;

/**
 * 响应码接口，由各业务模块实现以定义自己的响应码枚举。
 * <p>
 * 通用响应码见 {@link ResultCodes}（SUCCESS = "00000", FAILURE = "00001"）。
 * <p>
 * 业务模块推荐按以下方式扩展：
 * <pre>{@code
 * public enum UserResultCode implements ResultCode {
 *
 *     // ---- 用户模块 10xxx ----
 *     USER_NOT_FOUND("10001", "用户不存在"),
 *     PASSWORD_ERROR("10002", "密码错误"),
 *     ACCOUNT_DISABLED("10003", "账号已禁用"),
 *     ;
 *
 *     final String code;
 *     final String message;
 *     final boolean success;
 *
 *     UserResultCode(String code, String message) {
 *         this(code, message, false);
 *     }
 *
 *     UserResultCode(String code, String message, boolean success) {
 *         this.code = code;
 *         this.message = message;
 *         this.success = success;
 *     }
 *
 *     @Override public String code() { return code; }
 *     @Override public String message() { return message; }
 *     @Override public boolean success() { return success; }
 * }
 * }</pre>
 * <p>
 * 使用：
 * <pre>{@code
 * // 直接使用通用响应码
 * Result<Void> ok = ResultUtils.success();
 * Result<Void> fail = ResultUtils.failure();
 *
 * // 使用业务响应码
 * Result<Void> result = ResultUtils.create(UserResultCode.USER_NOT_FOUND);
 * Result<Void> result = ResultUtils.create(UserResultCode.PASSWORD_ERROR, "密码错误，请重试");
 * }</pre>
 *
 * @author magicianlib@gmail.com
 * @see ResultCodes
 */
public interface ResultCode {

    /**
     * 响应码
     */
    String code();

    /**
     * 响应信息
     */
    String message();

    /**
     * 是否为成功状态码
     */
    boolean success();
}