package com.ranjan.sharenow.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class FileStorageService {

    private final Path uploadDirectory;

    public FileStorageService(Path uploadDirectory) {
        // Normalize the base directory paths up front
        this.uploadDirectory = uploadDirectory.toAbsolutePath().normalize();
    }

    public Path save(String originalFilename, InputStream inputStream) throws IOException {
        Files.createDirectories(uploadDirectory);

        // Completely ignore the original filename on disk! Use only the UUID.
        String diskFilename = UUID.randomUUID().toString();
        Path destination = uploadDirectory.resolve(diskFilename).toAbsolutePath().normalize();

        // Baseline directory guard check
        if (!destination.startsWith(uploadDirectory.toAbsolutePath().normalize())) {
            throw new SecurityException("Invalid target file path creation.");
        }

        Files.copy(inputStream, destination);
        return destination; // Returns the pure UUID-based path to be tracked in repository
    }
}