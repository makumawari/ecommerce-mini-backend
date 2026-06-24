package com.example.ecommerce.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderRequest {

    @NotEmpty(message = "Don hang phai co it nhat 1 san pham")
    @Valid // bat validate ben trong tung OrderItemRequest cua list nay
    private List<OrderItemRequest> items;
}
