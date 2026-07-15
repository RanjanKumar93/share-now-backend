package com.ranjan.sharenow.web;

import com.ranjan.sharenow.config.ServerConfig;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class HttpResponseUtil {

    private HttpResponseUtil() {
    }

    public static void ok(HttpExchange exchange, String body) throws IOException {
        send(exchange, 200, "text/plain; charset=UTF-8", body);
    }

    public static void jsonOk(HttpExchange exchange, String jsonBody) throws IOException {
        send(exchange, 200, "application/json; charset=UTF-8", jsonBody);
    }

    public static void badRequest(HttpExchange exchange, String body) throws IOException {
        send(exchange, 400, "text/plain; charset=UTF-8", body);
    }

    public static void methodNotAllowed(HttpExchange exchange) throws IOException {
        send(exchange, 405, "text/plain; charset=UTF-8", "Method Not Allowed");
    }

    public static void notFound(HttpExchange exchange) throws IOException {
        send(exchange, 404, "text/plain; charset=UTF-8", "Not Found");
    }

    public static void internalServerError(HttpExchange exchange) throws IOException {
        send(exchange, 500, "text/plain; charset=UTF-8", "Internal Server Error");
    }

    // The single source of truth for sending responses
    private static void send(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        // CORS headers set once globally
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", ServerConfig.ALLOWED_ORIGIN);
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-File-Name");

        // Dynamically set content type
        exchange.getResponseHeaders().set("Content-Type", contentType);

        exchange.sendResponseHeaders(status, bytes.length);

        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}