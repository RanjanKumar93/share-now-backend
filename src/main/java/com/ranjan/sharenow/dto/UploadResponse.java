package com.ranjan.sharenow.dto;

public record UploadResponse(
        String inviteCode,
        String originalFilename,
        long expiresAt // Added for expiration tracking
) {
    public String toJson() {
        return String.format(
                "{\"inviteCode\":\"%s\",\"originalFilename\":\"%s\",\"expiresAt\":%d}",
                inviteCode, originalFilename, expiresAt
        );
    }
}