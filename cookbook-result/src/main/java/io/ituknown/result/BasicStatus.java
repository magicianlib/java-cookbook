package io.ituknown.result;

public enum BasicStatus implements Status {
    SUCCESS(0, "成功"),
    FAILURE(1, "失败"),
    ;

    final int code;
    final String message;

    BasicStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int code() {
        return this.code;
    }

    @Override
    public String message() {
        return this.message;
    }
}