# Học cải tiến TetRail từ chính codebase

Tài liệu được viết khi đọc commit `74e1008`, ngày 05/11/2026. Đây là bài thực hành: các đoạn cấu hình và code bên dưới là thay đổi đề xuất để bạn tự áp dụng, chưa được cài vào ứng dụng. **Cập nhật 05/11/2026:** bản `application/usecase/BookTicketUseCase.java` đã được xóa trong working tree; lỗi query `Timestamp`/`Instant` tiếp theo cũng đã được sửa. Xem kết quả chạy lại ở dưới.

Mục tiêu: bạn giải thích được vì sao sửa, chứng minh bằng test, rồi đóng gói và triển khai cùng một phiên bản đã kiểm tra.

Đọc theo thứ tự:

1. Tài liệu này: hiểu hệ thống, xác định vấn đề, chuẩn hóa cách build.
2. [Unit test và integration test](01-testing.md): làm một bài hoàn chỉnh với booking, sau đó kiểm tra PostgreSQL thật.
3. [SOLID và tính đúng của nghiệp vụ](02-design.md): refactor từng bước khi đã có test bảo vệ.
4. [Docker, CI và CD](03-delivery.md): đóng gói, tự động kiểm tra, phát hành và rollback.

## 1. Bản đồ dự án

Stack đọc từ POM: Java 21, Spring Boot 3.3.5, Maven multi-module, Spring Security/JWT, PostgreSQL/JPA, Flyway, Redis, JUnit 5/Mockito.

| Module | Vai trò đang có | File nên đọc đầu tiên |
|---|---|---|
| `start` | Điểm khởi động, cấu hình runtime, test context/API | `StartApplication`, `application.yaml` |
| `hoang-controller` | HTTP, validation đầu vào, chuyển exception thành HTTP | `BookingController`, `GlobalExceptionHandler` |
| `hoang-application` | Điều phối đăng nhập, đặt vé, đọc vé | `booking/usecase/BookTicketUseCase` |
| `hoang-domain` | Entity, trạng thái, interface repository | `Booking`, `Ticket`, `TicketRepository` |
| `hoang-infrastructure` | JPA query, JWT, Redis, email, migration | `JpaTicketRepository`, `RedisConfig` |

Luồng đặt vé hiện tại:

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Security filter
    participant W as BookingController
    participant U as BookTicketUseCase
    participant D as PostgreSQL
    C->>S: POST /api/bookings + JWT
    S->>W: User đã xác thực
    W->>U: command + userId
    U->>D: Đọc vé và tính tổng tiền
    U->>D: INSERT booking PENDING
    U->>D: UPDATE vé WHERE status = AVAILABLE
    alt Đủ số vé
        U-->>W: BookingResponse
        W-->>C: 201
    else Có vé không giữ được
        U-->>W: Exception, transaction rollback
        W-->>C: Hiện rơi vào handler 500; nên định nghĩa 409
    end
```

Điểm tốt cần giữ: controller khá mỏng; constructor injection phổ biến; nghiệp vụ đã dùng interface repository; tiền dùng `BigDecimal`; giữ vé dùng update có điều kiện trong DB; schema có migration và unique constraint; đã có test và CI.

Các module hiện là một ứng dụng triển khai chung. Chưa cần tách microservice để học các kỹ thuật này.

### Kết quả kiểm tra thực tế

Đã chạy `clean test` bằng Maven của IntelliJ, Java 21, dependency cache local ở chế độ offline. Lần chạy trong sandbox bị chặn đọc JAR; chạy lại ngoài sandbox đã biên dịch và thực thi suite:

| Module | Số test | Kết quả |
|---|---:|---|
| domain | 3 | Đạt |
| infrastructure | 14 | Đạt |
| application | 23 | Đạt |
| controller | 22 | Đạt |
| start | 8 | Lỗi nạp context |

Tổng 70 test: 62 đạt, 8 errors, 0 assertion failures, 0 skipped. Build **thất bại**. Nguyên nhân gốc trong report: `ConflictingBeanDefinitionException`, hai class `BookTicketUseCase` đăng ký cùng tên `bookTicketUseCase`. Sau khi sửa lỗi này phải chạy lại để biết có lỗi tiếp theo hay không.

**Kết quả chạy lại sau khi bản cũ được xóa:** `clean test` đi qua 5 module đầu nhưng `start` có 8 errors do lỗi query trong `JpaTicketRepository`: `CURRENT_TIMESTAMP` được Hibernate hiểu là `java.sql.Timestamp`, còn `Ticket.issuedAt` là `java.time.Instant`. Đã đổi ba câu UPDATE để nhận thời điểm kiểu `Instant` qua tham số, cập nhật chữ ký repository và nơi gọi. Chạy lại `clean test`: **BUILD SUCCESS, 70/70 test đạt**. Test hiện tại chứng minh query được Spring khởi tạo và suite hiện có chạy; chưa có integration test thực hiện cả ba câu UPDATE trên PostgreSQL thật. Các phần phía dưới mô tả ảnh chụp ban đầu của commit để bạn học cách tìm và sửa lỗi đầu tiên.

**Cập nhật bài testing:** đã thêm bốn unit test booking và inject `Clock` để kiểm tra chính xác hạn giữ vé. Chạy `clean test` sau thay đổi: **BUILD SUCCESS, 74/74 test đạt**. Phần PostgreSQL/Testcontainers chưa chạy vì Docker engine hiện chưa hoạt động; xem [bài testing](01-testing.md).

Report được sinh tại `<module>/target/surefire-reports/`; report và output build không được commit. Chưa kiểm tra Docker runtime, PostgreSQL/Redis thật hoặc deploy. Toàn bộ thay đổi được thêm trong lần nghiên cứu này là tài liệu trong `docs/learning`, không phải các bản sửa source được mô tả trong bài tập.

## 2. Những vấn đề cụ thể cần ưu tiên

Đường dẫn trong bảng tính từ root repo. “Đọc code” nghĩa là bằng chứng tĩnh, không phải kết quả đã tái hiện bằng request thực tế.

| Ưu tiên | Phát hiện và bằng chứng | Hệ quả / cách chứng minh |
|---|---|---|
| P0 | Hai class cùng tên `BookTicketUseCase`, cùng `@Service`: `application/usecase` và `application/booking/usecase` | Đã tái hiện xung đột tên `bookTicketUseCase` khi chạy suite: 8 test ở start lỗi context. |
| P1 | `start/pom.xml` có Boot plugin nhưng thiếu execution `repackage`; root chỉ import BOM, không kế thừa Boot starter parent | `package` chưa được cấu hình tạo executable Boot JAR. Kiểm tra `BOOT-INF/` trước khi Docker hóa. |
| P1 | `BookingExpirationScheduler` có `@Scheduled`, toàn bộ source chưa có `@EnableScheduling` | Chưa thấy cấu hình kích hoạt lịch chạy; booking có thể không tự nhả vé như mong đợi. |
| P1 | Scheduler import `CacheEvict` nhưng không dùng; đọc sơ đồ ghế có `@Cacheable` | Sau khi giải phóng vé, dữ liệu cache có thể giữ trạng thái cũ đến TTL. |
| P1 | `GetTicketsPageUseCase` dùng `if (tripId) ... else if (status)` | Request có cả hai filter bỏ qua `status`. Viết test với vé cùng chuyến nhưng khác trạng thái. |
| P1 | `SecurityConfig` chỉ yêu cầu authenticated cho các API vé; `TicketResponse` chứa thông tin hành khách; use case tra cứu không kiểm tra chủ sở hữu | Bất kỳ user đăng nhập nào cũng có đường truy vấn thông tin vé người khác. Cần test user A/user B và tách dữ liệu công khai khỏi dữ liệu riêng. |
| P1 | `application.yaml` chứa DB password và JWT secret trực tiếp; Actuator expose `*`, security dùng chung authenticated | Tách cấu hình môi trường; giới hạn endpoint vận hành và quyền truy cập. Nếu secret đã dùng thật, thay secret ở nơi sử dụng. |
| P1 | `application-test.yaml` dùng H2, `ddl-auto: create-drop`, Flyway tắt | Test hiện không chứng minh migration V1–V5, kiểu PostgreSQL hay tranh chấp update chạy đúng. |
| P2 | `hoang-application/pom.xml` phụ thuộc infrastructure; `LoginUseCase` import JWT provider và principal cụ thể | Thay cách phát token kéo theo thay tầng use case. Đây là bài DIP sát code nhất. |
| P2 | Root POM đặt nhiều starter trong `<dependencies>` | Các module con thừa hưởng web/security/JPA/actuator...; ranh giới module không được dependency graph bảo vệ. |
| P2 | `AuthE2ETest` dùng static token và `@Order` | Test phụ thuộc lẫn nhau; chạy một method riêng có thể lỗi vì thiếu dữ liệu do method trước tạo. |
| P2 | `RegisterUseCase` gửi email trong transaction | Email có thể đã gửi nhưng DB commit lỗi; email chậm kéo dài transaction. Học after-commit rồi outbox. |

Không cần sửa cả bảng cùng lúc. P0/P1 giúp hệ thống hoạt động đúng; refactor kiến trúc và công cụ chất lượng đi sau.

`@AutoConfigureMockMvc(addFilters = false)` trong `TicketControllerTest` phù hợp để kiểm tra mapping/JSON, nhưng không chứng minh phân quyền. `AuthE2ETest` dùng MockMvc là test tích hợp trong process, chưa phải kiểm thử HTTP qua ứng dụng đã đóng gói.

## 3. Bài 0 — Có một cách build lặp lại được

**Mục tiêu:** máy cá nhân và CI dùng cùng Java/Maven, đọc được lỗi đầu tiên của build.

Mở PowerShell tại `D:\hoang.com`:

```powershell
java -version
Get-Command mvn -ErrorAction SilentlyContinue
```

Tại thời điểm khảo sát, Java trên PATH là 21; Maven chưa có trên PATH nhưng có trong IntelliJ. Bạn có thể gọi trực tiếp:

```powershell
$tetrailMvn = 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.0.1\plugins\maven-plugin\lib\maven3\bin\mvn.cmd'
& $tetrailMvn -version
& $tetrailMvn -B -ntp clean test
```

`clean` xóa output build trong `target`, không xóa source. Lần kiểm tra đầu đã gặp class cũ có bytecode Java 23 trong khi runtime là Java 21; đây là lý do phải thử build sạch. Đồng bộ Project SDK, Maven Runner JRE và Maven Importer JDK trong IntelliJ về Java 21.

Sau khi lệnh trên chạy được, tạo Maven Wrapper bằng Maven đang có:

```powershell
& $tetrailMvn wrapper:wrapper '-Dmaven=3.9.9'
.\mvnw.cmd -version
.\mvnw.cmd -B -ntp clean test
```

3.9.9 ở đây là phiên bản cố định cho bài thực hành, không phải tuyên bố phiên bản mới nhất. Kiểm tra và commit `mvnw`, `mvnw.cmd`, `.mvn/wrapper/`; không commit `target`. Lần tạo wrapper có thể cần internet.

Từ các bài tiếp theo: Windows dùng `.\mvnw.cmd`; CI Linux dùng `bash ./mvnw`. Có thể thay bằng `& $tetrailMvn` khi chưa tạo wrapper.

**Đọc lỗi đúng cách:** tìm module thất bại trong Reactor Summary → đọc lỗi đầu tiên → đọc `Caused by` sâu nhất. Khi context fail, hàng loạt test fail theo thường chỉ có một nguyên nhân gốc.

**Bài tập sửa bean trùng:**

1. Tìm tất cả tham chiếu tới `BookTicketUseCase` bằng Find in Files hoặc `rg -n 'BookTicketUseCase'`.
2. Xác nhận `BookingController` đang import bản trong `application.booking.usecase`.
3. Đối chiếu hai implementation. Bản controller đang dùng còn có user association và cache eviction.
4. Loại bản cũ khỏi source sau khi kiểm tra không còn nơi dùng. Không chỉ đổi tên bean để giữ hai nghiệp vụ trùng nhau.
5. Chạy lại context test và toàn bộ suite; đừng sửa bằng bật bean overriding hay tắt test context.

Chạy riêng context test qua reactor:

```powershell
.\mvnw.cmd -pl start -am test '-Dtest=StartApplicationTests' '-Dsurefire.failIfNoSpecifiedTests=false'
```

`-pl` chọn module; `-am` build các module mà nó cần. Flag cuối cho phép các module phụ thuộc không chứa test được chọn; không dùng nó để che mất test trong pipeline toàn bộ.

**Hoàn thành khi:** build sạch dùng Java 21; không có bean trùng; context test chạy; bạn giải thích được thứ tự reactor không nhất thiết giống thứ tự `<modules>`.

## 4. Lộ trình theo các thay đổi nhỏ

| Bài | Sản phẩm cần có | Điều kiện hoàn thành |
|---|---|---|
| 0 | Wrapper, build sạch, hết bean trùng | Chạy được cùng lệnh trên máy và CI |
| 1 | Unit test booking | Thành công, thiếu vé, tranh chấp; tự làm test đỏ rồi sửa |
| 2 | Clock, input validation, exception nghiệp vụ | Test thời gian cố định, 400/409 rõ nghĩa |
| 3 | PostgreSQL integration test | Flyway chạy; rollback và tranh chấp có kiểm chứng |
| 4 | Scheduler và cache | Hết hạn giải phóng vé; đọc cache thấy trạng thái mới |
| 5 | Phân quyền và SOLID | User không đọc vé người khác; application không import infrastructure |
| 6 | Docker Compose local | App + DB + Redis khởi động từ source trên môi trường sạch |
| 7 | CI | PR phải qua verify, giữ report và kiểm tra image build |
| 8 | CD staging | Phát hành image theo SHA/digest, smoke test, diễn tập rollback |

Mỗi bài nên tách thành một commit/PR. Đừng trộn nâng Spring Boot, refactor domain, Docker và sửa nghiệp vụ vào một thay đổi lớn. Đánh giá nâng phiên bản framework bằng changelog và test tương thích trong PR riêng.

## 5. Cách học để không chỉ chép code

Với mỗi thay đổi, viết 5 dòng vào ghi chú của bạn:

1. Hành vi nào đang sai hoặc chưa được đảm bảo?
2. Tôi dự đoán test nào thất bại, vì sao?
3. Thay đổi nhỏ nhất để sửa là gì?
4. Bằng chứng sau sửa là gì? Có phần nào test chưa chứng minh?
5. Nếu tăng lên hai instance hoặc có request đồng thời thì sao?

Ví dụ: unit test ném exception khi không đủ vé **chưa chứng minh rollback**. Rollback là hành vi của transaction manager + DB, cần bài integration test.

Sau một bài, thử cố ý làm sai một điều kiện rồi xem test có bắt được không; hoàn tác thay đổi cố ý đó. Đây là cách đánh giá chất lượng test thiết thực hơn chạy theo 100% coverage.
