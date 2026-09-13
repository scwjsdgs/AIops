package com.opsagent.dto;

import lombok.Data;

@Data
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;
    private long timestamp = System.currentTimeMillis();

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> res = new ApiResponse<>();
        res.setCode(200);
        res.setMessage("success");
        res.setData(data);
        return res;
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        ApiResponse<T> res = new ApiResponse<>();
        res.setCode(code);
        res.setMessage(message);
        return res;
    }
}