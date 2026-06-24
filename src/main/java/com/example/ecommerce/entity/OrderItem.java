package com.example.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    // "Chup" lai gia san pham tai THOI DIEM mua, vi gia san pham co the thay doi sau nay.
    // Day la diem rat hay bi hoi trong phong van: "Tai sao luu price o ca Product va OrderItem?"
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
}
