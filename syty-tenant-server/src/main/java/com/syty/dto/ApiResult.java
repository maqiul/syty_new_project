package com.syty.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应包装
 *
 * 前后端约定:
 * - code: 200 成功, 其他错误码
 * - msg: 消息描述
 * - data: 数据体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    private int code;

    @JsonProperty("msg")
    private String message;

    private T data;

    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "success", data);
    }

    public static <T> ApiResult<T> success() {
        return new ApiResult<>(200, "success", null);
    }

    public static <T> ApiResult<T> error(int code, String message) {
        return new ApiResult<>(code, message, null);
    }

    public static <T> ApiResult<T> error(String message) {
        return new ApiResult<>(500, message, null);
    }
}
