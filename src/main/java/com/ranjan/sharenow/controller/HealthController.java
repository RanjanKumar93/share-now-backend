package com.ranjan.sharenow.controller;

import com.ranjan.sharenow.web.HttpResponseUtil;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class HealthController {

    public void handle(HttpExchange exchange) throws IOException {

        if (!exchange.getRequestMethod().equals("GET")) {
            HttpResponseUtil.methodNotAllowed(exchange);
            return;
        }

        HttpResponseUtil.ok(exchange, "Server is running");
    }
}