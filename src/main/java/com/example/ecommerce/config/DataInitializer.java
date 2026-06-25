package com.example.ecommerce.config;

import com.example.ecommerce.entity.Category;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.OrderItem;
import com.example.ecommerce.entity.OrderStatus;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.repository.CategoryRepository;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Chay 1 lan moi khi app khoi dong, seed du lieu mau de test API ngay khong can
 * tu INSERT bang tay: user (ADMIN + nhieu USER), category, product, order + orderItem.
 * Idempotent: chi insert khi bang dang rong, restart app nhieu lan khong bi duplicate.
 * Trong du an thuc te, day thuong duoc lam bang Flyway/Liquibase migration thay vi code nhu nay.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        Map<String, User> users = seedUsers();
        Map<String, Category> categories = seedCategories();
        Map<String, Product> products = seedProducts(categories);
        seedOrders(users, products);
    }

    private Map<String, User> seedUsers() {
        record SeedUser(String username, String rawPassword, String email, Role role) {}

        List<SeedUser> seedUsers = List.of(
                new SeedUser("admin", "admin123", "admin@example.com", Role.ADMIN),
                new SeedUser("manager", "manager123", "manager@example.com", Role.ADMIN),
                new SeedUser("user1", "user123", "user1@example.com", Role.USER),
                new SeedUser("user2", "user123", "user2@example.com", Role.USER),
                new SeedUser("user3", "user123", "user3@example.com", Role.USER),
                new SeedUser("user4", "user123", "user4@example.com", Role.USER),
                new SeedUser("user5", "user123", "user5@example.com", Role.USER)
        );

        for (SeedUser seed : seedUsers) {
            if (userRepository.findByUsername(seed.username()).isEmpty()) {
                userRepository.save(User.builder()
                        .username(seed.username())
                        .password(passwordEncoder.encode(seed.rawPassword()))
                        .email(seed.email())
                        .role(seed.role())
                        .build());
            }
        }

        return userRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(User::getUsername, u -> u));
    }

    private Map<String, Category> seedCategories() {
        List<String> names = List.of("Electronics", "Books", "Fashion", "Home & Kitchen", "Sports", "Toys");

        if (categoryRepository.count() == 0) {
            names.forEach(name -> categoryRepository.save(Category.builder().name(name).build()));
        }

        return categoryRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(Category::getName, c -> c));
    }

    private Map<String, Product> seedProducts(Map<String, Category> categories) {
        record SeedProduct(String name, String price, int stock, String category) {}

        List<SeedProduct> seedProducts = List.of(
                new SeedProduct("Laptop Dell XPS 13", "28000000", 10, "Electronics"),
                new SeedProduct("Laptop MacBook Air M2", "32000000", 8, "Electronics"),
                new SeedProduct("Dien thoai iPhone 15", "22000000", 15, "Electronics"),
                new SeedProduct("Dien thoai Samsung Galaxy S24", "19000000", 12, "Electronics"),
                new SeedProduct("Tai nghe Sony WH-1000XM5", "7500000", 20, "Electronics"),
                new SeedProduct("Ban phim co Keychron K8", "1800000", 25, "Electronics"),
                new SeedProduct("Chuot Logitech MX Master 3", "2100000", 30, "Electronics"),
                new SeedProduct("Man hinh LG UltraGear 27inch", "6500000", 10, "Electronics"),

                new SeedProduct("Clean Code", "350000", 40, "Books"),
                new SeedProduct("Effective Java", "420000", 35, "Books"),
                new SeedProduct("Design Patterns - GoF", "390000", 20, "Books"),
                new SeedProduct("Spring in Action", "450000", 18, "Books"),
                new SeedProduct("Atomic Habits", "180000", 50, "Books"),

                new SeedProduct("Ao thun nam basic", "150000", 100, "Fashion"),
                new SeedProduct("Quan jeans nu slimfit", "320000", 60, "Fashion"),
                new SeedProduct("Giay sneaker Adidas", "1650000", 40, "Fashion"),
                new SeedProduct("Tui xach da nu", "890000", 25, "Fashion"),

                new SeedProduct("Bo noi inox 5 day", "1250000", 15, "Home & Kitchen"),
                new SeedProduct("May xay sinh to Philips", "990000", 22, "Home & Kitchen"),
                new SeedProduct("Bo dao bep cao cap", "560000", 30, "Home & Kitchen"),

                new SeedProduct("Bong da Nike Strike", "450000", 35, "Sports"),
                new SeedProduct("Tham yoga 6mm", "320000", 45, "Sports"),
                new SeedProduct("Xe dap the thao", "3200000", 6, "Sports"),

                new SeedProduct("Lego City 60-mon", "780000", 18, "Toys"),
                new SeedProduct("Robot dieu khien tu xa", "650000", 0, "Toys")
        );

        if (productRepository.count() == 0) {
            seedProducts.forEach(seed -> productRepository.save(Product.builder()
                    .name(seed.name())
                    .price(new BigDecimal(seed.price()))
                    .stockQuantity(seed.stock())
                    .category(categories.get(seed.category()))
                    .build()));
        }

        return productRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(Product::getName, p -> p));
    }

    private void seedOrders(Map<String, User> users, Map<String, Product> products) {
        if (orderRepository.count() > 0) {
            return;
        }

        record SeedItem(String productName, int quantity) {}
        record SeedOrder(String username, OrderStatus status, int daysAgo, List<SeedItem> items) {}

        List<SeedOrder> seedOrders = List.of(
                new SeedOrder("user1", OrderStatus.CONFIRMED, 10, List.of(
                        new SeedItem("Laptop Dell XPS 13", 1),
                        new SeedItem("Chuot Logitech MX Master 3", 1)
                )),
                new SeedOrder("user1", OrderStatus.PENDING, 1, List.of(
                        new SeedItem("Clean Code", 2)
                )),
                new SeedOrder("user2", OrderStatus.CONFIRMED, 7, List.of(
                        new SeedItem("Dien thoai iPhone 15", 1),
                        new SeedItem("Tai nghe Sony WH-1000XM5", 1)
                )),
                new SeedOrder("user2", OrderStatus.CANCELLED, 3, List.of(
                        new SeedItem("Xe dap the thao", 1)
                )),
                new SeedOrder("user3", OrderStatus.CONFIRMED, 15, List.of(
                        new SeedItem("Ao thun nam basic", 3),
                        new SeedItem("Quan jeans nu slimfit", 2)
                )),
                new SeedOrder("user3", OrderStatus.PENDING, 0, List.of(
                        new SeedItem("Giay sneaker Adidas", 1)
                )),
                new SeedOrder("user4", OrderStatus.CONFIRMED, 5, List.of(
                        new SeedItem("Bo noi inox 5 day", 1),
                        new SeedItem("May xay sinh to Philips", 1),
                        new SeedItem("Bo dao bep cao cap", 1)
                )),
                new SeedOrder("user4", OrderStatus.CONFIRMED, 20, List.of(
                        new SeedItem("Effective Java", 1),
                        new SeedItem("Spring in Action", 1),
                        new SeedItem("Design Patterns - GoF", 1)
                )),
                new SeedOrder("user5", OrderStatus.PENDING, 2, List.of(
                        new SeedItem("Bong da Nike Strike", 2),
                        new SeedItem("Tham yoga 6mm", 1)
                )),
                new SeedOrder("user5", OrderStatus.CANCELLED, 12, List.of(
                        new SeedItem("Lego City 60-mon", 1)
                ))
        );

        for (SeedOrder seed : seedOrders) {
            User user = users.get(seed.username());

            List<OrderItem> items = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;

            for (SeedItem seedItem : seed.items()) {
                Product product = products.get(seedItem.productName());
                BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(seedItem.quantity()));
                total = total.add(lineTotal);

                items.add(OrderItem.builder()
                        .product(product)
                        .quantity(seedItem.quantity())
                        .price(product.getPrice())
                        .build());
            }

            Order order = Order.builder()
                    .user(user)
                    .orderDate(LocalDateTime.now().minusDays(seed.daysAgo()))
                    .totalPrice(total)
                    .status(seed.status())
                    .build();

            items.forEach(item -> item.setOrder(order));
            order.setItems(items);

            orderRepository.save(order);
        }
    }
}
