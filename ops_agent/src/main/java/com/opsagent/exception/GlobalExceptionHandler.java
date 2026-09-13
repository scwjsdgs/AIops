package com.opsagent.exception;

import com.opsagent.dto.ApiResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.concurrent.TimeoutException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WebClientResponseException.class)
    public ApiResponse<?> handleWebClientError(WebClientResponseException e) {
        return ApiResponse.error(502, "Upstream service error: " + e.getMessage());
    }

    @ExceptionHandler(TimeoutException.class)
    public ApiResponse<?> handleTimeout(TimeoutException e) {
        return ApiResponse.error(504, "Request timeout");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<?> handleException(Exception e) {
        e.printStackTrace();
        return ApiResponse.error(500, "Internal Server Error: " + e.getMessage());
    }
}