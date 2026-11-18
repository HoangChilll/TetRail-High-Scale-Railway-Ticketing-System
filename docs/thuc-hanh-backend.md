# Thực hành backend với TetRail

Bạn phụ trách luồng xem và giữ ghế trước giờ mở bán. Cuối bài, một người khác phải chạy lại được phép đo của bạn, biết một ghế có thể được giữ bao nhiêu lần và hiểu căn cứ cho thay đổi bạn đề xuất. Dành khoảng 60–90 phút mỗi buổi. Khi gặp kết quả khác dự đoán, ghi lại đầu vào và điều tra ngay ở buổi đó.

## Buổi 1 · Theo một lần đặt vé đến database

Mở `src/test/java/com/hoang/service/BookingServiceTest.java`. Tìm test `rejectsBookingWhenOnlyOneTicketCanBeReserved`. Trước khi chạy, viết ra hai con số là số vé khách yêu cầu và số vé repository báo đã giữ được. Dùng JDK 21 và chạy từ thư mục gốc trong PowerShell.

```powershell
.\mvnw.cmd '-Dtest=BookingServiceTest' test
```

Trong IDE, đặt breakpoint ở lệnh gọi `reserveTickets` trong `BookingService.createBooking` rồi debug riêng test trên. Xem `ticketIds`, `savedBooking.id` và `updatedCount`. Theo tiếp sang `TicketRepository.reserveTickets` để tìm điều kiện trên trạng thái vé, rồi mở `GlobalExceptionHandler.handleTicketUnavailable` để xem client sẽ nhận mã nào khi chạy qua HTTP.

**Bài tự kiểm**. Vẽ hai cột cho request A và B cùng muốn vé 11. Điền bốn dòng theo thời gian: cả hai đọc được vé, A cập nhật, A commit, B thử cập nhật. Ở mức cô lập mặc định Read Committed của PostgreSQL, nếu B cập nhật trong lúc A còn giữ khóa trên dòng đó, B đợi. Sau khi A commit, PostgreSQL kiểm tra lại điều kiện trên phiên bản mới của dòng. Hãy giải thích kết quả cho cả trường hợp A commit lẫn trường hợp A rollback. [PostgreSQL 16 về Read Committed](https://www.postgresql.org/docs/16/transaction-iso.html#XACT-READ-COMMITTED)

**Bàn giao**. Viết ba câu bằng lời của bạn. Câu một nêu điều kiện để giữ ghế. Câu hai nêu cách service xử lý khi chỉ giữ được một trong hai vé. Câu ba nêu phần bạn còn phải kiểm chứng trên DB thật. Gợi ý: test đang dùng Mockito, tự gán kết quả `thenReturn(1)` và khởi tạo service bằng `new`.

## Buổi 2 · Dựng dữ liệu và đi hết luồng HTTP

Dùng project Compose riêng để có DB và Redis riêng cho các bài tập. Các lệnh dưới đây dùng Docker Desktop đang chạy, cổng local 18089 còn trống và các giá trị trong `.env.example`. Giữ cùng cửa sổ PowerShell để dùng lại biến.

```powershell
$env:APP_PORT = '18089'
function lab { docker compose --env-file .env.example -p tetrail-mentor @args }
lab up -d --build --wait
lab ps
$base = 'http://localhost:18089'
Invoke-RestMethod "$base/actuator/health"
lab exec postgres psql -X -v ON_ERROR_STOP=1 -U tetrail -d tetrail
```

Tại dấu nhắc `tetrail=#`, dán đoạn SQL dưới đây. Nó tạo một tài khoản học tập, một đoàn tàu và 100 ghế. Mỗi lần chạy tạo thêm một chuyến mới với 100 vé trống để bạn làm lại bài đặt chỗ. Tài khoản được tạo trong DB lab vì endpoint đăng ký của repo gửi email, còn Compose mặc định chỉ chạy app, Redis và PostgreSQL.

```sql
BEGIN;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
INSERT INTO roles (name) VALUES ('ROLE_USER') ON CONFLICT (name) DO NOTHING;
INSERT INTO users (username, email, password)
VALUES ('mentor_lab', 'mentor@example.test', crypt('PracticeLab123', gen_salt('bf', 10)))
ON CONFLICT (username) DO NOTHING;
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u CROSS JOIN roles r
WHERE u.username = 'mentor_lab' AND r.name = 'ROLE_USER'
ON CONFLICT DO NOTHING;

INSERT INTO stations (code, name) VALUES ('LAB-A', 'Ga A'), ('LAB-B', 'Ga B')
ON CONFLICT (code) DO NOTHING;
INSERT INTO trains (train_number, name) VALUES ('MENTOR-01', 'Tau lab')
ON CONFLICT (train_number) DO NOTHING;
SELECT id AS train_id FROM trains WHERE train_number = 'MENTOR-01' \gset
SELECT id AS dep_id FROM stations WHERE code = 'LAB-A' \gset
SELECT id AS arr_id FROM stations WHERE code = 'LAB-B' \gset

INSERT INTO seats (train_id, seat_number, carriage_number, seat_class)
SELECT :train_id, 'S' || n, '1', 'STANDARD' FROM generate_series(1, 100) AS g(n)
ON CONFLICT (train_id, seat_number) DO NOTHING;
INSERT INTO trips (train_id, departure_station_id, arrival_station_id, departure_time, arrival_time)
VALUES (:train_id, :dep_id, :arr_id, now() + interval '1 day', now() + interval '1 day 2 hours')
RETURNING id AS trip_id \gset
INSERT INTO tickets (ticket_code, trip_id, seat_id, departure_station_id, arrival_station_id, fare, status)
SELECT format('LAB-%s-%s', :trip_id, s.id), :trip_id, s.id, :dep_id, :arr_id, 100000, 'AVAILABLE'
FROM seats s WHERE s.train_id = :train_id;
COMMIT;
SELECT trip_id, status, count(*) FROM tickets WHERE trip_id = :trip_id GROUP BY trip_id, status;
\q
```

Ghi lại `trip_id` thật và kiểm tra có đúng 100 vé `AVAILABLE`. Quay về PowerShell, đăng nhập và đọc sơ đồ ghế.

```powershell
$tripId = Read-Host 'Nhap trip_id vua tao'
$loginBody = @{ usernameOrEmail = 'mentor_lab'; password = 'PracticeLab123' } | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" -ContentType 'application/json' -Body $loginBody
$headers = @{ Authorization = "Bearer $($login.accessToken)" }
$seats = Invoke-RestMethod "$base/api/trips/$tripId/tickets" -Headers $headers
$seats.Count
$seats | Select-Object -First 3 ticketId,seatNumber,status
```

Chọn một `ticketId` từ kết quả. `seatId` nhận diện ghế vật lý và có thể xuất hiện trên nhiều chuyến. API booking nhận `ticketId`.

```powershell
$ticket = $seats | Where-Object status -eq 'AVAILABLE' | Select-Object -First 1
$buyer = "lab-$([guid]::NewGuid().ToString('N'))@example.test"
$body = @{ ticketIds = @($ticket.ticketId); buyerName = 'Lab'; buyerEmail = $buyer; buyerPhone = '0900000000' } | ConvertTo-Json
$booking = Invoke-RestMethod -Method Post -Uri "$base/api/bookings" -Headers $headers -ContentType 'application/json' -Body $body
$booking | Select-Object id,bookingCode,status,totalAmount
lab exec -T postgres psql -U tetrail -d tetrail -c "SELECT id, status, booking_id FROM tickets WHERE id = $($ticket.ticketId);"
```

**Bàn giao**. Bạn thấy booking `PENDING`, số tiền `100000`, vé `RESERVED` và `booking_id` bằng ID trong HTTP response. Gửi lại cùng request ngay. Lần này PowerShell sẽ báo lỗi HTTP 409. Dùng lệnh sau để kiểm tra xem lần thất bại có để lại booking thừa hay không.

```powershell
lab exec -T postgres psql -U tetrail -d tetrail -c "SELECT id, status FROM bookings WHERE buyer_email = '$buyer';"
```

Nếu nhận `401`, kiểm tra login và header bằng `Invoke-RestMethod "$base/api/auth/me" -Headers $headers`. Nếu sơ đồ trả mảng rỗng, kiểm tra `trip_id` trong SQL. Nếu POST đầu đã trả `409`, đọc lại trạng thái chính vé đó trong DB. Booking có thời hạn 10 phút và scheduler quét mỗi phút, nên thực hiện các bước đối chiếu liền nhau.

## Buổi 3 · Viết phép đo đầu tiên

Cài k6 trên Windows nếu lệnh `k6 version` chưa chạy được. Tạo file `seat-map.js` và tự gõ đoạn dưới. Mỗi lượt chạy gửi đúng một GET. Login đã thực hiện trước khi đo. [Cài đặt k6](https://grafana.com/docs/k6/latest/set-up/install-k6/)

```javascript
import http from 'k6/http';
import { check } from 'k6';

export const options = {
  scenarios: {
    readSeats: {
      executor: 'constant-arrival-rate',
      rate: Number(__ENV.RPS || 10),
      timeUnit: '1s',
      duration: __ENV.DURATION || '3m',
      preAllocatedVUs: 10,
      maxVUs: 30,
    },
  },
  thresholds: {
    checks: ['rate==1'],
    http_req_duration: ['p(95)<300'],
    dropped_iterations: ['count==0'],
  },
};

export default function () {
  const response = http.get(__ENV.SEATS_URL, {
    headers: { Authorization: 'Bearer ' + __ENV.ACCESS_TOKEN },
  });
  check(response, {
    'HTTP 200': (r) => r.status === 200,
    '100 ghe cua chuyen lab': (r) => r.status === 200 && r.json().length === 100,
  });
}
```

Từ thư mục chứa script, làm nóng app trong 30 giây rồi chạy phép đo ba lần. Có thể lưu output bằng `Tee-Object` như ví dụ. Mốc 300 ms là giả định cho bài tập.

```powershell
$env:SEATS_URL = "$base/api/trips/$tripId/tickets"
$env:ACCESS_TOKEN = $login.accessToken
k6 run -e RPS=5 -e DURATION=30s seat-map.js
1..3 | ForEach-Object { k6 run -e RPS=10 seat-map.js | Tee-Object "read-10rps-$_.txt" }
```

**Cách đọc**. Ở 10 lượt/giây trong 180 giây, bạn dự kiến khoảng 1.800 GET mỗi lần đo. Đọc `http_reqs` để biết đã gửi bao nhiêu, `checks` để biết dữ liệu trả về đúng không, `http_req_duration` để đọc p95, và `dropped_iterations` để biết bao nhiêu lượt chưa khởi chạy. Nếu p95 là 80 ms, khoảng 95% request trong lần đó có thời gian HTTP tối đa khoảng 80 ms. Nếu có lượt bị bỏ, kiểm tra CPU máy phát tải và số VU trước khi quy kết giới hạn cho ứng dụng. [Cách k6 phát tải](https://grafana.com/docs/k6/latest/using-k6/scenarios/executors/constant-arrival-rate/) · [Thresholds](https://grafana.com/docs/k6/latest/using-k6/thresholds/)

**Bàn giao**. Một bảng có từng lần chạy, commit, số ghế, RPS mục tiêu/thực tế, p95, số check sai và số lượt bỏ. Ghi cấu hình máy, giới hạn CPU/RAM của Docker và việc k6 chạy cùng máy. Lưu cả lần thất bại và nguyên nhân bạn tìm được.

## Buổi 4 · Kiểm tra tranh ghế và rollback

1. Chọn một vé còn trống. Dùng k6 với `per-vu-iterations`, đặt `vus: 20, iterations: 1`. Cho mỗi VU gửi một POST với cùng `ticketId`, cùng email riêng của đợt đo. Ghi số phản hồi theo từng mã, đối chiếu DB bằng email đó và ID vé ngay sau khi chạy. Kỳ vọng đúng 1 lần `201`, 19 lần `409`, một booking mới gắn với vé. Muốn đo độ trễ của người đặt thành công, tách số liệu theo mã. Hai mươi VU tạo một đợt tranh chấp, thời điểm PostgreSQL nhận từng câu lệnh vẫn có thể lệch nhau.
2. Đối chứng bằng 20 vé trống khác nhau. Cho VU thứ nhất lấy phần tử đầu tiên, VU thứ hai lấy phần tử tiếp theo. Cả 20 lượt phải nhận `201`; kiểm tra đủ 20 booking và đủ 20 vé đã giữ.
3. Thử một request chứa hai vé thuộc cùng chuyến, một vé còn trống và một vé vừa bị giữ. Dùng email mới để đối chiếu. Kỳ vọng HTTP 409, không có booking của request này và vé vốn trống vẫn `AVAILABLE`. Đây là thí nghiệm DB thật cho test đã debug ở buổi 1. Nếu sai, lưu cả hai ID, HTTP response và dữ liệu trước/sau rồi mới sửa code.

**Bàn giao**. Một bảng ghi điều dự đoán, điều quan sát qua HTTP và điều kiểm tra được trong DB cho từng trường hợp. Nếu muốn nhìn thấy hai lệnh chắc chắn chồng lấn, mở hai phiên psql trong DB lab, chọn một vé mới. Phiên A chạy `BEGIN` rồi cập nhật trạng thái của vé với điều kiện `status = 'AVAILABLE'`, để nguyên transaction. Phiên B chạy cùng UPDATE trong transaction riêng. Quan sát B trong khi A mở, sau đó cho A rollback và đọc số dòng B đã cập nhật. Rollback cả B để phục hồi vé lab. Làm lại với A commit trên một vé lab mới và giải thích sự khác nhau.

## Buổi 5 · Điều tra một nghi vấn trong chính project

Đọc `TicketService.getTripTickets`, `BookingService.createBooking`, `RedisConfig` và `JwtAuthenticationFilter`. Cache sơ đồ ghế có key theo chuyến, TTL hai phút. Booking thành công xóa toàn bộ cache `trip_tickets`. Bộ lọc JWT còn nạp user từ DB trên request đã xác thực. Vì vậy, khi điều tra cache, hãy đếm riêng những câu SQL đọc vé/ghế.

1. Dùng hai chuyến A, B. Để có chuyến thứ hai, chạy lại phần seed của buổi 2 và ghi ID mới. Đọc B một lần để tạo cache. Trong khoảng ngắn hơn TTL, đọc B lần nữa, đặt một ghế của A rồi đọc B lần thứ ba. Bật log `org.hibernate.SQL=DEBUG` trong profile local cho lần chẩn đoán. Đánh dấu từng HTTP request và kiểm tra lần nào truy vấn dữ liệu vé của B. Tắt log SQL chi tiết trước các phép đo thời gian.
2. Giữ tải đọc B cố định, trước tiên đo khi chỉ có GET, sau đó thêm một POST thành công mỗi 5 giây trên A trong ba phút. Chuẩn bị ít nhất 36 vé A riêng cho lượt đo có ghi. Theo dõi số truy vấn đọc vé B, p95 GET và tài nguyên bằng `docker stats`. Có thể thấy thêm truy vấn trong khi p95 gần như cũ, vì một cache miss sau mỗi lần đặt có thể chiếm dưới 5% số lượt đọc. Hãy ghi cả hai quan sát.
3. Nếu dữ liệu ủng hộ giả thuyết, thử thu hẹp phạm vi xóa cache đến các chuyến có vé vừa đặt. Một booking nhận danh sách vé, hãy xét đủ chuyến xuất hiện trong danh sách. Kiểm tra sơ đồ A phản ánh vé vừa giữ, sơ đồ B vẫn đúng, và cả ba bài ở buổi 4 vẫn đạt. Chạy lại cùng tải, cùng số ghế trống ban đầu và ba lần mỗi cấu hình. Nếu giả thuyết sai hoặc mức cải thiện nhỏ hơn độ dao động, ghi kết luận đó.

**Bàn giao**. Một trang gồm câu hỏi, cách tái hiện, bảng trước/sau, đánh đổi và điều còn chưa biết. Ví dụ, nếu chỉ đo trên một laptop với một instance, hãy nêu cấu hình đó khi mô tả khả năng chịu tải.

## Chuẩn bị phỏng vấn

Dành 90 giây để nói về một bài bạn đã tự làm: yêu cầu nghiệp vụ, giả thuyết ban đầu, phép thử khiến bạn tin hoặc đổi ý, kết quả và giới hạn. Mở đúng test hoặc số liệu nếu người nghe muốn xem sâu hơn.

- Nếu người phỏng vấn hỏi vì sao hai client có thể cùng nhìn thấy ghế trống, hãy vẽ lại thứ tự hai transaction ở buổi 1.
- Nếu họ hỏi căn cứ nào cho thấy booking không bị lưu dở, hãy chỉ dữ liệu của bài hai vé ở buổi 4.
- Nếu họ hỏi hệ thống chịu được bao nhiêu tải, hãy nêu endpoint, dữ liệu, máy chạy, thời gian, RPS thực tế, p95 và lỗi từ buổi 3. Mức cao nhất đã thử chỉ cho biết kết quả tại mức đó.
- Trong CV, chỉ điền số bạn tự đo. Ví dụ sau khi làm xong: “Viết kịch bản k6 đo API sơ đồ [số ghế] ghế ở [RPS] trong [thời lượng]; kiểm chứng bằng PostgreSQL rằng [kết quả bài tranh ghế].”

Sau buổi học, `lab stop` dừng stack và giữ dữ liệu để hôm sau học tiếp. Mở một cửa sổ PowerShell mới thì khai báo lại biến, hàm `lab` và đăng nhập lại.
