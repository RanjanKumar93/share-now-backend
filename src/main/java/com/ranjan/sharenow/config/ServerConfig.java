package com.ranjan.sharenow.config;

import java.nio.file.Path;
import java.time.Duration;

public final class ServerConfig {

    // HTTP Port Config
    public static final int HTTP_PORT = Integer.parseInt(
            System.getenv().getOrDefault("SERVER_PORT", "8080")
    );

    // Dynamic Storage Path
    public static final Path UPLOAD_DIRECTORY = Path.of(
            System.getenv().getOrDefault("UPLOAD_DIR", "uploads")
    );

    // Expiration Management Timers
    public static final Duration FILE_LIFESPAN = Duration.ofMinutes(
            Long.parseLong(System.getenv().getOrDefault("FILE_LIFESPAN_MINUTES", "10"))
    );

    public static final Duration LIVE_TUNNEL_TIMEOUT = Duration.ofMinutes(
            Long.parseLong(System.getenv().getOrDefault("LIVE_TUNNEL_TIMEOUT_MINUTES", "2"))
    );

    public static final long CLIENT_WAIT_TIME_MINUTES = Long.parseLong(
            System.getenv().getOrDefault("CLIENT_WAIT_TIME_MINUTES", "2")
    );

    // 🔒 NEW: Centralized Upload Payload Limits (Max 1024MB Default)
    public static final long MAX_FILE_SIZE_BYTES = Long.parseLong(
            System.getenv().getOrDefault("MAX_FILE_SIZE_BYTES", String.valueOf(1024L * 1024 * 1024))
    );

    // 🔒 Security CORS Boundary Configuration
    public static final String ALLOWED_ORIGIN = System.getenv().getOrDefault(
            "ALLOWED_ORIGIN", "http://localhost:4200"
    );

    private ServerConfig() {
        // Prevents instantiation instantiation
    }
}