package com.example.ecommerce.entity;

/**
 * Enum phan quyen. Dung enum thay vi String "ADMIN"/"USER" de:
 * - Tranh loi danh may (typo) khi so sanh chuoi
 * - IDE ho tro autocomplete, compiler check loi luc compile
 */
public enum Role {
    ADMIN,
    USER
}
