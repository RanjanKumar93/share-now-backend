# =========================================================================
# 🏗️ STAGE 1: Build & Compile Environment (Lightweight Alpine Base)
# =========================================================================
# We start with a generic, stable Alpine Linux image instead of guessing tags
FROM alpine:3.20 AS builder

# Install OpenJDK 21 and Maven directly using the Alpine package manager
# This bypasses the broken Docker Hub tag issue entirely!
RUN apk add --no-cache openjdk21 maven

WORKDIR /app

# Optimize caching layers by copying the descriptor first
COPY pom.xml .

# Download dependencies safely ahead of code transfers
RUN mvn dependency:go-offline -B

# Copy the application source code files
COPY src ./src

# Compile and package into a clean executable runnable package
RUN mvn clean package -DskipTests

# =========================================================================
# 🛡️ STAGE 2: Secure Production Runtime Environment (JRE 21 Alpine)
# =========================================================================
# We swap to the official lightweight eclipse-temurin JRE container runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Configure unprivileged non-root sandbox execution spaces
RUN mkdir uploads && \
    addgroup -S appgroup && \
    adduser -S appuser -G appgroup

# Grab the compiled .jar file output directly out of the builder step
COPY --from=builder /app/target/share-now-1.0-SNAPSHOT.jar app.jar

# Adjust local ownership rules so our unprivileged user has read/write access
RUN chown -R appuser:appgroup /app

USER appuser
EXPOSE 8080

ENV SERVER_PORT=8080
ENV ALLOWED_ORIGIN=http://localhost:4200
ENV UPLOAD_DIR=/app/uploads
ENV MAX_FILE_SIZE_BYTES=524288000

ENTRYPOINT ["java", "-jar", "app.jar"]