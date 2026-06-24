package com.example.ecommerce.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductRequest {

    @NotBlank(message = "Ten san pham khong duoc trong")
    private String name;

    @NotNull(message = "Gia khong duoc trong")
    @DecimalMin(value = "0.0", inclusive = false, message = "Gia phai > 0")
    private BigDecimal price;

    @NotNull(message = "So luong ton khong duoc trong")
    @Min(value = 0, message = "So luong ton phai >= 0")
    private Integer stockQuantity;

    @NotNull(message = "Phai chon category")
    private Long categoryId;
}
