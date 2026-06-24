package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.OrderItemRequest;
import com.example.ecommerce.dto.request.OrderRequest;
import com.example.ecommerce.dto.response.OrderResponse;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.exception.InsufficientStockException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.*;

/**
 * TIEU CHI 5: Unit Test cho OrderService.
 *
 * Trong Unit Test, KHONG ket noi DB thuc su. Ta "gia lap" (mock) cac Repository
 * de chi tap trung kiem tra LOGIC trong Service co dung khong - day la diem khac biet
 * voi Integration Test (Integration Test moi can DB thuc/H2 thuc).
 *
 * @ExtendWith(MockitoExtension.class): cho phep dung @Mock, @InjectMocks cua Mockito trong JUnit 5.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    // @InjectMocks: Mockito tu dong tao OrderService va "tiem" 3 mock o tren vao
    // (giong nhu @RequiredArgsConstructor cua Lombok lam trong code thuc, nhung o day la cho test)
    @InjectMocks
    private OrderService orderService;

    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).username("user1").build();

        testProduct = Product.builder()
                .id(100L)
                .name("Laptop Dell XPS")
                .price(new BigDecimal("1500.00"))
                .stockQuantity(10) // ton kho ban dau: 10
                .build();
    }

    @Test
    @DisplayName("Tao don hang THANH CONG khi du hang trong kho")
    void createOrder_Success_WhenStockIsSufficient() {
        // ---- GIVEN: chuan bi du lieu va "day" mock tra ve gi khi duoc goi ----
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(100L);
        itemRequest.setQuantity(3); // mua 3 cai, con 10 -> du hang

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setItems(List.of(itemRequest));

        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(testUser));
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
        // Khi orderRepository.save(...) duoc goi, gia lap tra ve chinh doi tuong Order duoc truyen vao,
        // kem id = 1L de mo phong hanh vi cua DB thuc (tu sinh id sau khi INSERT)
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        // ---- WHEN: goi method can test ----
        OrderResponse response = orderService.createOrder("user1", orderRequest);

        // ---- THEN: kiem tra ket qua ----
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("CONFIRMED");
        assertThat(response.getTotalPrice()).isEqualByComparingTo("4500.00"); // 1500 * 3
        assertThat(response.getItems()).hasSize(1);

        // Kiem tra LOGIC TRU KHO co chay dung khong: 10 - 3 = 7
        assertThat(testProduct.getStockQuantity()).isEqualTo(7);

        // Xac nhan orderRepository.save() THUC SU duoc goi dung 1 lan (don hang da duoc luu)
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Tao don hang THAT BAI khi het hang - phai throw InsufficientStockException")
    void createOrder_ThrowsException_WhenStockIsInsufficient() {
        // ---- GIVEN ----
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(100L);
        itemRequest.setQuantity(20); // mua 20 cai nhung kho chi co 10 -> KHONG du hang

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setItems(List.of(itemRequest));

        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(testUser));
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));

        // ---- WHEN + THEN ----
        // assertThatThrownBy: khang dinh rang goi method nay PHAI throw dung loai exception mong doi
        assertThatThrownBy(() -> orderService.createOrder("user1", orderRequest))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("khong du hang");

        // QUAN TRONG: vi co loi xay ra, orderRepository.save() KHONG DUOC PHEP goi toi -
        // day chinh la cach unit test xac nhan "rollback se xay ra" (trong thuc te @Transactional
        // cua Spring se rollback con cac thay doi da UPDATE truoc do, o day ta xac nhan
        // logic chua di den buoc luu don hang khi phat hien het hang).
        verify(orderRepository, never()).save(any(Order.class));

        // So luong ton khong bi tru (van la 10) vi loi xay ra TRUOC khi tru kho cho item nay
        assertThat(testProduct.getStockQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("Tao don hang THAT BAI khi san pham khong ton tai")
    void createOrder_ThrowsException_WhenProductNotFound() {
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(999L); // id khong ton tai
        itemRequest.setQuantity(1);

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setItems(List.of(itemRequest));

        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(testUser));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder("user1", orderRequest))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepository, never()).save(any(Order.class));
    }
}
