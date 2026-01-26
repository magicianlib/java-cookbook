package io.ituknown.result;

/**
 * 响应码接口
 * <p>
 * 使用示例:
 * <pre>{@code
 * public enum ExampleErrorCode implements ErrorCode {
 *
 *     // 根据各自服务业务需求在这里定义扩展响应码
 *
 *     SUCCESS(0, "Success", true),
 *
 *     // 如用户系统, 用户密码错误
 *     USER_OR_PASSWORD_ERROR(10001, "用户名或密码错误"),
 *
 *     // other
 *     ;
 *
 *     // 下面的代码不需要变化, 直接拷贝即可
 *
 *     final int code;
 *     final String message;
 *     final boolean success;
 *
 *     ExampleErrorCode(int code, String message) {
 *         this(code, message, false);
 *     }
 *
 *     ExampleErrorCode(int code, String message, boolean success) {
 *         this.code = code;
 *         this.message = message;
 *         this.success = success;
 *     }
 *
 *     @Override
 *     public int code() {
 *         return code;
 *     }
 *
 *     @Override
 *     public String message() {
 *         return message;
 *     }
 *
 *     @Override
 *     public boolean success() {
 *         return success;
 *     }
 * }
 * }</pre>
 *
 * @author magicianlib@gmail.com
 */
public interface ErrorCode {

    /**
     * 响应码
     */
    int code();

    /**
     * 响应信息
     */
    String message();

    /**
     * 是否成功为状态码
     */
    boolean success();
}