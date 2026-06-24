package com.example.ecommerce.config;

import com.example.ecommerce.entity.Category;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.repository.CategoryRepository;
import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Chay 1 lan moi khi app khoi dong, tao san:
 * - 1 user ADMIN: username=admin / password=admin123
 * - 1 user thuong: username=user1 / password=user123
 * - 1 category mau "Electronics"
 * De ban co the test API ngay khong can tu INSERT du lieu bang tay.
 * Trong du an thuc te, day thuong duoc lam bang Flyway/Liquibase migration thay vi code nhu nay.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername("admin").isEmpty()) {
            userRepository.save(User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .email("admin@example.com")
                    .role(Role.ADMIN)
                    .build());
        }

        if (userRepository.findByUsername("user1").isEmpty()) {
            userRepository.save(User.builder()
                    .username("user1")
                    .password(passwordEncoder.encode("user123"))
                    .email("user1@example.com")
                    .role(Role.USER)
                    .build());
        }

        if (categoryRepository.count() == 0) {
            categoryRepository.save(Category.builder().name("Electronics").build());
            categoryRepository.save(Category.builder().name("Books").build());
        }
    }
}
