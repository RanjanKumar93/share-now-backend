package com.ranjan.sharenow.controller;

import com.ranjan.sharenow.config.ServerConfig;
import com.ranjan.sharenow.dto.MultipartFile;
import com.ranjan.sharenow.dto.UploadResponse;
import com.ranjan.sharenow.parser.MultipartParser;
import com.ranjan.sharenow.service.UploadService;
import com.ranjan.sharenow.service.LiveStreamService;
import com.ranjan.sharenow.web.HttpResponseUtil;
import com.sun.net.httpserver.HttpExchange;

public class UploadController {

    private final MultipartParser multipartParser;
    private final UploadService uploadService;
    private final LiveStreamService liveStreamService;

    public UploadController(MultipartParser multipartParser, UploadService uploadService, LiveStreamService liveStreamService) {
        this.multipartParser = multipartParser;
        this.uploadService = uploadService;
        this.liveStreamService = liveStreamService;
    }

    public void handle(HttpExchange exchange) throws Exception {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            throw new UnsupportedOperationException("Method not allowed");
        }

        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();
        boolean isLiveMode = query != null && query.contains("mode=live");

        // Step 1: Initialize the memory tunnel mapping framework
        if (isLiveMode && path.equals("/upload/init")) {
            // Read original filename from a custom header sent by the frontend/client
            String filename = exchange.getRequestHeaders().getFirst("X-File-Name");
            if (filename == null || filename.trim().isEmpty()) {
                filename = "live-shared-file";
            }

            String inviteCode = liveStreamService.initializeTunnel(filename);
            long expiresAtEpochMillis = System.currentTimeMillis() + ServerConfig.LIVE_TUNNEL_TIMEOUT.toMillis();

            // Package it into your DTO record
            UploadResponse response = new UploadResponse(inviteCode, filename, expiresAtEpochMillis);

            // Send it as structured JSON
            HttpResponseUtil.jsonOk(exchange, response.toJson());
            return;
        }

        // Step 2: Accept pure raw streaming payload bytes directly
        if (path.startsWith("/upload/stream/")) {
            String inviteCode = path.substring(path.lastIndexOf('/') + 1);

            // Direct streaming connection: No multipart wrappers, no protocol clutter
            liveStreamService.streamToTunnel(inviteCode, exchange.getRequestBody());

            HttpResponseUtil.ok(exchange, "Transfer Complete");
            return;
        }

        // Default: Traditional Stored Cloud Drop Mode (Keeps Multi-part compatibility for traditional forms)
        MultipartFile multipartFile = multipartParser.parse(exchange.getRequestBody(), exchange.getRequestHeaders().getFirst("Content-Type"));
        UploadResponse response = uploadService.upload(multipartFile.filename(), multipartFile.inputStream());
        HttpResponseUtil.jsonOk(exchange, response.toJson());
    }
}