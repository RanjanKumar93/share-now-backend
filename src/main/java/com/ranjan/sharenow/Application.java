package com.ranjan.sharenow;

import com.ranjan.sharenow.config.ServerConfig;
import com.ranjan.sharenow.controller.DownloadController;
import com.ranjan.sharenow.controller.EchoController;
import com.ranjan.sharenow.controller.HealthController;
import com.ranjan.sharenow.controller.UploadController;
import com.ranjan.sharenow.parser.MultipartParser;
import com.ranjan.sharenow.repository.InMemorySharedFileRepository;
import com.ranjan.sharenow.repository.SharedFileRepository;
import com.ranjan.sharenow.service.FileExpirationService;
import com.ranjan.sharenow.service.FileStorageService;
import com.ranjan.sharenow.service.InviteCodeService;
import com.ranjan.sharenow.service.UploadService;
import com.ranjan.sharenow.service.TunnelStreamService;
import com.ranjan.sharenow.service.LiveStreamService;
import com.ranjan.sharenow.service.DownloadService;
import com.ranjan.sharenow.web.RouteRegistrar;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Application {

    public void start() throws Exception {

        SharedFileRepository repository = new InMemorySharedFileRepository();

        InviteCodeService inviteCodeService = new InviteCodeService(repository);
        FileStorageService fileStorageService = new FileStorageService(ServerConfig.UPLOAD_DIRECTORY);
        UploadService uploadService = new UploadService(fileStorageService, inviteCodeService, repository);
        TunnelStreamService tunnelStreamService = new TunnelStreamService();
        FileExpirationService expirationService = new FileExpirationService(repository, tunnelStreamService);

        LiveStreamService liveStreamService = new LiveStreamService(tunnelStreamService, repository, inviteCodeService);
        DownloadService downloadService = new DownloadService(repository, tunnelStreamService);

        MultipartParser multipartParser = new MultipartParser();

        HealthController healthController = new HealthController();
        EchoController echoController = new EchoController();
        UploadController uploadController = new UploadController(multipartParser, uploadService, liveStreamService);
        DownloadController downloadController = new DownloadController(downloadService);

        HttpServer server = HttpServer.create(new InetSocketAddress(ServerConfig.HTTP_PORT), 0);

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        server.setExecutor(executor);

        RouteRegistrar registrar = new RouteRegistrar(server, healthController, echoController, uploadController, downloadController);
        registrar.registerRoutes();

        server.start();
        expirationService.start();

        System.out.println("Server started on port " + ServerConfig.HTTP_PORT);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down Application...");
            expirationService.stop();
            server.stop(0);
            executor.shutdown();
        }));
    }
}