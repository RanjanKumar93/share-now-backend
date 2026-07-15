package com.ranjan.sharenow.controller;

import com.ranjan.sharenow.config.ServerConfig;
import com.ranjan.sharenow.model.SharedFile;
import com.ranjan.sharenow.service.DownloadService;
import com.ranjan.sharenow.web.HttpResponseUtil;
import com.sun.net.httpserver.HttpExchange;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
        String inviteCode = path.substring(path.lastIndexOf('/') + 1).trim();

        if (inviteCode.isEmpty()) {
            throw new IllegalArgumentException("Missing invite code.");
        }

        Optional<SharedFile> sharedFileOpt = downloadService.verifyAndGetMetadata(inviteCode);

        if (sharedFileOpt.isEmpty()) {
            HttpResponseUtil.notFound(exchange);
            return;
        }

        SharedFile sharedFile = sharedFileOpt.get();

        downloadService.preFlightCheck(sharedFile);

        // ------------------------------------------------------------------
        // Secure filename handling
        // ------------------------------------------------------------------

        String originalFilename = sharedFile.originalFilename();

        // Prevent HTTP header injection
        originalFilename = originalFilename.replace("\r", "").replace("\n", "");

        // ASCII fallback for older browsers
        String fallbackFilename = originalFilename.replaceAll("[^\\x20-\\x7E]", "_");

        // RFC 5987 UTF-8 filename for modern browsers
        String encodedFilename = URLEncoder.encode(originalFilename, StandardCharsets.UTF_8).replace("+", "%20");

        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + fallbackFilename + "\"; filename*=UTF-8''" + encodedFilename);

        exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");

        // ------------------------------------------------------------------
        // CORS
        // ------------------------------------------------------------------

        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", ServerConfig.ALLOWED_ORIGIN);

        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");

        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-File-Name");

        // Allow Angular to read Content-Disposition
        exchange.getResponseHeaders().set("Access-Control-Expose-Headers", "Content-Disposition");

        // ------------------------------------------------------------------
        // Stream file
        // ------------------------------------------------------------------

        long fileSize = downloadService.calculateResponseSize(sharedFile);

        exchange.sendResponseHeaders(200, fileSize);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            downloadService.pipeData(sharedFile, outputStream);
            outputStream.flush();
        }

        System.out.printf("Download completed. InviteCode=%s File=%s%n", inviteCode, originalFilename);
    }
}