# LearningHub Backend

Backend REST API cho nền tảng học trực tuyến LearningHub. Hệ thống quản lý người dùng, khóa học, quá trình học, điểm, thanh toán, doanh thu giảng viên, rút tiền, đánh giá, chat realtime, thông báo và chứng chỉ.

## Tổng quan nhanh

- **135 REST API endpoint**: 93 endpoint cho auth/public/user/teacher và 42 endpoint quản trị.
- **1 WebSocket/STOMP message endpoint** cho chat realtime.
- **31 controller class**: 30 REST controller và 1 WebSocket controller.
- **21 test class** gồm controller slice test, service unit test, repository integration test với H2 và application context test.
- **CI/CD bằng GitHub Actions**: chạy test, build Maven, build/push Docker image và tự động triển khai lên EC2 bằng Docker Compose.
- Tìm kiếm động bằng **JPA Specification**, phân trang và sắp xếp cho course/user.
- Giảm truy vấn **N+1** bằng `@EntityGraph`, JPQL `join fetch` và `@BatchSize`.
- Course hỗ trợ **soft delete**, khóa chỉnh sửa sau khi xóa, admin có thể khôi phục và scheduled job dọn chapter/lesson.

## Mục tiêu dự án

- Tổ chức REST API theo layered architecture: controller → service → repository.
- Xử lý authentication/authorization với nhiều role và permission.
- Bảo đảm tính nhất quán khi cập nhật điểm, mua khóa học, thanh toán và rút tiền đồng thời.
- Tích hợp MySQL, Redis, Amazon S3, VNPAY, payOS, Google OAuth2, WebSocket/STOMP và Brevo.
- Dùng DTO/mapper thay vì trả trực tiếp JPA Entity.

## Công nghệ

- Java 21, Spring Boot, Maven
- Spring Web MVC, Validation, AOP, WebSocket/STOMP
- Spring Security, JWT, OAuth2 Resource Server và Google OAuth2
- Spring Data JPA, Hibernate, MySQL 8+
- Flyway database migration
- Redis, Spring Cache, rate limiting và token blacklist
- MapStruct, Lombok
- AWS SDK for Java v2 (S3)
- VNPAY Sandbox, payOS hosted checkout/webhook, Brevo Transactional Email
- Thymeleaf, OpenHTMLtoPDF, Springdoc OpenAPI/Swagger

## Tính năng

### Authentication và authorization

- Đăng nhập bằng JWT access token/refresh token và Google.
- Đăng ký, xác minh email, gửi lại mã, quên và đặt lại mật khẩu.
- Role chính: `USER`, `TEACHER`, `ADMIN`.
- Method-level authorization bằng Spring Security.
- Token blacklist, giới hạn đăng nhập sai và rate limiting qua Redis.

### Course và learning

- CRUD course, chapter, lesson; upload thumbnail, video và tài liệu.
- Quy trình `DRAFT → PENDING → APPROVED/REJECTED/BANNED/DELETED`.
- Chủ sở hữu có thể soft-delete course sang trạng thái `DELETED`; course bị ẩn khỏi luồng công khai và chuyển sang chế độ chỉ đọc.
- Admin có thể khôi phục course `DELETED` về `DRAFT` trước khi nội dung bị dọn; job định kỳ xóa chapter/lesson của course đã xóa mềm, còn tài nguyên S3 không còn tham chiếu được xử lý bởi `S3OrphanCleanupJob`.
- Tìm kiếm, lọc, sắp xếp và phân trang bằng JPA Specification.
- Mua khóa học bằng point, enrollment, yêu thích, đánh giá và phản hồi.
- Theo dõi tiến độ lesson/course và danh sách khóa học của người dùng.
- Preview khóa học, thông tin giảng viên và khóa học theo giảng viên.

### Point, payment và withdrawal

- Ví point và lịch sử point transaction.
- Nạp point qua VNPAY Sandbox hoặc payOS; tỷ lệ mặc định `1.000 VND = 1 point`.
- Return URL/IPN của VNPAY và webhook có chữ ký của payOS; callback được xác minh và xử lý idempotent.
- Admin điều chỉnh/cộng/trừ/thưởng point.
- Quản lý doanh thu, tài khoản ngân hàng và yêu cầu rút tiền của giảng viên.
- Job tự cập nhật payment hết hạn và vô hiệu hóa advertisement hết hạn.

### S3 và certificate

- Upload avatar, thumbnail, advertisement, hồ sơ, chứng từ và payment proof.
- Presigned upload URL cho video, tài liệu và hình ảnh chat lớn.
- Presigned download URL cho object private.
- Database lưu S3 object key, không lưu full URL cố định.
- Dọn S3 orphan object theo lịch.
- Xóa nội dung chapter/lesson của course đã soft-delete theo job riêng.
- Sinh certificate PDF từ template HTML, lưu trên S3 và xác minh bằng verification code.

### Realtime và notification

- Chat hỗ trợ, hỏi đáp khóa học và liên hệ học viên bằng WebSocket/STOMP.
- Tin nhắn văn bản và hình ảnh; ảnh upload trực tiếp lên S3 qua presigned URL.
- Thông báo realtime, unread count, đánh dấu đã đọc và xóa thông báo.
- Job dọn thông báo cũ.

### Redis cache

- Cache course detail, danh sách course và gợi ý tiêu đề.
- TTL riêng cho từng loại cache.
- Evict cache khi course thay đổi hoặc đổi trạng thái.
- Chỉ cache response DTO, không cache managed JPA Entity.

## Kiến trúc

```mermaid
flowchart LR
    Client[Web / Mobile Client] --> API[REST Controller]
    Client <-->|STOMP| WS[WebSocket]
    API --> Security[Spring Security]
    API --> Service[Service Layer]
    WS --> Service
    Service --> Repository[Repository]
    Repository --> MySQL[(MySQL)]
    Service --> Redis[(Redis)]
    Service --> S3[(Amazon S3)]
    Service --> VNPAY[VNPAY Sandbox]
    Service --> PayOS[payOS Checkout/Webhook]
    Service --> Brevo[Brevo Email]
```

```text
controller → service interface → service implementation → repository
                         ↘ DTO ↔ mapper
```

- Controller nhận request và trả `ApiResponse`.
- Service chứa business logic và transaction boundary.
- Repository làm việc với persistence layer.
- Mapper chuyển đổi Entity/DTO.
- Exception handler dùng chung chuyển lỗi thành response chuẩn và `ErrorCode`.

## Các điểm kỹ thuật quan trọng

### Transaction và concurrent request

Business logic cập nhật nhiều dữ liệu liên quan được bọc bằng transaction. Một số luồng nhạy cảm như point balance, mua course, payment và withdrawal dùng pessimistic locking để hạn chế lost update, double-spend và xử lý trùng khi có concurrent request.

### Payment idempotency

Payment được lưu trước khi chuyển sang VNPAY. Backend xác minh callback/IPN, kiểm tra trạng thái hiện tại và chỉ cộng point một lần dù callback được gửi lại nhiều lần.

### Quản lý object trên S3

File nhỏ có thể đi qua backend; video, tài liệu và ảnh chat dùng presigned URL để client upload trực tiếp. Database chỉ giữ object key; URL tải private được tạo khi client yêu cầu và có thời hạn.

### Redis strategy

Redis dùng cho cache, rate limit và blacklist token. Cache liên quan course được evict sau khi course cập nhật hoặc đổi trạng thái.

### Xử lý N+1 query

- Dùng `@EntityGraph` tại repository để tải trước các quan hệ cần cho response như course-author, enrollment-course, review-user/course, conversation-participants và point transaction.
- Dùng `@BatchSize` cho collection role của user để tránh phát sinh một truy vấn cho từng tài khoản khi phân trang danh sách.
- Mapping sang DTO được thực hiện sau khi dữ liệu cần thiết đã được fetch, hạn chế truy cập lazy association ngoài chủ đích.

### Tìm kiếm động bằng JPA Specification

- `CourseSpecification` phục vụ tìm kiếm course công khai và tìm kiếm quản trị với điều kiện trạng thái khác nhau.
- `UserSpecification` ghép các điều kiện username, họ tên, role, trạng thái tài khoản và các bộ lọc quản trị.
- `JpaSpecificationExecutor` kết hợp Specification với `Pageable`, cho phép thêm bộ lọc mới mà không phải tạo nhiều repository method cố định.

### Vòng đời soft-delete của course

1. Chủ sở hữu xóa course: backend chuyển trạng thái sang `DELETED`, không xóa ngay bản ghi course.
2. Course `DELETED` bị ẩn khỏi kết quả công khai và không cho chủ sở hữu sửa chapter/lesson.
3. Admin vẫn xem được course đã xóa và có thể restore về `DRAFT`.
4. `DeletedCourseContentCleanupJob` chạy theo lịch để xóa chapter và lesson thuộc course `DELETED`.
5. `S3OrphanCleanupJob` xử lý các object S3 không còn được tham chiếu theo grace period riêng.

## Cấu hình môi trường

Ứng dụng dùng ba file YAML:

| File | Mục đích |
|---|---|
| `src/main/resources/application.yaml` | Cấu hình dùng chung, đọc biến môi trường; server context path là `/api/v1` |
| `src/main/resources/application-dev.yml` | Local/dev: `ddl-auto: validate`, bật SQL log và Swagger |
| `src/main/resources/application-prod.yml` | Production: `ddl-auto: validate`, tắt SQL log và Swagger |
| `src/main/resources/db/migration` | Flyway migrations; schema được nâng cấp trước khi Hibernate validate |

Chọn profile bằng `SPRING_PROFILES_ACTIVE`:

```env
SPRING_PROFILES_ACTIVE=dev   # local
SPRING_PROFILES_ACTIVE=prod  # production
```

### Biến môi trường backend

Đặt các giá trị này trong environment, secret manager hoặc file `.env` của backend. Không commit credential thật.

```env
# Server và ứng dụng
SERVER_PORT=8080
FRONTEND_URL=http://localhost:5173
OPENAPI_SERVICE_SERVER=http://localhost:8080/api/v1

# Tài khoản admin khởi tạo
APP_ADMIN_USERNAME=admin
APP_ADMIN_PASSWORD=change-me
APP_ADMIN_FULL_NAME=LearningHub Admin
APP_ADMIN_EMAIL=admin@example.com
APP_ADMIN_PHONE_NUMBER=0000000000

# Profile và database
SPRING_PROFILES_ACTIVE=dev
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/learninghub
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=your-password
SPRING_FLYWAY_ENABLED=true
SPRING_FLYWAY_BASELINE_ON_MIGRATE=true

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_DATABASE=0

# Security và Google
JWT_KEY=your-long-jwt-signing-key
GOOGLE_CLIENT_ID=your-google-client-id

# Email/Brevo
BREVO_API_KEY=your-brevo-api-key
BREVO_FROM_EMAIL=no-reply@example.com

# Amazon S3
S3_ACCESS_KEY=your-access-key
S3_SECRET_KEY=your-secret-key
S3_BUCKET_NAME=your-private-bucket
S3_BASE_PREFIX=learninghub

# VNPAY Sandbox
VNPAY_PAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_RETURN_URL=http://localhost:8080/api/v1/payment-callbacks/vnpay/return
VNPAY_FRONTEND_RETURN_URL=http://localhost:5173/payment-result
VNPAY_TMN_CODE=your-tmn-code
VNPAY_HASH_SECRET=your-hash-secret

# payOS (chỉ backend; không đưa các key này vào frontend)
PAYOS_CLIENT_ID=your-client-id
PAYOS_API_KEY=your-api-key
PAYOS_CHECKSUM_KEY=your-checksum-key
PAYOS_RETURN_URL=https://learninghub.id.vn/payment-result
PAYOS_CANCEL_URL=https://learninghub.id.vn/payment-result
```

Webhook payOS cấu hình trong merchant dashboard:

```text
https://api.learninghub.id.vn/api/v1/payment-callbacks/payos/webhook
```

Đây là endpoint server-to-server nên không yêu cầu JWT. `PAYOS_RETURN_URL` và
`PAYOS_CANCEL_URL` là URL trình duyệt quay về frontend; chúng không thay thế webhook.

### Scheduled jobs

Các job được cấu hình trong `application.yaml` và chạy theo múi giờ `Asia/Ho_Chi_Minh`:

| Job | Lịch mặc định |
|---|---|
| Payment expiration | Chạy lặp mỗi 60 giây |
| Notification cleanup | 03:00 mỗi ngày, giữ 60 ngày |
| Advertisement deactivation | 00:00 mỗi ngày |
| S3 orphan cleanup | 03:00 Chủ nhật, grace period 24 giờ |
| Deleted course content cleanup | Bật theo cấu hình |

## Yêu cầu cài đặt

- JDK 21
- MySQL 8+
- Redis 7+
- Tài khoản/bucket Amazon S3
- VNPAY Sandbox credentials hoặc payOS credentials
- Brevo API key

Khởi tạo database:

```sql
CREATE DATABASE learninghub
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

Khi ứng dụng khởi động, Flyway tự chạy các file trong `src/main/resources/db/migration` rồi Hibernate dùng `ddl-auto: validate` để kiểm tra mapping. Với database đã tồn tại từ trước, hãy backup trước và để `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true` cho lần chạy đầu để tạo mốc Flyway version `1` mà không sửa dữ liệu hiện có; sau khi kiểm tra bảng `flyway_schema_history`, nên chuyển biến này thành `false` ở production. Database mới sẽ chạy migration `V1__initial_schema.sql`.

Chạy Redis nhanh bằng Docker:

```bash
docker run --name learninghub-redis -p 6379:6379 -d redis:7-alpine
```

## Chạy local

Windows PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
.\mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
export SPRING_PROFILES_ACTIVE=dev
./mvnw spring-boot:run
```

Địa chỉ mặc định:

```text
API:        http://localhost:8080/api/v1
Swagger UI: http://localhost:8080/api/v1/swagger-ui/index.html
```

Swagger và API docs bị tắt trong profile `prod`.

## Docker và Docker Compose

`Dockerfile` dùng build nhiều tầng với Maven/Corretto 21. `docker-compose.yml` chạy MySQL, Redis và backend trong cùng network. Cần khai báo các biến compose như `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD`, `REDIS_PASSWORD`, `REDIS_DATABASE`, `DOCKER_USERNAME`, `IMAGE_TAG` và `SERVER_PORT` trong `.env`.

```bash
docker compose up -d
```

Build image backend:

```bash
docker build -t learninghub-backend .
```

## CI/CD

Hai workflow nằm trong `.github/workflows`:

### Continuous Integration — `ci.yml`

Pipeline chạy khi push lên `main` hoặc `master`:

1. Checkout source code.
2. Khởi tạo Redis service cho test.
3. Cài Amazon Corretto JDK 21 và cache Maven dependency.
4. Chạy toàn bộ test bằng `mvn -B test`.
5. Build artifact bằng `mvn -B package -DskipTests`.
6. Build Docker image và push lên Docker Hub với tag là Git commit SHA.

### Continuous Deployment — `cd.yml`

CD chỉ chạy khi workflow CI hoàn tất thành công:

1. Kết nối EC2 qua SSH bằng GitHub Secrets.
2. Cập nhật `IMAGE_TAG` trong file `.env` trên server bằng commit SHA vừa build.
3. Pull image mới bằng Docker Compose.
4. Recreate riêng service backend và dọn Docker image cũ.

Các biến/secret cần cấu hình trên GitHub: `DOCKER_USERNAME`, `DOCKER_TOKEN`, `SSH_HOST`, `SSH_USERNAME`, `SSH_PRIVATE_KEY`.

## API tiêu biểu

Tất cả endpoint bên dưới đều có prefix `/api/v1`:

### Thống kê API

| Nhóm | Controller | REST endpoint |
|---|---:|---:|
| Auth, public, learner và teacher | 21 | 93 |
| Admin | 9 | 42 |
| **Tổng REST API** | **30** | **135** |

Ngoài REST API, `ChatWebSocketController` cung cấp 1 `@MessageMapping("/chat.send")` cho luồng gửi tin nhắn STOMP. Số liệu được tính trực tiếp từ các method có `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping` hoặc `@DeleteMapping` trong source hiện tại.

### Một số endpoint chính

| Module | Method | Endpoint | Chức năng |
|---|---|---|---|
| Auth | POST | `/auth/login` | Đăng nhập và nhận token |
| Auth | POST | `/auth/login/google` | Đăng nhập Google |
| User | GET | `/users/me` | Lấy người dùng hiện tại |
| Course | GET | `/courses/list` | Tìm kiếm course đã approved |
| Course | GET | `/courses/{id}` | Chi tiết course |
| Course | GET | `/courses/{id}/preview` | Preview course |
| Course | GET | `/courses/teacher/{teacherId}` | Course theo giảng viên |
| Enrollment | POST | `/enrollments/buy` | Mua course bằng point |
| Progress | POST | `/lesson-progress/{lessonId}/complete` | Hoàn thành lesson |
| Payment | POST | `/payments/deposits` | Tạo checkout nạp point; chọn cổng bằng `paymentMethod` (`VNPAY` hoặc `PAYOS`) |
| Payment | GET | `/payments/deposits` | Xem lịch sử nạp point của người dùng hiện tại |
| Payment | GET | `/payments/deposits/{transactionRef}` | Xem trạng thái một giao dịch của người dùng hiện tại |
| Payment callback | GET | `/payment-callbacks/vnpay/ipn` | Nhận và xác minh VNPAY IPN |
| Payment callback | GET | `/payment-callbacks/vnpay/return` | Xử lý VNPAY browser return |
| Payment callback | POST | `/payment-callbacks/payos/webhook` | Nhận và xác minh webhook payOS |
| Point | GET | `/users/me/points/transactions` | Lịch sử point transaction |
| Certificate | GET | `/certificates/courses/{courseId}/download` | Tải certificate |
| Certificate | GET | `/certificates/verify/{code}` | Xác minh certificate |
| Teacher | POST | `/teachers/register` | Đăng ký giảng viên |
| Withdrawal | POST | `/teacher/withdrawals` | Tạo yêu cầu rút tiền |
| Conversation | GET | `/conversations` | Danh sách cuộc trò chuyện |
| Conversation | GET | `/conversations/{id}/messages` | Lịch sử tin nhắn |
| Conversation | GET | `/conversations/upload-url` | Presigned URL upload ảnh chat |
| Notification | GET | `/notifications` | Danh sách thông báo |
| Admin course | PATCH | `/admin/course/{id}/approve` | Duyệt course |
| Admin teacher | POST | `/admin/teachers/{userId}/approve` | Duyệt hồ sơ giảng viên |
| Admin advertisement | DELETE | `/admin/advertisements/expired` | Xóa quảng cáo hết hạn |

Danh sách request/response đầy đủ được cung cấp tại Swagger trong profile `dev`.

## WebSocket/STOMP

- SockJS endpoint: `/api/v1/ws`
- Gửi tin nhắn: `/app/chat.send`
- Subscribe chat conversation: `/topic/conversation/{conversationId}`
- Subscribe thông báo cá nhân: `/user/queue/notifications`
- Subscribe chat notification: `/user/queue/chat-notifications`

Client phải gửi access token trong STOMP `CONNECT` header:

```text
Authorization: Bearer <access-token>
```

## Redis cache

TTL tham khảo trong cấu hình service:

| Cache | TTL |
|---|---:|
| Course detail | 15 phút |
| Course list | 5 phút |
| Course title suggestion | 10 phút |

Kiểm tra cache:

```bash
redis-cli SCAN 0 MATCH "learninghub::*" COUNT 100
redis-cli TTL "learninghub::courses::1"
```

## Cấu trúc source

```text
learninghub/
├─ .github/workflows/
│  ├─ ci.yml                         # Test, Maven build, build/push Docker image
│  └─ cd.yml                         # Deploy image lên EC2 bằng Docker Compose
├─ src/main/java/com/dxh/learninghub/
│  ├─ aspect/                        # AOP: rate limit và cross-cutting concern
│  ├─ configuration/                 # Security, Redis, WebSocket, OpenAPI, S3, VNPAY
│  ├─ constant/                      # Hằng số, cache name và cấu hình dùng chung
│  ├─ controller/
│  │  └─ admin/                      # REST controller dành cho quản trị
│  ├─ dto/
│  │  ├─ request/                    # Request DTO và validation contract
│  │  ├─ payment/                    # DTO callback/checkout của các cổng thanh toán
│  │  └─ response/                   # Response DTO/API response dùng chung
│  ├─ entity/                        # JPA entity và model persistence
│  ├─ enums/                         # Enum trạng thái và nghiệp vụ
│  ├─ exception/                     # ErrorCode, AppException, global handler
│  ├─ job/                           # Scheduled cleanup/expiration jobs
│  ├─ mapper/                        # MapStruct mapper
│  ├─ repo/
│  │  └─ specification/              # CourseSpecification, UserSpecification
│  ├─ service/
│  │  ├─ interfac/                   # Service contract (tên thư mục hiện tại)
│  │  │  └─ PaymentService.java      # Orchestration payment dùng chung VNPAY/payOS
│  │  └─ impl/                       # Business logic và transaction boundary
│  ├─ utils/
│  │  └─ storage/                    # Tiện ích xử lý object key/storage
│  └─ validator/                     # Custom Bean Validation
├─ src/main/resources/
│  ├─ db/migration/                  # Flyway migration
│  ├─ templates/                     # Email/certificate HTML template
│  ├─ application.yaml               # Cấu hình dùng chung
│  ├─ application-dev.yml            # Profile local/dev
│  └─ application-prod.yml           # Profile production
├─ src/test/java/com/dxh/learninghub/
│  ├─ controller/                    # WebMvc controller slice test
│  ├─ service/                       # Service unit test
│  └─ repo/                          # Repository integration test
├─ src/test/resources/
│  └─ application-test.yaml          # H2 MySQL compatibility mode
├─ Dockerfile
├─ docker-compose.yml
└─ pom.xml
```

## Testing

Backend hiện có **21 test class**:

- `@WebMvcTest` cho các controller quan trọng: authentication, user, course, enrollment, conversation, VNPAY và các controller admin chính.
- Unit test bằng Mockito cho service authentication, user, course, enrollment, chat, VNPAY, withdrawal, Turnstile và service admin.
- `@DataJpaTest` cho repository integration test; profile test dùng H2 in-memory ở MySQL compatibility mode.
- `@SpringBootTest` kiểm tra application context.

```bash
./mvnw test
```

CI chạy lại toàn bộ test trước khi Maven package và Docker image được tạo. Testcontainers, security integration test và concurrency test có thể bổ sung khi mở rộng dự án.

## Roadmap

- Bổ sung integration test bằng Testcontainers.
- Bổ sung các migration Flyway tiếp theo cho mọi thay đổi schema.
- Bổ sung dependency scanning, metrics và tracing cho pipeline CI/CD hiện có.
- Centralized logging và event-driven flow cho advertisement/email.
- Mở rộng kiểm thử VNPAY callback và concurrent course purchase.
