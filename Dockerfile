FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy only build inputs; local secrets and target directories stay outside the image.
COPY pom.xml ./
COPY src ./src
RUN mvn -B -ntp -DskipTests package \
    && cp target/tetrail.jar /workspace/app.jar

FROM eclipse-temurin:21-jre-jammy AS runtime
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app --home-dir /app app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/app.jar ./app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
