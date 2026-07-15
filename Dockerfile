# =========================================================================
# 🏗️ STAGE 1: Build & Compile Environment (Heavy Weight)
# =========================================================================
# We select a Maven image packed with JDK 25 running on a lightweight Alpine Linux base.
FROM maven:3.9.9-eclipse-temurin-25-alpine AS builder

# Set the active working directory inside the build sandbox container
WORKDIR /app

# Step 1: Optimization Trick - Copy ONLY the pom.xml first to download dependencies.
# This heavily utilizes Docker's caching layers so code updates don't trigger re-downloading plugins.
COPY pom.xml .

# Download the Maven project dependency trees ahead of copying source code
RUN mvn dependency:go-offline -B

# Step 2: Copy the actual project source tree directories
COPY src ./src

# Compile the source, execute checks, and pack the code into a runnable .jar file
RUN mvn clean package -DskipTests

# =========================================================================
# 🛡️ STAGE 2: Secure Production Runtime Environment (Ultra Light)
# =========================================================================
# We throw away the compiler, Maven caches, and source files. We only bring in the execution JRE.
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Create the runtime storage folder directory and enforce strict unprivileged Linux profile user security.
# Running containers as 'root' leaves the server vulnerable to runtime escapes.
RUN mkdir uploads && \
    addgroup -S appgroup && \
    adduser -S appuser -G appgroup

# Copy ONLY the optimized compiled executable .jar file out of Stage 1 (builder) into Stage 2
COPY --from=builder /app/target/share-now-1.0-SNAPSHOT.jar app.jar

# Adjust local folder directory ownership flags so our non-root app user can read/write files to it safely
RUN chown -R appuser:appgroup /app

# Switch execution execution rules over to the unprivileged profile user
USER appuser

# Document that the application container expects runtime execution network exposure on port 8080
EXPOSE 8080

# Configure production environment variables with safe defaults (Overridable in deployment)
ENV SERVER_PORT=8080
ENV ALLOWED_ORIGIN=http://localhost:4200
ENV UPLOAD_DIR=/app/uploads
ENV MAX_FILE_SIZE_BYTES=524288000

# Execute the Java process binary package directly
ENTRYPOINT ["java", "-jar", "app.jar"]