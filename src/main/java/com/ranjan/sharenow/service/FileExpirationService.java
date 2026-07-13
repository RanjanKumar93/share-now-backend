package com.ranjan.sharenow.service;

import com.ranjan.sharenow.config.ServerConfig;
import com.ranjan.sharenow.model.ShareType;
import com.ranjan.sharenow.model.SharedFile;
import com.ranjan.sharenow.repository.SharedFileRepository;

import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FileExpirationService {

    private final SharedFileRepository repository;
    private final TunnelStreamService tunnelService; // Add this dependency
    private final ScheduledExecutorService scheduler;

    public FileExpirationService(SharedFileRepository repository, TunnelStreamService tunnelService) {
        this.repository = repository;
        this.tunnelService = tunnelService;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "FileExpirationWorker");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::sweepExpiredFiles, 1, 1, TimeUnit.MINUTES);
    }

    public void stop() {
        scheduler.shutdown();
    }

    private void sweepExpiredFiles() {
        try {
            Instant now = Instant.now();
            List<SharedFile> activeFiles = repository.findAll();

            for (SharedFile file : activeFiles) {
                if (file.shareType() == ShareType.STORED) {
                    // Disk file cleanup check
                    if (Duration.between(file.createdAt(), now).compareTo(ServerConfig.FILE_LIFESPAN) > 0) {
                        repository.delete(file.inviteCode());
                        Files.deleteIfExists(file.storedPath());
                    }
                } else {
                    // --- HARDENED LIVE TUNNEL CONNECTION GUARD ---
                    TunnelStreamService.TunnelSession session = tunnelService.getTunnel(file.inviteCode());

                    if (session != null) {
                        // Check if the downloader has NOT connected yet (latch is still at 1)
                        if (session.downloaderConnected.getCount() > 0) {
                            // The uploader initialized it, but no downloader claimed it within 2 minutes
                            if (Duration.between(file.createdAt(), now).compareTo(ServerConfig.FILE_LIFESPAN) > 0) {
                                System.out.println("Cleaning up un-connected, abandoned live tunnel session: " + file.inviteCode());
                                tunnelService.closeTunnel(file.inviteCode());
                                repository.delete(file.inviteCode());
                            }
                        }
                        // NOTE: If getCount() is 0, a downloader IS connected and active.
                        // We step away and trust the streaming thread's finally block to clean it up when done.
                    } else {
                        // Fallback: If the repository tracking record somehow leaked but the
                        // memory tunnel session is long gone, purge the stale db entry cleanly.
                        repository.delete(file.inviteCode());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error in expiration sweep: " + e.getMessage());
        }
    }
}