package com.example.settlement.presentation.common;

import com.example.settlement.domain.common.exception.ErrorCode;

public record ApiResponse<T>(
        boolean success,
        T data,
        String errorCode,
        String message
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static ApiResponse<Void> fail(ErrorCode code) {
        return new ApiResponse<>(false, null, code.getCode(), code.getMessage());
    }

    public static ApiResponse<Void> fail(String message) {
        return new ApiResponse<>(false, null, null, message);
    }
}
