package com.example.ecommerce.repository;

import com.example.ecommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /*
     * FIX N+1: JOIN FETCH p.category ép Hibernate lấy luôn category
     * trong CÙNG 1 câu SQL (JOIN), thay vì để Product.category ở dạng
     * proxy rỗng rồi phải query riêng từng cái khi code gọi getCategory().
     *
     * An toàn để dùng cùng Pageable vì category là quan hệ @ManyToOne
     * (mỗi product chỉ có 1 category) -> JOIN không làm nhân dòng kết quả,
     * LIMIT/OFFSET vẫn được DB xử lý đúng, không bị cảnh báo
     * "firstResult/maxResults specified with collection fetch".
     */
    @Query("SELECT p FROM Product p JOIN FETCH p.category")
    Page<Product> findAllWithCategory(Pageable pageable);
    
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);
}
