package com.ranjan.sharenow.controller;

import com.ranjan.sharenow.model.SharedFile;
import com.ranjan.sharenow.service.DownloadService;
import com.ranjan.sharenow.web.HttpResponseUtil;
import com.sun.net.httpserver.HttpExchange;
import java.io.OutputStream;
import java.util.Optional;

public class DownloadController {

    private final DownloadService downloadService;

    public DownloadController(DownloadService downloadService) {
        this.downloadService = downloadService;
    }

    public void handle(HttpExchange exchange) throws Exception {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            throw new UnsupportedOperationException("Method not allowed");
        }

        String path = exchange.getRequestURI().getPath();
        String inviteCode = path.substring(path.lastIndexOf('/') + 1);

        if (inviteCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing invite code parameter.");
        }

        Optional<SharedFile> sharedFileOpt = downloadService.verifyAndGetMetadata(inviteCode);
        if (sharedFileOpt.isEmpty()) {
            HttpResponseUtil.notFound(exchange);
            return;
        }

        SharedFile sharedFile = sharedFileOpt.get();

        downloadService.preFlightCheck(sharedFile);

        // NEW HARDENED CODE:
        String originalName = sharedFile.originalFilename();

// 1. Remove dangerous Carriage Return / Line Feed characters to prevent HTTP response splitting
        originalName = originalName.replaceAll("[\\r\\n]", "");

// 2. Percent-encode the string (turns quotes into %22, spaces into %20, keeping it safe)
        String encodedName = java.net.URLEncoder.encode(originalName, java.nio.charset.StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

// 3. Set the headers securely using the modern browser standard
        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"file\"; filename*=UTF-8''" + encodedName);
        exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");

        long payloadSize = downloadService.calculateResponseSize(sharedFile);
        exchange.sendResponseHeaders(200, payloadSize);

        try (OutputStream os = exchange.getResponseBody()) {
            downloadService.pipeData(sharedFile, os);
            os.flush();
        }

        System.out.println("File successfully processed for token code: " + inviteCode);
    }
}