package com.ranjan.sharenow.model;

import java.nio.file.Path;
import java.time.Instant;

public record SharedFile(String inviteCode, String originalFilename, Path storedPath, Instant createdAt, ShareType shareType) {

}