

# ── Stage 1: Build ───────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app

COPY pom.xml .
COPY src ./src


RUN apt-get update && apt-get install -y maven && \
    mvn clean package -DskipTests

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN groupadd -r appgroup && useradd -r -g appgroup appuser

COPY --from=builder /app/target/*.jar app.jar

RUN chown appuser:appgroup app.jar
USER appuser

EXPOSE 8082

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
