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

    // Inside com.ranjan.sharenow.service.FileStorageService.java

    /**
     * 🧹 Cleans up all orphaned raw files inside the base upload directory.
     * This prevents storage leakage across server restarts or unexpected crashes.
     */
    public void purgeOrphanedFiles() {
        try {
            java.io.File uploadDir = this.uploadDirectory.toFile();
            if (uploadDir.exists() && uploadDir.isDirectory()) {
                java.io.File[] files = uploadDir.listFiles();
                if (files != null && files.length > 0) {
                    System.out.printf("[Storage Engine] Found %d orphaned files from a previous session. Purging...%n", files.length);
                    for (java.io.File file : files) {
                        if (file.isFile()) {
                            boolean deleted = file.delete();
                            if (!deleted) {
                                System.err.println("[Storage Engine] Failed to delete orphaned file: " + file.getName());
                            }
                        }
                    }
                    System.out.println("[Storage Engine] Startup disk cleanup completed successfully.");
                }
            }
        } catch (Exception e) {
            System.err.println("[Storage Engine] Warning: Critical failure running disk cleanup sweep: " + e.getMessage());
        }
    }
}