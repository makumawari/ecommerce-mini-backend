package com.example.ecommerce.exception;

/**
 * Truong hop dac biet cua BadRequest: het hang.
 * Tach rieng ra de:
 * 1. Code goi noi nao bat duoc loi nay biet ro nguyen nhan ngay (khong can doc message)
 * 2. Co the xu ly khac biet trong GlobalExceptionHandler neu can (vi du tra them ma loi rieng)
 */
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
