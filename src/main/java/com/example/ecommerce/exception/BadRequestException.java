package com.example.ecommerce.exception;

/**
 * Dung cho cac loi do INPUT/LOGIC nghiep vu sai, vi du:
 * - So luong dat hang <= 0
 * - Username da ton tai khi dang ky
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
