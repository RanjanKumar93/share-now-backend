package com.ranjan.sharenow.repository;

import com.ranjan.sharenow.model.SharedFile;

import java.util.List;
import java.util.Optional;

public interface SharedFileRepository {

    void save(SharedFile file);

    Optional<SharedFile> findByInviteCode(String inviteCode);

    boolean exists(String inviteCode);

    void delete(String inviteCode);

    List<SharedFile> findAll();

    // FIX: Add this atomic operation to close the TOCTOU gap
    boolean reservePlaceholder(String inviteCode);

}