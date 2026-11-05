# SOLID, ranh giới module và tính đúng của nghiệp vụ

Làm sau khi đã có test cho hành vi cần sửa. Refactor giữ nguyên hành vi; sửa bug thay đổi hành vi. Tách hai việc để biết test thất bại vì đâu.

## 1. Hiểu SOLID qua code hiện tại

| Nguyên tắc | Ý nghĩa thực hành | Áp dụng trong TetRail |
|---|---|---|
| S — Single Responsibility | Một thành phần có một lý do chính để thay đổi | Scheduler chỉ kích hoạt lịch; use case xử lý hết hạn; controller chỉ xử lý HTTP. |
| O — Open/Closed | Thêm biến thể mà ít sửa logic đã ổn định | `NotificationService` nhận danh sách factory đã đi đúng hướng; thêm sender không cần thêm `if` trong service. |
| L — Liskov Substitution | Các implementation giữ cùng hợp đồng hành vi | Fake repository và JPA repository phải hiểu `reserveTickets` giống nhau: chỉ AVAILABLE được giữ, trả số row cập nhật. |
| I — Interface Segregation | Bên sử dụng phụ thuộc đúng phần nó cần | Khi đủ phức tạp, tách `TicketQuery` và `TicketReservation`; chưa cần một interface cho mỗi method. |
| D — Dependency Inversion | Chính sách nghiệp vụ phụ thuộc abstraction, chi tiết triển khai cung cấp implementation | Use case phát token qua port, không import `JwtTokenProvider` từ infrastructure. |

SOLID không yêu cầu mọi class đều có interface, mọi method phải cực ngắn, hay domain phải hoàn toàn không có annotation ngay lập tức. Bạn chọn mức tách theo chi phí thay đổi và test cần viết.

## 2. Bài DIP — Bỏ chiều phụ thuộc application → infrastructure

Hiện tại:

```text
controller → application → infrastructure → domain
                    └────────────────────→ domain
```

Hướng mong muốn, mỗi mũi tên là dependency lúc compile:

```text
application → domain
infrastructure → application (để triển khai application ports)
infrastructure → domain
controller → application
start → controller + infrastructure (ghép các bean)
```

Đặt port trong application hoặc domain tùy vai trò; ở đây phát access token phục vụ use case đăng nhập nên chọn application. Infrastructure phụ thuộc application để triển khai port là hợp lý; không được còn chiều ngược lại tạo vòng.

**Bước 1: tạo abstraction không mang kiểu Spring Security.** Ví dụ các file riêng trong `application/auth/port`:

```java
public record AuthenticatedUser(Long id, String username, java.util.Set<String> authorities) {}
```

```java
public interface CredentialAuthenticator {
    AuthenticatedUser authenticate(String usernameOrEmail, String password);
}
```

```java
public interface AccessTokenIssuer {
    String issue(AuthenticatedUser user);
}
```

Đây là phác thảo hợp đồng, chưa phải patch đầy đủ. Khi triển khai thêm package và viết adapter xử lý đúng user status, password, authorities và lỗi xác thực.

**Bước 2:** adapter ở infrastructure bọc `AuthenticationManager` và `JwtTokenProvider`; adapter trả record trên. Use case cần entity để lưu refresh token thì tra bằng `UserRepository` theo ID, hoặc cải tiến hợp đồng lưu refresh token theo user ID; không ép port chứa `UserDetailsCustom` chỉ để tránh một thay đổi nhỏ.

**Bước 3:** đổi Login/Refresh use case sang port và cập nhật unit test mock port. Giữ test integration cho JWT/security adapter.

**Bước 4:** giải quyết `AuthController`, `BookingController` đang nhận `UserDetailsCustom`: dùng principal abstraction ở module thích hợp hoặc resolver trong tầng web. Kiểm tra mọi import infrastructure, không chỉ riêng LoginUseCase.

**Bước 5:** đổi Maven dependency trong cùng thay đổi có thể build được:

- Bỏ infrastructure khỏi POM application sau khi import đã được xử lý.
- Thêm application vào infrastructure nếu port đặt ở application.
- Thêm infrastructure trực tiếp vào `start/pom.xml` vì hiện `start` đang nhận nó gián tiếp qua application.
- Controller chỉ phụ thuộc application sau khi xử lý kiểu principal.

**Bước 6:** chuyển starter trong root `<dependencies>` về đúng module. `<dependencyManagement>` quản lý phiên bản, không tự thêm dependency vào classpath. Giữ test tooling dùng chung có chủ đích; domain hiện dùng JPA annotation nên vẫn cần Jakarta Persistence, chưa thể xóa sạch framework dependency một cách cơ học.

Kiểm tra:

```powershell
rg -n 'import com.hoang.ddd.infrastructure' hoang-application/src/main/java
.\mvnw.cmd -B -ntp clean verify
```

Kết quả tìm import phải rỗng. Có thể thêm ArchUnit về sau để ngăn application phụ thuộc infrastructure; test kiến trúc cần đi cùng dependency POM, không thay thế nó.

## 3. Bài SRP — Scheduler có thể test, có thể bật/tắt

Hiện `BookingExpirationScheduler` vừa chọn booking, đổi trạng thái, giải phóng vé, vừa định lịch. Tách thành:

```text
BookingExpirationScheduler → ExpireBookingsUseCase → repositories
```

Đưa transaction và nghiệp vụ vào method public của use case bean riêng. Scheduler gọi bean đó để Spring proxy áp dụng transaction; đừng gọi method `@Transactional` nội bộ cùng object rồi cho rằng annotation luôn có hiệu lực.

Tạo cấu hình trong `start/src/main/java/com/hoang/SchedulingConfig.java`:

```java
package com.hoang;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {}
```

Profile test đặt `app.scheduling.enabled=false`; test nghiệp vụ hết hạn gọi use case trực tiếp với Clock cố định. Viết một test riêng kiểm tra cấu hình đăng ký scheduled task khi bật. `@Scheduled` cần cơ chế kích hoạt tương ứng. [Spring scheduling](https://docs.spring.io/spring-framework/reference/integration/scheduling.html).

Khi mới một instance: có thể xử lý lô nhỏ theo thứ tự ID. Khi hai instance: cả hai có thể thấy cùng booking PENDING. Dùng chuyển trạng thái có điều kiện `WHERE status = PENDING AND expires_at <= now`, khóa row hoặc chiến lược claim batch; chỉ bên claim thành công được giải phóng vé. Định nghĩa một transaction bao cả chuyển trạng thái và release.

Trước khi thêm thanh toán, viết test race hết hạn ↔ xác nhận thanh toán. Chỉ khóa scheduler toàn cục không tự giải quyết xung đột với request thanh toán.

## 4. Bài cache — Đảm bảo dữ liệu đọc phù hợp

Hiện cache `trip_tickets` có TTL 2 phút trong `RedisConfig`; booking xóa toàn bộ cache, scheduler không xóa. Khai báo `ticket_detail` có TTL không có nghĩa cache đó đã được sử dụng: hãy tìm `@Cacheable`/lệnh truy cập thật.

Thực hành theo thứ tự:

1. Test gọi đọc sơ đồ ghế lần 1 và lần 2 qua **Spring bean**, với Redis thật. Lần 2 phải deserialize được DTO có `Instant`, `BigDecimal` và danh sách; gọi trực tiếp object không kiểm tra cache.
2. Test đọc → đặt vé → đọc: trạng thái phải chuyển sang RESERVED.
3. Test đọc → hết hạn → đọc: phải trở lại AVAILABLE.
4. Test transaction thất bại không để lại thay đổi cache sai.
5. Test Redis unavailable và xác định policy cho API đọc/ghi; không tự bỏ qua lỗi mà không có metric/log.

Bước đầu có thể dùng eviction toàn bộ để sửa tính đúng. Sau đó xóa theo các trip ID bị ảnh hưởng để tránh một booking làm lạnh cache mọi chuyến.

Eviction phải gắn với commit DB. Có thể dùng cache manager `transactionAware()` cho thao tác hỗ trợ, hoặc event `AFTER_COMMIT` và handler xóa cache. Kiểm tra cả thứ tự proxy transaction/cache bằng integration test. [Spring Data Redis cache](https://docs.spring.io/spring-data/redis/reference/redis/redis-cache.html).

After-commit không đảm bảo DB và Redis có transaction nguyên tử chung: DB commit xong nhưng Redis có thể lỗi. Luồng đọc song song còn có thể nạp lại snapshot cũ. Chọn TTL, retry/event/outbox hoặc version của cache theo độ chính xác cần có. Điều kiện giữ vé cuối cùng vẫn phải kiểm tra trong PostgreSQL, không dùng cache làm nguồn quyết định ghế còn bán được.

## 5. Hợp đồng lỗi và dữ liệu riêng tư

Tạo exception cụ thể như `TicketUnavailableException`, `TicketNotFoundException`, rồi mapping tại advice. Hiện conflict booking ném `IllegalStateException` và rơi vào 500; `DataIntegrityViolationException` lại luôn mô tả trùng username/email kể cả constraint booking.

Không đổi mọi `IllegalStateException` thành 409: lỗi lập trình cũng có thể dùng exception đó. Mapping theo loại lỗi nghiệp vụ giúp test và client hiểu đúng.

Tách hai kiểu response:

- Sơ đồ ghế: seat ID/number, fare, status, trip ID; không chứa booking ID hay thông tin định danh hành khách.
- Chi tiết vé riêng tư: trả cho chủ booking hoặc role được phép; truy vấn kiểm tra owner trong application/repository, không chỉ ẩn nút trên frontend.

Test tối thiểu: chưa login → 401; A đọc vé A → 200; B đọc vé A → 403/404; admin theo policy → kết quả được quy định. Sơ đồ ghế phải không có trường dữ liệu cá nhân dù DB đang chứa các trường đó.

## 6. Những bài nâng cao đáng làm sau nền tảng

**Refresh token race:** hiện hai transaction có thể cùng đọc token chưa revoked rồi cùng tạo token mới. Viết integration test hai request; chọn row lock hoặc revoke có điều kiện và kiểm tra affected rows. Xem xét lưu hash refresh token; access token đã phát vẫn có thể dùng đến expiry nếu chưa có cơ chế thu hồi riêng.

**Email và transaction:** tạo event gửi sau commit cho bước học đầu tiên. Nếu cần đảm bảo không mất email khi process chết sau commit, ghi outbox cùng transaction; worker gửi với retry và idempotency. Không gọi dịch vụ bên ngoài trong transaction dài nếu có thể tách được.

**N+1:** `TicketResponse.of()` truy cập seat/station LAZY trong vòng lặp. Đo số query cho 1 và 100 vé; cân nhắc projection hoặc `@EntityGraph` phù hợp. Đừng đổi tất cả quan hệ thành EAGER. Thử `open-in-view=false` sau khi transaction/projection đã đúng.

**Booking code:** chuỗi 8 ký tự lấy từ UUID có không gian nhỏ hơn UUID đầy đủ. Unique constraint là chốt cuối, nhưng cần thiết kế mã đủ rộng và chính sách retry lỗi collision trong transaction mới; không chỉ giả định không bao giờ trùng.

**Idempotency:** client có thể gửi lại POST sau timeout dù lần đầu đã commit. Thêm idempotency key, unique constraint, payload hash và response đã lưu; cùng key/cùng payload trả lại kết quả cũ, khác payload phải bị từ chối.

**Migration:** không sửa V1–V5 đã chạy trên môi trường chia sẻ; thêm V6. V5 tạo unique constraint toàn cục có thể thất bại nếu dữ liệu lịch sử chứa nhiều row cho cùng trip/seat. Ngoài migrate DB rỗng, thử nâng cấp snapshot V4 có dữ liệu đại diện; quyết định chuyển đổi dữ liệu trước khi deploy.

**Domain model:** hiện entity nhiều setter. Bắt đầu với hành vi như `Booking.expire(now)` giữ invariant PENDING và thời hạn, rồi test thuần Java. Atomic SQL update bỏ qua method entity nên vẫn phải bảo đảm invariant ở câu query/transaction. Chưa cần tách đôi mọi entity JPA và domain trong một lần.

**Hoàn thành:** bạn chỉ ra được một thay đổi áp dụng mỗi nguyên tắc SOLID có lợi gì, giữ test xanh, và biết phần nào còn cần integration test để chứng minh.
