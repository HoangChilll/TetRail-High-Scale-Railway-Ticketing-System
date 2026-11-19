# TetRail

Backend Spring Boot, dùng JDK 21, PostgreSQL 16 và Redis 7.4. Dự án có một module Maven tại `pom.xml`. Mã ứng dụng nằm trong `src/main`, bộ test trong `src/test`, cấu hình chạy từ máy phát triển trong `config/` và tài liệu thực hành trong `docs/`.

## Chạy StartApplication trong IntelliJ IDEA (Windows)

Mở Docker Desktop. Tại thư mục gốc project, tạo cấu hình local một lần:

```powershell
if (!(Test-Path .env)) { Copy-Item .env.example .env }
```

Nếu đã có `.env`, giữ file hiện tại. Các giá trị mẫu chỉ dành cho máy phát triển.

Khởi động PostgreSQL và Redis trước khi chạy ứng dụng:

```powershell
docker compose -f compose.yaml -f config/compose.local.yaml up -d --wait postgres redis
```

Trong IntelliJ, mở `pom.xml` dưới dạng Maven project. Nếu dự án đang mở, chọn **Reload All Maven Projects** trong cửa sổ Maven. Nếu POM còn hiển thị màu xám, bỏ chọn `pom.xml` trong **Settings → Build Tools → Maven → Ignored Files**, rồi tải lại Maven.

Chọn **StartApplication** trên thanh Run rồi bấm **▶** hoặc mở `src/main/java/com/hoang/StartApplication.java` và chạy `main()`. Cấu hình dùng JDK của module `hoang.com`, thư mục làm việc là gốc dự án và profile `local` đọc các giá trị trong `.env`.

Kiểm tra ứng dụng:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

Kết quả mong đợi: `status` là `UP`. Project cung cấp API; đường dẫn `/` yêu cầu xác thực và không có trang giao diện.

Profile local dùng PostgreSQL tại `localhost:15432`, Redis tại `localhost:16379`, với dữ liệu riêng trong stack `tetrail-local`. Có thể đổi bằng `LOCAL_POSTGRES_PORT` và `LOCAL_REDIS_PORT` trong `.env`; sau đó chạy lại lệnh Compose. Dừng dependency và giữ dữ liệu bằng:

```powershell
docker compose -f compose.yaml -f config/compose.local.yaml stop postgres redis
```

## Chạy toàn bộ bằng Docker

Sau khi tạo `.env` như trên:

```powershell
docker compose up -d --build --wait
```

Dùng một cách chạy ứng dụng tại một thời điểm vì mặc định cùng dùng cổng 8080. Cách Docker dùng `APP_PORT` trong `.env` để đổi cổng HTTP; cách IntelliJ/Maven dùng biến môi trường `SERVER_PORT`.

## Kiểm tra build

```powershell
.\mvnw.cmd clean verify
```

SMTP và Telegram cần cấu hình dịch vụ thật nếu muốn gửi thông báo. Chạy local không kiểm tra SMTP trong health endpoint; chức năng đăng ký vẫn cần SMTP để gửi email chào mừng.
