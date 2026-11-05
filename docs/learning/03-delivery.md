# Docker, CI và CD theo từng bước

Điều kiện bắt đầu: sửa bean trùng, chạy được suite; làm bài PostgreSQL integration trước khi bật `verify` thành cổng kiểm tra. Các cấu hình sau là mẫu thực hành chưa được áp dụng/khởi chạy. Image tag trong lab là lựa chọn cố định theo major, không phải cam kết phiên bản mới nhất; môi trường thật nên chốt patch/digest đã kiểm tra.

## 1. Tạo đúng artifact trước khi tạo image

Root POM của bạn chỉ import `spring-boot-dependencies`; nó không thừa hưởng execution của `spring-boot-starter-parent`. Bổ sung vào plugin đã có trong `start/pom.xml`:

```xml
<executions>
    <execution>
        <goals>
            <goal>repackage</goal>
        </goals>
    </execution>
</executions>
```

Giữ version Boot plugin cùng version dependency BOM. [Spring Boot 3.3 packaging](https://docs.spring.io/spring-boot/3.3/maven-plugin/packaging.html).

Chạy:

```powershell
.\mvnw.cmd -B -ntp clean verify
jar tf start/target/hoang-start-1.0-SNAPSHOT.jar | Select-String 'BOOT-INF/' | Select-Object -First 5
```

Kỳ vọng có `BOOT-INF/classes` và dependency trong `BOOT-INF/lib`. Đây mới là kiểm tra cấu trúc; cần smoke test khởi động image ở bước sau để xác nhận runtime.

## 2. Tách cấu hình ra khỏi source

Trong `application.yaml`, thay các giá trị nhạy cảm bằng biến môi trường:

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  flyway:
    baseline-on-migrate: false

jwt:
  secret: ${JWT_SECRET}

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: never
```

**Merge vào các block YAML hiện có; không tạo thêm key `spring` hoặc `jwt` trùng nhau.** Giữ cấu hình Redis, expiration và mail cần thiết. Profile test đã có datasource và JWT riêng. `baseline-on-migrate=false` thích hợp cho DB mới của lab; DB cũ chưa có lịch sử Flyway phải có kế hoạch baseline riêng.

`JwtTokenProvider` đang decode Base64, trong khi comment YAML gọi secret là Hex. Chuẩn hóa thành Base64 và sửa comment. Tạo key 32 byte cho local bằng PowerShell, giữ trong biến, không in ra log:

```powershell
$tetrailKeyBytes = New-Object byte[] 32
$tetrailRng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$tetrailRng.GetBytes($tetrailKeyBytes)
$tetrailRng.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($tetrailKeyBytes)
$env:POSTGRES_PASSWORD = 'local-lab-only-change-me'
```

Giá trị password trên chỉ là ví dụ local. Nếu secret trong repo từng dùng thật, cần thay ở môi trường đó; sửa file không thu hồi secret đã lộ. Không dùng JWT key ví dụ của test ở production.

## 3. Bài Docker local

Image là gói filesystem/runtime; container là một process chạy từ image; volume giữ dữ liệu qua việc tạo lại container; Compose nối nhiều service phục vụ một môi trường.

Tạo `Dockerfile` ở root:

```dockerfile
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN groupadd --gid 10001 app && useradd --uid 10001 --gid app --no-create-home app
COPY --chown=10001:10001 start/target/hoang-start-1.0-SNAPSHOT.jar /app/app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

Bài này build JAR bằng Maven một lần rồi đưa chính JAR đó vào image. JRE đủ chạy ứng dụng; user không phải root; dạng exec của ENTRYPOINT giúp JVM nhận tín hiệu dừng. Tên JAR phải cập nhật nếu đổi version/artifactId.

Tạo `.dockerignore` theo allowlist cho Dockerfile trên:

```dockerignore
**
!start/
!start/target/
!start/target/hoang-start-1.0-SNAPSHOT.jar
!Dockerfile
!.dockerignore
```

Như vậy `.git`, secret local và source không cần thiết không vào build context. Kiểu ignore này dành cho Dockerfile chỉ copy JAR; nếu làm multi-stage biên dịch source thì phải đổi allowlist.

Tạo `compose.yaml` ở root:

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: tetrail
      POSTGRES_USER: tetrail
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:?Set POSTGRES_PASSWORD}
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U $$POSTGRES_USER -d $$POSTGRES_DB"]
      interval: 5s
      timeout: 3s
      retries: 20

  redis:
    image: redis:7-alpine
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 20

  app:
    build: .
    ports:
      - "127.0.0.1:8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/tetrail
      SPRING_DATASOURCE_USERNAME: tetrail
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD:?Set POSTGRES_PASSWORD}
      REDIS_HOST: redis
      JWT_SECRET: ${JWT_SECRET:?Set JWT_SECRET}
      MANAGEMENT_HEALTH_MAIL_ENABLED: "false"
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy

volumes:
  pgdata:
```

`localhost` trong app container là chính app container; DB hostname phải là service `postgres`, Redis là `redis`. Mẫu không publish DB/Redis ra host; nếu chạy app từ IDE thì thêm port bind loopback cho chúng và đổi datasource về localhost.

Compose chỉ chờ container chạy nếu dùng dependency mặc định; `service_healthy` kết hợp healthcheck chờ dependency sẵn sàng. [Docker Compose startup order](https://docs.docker.com/compose/how-tos/startup-order/).

Mẫu này phục vụ boot/database/cache lab. Nó chưa cấu hình SMTP nhận mail: `MANAGEMENT_HEALTH_MAIL_ENABLED=false` chỉ bỏ kiểm tra mail ở health, không làm email hoạt động. Trước khi thử đăng ký end-to-end, thêm SMTP sandbox local và trỏ `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, auth/TLS về sandbox; kiểm tra thư trong hộp thư sandbox.

Cho phép **chính xác** `/actuator/health` trong `SecurityConfig` trước `.anyRequest().authenticated()` để smoke test không cần JWT. Giữ health details tắt; không mở `permitAll` cho toàn bộ Actuator.

Chạy trong terminal đã có các biến local:

```powershell
docker info
.\mvnw.cmd -B -ntp clean verify
docker compose config --quiet
docker compose up -d --build
docker compose ps
docker compose logs --tail 100 app
Invoke-RestMethod http://localhost:8080/actuator/health
```

Nếu request đầu gặp connection refused, app có thể còn boot; đọc log và thử lại có giới hạn. `depends_on` chưa chứng minh ứng dụng đã sẵn sàng nhận HTTP.

**Kỳ vọng:** Postgres/Redis healthy, log ứng dụng báo started, `/actuator/health` là `UP`, gọi endpoint bảo vệ không token nhận 401. Tiếp theo làm smoke nghiệp vụ với fixture và SMTP sandbox.

Dừng lab bằng `docker compose down`; volume vẫn giữ DB. `down -v` sẽ xóa dữ liệu volume, chỉ dùng khi chủ động reset DB lab. Đổi POSTGRES_PASSWORD trong env không tự đổi password DB đã khởi tạo trong volume cũ.

Nếu muốn lưu biến local vào `.env`, thêm `.env` và `.env.*` vào `.gitignore`, ngoại trừ `.env.example` chỉ có tên biến/placeholder. Không in `docker compose config` đầy đủ vào log chia sẻ vì nó có thể hiện secret.

**Bài tập thêm:** tự viết Dockerfile multi-stage có Maven/JDK ở build stage và JRE ở runtime stage; so sánh caching, kích thước image và khả năng tái lập. Khi đưa vào CI, chọn rõ nơi build artifact để tránh vô tình đóng gói JAR khác bản vừa test.

## 4. Nâng CI hiện tại

Workflow hiện có test rồi package; chưa có integration-test phase, image hay deploy. Bản mẫu sau thay nội dung `.github/workflows/ci.yml` sau khi các bài trước hoàn thành. Dùng major action đang có trong repo; khi áp dụng thực tế kiểm tra release tương thích và pin commit SHA đã xác minh.

```yaml
name: TetRail CI

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]
  workflow_dispatch:

permissions:
  contents: read

jobs:
  verify:
    runs-on: ubuntu-latest
    timeout-minutes: 20
    permissions:
      contents: read
      packages: write
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven

      - name: Verify all modules
        run: bash ./mvnw -B -ntp clean verify

      - name: Save test reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-reports
          path: |
            **/target/surefire-reports/**
            **/target/failsafe-reports/**
            **/target/site/jacoco/**

      - name: Build the runtime image from the verified JAR
        run: |
          IMAGE="ghcr.io/${GITHUB_REPOSITORY,,}:${GITHUB_SHA}"
          echo "IMAGE=$IMAGE" >> "$GITHUB_ENV"
          docker build -t "$IMAGE" .

      - name: Authenticate registry
        if: github.event_name == 'push' && github.ref == 'refs/heads/main'
        env:
          GHCR_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: printf '%s' "$GHCR_TOKEN" | docker login ghcr.io -u "$GITHUB_ACTOR" --password-stdin

      - name: Publish image
        if: github.event_name == 'push' && github.ref == 'refs/heads/main'
        run: docker push "$IMAGE"
```

Testcontainers chạy trên runner có Docker; PostgreSQL test dùng container riêng, không cần secret DB production. PR build image để bắt lỗi Dockerfile nhưng không login/push. Image name chuyển repository thành chữ thường vì registry yêu cầu định dạng phù hợp. Phải cho repository quyền publish GHCR package tương ứng. [GitHub hướng dẫn publish image](https://docs.github.com/en/actions/tutorials/publish-packages/publish-docker-images).

Mẫu CI trên kiểm tra test và image build, **chưa chạy smoke container trên runner**. Bổ sung bước Compose ephemeral + kiểm tra health/auth, thu log khi lỗi và `down` trong cleanup trước khi coi image sẵn sàng release. Không publish nếu smoke thất bại.

Trong GitHub repository, cấu hình ruleset/branch protection để `verify` là required check trước merge; không dùng `continue-on-error` cho test. Coverage report chưa có cho đến khi bạn thêm JaCoCo.

## 5. CD — Làm staging trước

CI trả lời “thay đổi có đạt kiểm tra không?”. Continuous Delivery tạo bản có thể phát hành và có thể có bước phê duyệt; Continuous Deployment tự triển khai sau khi đạt điều kiện. Push image lên GHCR mới là xuất bản artifact, chưa phải deploy.

Repo chưa mô tả đích triển khai. Bài này chọn **một máy Linux staging với Docker Compose**, không giả định bạn đã có VPS hay tài khoản cloud. Chưa có địa chỉ server/secret nên không có thao tác deploy thật trong lần khảo sát này.

Thực hành staging theo thứ tự:

1. Chuẩn bị Docker/Compose và thư mục `/opt/tetrail`, quyền ghi cho tài khoản deploy.
2. Đặt `compose.deploy.yaml` trên server: dựa trên Compose local nhưng thay `app.build` bằng `app.image: ${APP_IMAGE:?Set APP_IMAGE}`. Giữ DB volume; cấu hình backup và SMTP sandbox. App bind loopback, reverse proxy phụ trách HTTPS nếu cần truy cập từ ngoài.
3. Tạo `.env.staging` trên server chứa DB password/JWT key; đặt quyền chỉ tài khoản deploy đọc. Server đăng nhập GHCR bằng quyền **read packages** nếu image private.
4. Lấy digest của image vừa publish. Đặt `APP_IMAGE=ghcr.io/owner/repo@sha256:...`; giữ lại digest phiên bản đang chạy trước đó.
5. Chạy lệnh dưới tại `/opt/tetrail`. Thay digest mẫu bằng giá trị thật; không copy nguyên dấu `...`.

```bash
export APP_IMAGE='ghcr.io/owner/repo@sha256:...'
docker compose --env-file .env.staging -f compose.deploy.yaml pull app
docker compose --env-file .env.staging -f compose.deploy.yaml up -d --no-build
curl --fail --retry 12 --retry-delay 5 --retry-connrefused --max-time 5 http://127.0.0.1:8080/actuator/health
```

6. Smoke test endpoint bảo vệ + hành trình nghiệp vụ bằng tài khoản staging. Health UP chưa chứng minh đặt vé hoạt động.
7. Khi smoke fail: thu log, đặt lại `APP_IMAGE` bằng digest trước, chạy `up -d --no-build` và kiểm tra lại. Image cũ chỉ chạy được nếu schema vẫn tương thích.
8. Diễn tập quy trình này thủ công một lần, rồi đưa đúng các lệnh vào job deploy.

Job CD cần `needs: verify` nếu chung workflow; chỉ chạy push main; `environment: staging`; secrets/vars cho host, user và key/credential; SSH host key phải được xác minh. Dùng `concurrency` riêng cho staging và không hủy deploy đang dở. Nếu tách workflow, phải kiểm tra upstream workflow thành công và đúng commit, không chọn bừa image `latest`.

Pipeline production về sau promote **cùng digest đã qua staging**, không build lại source. Ghi release SHA/digest và kết quả smoke để truy vết. Tag chứa SHA hữu ích nhưng tag vẫn có thể bị ghi đè; digest xác định nội dung image.

Compose một instance có thể có gián đoạn khi recreate app; bài này không hứa zero downtime. Muốn tránh gián đoạn cần chiến lược nhiều instance/reverse proxy và readiness phù hợp.

## 6. Migration và rollback

Flyway không tự hoàn tác dữ liệu chỉ vì bạn chạy lại image cũ. Ưu tiên thay đổi tương thích:

1. Thêm cột/bảng mới có thể cùng tồn tại với code cũ.
2. Deploy code chuyển tiếp, backfill dữ liệu và kiểm tra.
3. Sau khi hết cửa sổ rollback mới xóa cấu trúc cũ.

Backup phải có bài restore thử; không coi file backup tồn tại là đủ. Kiểm tra migration trên DB rỗng và snapshot dữ liệu phiên bản trước.

**Hoàn thành bài delivery:** từ checkout sạch chạy được verify và Compose; một PR lỗi không merge được; biết image nào đang chạy; triển khai staging thành công và rollback về digest cũ đã được diễn tập.
