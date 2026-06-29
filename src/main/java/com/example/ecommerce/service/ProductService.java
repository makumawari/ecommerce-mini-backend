package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.ProductRequest;
import com.example.ecommerce.dto.response.ProductResponse;
import com.example.ecommerce.entity.Category;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.CategoryRepository;
import com.example.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TIEU CHI 1: Quan ly san pham voi Pagination & Sorting.
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Diem mau chot cua Tieu chi 1:
     * KHONG dung productRepository.findAll() (tra ve TOAN BO bang, voi 1 trieu san pham se
     * lam app cham/crash do load het vao memory + cau SQL khong co LIMIT).
     *
     * Thay vao do dung findAll(Pageable) - Spring Data JPA tu sinh SQL dang:
     *   SELECT * FROM products ORDER BY price ASC LIMIT 20 OFFSET 0
     * Pageable nay duoc tao tu Controller (page, size, sort lay tu query param cua client).
     */
    @Transactional(readOnly = true) // readOnly = true: Hibernate toi uu, khong can theo doi thay doi entity vi chi doc
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        Page<Product> productPage = productRepository.findAllWithCategory(pageable);
        return productPage.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = findProductOrThrow(id);
        return toResponse(product);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay category id: " + request.getCategoryId()));

        Product product = Product.builder()
                .name(request.getName())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .category(category)
                .build();

        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = findProductOrThrow(id);

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay category id: " + request.getCategoryId()));

        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(category);

        // Khong can goi productRepository.save() o day vi product la "managed entity"
        // (duoc Hibernate theo doi trong pham vi transaction) - Hibernate tu UPDATE khi transaction commit.
        // Tuy nhien goi save() ro rang cung khong sai, nhieu nguoi van goi cho de doc:
        return toResponse(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = findProductOrThrow(id);
        productRepository.delete(product);
    }

    private Product findProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay san pham voi id: " + id));
    }

    private ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .price(p.getPrice())
                .stockQuantity(p.getStockQuantity())
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .build();
    }
}
