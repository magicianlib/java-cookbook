package io.ituknown.result;

public interface Status {
    /**
     * 响应码
     */
    int code();

    /**
     * 响应信息
     */
    String message();
}