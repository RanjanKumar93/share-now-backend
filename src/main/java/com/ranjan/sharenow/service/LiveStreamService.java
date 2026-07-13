package com.ranjan.sharenow.service;

import com.ranjan.sharenow.config.ServerConfig;
import com.ranjan.sharenow.model.ShareType;
import com.ranjan.sharenow.model.SharedFile;
import com.ranjan.sharenow.repository.SharedFileRepository;
import com.ranjan.sharenow.service.TunnelStreamService.TunnelSession;

import java.io.InputStream;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class LiveStreamService {

    private final TunnelStreamService tunnelService;
    private final SharedFileRepository repository;
    private final InviteCodeService inviteCodeService;

    public LiveStreamService(TunnelStreamService tunnelService, SharedFileRepository repository, InviteCodeService inviteCodeService) {
        this.tunnelService = tunnelService;
        this.repository = repository;
        this.inviteCodeService = inviteCodeService;
    }

    public String initializeTunnel(String filename) throws Exception {
        // 3. Replaced random math with the safe, collision-checked service generator
        String inviteCode = inviteCodeService.generate();

        // Initialize the memory pipes
        tunnelService.createTunnel(inviteCode, filename);

        // Log tracking metadata to database records
        repository.save(new SharedFile(inviteCode, filename, null, Instant.now(), ShareType.LIVE_STREAM));

        return inviteCode;
    }

    public void streamToTunnel(String inviteCode, InputStream uploadStream) throws Exception {
        TunnelSession session = tunnelService.getTunnel(inviteCode);
        if (session == null) {
            throw new IllegalArgumentException("Live tunnel session not found, expired, or invalid.");
        }

        try {
            boolean clientArrived = session.downloaderConnected.await(ServerConfig.CLIENT_WAIT_TIME_MINUTES, TimeUnit.MINUTES);
            if (!clientArrived) {
                throw new java.util.concurrent.TimeoutException("Downloader failed to connect within the allowed " + ServerConfig.CLIENT_WAIT_TIME_MINUTES + " minute window.");
            }

            byte[] buffer = new byte[4096];
            int read;
            while ((read = uploadStream.read(buffer)) != -1) {
                session.pout.write(buffer, 0, read);
            }
            session.pout.flush();
        } finally {
            // This block is now guaranteed to seal both pipe endpoints and wipe memory traces completely
            tunnelService.closeTunnel(inviteCode);
            repository.delete(inviteCode);
        }
    }
}