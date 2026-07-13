package com.ranjan.sharenow.controller;

import com.ranjan.sharenow.web.HttpResponseUtil;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class EchoController {

    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            HttpResponseUtil.methodNotAllowed(exchange);
            return;
        }

        byte[] bytes = exchange.getRequestBody().readAllBytes();
        String echoData = new String(bytes, StandardCharsets.UTF_8);
        HttpResponseUtil.ok(exchange, echoData.isEmpty() ? "Echo: No payload received" : echoData);
    }
}