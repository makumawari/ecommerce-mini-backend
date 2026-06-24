package com.example.ecommerce.config;

import com.example.ecommerce.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Trai tim cua Tieu chi 3: cau hinh toan bo Spring Security.
 *
 * Luong hoat dong tong quat:
 * Request vao -> JwtAuthFilter (doc token, xac dinh "ban la ai")
 *             -> Spring Security kiem tra URL nay co yeu cau quyen gi (authorizeHttpRequests)
 *             -> Neu vao den Controller, @PreAuthorize tren method se kiem tra lan cuoi
 */
@Configuration
@EnableMethodSecurity // bat @PreAuthorize, @PostAuthorize tren tung method cua Controller/Service
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt: thuat toan ma hoa 1 chieu, tu sinh "salt" rieng cho moi lan ma hoa
        // -> 2 user cung password "123456" se co hash khac nhau, chong tan cong ra bang (rainbow table)
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // API REST dung token, khong dung session/cookie -> tat CSRF (CSRF chi can thiet cho session-based auth)
            .csrf(csrf -> csrf.disable())

            // Khong tao HttpSession, moi request phai tu mang token rieng -> dung chuan "stateless" cua REST API
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                // Cho phep public: dang ky/dang nhap, xem H2 console (chi de dev)
                .requestMatchers("/api/auth/**", "/h2-console/**").permitAll()

                // Xem san pham/category: ai cũng xem duoc (ke ca chua dang nhap), nhung phai dung method GET
                .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**").permitAll()

                // Tat ca request con lai BAT BUOC phai dang nhap (token hop le).
                // Phan quyen CHI TIET hon (ADMIN/USER) se dung @PreAuthorize ngay tren method cua Controller,
                // ly do: de doc, de thay ro tung API can quyen gi ngay tai noi khai bao API do.
                .anyRequest().authenticated()
            )

            // Cho phep hien thi H2 console trong iframe (mac dinh Spring Security chan bang header X-Frame-Options)
            .headers(headers -> headers.frameOptions(frame -> frame.disable()))

            .authenticationProvider(authenticationProvider())

            // Chen JwtAuthFilter vao TRUOC filter mac dinh xu ly username/password cua Spring Security
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
