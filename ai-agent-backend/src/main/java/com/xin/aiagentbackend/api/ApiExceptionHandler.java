package com.xin.aiagentbackend.api;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import com.xin.aiagentbackend.tools.common.AmapApiException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApi(ApiException error) {
        return ResponseEntity.status(error.getStatus())
                .body(new ApiResponse<>(false, error.getCode(), error.getMessage(), null));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class,
            HttpMessageNotReadableException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiResponse<Void>> handleInvalid(Exception error) {
        String message = error instanceof MethodArgumentNotValidException invalid
                ? invalid.getBindingResult().getFieldErrors().stream()
                    .findFirst().map(e -> e.getField() + ": " + e.getDefaultMessage()).orElse("请求参数无效")
                : error instanceof ConstraintViolationException violation
                    ? violation.getConstraintViolations().stream().findFirst()
                        .map(v -> v.getMessage()).orElse("请求参数无效")
                    : "请求参数无效或 JSON 格式错误";
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "INVALID_REQUEST", message, null));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDatabase(DataAccessException error) {
        log.error("Business database operation failed", error);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "STORAGE_ERROR", "数据暂时无法保存或读取，请稍后重试", null));
    }

    @ExceptionHandler(AmapApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleAmap(AmapApiException error) {
        log.warn("Amap request could not be completed: {}", error.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiResponse<>(false, "MAP_SERVICE_UNAVAILABLE", error.getMessage(), null));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleStatus(ResponseStatusException error) {
        HttpStatus status = HttpStatus.valueOf(error.getStatusCode().value());
        String message = error.getReason() == null ? "请求失败" : error.getReason();
        return ResponseEntity.status(status)
                .body(new ApiResponse<>(false, status.name(), message, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception error) {
        log.error("Unhandled business API error", error);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "INTERNAL_ERROR", "服务暂时不可用，请稍后重试", null));
    }
}
