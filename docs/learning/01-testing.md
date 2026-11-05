# Bài thực hành testing

Đọc [bài 0](README.md) trước. **Cập nhật 05/11/2026:** test booking và `Clock` đã được cài vào code, chạy `clean test` thành công: 74/74 test đạt (4 test mới). Các phần PostgreSQL/Testcontainers phía dưới vẫn là bài thực hành chưa chạy vì Docker engine trên máy khảo sát chưa hoạt động.

## Kiến thức nền trong 5 phút

- **Unit test:** gọi một class Java thật, thay dependency bên ngoài bằng mock. Nhanh và dễ cô lập lỗi; không chạy SQL, transaction hoặc Spring cache proxy.
- **Integration test:** ghép các thành phần thật để kiểm tra chúng phối hợp với nhau. `@SpringBootTest` khởi động context; test PostgreSQL dùng DB thật trong container còn kiểm tra Flyway/JPA/transaction.
- **Mock:** object giả do Mockito tạo, bạn định trước nó trả gì (`when(...).thenReturn(...)`) và kiểm tra có được gọi không (`verify(...)`). Nó không lưu dữ liệu thật.
- **Assertion:** câu khẳng định kết quả; sai thì test đỏ. Ví dụ `assertThat(total).isEqualByComparingTo("250000")`.
- **Fixture:** dữ liệu dựng trước để test chạy. Fixture cần đủ để tái hiện tình huống, không cần sao chép toàn bộ hệ thống.
- **Transaction:** một nhóm thao tác DB cùng thành công hoặc cùng rollback. Muốn chứng minh rollback phải đọc DB thật **sau** transaction thất bại; Mockito chỉ thấy exception.
- **Clock cố định:** thay đồng hồ hệ thống bằng thời điểm do test chọn, giúp kiểm tra chính xác “10 phút sau” mà không chờ 10 phút.
- **Testcontainers:** thư viện khởi động PostgreSQL tạm trong Docker khi chạy integration test; kết thúc test thì container được dọn. Nó cần Docker engine đang chạy.

## 1. Chọn loại test bằng câu hỏi cần trả lời

| Câu hỏi | Loại test | Thành phần thật |
|---|---|---|
| Tổng tiền, thiếu vé, xử lý reserve thất bại có đúng? | Unit, JUnit + Mockito | Use case, entity, DTO |
| Body sai trả 400, conflict trả 409? | MVC slice | Spring MVC, validation, advice |
| User A có đọc được vé user B? | Security integration | Security chain và kiểm tra quyền |
| SQL, unique constraint, rollback có đúng? | PostgreSQL integration | JPA, transaction, Flyway, PostgreSQL |
| Cache lần hai có deserialize được, hết hạn có invalidate? | Redis integration | Spring cache proxy + Redis |
| JAR/image có khởi động và nhận HTTP thật? | Smoke/E2E | Artifact đã đóng gói + các dependency |

Một test gọi `new BookTicketUseCase(...)` không có Spring proxy: `@Transactional`, `@CacheEvict` và `@Valid` không tự hoạt động. Test đó vẫn rất hữu ích, nhưng cần nói đúng nó chứng minh điều gì.

## 2. Bài 1 — Viết unit test booking đầu tiên

Đặt file ở:

```text
hoang-application/src/test/java/com/hoang/ddd/application/booking/usecase/BookTicketUseCaseTest.java
```

File test đã được tạo trong repo. Không cần thêm dependency: root POM đã có Spring Boot starter test chứa JUnit, Mockito, AssertJ. Hãy mở file này và tự gõ lại từng test vào một nhánh học tập nếu muốn luyện tay.

Đọc test theo ba bước: **Arrange** tạo đầu vào và hành vi dependency; **Act** gọi hành vi cần kiểm tra; **Assert** kiểm tra kết quả có ý nghĩa nghiệp vụ.

```java
package com.hoang.ddd.application.booking.usecase;

import com.hoang.ddd.application.booking.dto.CreateBookingCommand;
import com.hoang.ddd.domain.model.entity.Booking;
import com.hoang.ddd.domain.model.entity.Ticket;
import com.hoang.ddd.domain.repository.BookingRepository;
import com.hoang.ddd.domain.repository.TicketRepository;
import com.hoang.ddd.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookTicketUseCaseTest {
    @Mock BookingRepository bookings;
    @Mock TicketRepository tickets;
    @Mock UserRepository users;

    private BookTicketUseCase useCase;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-05T00:00:00Z"), ZoneOffset.UTC);
        useCase = new BookTicketUseCase(bookings, tickets, users, clock);
    }

    private CreateBookingCommand command() {
        return CreateBookingCommand.builder()
                .ticketIds(List.of(11L, 12L))
                .buyerName("Nguyen Van A")
                .buyerEmail("learner@example.com")
                .buyerPhone("0900000000")
                .build();
    }

    private List<Ticket> availableTickets() {
        return List.of(
                Ticket.builder().id(11L).fare(new BigDecimal("100000")).build(),
                Ticket.builder().id(12L).fare(new BigDecimal("150000")).build());
    }

    private void assignBookingIdWhenSaved() {
        when(bookings.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId(99L);
            return booking;
        });
    }

    @Test
    void createsPendingBookingWithSumOfTicketFares() {
        var command = command();
        when(tickets.findByIdIn(command.getTicketIds())).thenReturn(availableTickets());
        assignBookingIdWhenSaved();
        when(tickets.reserveTickets(command.getTicketIds(), 99L, command.getBuyerName(),
                Instant.parse("2026-11-05T00:00:00Z")))
                .thenReturn(2);

        var result = useCase.execute(command, null);

        assertThat(result.getId()).isEqualTo(99L);
        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getCurrency()).isEqualTo("VND");
        assertThat(result.getTotalAmount()).isEqualByComparingTo("250000");
        var saved = ArgumentCaptor.forClass(Booking.class);
        verify(bookings).save(saved.capture());
        assertThat(saved.getValue().getBuyerEmail()).isEqualTo(command.getBuyerEmail());
        verify(tickets).reserveTickets(command.getTicketIds(), 99L, command.getBuyerName(),
                Instant.parse("2026-11-05T00:00:00Z"));
    }

    @Test
    void doesNotCreateBookingWhenSomeTicketsDoNotExist() {
        var command = command();
        when(tickets.findByIdIn(command.getTicketIds()))
                .thenReturn(List.of(availableTickets().getFirst()));

        assertThatThrownBy(() -> useCase.execute(command, null))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(bookings);
        verify(tickets, never()).reserveTickets(anyList(), anyLong(), anyString(), any(Instant.class));
    }

    @Test
    void rejectsBookingWhenAnotherBuyerHasReservedOneTicket() {
        var command = command();
        when(tickets.findByIdIn(command.getTicketIds())).thenReturn(availableTickets());
        assignBookingIdWhenSaved();
        when(tickets.reserveTickets(command.getTicketIds(), 99L, command.getBuyerName(),
                Instant.parse("2026-11-05T00:00:00Z")))
                .thenReturn(1);

        assertThatThrownBy(() -> useCase.execute(command, null))
                .isInstanceOf(IllegalStateException.class);
    }
}
```

Test cuối đặc tả exception hiện tại. Sau khi tạo `TicketUnavailableException`, cập nhật assertion sang exception đó. **Không** viết `verify(bookings).delete(...)`: transaction rollback không phải use case gọi delete thủ công.

Để chạy riêng, sau khi đã từng chạy `clean test` bằng Java 21:

```powershell
.\mvnw.cmd -pl hoang-application -am test '-Dtest=BookTicketUseCaseTest' '-Dsurefire.failIfNoSpecifiedTests=false'
```

Giải thích từng công cụ:

- `@Mock`: repository giả, không kết nối DB.
- `thenReturn`: điều khiển kết quả dependency để đưa use case vào nhánh cần kiểm tra.
- `thenAnswer`: mô phỏng DB gán ID khi save; không giả lập toàn bộ DB.
- `ArgumentCaptor`: xem booking thật mà use case định lưu.
- `isEqualByComparingTo`: so giá trị tiền, không vướng khác scale `250000`/`250000.00`.
- Stub đặt trong từng test để tránh dữ liệu thừa và Mockito báo unused stubbing.

**Tự thực hành:** tạm đổi phép cộng tiền thành trả `BigDecimal.ZERO`; test đầu phải đỏ. Khôi phục implementation rồi chạy xanh. Tiếp theo tự thêm test người dùng đăng nhập được gắn vào booking.

## 3. Bài 2 — Test thời gian mà không sleep

`Instant.now()` trong use case làm assertion thời điểm hết hạn khó chính xác. Inject `java.time.Clock`:

Ba bước sau đã được cài trong repo; hãy đọc và giải thích lại chúng:

1. `private final Clock clock;` được thêm vào bản `BookTicketUseCase` đang dùng.
2. Use case lấy `Instant now = Instant.now(clock)` một lần, dùng cho thời hạn `now.plus(10, ChronoUnit.MINUTES)` và thời điểm cập nhật vé.
3. Bean Clock trong package `com.hoang` thuộc `start`:

```java
package com.hoang;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
```

4. Trong setup của test, truyền thêm clock cố định:

```java
Clock clock = Clock.fixed(Instant.parse("2026-11-05T00:00:00Z"), ZoneOffset.UTC);
useCase = new BookTicketUseCase(bookings, tickets, users, clock);
```

5. Test thành công assert `expiresAt` bằng `2026-11-05T00:10:00Z`.

Thêm import `java.time.Clock`, `Instant`, `ZoneOffset`. Bạn không cần mock Clock: clock cố định là implementation thật phù hợp cho test.

**Bài biên:** một booking có `expiresAt == now` thì đã hết hạn chưa? Chọn quy tắc rồi kiểm tra repository đang dùng `Before` (nhỏ hơn), có cần `LessThanEqual` không. Quy tắc phải thống nhất với bước xác nhận thanh toán.

## 4. Bài 3 — Validation và HTTP contract

Hiện DTO chỉ có `@NotEmpty` cho danh sách vé. Cần quyết định thêm:

| Đầu vào | Kỳ vọng đề xuất |
|---|---|
| Danh sách rỗng/null | 400, không gọi repository |
| ID âm hoặc null trong danh sách | 400 |
| Hai ID trùng nhau | 400, thông báo rõ trùng ID |
| Nhiều hơn giới hạn mua một lần | 400; giới hạn do nghiệp vụ quyết định |
| Vé vừa được người khác giữ | 409 |
| Không tìm thấy vé tra cứu | 404 |
| User không có quyền đọc vé | 403, hoặc 404 nếu chọn che sự tồn tại |

Ví dụ DTO: `List<@NotNull @Positive Long> ticketIds` và `@Size(max = 6)` nếu bạn quyết định giới hạn là 6. Đây là ví dụ quyết định nghiệp vụ, chưa phải quy định của hệ thống.

Kiểm tra trùng ID trong use case bằng số phần tử của `HashSet`. Giữ validation nghiệp vụ ở application/domain nếu use case còn được gọi từ job hoặc consumer ngoài HTTP.

Tạo `BookingControllerTest` theo `TicketControllerTest` để kiểm tra JSON/validation. Tạo một nhóm test khác có SecurityConfig thật để kiểm tra 401/403; tắt filter chỉ để test web slice không thể kiểm tra quyền.

## 5. Bài 4 — Integration test PostgreSQL và Flyway

Không cần bỏ H2 ngay. Thêm test riêng cho điều H2 + Mockito chưa chứng minh: schema PostgreSQL thật, unique constraint, atomic update và rollback.

Thêm dependency test vào `start/pom.xml`:

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

Đây là tọa độ/API của Testcontainers 1.x do BOM Spring Boot 3.3.5 quản lý. Không chép dependency 2.x từ tài liệu mới vào bài này mà chưa đánh giá tương thích. [Tài liệu PostgreSQL Testcontainers](https://java.testcontainers.org/modules/databases/postgres/) giải thích container DB dùng cho test.

Để auth test không đòi Redis, thêm `@ConditionalOnProperty(name = "app.cache.enabled", havingValue = "true", matchIfMissing = true)` vào `RedisConfig`. Import annotation từ `org.springframework.boot.autoconfigure.condition`. Profile test đặt `app.cache.enabled: false` và `spring.cache.type: none`. Chỉ đặt cache type không đủ khi code tự khai báo RedisCacheManager bean.

Nếu đã bật scheduler ở bài thiết kế, profile test cũng đặt `app.scheduling.enabled: false`.

Tạo `start/src/test/java/com/hoang/PostgresMigrationIT.java`:

```java
package com.hoang;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "app.cache.enabled=false",
        "spring.cache.type=none",
        "app.scheduling.enabled=false"
})
@ActiveProfiles("test")
@Testcontainers
class PostgresMigrationIT {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @MockBean JavaMailSender mailSender;
    @Autowired JdbcTemplate jdbc;

    @Test
    void appliesTheTicketInventoryMigration() {
        Integer applied = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where version = '5' and success = true",
                Integer.class);
        assertThat(applied).isEqualTo(1);
    }
}
```

PostgreSQL 16 là lựa chọn cố định cho lab; nếu môi trường chạy thật dùng major khác, đổi test cho khớp. Test này chỉ chứng minh context + migration khởi động được, chưa kiểm tra hết nghiệp vụ.

Tên `*IT` cần Failsafe. Thêm plugin vào root `<build><plugins>`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <version>3.5.4</version>
    <executions>
        <execution>
            <goals>
                <goal>integration-test</goal>
                <goal>verify</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Chạy Docker Desktop với Linux containers rồi:

```powershell
docker info
.\mvnw.cmd -B -ntp verify
```

`test` chạy Surefire; `verify` đi qua cả integration-test và bước xác nhận kết quả Failsafe. Không chỉ gọi `integration-test` rồi cho rằng build đã kiểm tra hết lỗi. [Maven Failsafe](https://maven.apache.org/surefire/maven-failsafe-plugin/usage.html).

`AuthE2ETest` hiện vẫn chạy qua Surefire vì tên kết thúc bằng `Test`; chỉ đổi thành `AuthFlowIT` sau khi đã nối Failsafe. Có thể gom hành trình register → login → refresh → logout vào một test với biến local, và tách từng tình huống lỗi thành test độc lập.

## 6. Bài 5 — Chứng minh không bán trùng và không giữ vé một phần

Tạo `BookingConcurrencyIT` trong `start`. Chuẩn bị fixture **đã commit** gồm ga, tàu, ghế, chuyến và vé AVAILABLE. Gọi use case thật qua Spring bean, không mock repository.

Thử nghiệm A — rollback:

1. Vé 11 AVAILABLE, vé 12 đã RESERVED bởi booking khác.
2. Gọi đặt cả 11 và 12.
3. Kỳ vọng exception tranh chấp.
4. Đọc lại trong transaction mới: vé 11 vẫn AVAILABLE, vé 12 giữ nguyên booking cũ; không còn booking mới bị tạo dở.

Thử nghiệm B — đồng thời:

1. Một vé AVAILABLE, hai người cùng yêu cầu vé đó.
2. Dùng executor hai thread; `CountDownLatch` để cùng bắt đầu, timeout rõ ràng.
3. Mỗi lời gọi đi qua Spring bean để có transaction riêng.
4. Chờ cả hai kết thúc, luôn đóng executor trong `finally`.
5. Assert đúng một booking thành công; request thua nhận conflict theo contract.
6. Đọc DB trong transaction mới: vé có đúng một booking, không có booking thua còn PENDING.

Không đặt transaction bao ngoài test rồi chờ worker đọc fixture chưa commit. Không dùng `Thread.sleep` để giả định hai request đã chạy cùng lúc. Không đặt barrier **sau khi** một transaction đã khóa row rồi đợi transaction kia lấy chính row đó: test có thể tự gây deadlock.

Mẫu hai thread là kiểm tra tranh chấp cơ bản. Nếu cần chứng minh sự chồng lấn ở điểm reserve, bổ sung đồng bộ trước câu UPDATE và timeout; không coi stress test chạy may mắn nhiều lần là chứng minh tuyệt đối.

## 7. Coverage có mục đích

Trước hết xem những nhánh rủi ro đã có test chưa: đặt trùng, giữ một phần, hết hạn, truy cập vé người khác, replay refresh token. Sau đó mới đo coverage.

Khi thêm JaCoCo, root đang có `<argLine>-XX:+EnableDynamicAgentLoading</argLine>` trong Surefire. Cần giữ argLine của agent bằng late evaluation:

```xml
<!-- Trong properties: tránh placeholder chưa được gán khi agent không chạy -->
<argLine></argLine>

<!-- Trong configuration của Surefire và Failsafe -->
<argLine>@{argLine} -XX:+EnableDynamicAgentLoading</argLine>
```

Thêm `jacoco-maven-plugin` với version cố định đã kiểm tra hỗ trợ Java 21, goal `prepare-agent`, `report` ở phase `verify`; sau đó đọc từng module tại `target/site/jacoco/index.html`. [JaCoCo giải thích argLine](https://www.jacoco.org/jacoco/trunk/doc/prepare-agent-mojo.html).

Report ở `start` không tự tổng hợp coverage source module khác. Tổng hợp multi-module cần `report-aggregate` và module report có dependency phù hợp. Chỉ đặt ngưỡng sau khi có baseline, ưu tiên branch coverage của nghiệp vụ và nâng dần; đừng thêm test getter/setter để làm đẹp số.

**Bạn đạt bài này khi:** phân biệt được mock với DB thật, tự viết được một test đỏ có ý nghĩa, giải thích được vì sao unit test không đảm bảo transaction/cache, và có test PostgreSQL cho rollback/tranh chấp.
