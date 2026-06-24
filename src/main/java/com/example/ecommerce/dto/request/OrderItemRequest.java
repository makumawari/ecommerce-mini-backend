package com.example.ecommerce.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemRequest {

    @NotNull(message = "productId khong duoc trong")
    private Long productId;

    @NotNull(message = "quantity khong duoc trong")
    @Min(value = 1, message = "So luong dat hang phai >= 1")
    private Integer quantity;
}
