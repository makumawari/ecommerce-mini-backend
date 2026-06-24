package com.example.ecommerce.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Format JSON tra ve THONG NHAT cho moi loi, thay vi 500 + stacktrace lung tung.
 * Vi du response thuc te:
 * {
 *   "timestamp": "2026-06-21T10:30:00",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Khong tim thay san pham voi id: 99",
 *   "path": "/api/products/99"
 * }
 */
@Getter
@Builder
@AllArgsConstructor
public class ErrorResponse {

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    private int status;
    private String error;
    private String message;
    private String path;
}
