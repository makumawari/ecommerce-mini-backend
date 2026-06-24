package com.example.ecommerce.repository;

import com.example.ecommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /*
     * QUAN TRONG: JpaRepository<Product, Long> da co san method findAll(Pageable pageable)
     * tra ve Page<Product> - tu dong sinh ra cau SQL co LIMIT/OFFSET va ORDER BY.
     * Day chinh la thu thay the cho findAll() khong phan trang.
     *
     * Vi du Pageable duoc tao tu Controller:
     *   Pageable pageable = PageRequest.of(page, size, Sort.by("price").ascending());
     *   productRepository.findAll(pageable);
     *
     * Neu can loc theo ten san pham (tim kiem) + van phan trang, dung them method nay:
     */
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);
}
