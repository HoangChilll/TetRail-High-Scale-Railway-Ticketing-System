FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy only build inputs; local secrets and target directories stay outside the image.
COPY pom.xml ./
COPY hoang-domain ./hoang-domain
COPY hoang-infrastructure ./hoang-infrastructure
COPY hoang-application ./hoang-application
COPY hoang-controller ./hoang-controller
COPY start ./start
RUN mvn -B -ntp -pl start -am -DskipTests package \
    && cp start/target/hoang-start-*.jar /workspace/app.jar

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
