# E-Commerce Mini Backend

Project luyện tập phỏng vấn Java Developer 1 YOE — đầy đủ 5 tiêu chí: Pagination,
Transaction, Spring Security + JWT, Global Exception Handling, Unit Test (JUnit5 + Mockito).

## 1. Yêu cầu môi trường
- JDK 17+
- Maven 3.6+
- Không cần cài database — project dùng **H2 in-memory**, chạy xong mất dữ liệu, khởi động lại tự seed lại.

## 2. Cách chạy

```bash
cd ecommerce-mini-backend
mvn clean install      # tải dependency + build + chạy unit test luôn (Tiêu chí 5)
mvn spring-boot:run    # chạy app, mặc định ở http://localhost:8080
```

Nếu chỉ muốn build mà KHÔNG chạy test (để chạy nhanh hơn lúc dev):
```bash
mvn clean install -DskipTests
```

Chỉ chạy riêng test (Tiêu chí 5):
```bash
mvn test
```

Xem dữ liệu trong DB lúc app đang chạy: mở `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:ecommerce_db`
- Username: `sa`, Password: (để trống)

## 3. Tài khoản có sẵn (do DataInitializer tự tạo lúc khởi động)
| username | password | role  |
|----------|----------|-------|
| admin    | admin123 | ADMIN |
| user1    | user123  | USER  |

Có sẵn 2 category: `Electronics` (id=1), `Books` (id=2).

## 4. Luồng test API bằng curl (theo đúng thứ tự để dễ hiểu)

### Bước 1 — Đăng nhập lấy token ADMIN
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```
Copy giá trị `token` trong response, đặt vào biến để dùng tiếp:
```bash
ADMIN_TOKEN="<paste-token-vao-day>"
```

### Bước 2 — ADMIN tạo sản phẩm (Tiêu chí 3: chỉ ADMIN mới làm được)
```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"name":"Laptop Dell XPS 13","price":25000000,"stockQuantity":5,"categoryId":1}'
```

### Bước 3 — Xem danh sách sản phẩm CÓ phân trang (Tiêu chí 1, không cần token)
```bash
curl "http://localhost:8080/api/products?page=0&size=5&sort=price,desc"
```

### Bước 4 — Đăng ký + đăng nhập tài khoản USER mới (hoặc dùng user1/user123 có sẵn)
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","password":"user123"}'
```
```bash
USER_TOKEN="<paste-token-vao-day>"
```

### Bước 5 — USER thử tạo sản phẩm -> phải bị từ chối 403 (Tiêu chí 3 + 4)
```bash
curl -i -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d '{"name":"Hack","price":1,"stockQuantity":1,"categoryId":1}'
```
Kết quả mong đợi: HTTP 403, JSON message rõ ràng, KHÔNG phải stacktrace.

### Bước 6 — USER tạo đơn hàng (Tiêu chí 2: @Transactional)
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d '{"items":[{"productId":1,"quantity":2}]}'
```

### Bước 7 — Thử mua vượt số lượng tồn -> phải bị từ chối + rollback
```bash
curl -i -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d '{"items":[{"productId":1,"quantity":9999}]}'
```
Kiểm tra lại tồn kho sản phẩm 1 — phải KHÔNG đổi so với trước lệnh này (vì rollback).

### Bước 8 — Xem đơn hàng của chính mình
```bash
curl "http://localhost:8080/api/orders/my" -H "Authorization: Bearer $USER_TOKEN"
```

### Bước 9 — Thử gọi API sản phẩm không tồn tại -> kiểm tra format lỗi (Tiêu chí 4)
```bash
curl -i "http://localhost:8080/api/products/9999"
```

## 5. Map nhanh: Tiêu chí ↔️ File code tương ứng
| Tiêu chí | File chính cần đọc |
|---|---|
| 1. Pagination/Sorting | `ProductController.getAllProducts`, `ProductService.getAllProducts` |
| 2. @Transactional | `OrderService.createOrder` |
| 3. Security/JWT | `SecurityConfig`, `JwtUtil`, `JwtAuthFilter`, `CustomUserDetailsService` |
| 4. Exception Handling | `GlobalExceptionHandler`, các class trong package `exception` |
| 5. Unit Test | `OrderServiceTest`, `ProductServiceTest` |

## 6. Những điều CÓ THỂ bị hỏi thêm khi phỏng vấn (đã chuẩn bị sẵn câu trả lời trong code)
- "Tại sao không dùng `findAll()`?" → xem comment trong `ProductService`.
- "Làm sao tránh 2 người mua cùng lúc làm âm kho (oversell)?" → xem `@Version` trong `Product.java`
  (Optimistic Locking) — đây là điểm cộng lớn nếu bạn tự nói ra được.
- "Tại sao lưu `price` ở cả `Product` và `OrderItem`?" → xem comment trong `OrderItem.java`
  (snapshot giá tại thời điểm mua).
- "`@Transactional` rollback khi nào, không rollback khi nào?" → xem comment trong `OrderService.createOrder`.
