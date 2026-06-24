package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.ProductRequest;
import com.example.ecommerce.dto.response.ProductResponse;
import com.example.ecommerce.entity.Category;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.CategoryRepository;
import com.example.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("Lay danh sach san pham co Pagination tra ve dung du lieu")
    void getAllProducts_ReturnsPagedResult() {
        Category category = Category.builder().id(1L).name("Electronics").build();
        Product product = Product.builder()
                .id(1L).name("Laptop").price(new BigDecimal("1000")).stockQuantity(5).category(category)
                .build();

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<Product> page =
                new org.springframework.data.domain.PageImpl<>(List.of(product), pageable, 1);

        when(productRepository.findAll(pageable)).thenReturn(page);

        org.springframework.data.domain.Page<ProductResponse> result = productService.getAllProducts(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Laptop");
        assertThat(result.getContent().get(0).getCategoryName()).isEqualTo("Electronics");
    }

    @Test
    @DisplayName("Tao san pham THAT BAI khi category khong ton tai")
    void createProduct_ThrowsException_WhenCategoryNotFound() {
        ProductRequest request = new ProductRequest();
        request.setName("Chuot may tinh");
        request.setPrice(new BigDecimal("99.99"));
        request.setStockQuantity(10);
        request.setCategoryId(999L);

        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("category");

        verify(productRepository, org.mockito.Mockito.never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Lay san pham theo id KHONG ton tai phai throw ResourceNotFoundException")
    void getProductById_ThrowsException_WhenNotFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("id: 1");
    }
}
