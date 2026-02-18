# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests -Pdefault

# Run stage (Debian-based JDK image)
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copy the jar from the build stage
COPY --from=build /app/target/*.jar app.jar


RUN apt-get update && apt-get install -y curl

COPY env-entry.sh /app/env-entry.sh
RUN chmod +x /app/env-entry.sh
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 CMD curl -fsS http://localhost:8080/v1/api/test/health || exit 1
RUN chmod +x /app/env-entry.sh && sed -i 's/\r$//' /app/env-entry.sh
ENTRYPOINT ["/app/env-entry.sh"]
