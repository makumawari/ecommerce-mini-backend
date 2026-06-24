package com.example.ecommerce.exception;

/**
 * Throw exception nay khi tim 1 record (Product, Order, User...) theo id ma khong thay.
 * RuntimeException -> khong bat buoc try/catch hay khai bao throws, goi o dau cung duoc.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
