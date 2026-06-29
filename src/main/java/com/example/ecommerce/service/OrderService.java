package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.OrderItemRequest;
import com.example.ecommerce.dto.request.OrderRequest;
import com.example.ecommerce.dto.response.OrderItemResponse;
import com.example.ecommerce.dto.response.OrderResponse;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.exception.InsufficientStockException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * TIEU CHI 2: Xu ly don hang voi @Transactional.
 *
 * Logic: kiem tra kho -> tru kho -> tao Order -> tao OrderItem.
 * Neu BAT KY san pham nao khong du hang giua chung -> TOAN BO phai rollback,
 * khong duoc de tinh trang "tru kho san pham A roi nhung loi o san pham B
 * ma don hang van bi tao mot nua".
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /**
     * @Transactional (KHONG readOnly): danh dau toan bo method nay la MOT giao dich DB duy nhat.
     *
     * Co che hoat dong:
     * - Spring tao 1 Proxy boc quanh OrderService, mo 1 DB transaction TRUOC khi vao method.
     * - Neu method chay xong binh thuong -> COMMIT (luu het thay doi vao DB).
     * - Neu co RuntimeException (vi du InsufficientStockException) duoc throw ra ngoai method
     *   -> ROLLBACK TOAN BO (moi cau UPDATE/INSERT da chay trong method nay bi huy bo,
     *   coi nhu chua tung xay ra).
     *
     * LUU Y quan trong hay bi hoi trong phong van:
     * 1. @Transactional CHI rollback voi RuntimeException (unchecked), KHONG tu rollback voi
     *    checked Exception (vi du IOException) tru khi khai bao them rollbackFor = Exception.class.
     * 2. @Transactional chi co hieu luc khi method duoc goi TU BEN NGOAI class (qua Spring Proxy).
     *    Neu mot method @Transactional tu goi method @Transactional khac TRONG CUNG class
     *    (this.otherMethod()), annotation se KHONG co tac dung vi khong di qua Proxy.
     */
    @Transactional
    public OrderResponse createOrder(String username, OrderRequest request) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user: " + username));

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;

        // Tao Order truoc (chua co items) de co the gan quan he 2 chieu Order <-> OrderItem
        Order order = Order.builder()
                .user(user)
                .orderDate(LocalDateTime.now())
                .status(OrderStatus.PENDING)
                .totalPrice(BigDecimal.ZERO) // se cap nhat lai ben duoi
                .build();

        for (OrderItemRequest itemReq : request.getItems()) {

            // BUOC 1: KIEM TRA HANG TRONG KHO
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Khong tim thay san pham id: " + itemReq.getProductId()));

            if (product.getStockQuantity() < itemReq.getQuantity()) {
                // Throw RuntimeException -> Spring se TU DONG rollback toan bo transaction nay,
                // bao gom ca nhung lan tru kho da chay TRUOC do trong vong lap.
                throw new InsufficientStockException(
                        "San pham '" + product.getName() + "' khong du hang. Con lai: "
                                + product.getStockQuantity() + ", yeu cau: " + itemReq.getQuantity());
            }

            // BUOC 2: TRU SO LUONG TON KHO
            // Nho co @Version tren Product (xem lai Product.java) -> Hibernate tu kem dieu kien
            // "WHERE id = ? AND version = ?" khi UPDATE, neu request khac da sua truoc -> bao loi
            // thay vi am tham ghi de, tranh oversell khi nhieu nguoi mua cung luc.
            product.setStockQuantity(product.getStockQuantity() - itemReq.getQuantity());

            // BUOC 3: TAO OrderItem (chup lai gia hien tai cua san pham)
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .price(product.getPrice())
                    .build();

            orderItems.add(orderItem);
            totalPrice = totalPrice.add(product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())));
        }

        order.setItems(orderItems);
        order.setTotalPrice(totalPrice);
        order.setStatus(OrderStatus.CONFIRMED);

        // BUOC 4: LUU DON HANG
        // cascade = ALL tren Order.items (xem Order.java) -> save Order se tu dong save luon
        // tat ca OrderItem ben trong, khong can goi orderItemRepository.save() rieng.
        Order savedOrder = orderRepository.save(order);

        return toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user: " + username));

        // BUOC 1: lay danh sach Order CO PHAN TRANG (LIMIT/OFFSET dung o DB).
        // O day Order.items VAN la proxy rong, chua dung den nen chua trigger query nao them.
        Page<Order> orderPage = orderRepository.findByUserId(user.getId(), pageable);
        if (orderPage.isEmpty()) {
            return orderPage.map(this::toResponse);
        }

        // BUOC 2: chi voi DUNG cac order nam trong trang nay (vi du 10 order),
        // goi 1 query DUY NHAT lay full items + product cua chung.
        // FIX N+1: truoc day moi order trigger 1 query items + moi item trigger
        // 1 query product (co the 30+ query); gio chi con DUNG 1 query o buoc nay.
        List<Long> orderIds = orderPage.getContent().stream().map(Order::getId).toList();
        Map<Long, Order> fullOrdersById = orderRepository.findAllWithItemsByIdIn(orderIds).stream()
                .collect(Collectors.toMap(Order::getId, o -> o));

        // Map lai tu danh sach "day du" o buoc 2, nhung GIU NGUYEN thu tu/sort
        // cua orderPage o buoc 1 (JOIN FETCH co the tra ve thu tu khac voi yeu cau sort ban dau).
        return orderPage.map(o -> toResponse(fullOrdersById.get(o.getId())));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id, String username, boolean isAdmin) {
        // FIX N+1: findByIdWithItems da JOIN FETCH san user + items + product,
        // nen toResponse() ben duoi khong can query rieng nua.
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay don hang id: " + id));

        if (!isAdmin && !order.getUser().getUsername().equals(username)){
                throw new AccessDeniedException("User khong co quyen xem don hang nay!");
        }
        return toResponse(order);
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .subTotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .orderDate(order.getOrderDate())
                .totalPrice(order.getTotalPrice())
                .status(order.getStatus().name())
                .items(itemResponses)
                .build();
    }
}