package com.example.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // Dung BigDecimal cho tien, TUYET DOI khong dung float/double (sai so lam tron)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @ManyToOne(fetch = FetchType.LAZY) // LAZY: chi load Category khi thuc su can, tranh load du lieu thua
    @JoinColumn(name = "category_id")
    private Category category;

    /**
     * Version dung cho Optimistic Locking - giai phap chong tinh trang
     * 2 request cung tru kho mot luc gay oversell (ban qua so luong ton).
     * Hibernate se tu dong check version moi lan UPDATE, neu version da bi
     * thay doi boi transaction khac -> throw OptimisticLockException.
     * Day la diem cong neu ban nhac toi trong buoi phong van.
     */
    @Version
    private Long version;
}
