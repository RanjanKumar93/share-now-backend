package com.ranjan.sharenow.web;

import com.ranjan.sharenow.controller.DownloadController;
import com.ranjan.sharenow.controller.EchoController;
import com.ranjan.sharenow.controller.HealthController;
import com.ranjan.sharenow.controller.UploadController;
import com.sun.net.httpserver.HttpServer;

public class RouteRegistrar {

    private final HttpServer server;
    private final HealthController healthController;
    private final EchoController echoController;
    private final UploadController uploadController;
    private final DownloadController downloadController;

    public RouteRegistrar(HttpServer server, HealthController healthController, EchoController echoController, UploadController uploadController, DownloadController downloadController) {
        this.server = server;
        this.healthController = healthController;
        this.echoController = echoController;
        this.uploadController = uploadController;
        this.downloadController = downloadController;
    }

    public void registerRoutes() {
        server.createContext("/health", RouteHandler.safe(healthController::handle));
        server.createContext("/echo", RouteHandler.safe(echoController::handle));
        server.createContext("/upload", RouteHandler.safe(uploadController::handle)); // Handles /upload/init and /upload/stream/
        server.createContext("/download", RouteHandler.safe(downloadController::handle));
    }
}