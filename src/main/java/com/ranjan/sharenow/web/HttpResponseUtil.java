package com.ranjan.sharenow.web;

import com.ranjan.sharenow.config.ServerConfig;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class HttpResponseUtil {

    private HttpResponseUtil() {
    }

    public static void ok(HttpExchange exchange, String body) throws IOException {
        send(exchange, 200, body);
    }

    public static void badRequest(HttpExchange exchange, String body) throws IOException {
        send(exchange, 400, body);
    }

    public static void methodNotAllowed(HttpExchange exchange) throws IOException {
        send(exchange, 405, "Method Not Allowed");
    }

    public static void notFound(HttpExchange exchange) throws IOException {
        send(exchange, 404, "Not Found");
    }

    public static void internalServerError(HttpExchange exchange) throws IOException {
        send(exchange, 500, "Internal Server Error");
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        // Enforce Secure Configured Whitelist Boundaries instead of arbitrary wildcards
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", ServerConfig.ALLOWED_ORIGIN);
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-File-Name");
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");

        exchange.sendResponseHeaders(status, bytes.length);

        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}