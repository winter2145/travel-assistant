package com.xin.aiagentbackend.api;

public record ApiResponse<T>(boolean success, String code, String message, T data) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", "ok", data);
    }
}
