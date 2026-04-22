# ─── Stage 1: Build native image ──────────────────────────────────
FROM ghcr.io/graalvm/native-image-community:21-ol9 AS build
RUN microdnf install -y maven
WORKDIR /app
COPY pom.xml .
# Download dependencies first (better layer caching)
RUN mvn dependency:go-offline -Pnative -B 2>/dev/null || true
COPY src ./src
COPY ../kafka-events ../kafka-events
RUN cd ../kafka-events && mvn clean install -DskipTests -B && \
    cd ../app && mvn clean package -Pnative -DskipTests -B

# ─── Stage 2: Minimal runtime ─────────────────────────────────────
FROM debian:bookworm-slim
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
WORKDIR /app
COPY --from=build /app/target/notification-service .
RUN chown -R appuser:appgroup /app
USER appuser
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD curl -f http://localhost:8083/actuator/health || exit 1
EXPOSE 8083
ENTRYPOINT ["./notification-service"]