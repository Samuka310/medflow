package com.medflow.dto.generic;
import lombok.AllArgsConstructor;
import lombok.Getter;
@Getter
@AllArgsConstructor
public class ApiResult<T> {
    private boolean success;
    private String message;
    private T data;
    public static <T> ApiResult<T> success(T data) { return new ApiResult<>(true, null, data); }
    public static <T> ApiResult<T> success(T data, String message) { return new ApiResult<>(true, message, data); }
    public static <T> ApiResult<T> error(String message) { return new ApiResult<>(false, message, null); }
}
