package com.ranjan.sharenow.service;

import com.ranjan.sharenow.config.ServerConfig;
import com.ranjan.sharenow.model.ShareType;
import com.ranjan.sharenow.model.SharedFile;
import com.ranjan.sharenow.repository.SharedFileRepository;
import com.ranjan.sharenow.service.TunnelStreamService.TunnelSession;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class DownloadService {

    private final SharedFileRepository repository;
    private final TunnelStreamService tunnelService;

    public DownloadService(SharedFileRepository repository, TunnelStreamService tunnelService) {
        this.repository = repository;
        this.tunnelService = tunnelService;
    }

    public void preFlightCheck(SharedFile file) throws Exception {
        if (file.shareType() == ShareType.STORED) {
            java.io.File diskFile = file.storedPath().toFile();
            if (!diskFile.exists() || !diskFile.canRead()) {
                throw new java.io.FileNotFoundException("Stored file is missing or unreadable on disk.");
            }
        } else {
            if (tunnelService.getTunnel(file.inviteCode()) == null) {
                throw new IllegalStateException("Live tunnel session disappeared unexpectedly.");
            }
        }
    }

    public Optional<SharedFile> verifyAndGetMetadata(String inviteCode) {
        Optional<SharedFile> sharedFileOpt = repository.findByInviteCode(inviteCode);
        if (sharedFileOpt.isEmpty()) {
            return Optional.empty();
        }

        SharedFile sharedFile = sharedFileOpt.get();

        if (sharedFile.shareType() == ShareType.STORED) {
            // Disk file expiration check
            if (Duration.between(sharedFile.createdAt(), Instant.now()).compareTo(ServerConfig.FILE_LIFESPAN) > 0) {
                return Optional.empty();
            }
            if (!Files.exists(sharedFile.storedPath())) {
                return Optional.empty();
            }
        } else {
            // --- HARDENED LIVE TUNNEL HEALTH CHECK ---
            // Verify that the memory tunnel session actually exists in active flight
            if (tunnelService.getTunnel(inviteCode) == null) {
                // Lazy cleanup: Strip out the dead repository record right here
                repository.delete(inviteCode);
                return Optional.empty();
            }
        }

        return Optional.of(sharedFile);
    }

    public long calculateResponseSize(SharedFile file) throws Exception {
        if (file.shareType() == ShareType.STORED) {
            return Files.size(file.storedPath());
        }
        return 0; // 0 sets chunked transfer encoding natively over HTTP for live mode
    }

    public void pipeData(SharedFile file, OutputStream outputStream) throws Exception {
        if (file.shareType() == ShareType.STORED) {
            // Option A: Pipe data from disk storage channels
            try (FileInputStream fis = new FileInputStream(file.storedPath().toFile())) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
        } else {
            // Option B: Pipe data from active Live Tunnel memory buffer
            TunnelSession session = tunnelService.getTunnel(file.inviteCode());
            if (session == null) {
                throw new IllegalStateException("Live tunnel session disappeared unexpectly.");
            }

            // Signal uploader to begin streaming payload
            session.downloaderConnected.countDown();

            try (InputStream pin = session.pin) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = pin.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
            }
        }
    }
}