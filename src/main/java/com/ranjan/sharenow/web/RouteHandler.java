package com.ranjan.sharenow.web;

import com.ranjan.sharenow.config.ServerConfig;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;

@FunctionalInterface
public interface RouteHandler {
    void handleRequest(HttpExchange exchange) throws Exception;

    static com.sun.net.httpserver.HttpHandler safe(RouteHandler handler) {
        return exchange -> {
            // INTERCEPT CORRESPONDING BROWSER PRE-FLIGHT HANDSHAKES UNIVERSALLY
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", ServerConfig.ALLOWED_ORIGIN);
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-File-Name");
                // 204 No Content confirms parameters are ready without streaming unnecessary data
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            try {
                handler.handleRequest(exchange);
            } catch (IllegalArgumentException e) {
                System.err.println("Bad Request [" + exchange.getRequestURI() + "]: " + e.getMessage());
                safelySendError(exchange, 400, "Malformed request: " + e.getMessage());
            } catch (UnsupportedOperationException e) {
                safelySendError(exchange, 405, "Method Not Allowed");
            } catch (Exception e) {
                System.err.println("Internal Server Error processing request: ");
                e.printStackTrace();
                safelySendError(exchange, 500, "Internal Server Error");
            }
        };
    }

    private static void safelySendError(HttpExchange exchange, int statusCode, String message) {
        try {
            // Hardening: Prevent cascading crashes if headers were already sent
            if (exchange.getResponseCode() > 0) {
                System.err.println("Cannot emit error status code " + statusCode + " - Headers already committed.");
                exchange.close();
                return;
            }

            if (statusCode == 400) HttpResponseUtil.badRequest(exchange, message);
            else if (statusCode == 405) HttpResponseUtil.methodNotAllowed(exchange);
            else HttpResponseUtil.internalServerError(exchange);
        } catch (IOException ioe) {
            System.err.println("Failed to cleanly transmit error frame: " + ioe.getMessage());
        }
    }
}