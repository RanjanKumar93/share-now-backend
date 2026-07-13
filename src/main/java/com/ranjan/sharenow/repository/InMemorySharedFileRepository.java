package com.ranjan.sharenow.repository;

import com.ranjan.sharenow.model.SharedFile;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySharedFileRepository implements SharedFileRepository {

    private final Map<String, SharedFile> storage = new ConcurrentHashMap<>();

    @Override
    public void save(SharedFile file) {
        storage.put(file.inviteCode(), file);
    }

    @Override
    public Optional<SharedFile> findByInviteCode(String inviteCode) {
        return Optional.ofNullable(storage.get(inviteCode));
    }

    @Override
    public boolean exists(String inviteCode) {
        return storage.containsKey(inviteCode);
    }

    @Override
    public void delete(String inviteCode) {
        storage.remove(inviteCode);
    }

    @Override
    public List<SharedFile> findAll() {
        return List.copyOf(storage.values());
    }

    // FIX: Atomic operation. Returns true if the key was empty and successfully claimed.
    @Override
    public boolean reservePlaceholder(String inviteCode) {
        SharedFile placeholder = new SharedFile(inviteCode, "RESERVED_PLACEHOLDER", null, Instant.now(), null);
        return storage.putIfAbsent(inviteCode, placeholder) == null;
    }
}