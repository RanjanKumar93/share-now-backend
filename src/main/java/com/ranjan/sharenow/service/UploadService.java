package com.ranjan.sharenow.service;

import com.ranjan.sharenow.config.ServerConfig;
import com.ranjan.sharenow.dto.UploadResponse;
import com.ranjan.sharenow.model.ShareType;
import com.ranjan.sharenow.model.SharedFile;
import com.ranjan.sharenow.repository.SharedFileRepository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;

public class UploadService {

    private final FileStorageService fileStorageService;
    private final InviteCodeService inviteCodeService;
    private final SharedFileRepository repository;

    public UploadService(FileStorageService fileStorageService, InviteCodeService inviteCodeService, SharedFileRepository repository) {

        this.fileStorageService = fileStorageService;
        this.inviteCodeService = inviteCodeService;
        this.repository = repository;
    }

    public UploadResponse upload(String originalFilename, InputStream inputStream) throws IOException {

        Path storedPath = fileStorageService.save(originalFilename, inputStream);

        String inviteCode = inviteCodeService.generate();

        SharedFile sharedFile = new SharedFile(inviteCode, originalFilename, storedPath, Instant.now(), ShareType.STORED);
        repository.save(sharedFile);

        long expiresAt = System.currentTimeMillis() + ServerConfig.FILE_LIFESPAN.toMillis();

        return new UploadResponse(inviteCode, originalFilename, expiresAt);
    }
}