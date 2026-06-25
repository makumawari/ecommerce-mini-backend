# E-Commerce Mini Backend

Project luyện tập phỏng vấn Java Developer 1 YOE — đầy đủ 5 tiêu chí: Pagination,
Transaction, Spring Security + JWT, Global Exception Handling, Unit Test (JUnit5 + Mockito).

Project hỗ trợ 2 cách chạy: **chạy local với H2** (nhanh, không cần cài gì, phù hợp lúc code/test)
hoặc **chạy bằng Docker với PostgreSQL** (giống môi trường thật, data không mất khi tắt máy).

## 1. Cách 1 — Chạy local với H2 (mặc định, không cần Docker)

### Yêu cầu môi trường
- JDK 17+
- Maven 3.6+
- Không cần cài database — project dùng **H2 in-memory**, chạy xong mất dữ liệu, khởi động lại tự seed lại.

### Cách chạy

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

## 2. Cách 2 — Chạy bằng Docker với PostgreSQL (khuyên dùng để demo/chạy trên máy khác)

### Yêu cầu môi trường
- Docker + Docker Compose (Docker Desktop trên Windows/Mac)
- Không cần cài JDK/Maven/PostgreSQL thủ công — toàn bộ chạy trong container.

### Cách chạy

```bash
cd ecommerce-mini-backend
cp .env.example .env     # tạo file .env, sửa POSTGRES_PASSWORD và JWT_SECRET thành giá trị riêng
docker compose up -d     # build image + chạy app + PostgreSQL
```

App chạy ở `http://localhost:8080`, PostgreSQL chạy ở `localhost:5432`.

Xem log:
```bash
docker compose logs -f app
```

Dừng (giữ lại data):
```bash
docker compose down
```

Dừng và xoá luôn data (chỉ dùng khi muốn reset sạch từ đầu):
```bash
docker compose down -v
```

**Vì sao không mất data khi tắt máy?** PostgreSQL được lưu vào Docker named volume
(`postgres_data` trong [docker-compose.yml](docker-compose.yml)) — volume này tồn tại độc lập
với container, chỉ mất khi chủ động xoá bằng `docker compose down -v`. Restart máy, restart
Docker Desktop, hay `docker compose down` thường (không `-v`) đều giữ nguyên data.

**Lưu ý:** file `.env` chứa secret thật (password, JWT secret) nên KHÔNG được commit lên git
(đã có trong `.gitignore`). Khi đem code sang máy khác, copy lại `.env.example` thành `.env`
và tự đặt giá trị riêng.

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

### Bước 9 — Xem chi tiết 1 đơn hàng theo id — chỉ xem được đơn của chính mình
```bash
curl -i "http://localhost:8080/api/orders/1" -H "Authorization: Bearer $USER_TOKEN"
```
Nếu order id=1 thuộc về user khác (không phải user1), kết quả mong đợi: HTTP 403 Forbidden
(không phải 200, không lộ data của người khác). ADMIN gọi cùng API với order bất kỳ thì luôn
xem được (không bị giới hạn ownership).

### Bước 10 — Thử gọi API sản phẩm không tồn tại -> kiểm tra format lỗi (Tiêu chí 4)
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
| Order ownership (chống IDOR) | `OrderController.getOrderById`, `OrderService.getOrderById` |
| Docker / PostgreSQL | `Dockerfile`, `docker-compose.yml`, `application-docker.yml` |

## 6. Những điều CÓ THỂ bị hỏi thêm khi phỏng vấn (đã chuẩn bị sẵn câu trả lời trong code)
- "Tại sao không dùng `findAll()`?" → xem comment trong `ProductService`.
- "Làm sao tránh 2 người mua cùng lúc làm âm kho (oversell)?" → xem `@Version` trong `Product.java`
  (Optimistic Locking) — đây là điểm cộng lớn nếu bạn tự nói ra được.
- "Tại sao lưu `price` ở cả `Product` và `OrderItem`?" → xem comment trong `OrderItem.java`
  (snapshot giá tại thời điểm mua).
- "`@Transactional` rollback khi nào, không rollback khi nào?" → xem comment trong `OrderService.createOrder`.
- "User A có xem được order của User B không?" → KHÔNG. `OrderService.getOrderById` so sánh
  `order.getUser().getUsername()` với username trong JWT, không khớp thì throw
  `AccessDeniedException` → 403 (trừ ADMIN được bypass check). Đây là ví dụ về **IDOR**
  (Insecure Direct Object Reference) — lỗi bảo mật kinh điển khi chỉ check role mà quên check
  ownership của dữ liệu cụ thể.
- "Vì sao Postgres trong Docker không mất data khi tắt máy?" → xem mục Docker ở trên, dùng
  **named volume** thay vì lưu trong filesystem của container.
