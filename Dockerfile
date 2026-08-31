FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace

# Copy the build definition first so dependency resolution remains cached when only application
# sources change.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle

RUN chmod +x gradlew \
    && ./gradlew dependencies --no-daemon

COPY src src

RUN ./gradlew bootJar --no-daemon \
    && find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -exec cp {} /workspace/app.jar \;

FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app

# curl is the only runtime package needed for the Compose Actuator healthcheck.
RUN apt-get update \
    && apt-get install --no-install-recommends --yes curl \
    && rm -rf /var/lib/apt/lists/*

RUN useradd --system --uid 10001 --create-home --shell /usr/sbin/nologin appuser

COPY --from=builder --chown=appuser:appuser /workspace/app.jar app.jar

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65.0 -XX:InitialRAMPercentage=25.0 -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Seoul"

USER appuser

EXPOSE 8080
STOPSIGNAL SIGTERM

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
