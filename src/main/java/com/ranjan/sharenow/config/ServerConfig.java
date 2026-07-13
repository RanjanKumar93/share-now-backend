package com.ranjan.sharenow.config;

import java.nio.file.Path;
import java.time.Duration;

public final class ServerConfig {

    public static final int HTTP_PORT = 8080;

    public static final Path UPLOAD_DIRECTORY = Path.of("uploads");

    public static final Duration FILE_LIFESPAN = Duration.ofMinutes(10);

    // Live mode tunnel initialization abandonment timeout window
    public static final Duration LIVE_TUNNEL_TIMEOUT = Duration.ofMinutes(2);

    // Set this to your frontend domain in production (e.g., "https://sharenow.ranjan.com")
    public static final String ALLOWED_ORIGIN = "http://localhost:3000";

    private ServerConfig() {
    }
}