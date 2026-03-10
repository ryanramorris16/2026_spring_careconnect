# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy dependency descriptors first for better layer caching
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN mvn dependency:go-offline -B -Pdefault

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -Pdefault


FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Install curl for health checks
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN groupadd --system appuser && \
    useradd --system --gid appuser --no-create-home appuser

# Copy the jar from the build stage
COPY --from=build /app/target/*.jar app.jar


RUN apt-get update && apt-get install -y curl

COPY env-entry.sh /app/env-entry.sh
RUN chmod +x /app/env-entry.sh
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 CMD curl -fsS http://localhost:8080/v1/api/test/health || exit 1
RUN chmod +x /app/env-entry.sh && sed -i 's/\r$//' /app/env-entry.sh
ENTRYPOINT ["/app/env-entry.sh"]
