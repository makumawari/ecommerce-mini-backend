package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.ProductRequest;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.dto.response.ProductResponse;
import com.example.ecommerce.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * TIEU CHI 1: GET /api/products?page=0&size=10&sort=price,asc
     *
     * Pageable duoc Spring tu dong "bom" gia tri tu query param nho co @PageableDefault.
     * Vi du goi: GET /api/products?page=1&size=5&sort=price,desc
     * -> Spring tu tao Pageable(page=1, size=5, sort=price DESC) truyen vao day,
     * khong can tu parse query param thu cong.
     */
    @GetMapping
    public ResponseEntity<PageResponse<ProductResponse>> getAllProducts(
            @PageableDefault(page = 0, size = 10, sort = "id") Pageable pageable
    ) {
        Page<ProductResponse> result = productService.getAllProducts(pageable);
        return ResponseEntity.ok(PageResponse.from(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    /**
     * TIEU CHI 3: Chi ADMIN moi them/sua/xoa san pham.
     * @PreAuthorize chay TRUOC khi vao body method, neu khong du quyen -> throw AccessDeniedException
     * -> roi GlobalExceptionHandler bat va tra ve 403.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
