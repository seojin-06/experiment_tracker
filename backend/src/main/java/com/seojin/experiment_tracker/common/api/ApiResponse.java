package com.seojin.experiment_tracker.common.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private ErrorBody error;

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ErrorBody {
        private String code;
        private String msg;
    }

    public static <T> ApiResponse<T> ok(T data){
        return ApiResponse.<T>builder().success(true).data(data).build();
    }
    public static <T> ApiResponse<T> fail(String code, String message){
        return ApiResponse.<T>builder()
                .success(false)
                .error(ErrorBody.builder().code(code).msg(message).build())
                .build();
    }
}
