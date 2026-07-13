package com.ranjan.sharenow.service;

import com.ranjan.sharenow.repository.SharedFileRepository;

import java.util.Random;

public class InviteCodeService {

    private final SharedFileRepository repository;
    private final Random random = new Random();

    private static final int MIN_CODE = 100000;
    private static final int CODE_RANGE = 900000;

    public InviteCodeService(SharedFileRepository repository) {
        this.repository = repository;
    }

    public String generate() {
        String code;
        boolean securedSlot = false;

        do {
            // Generate standard numeric invite sequence candidate
            code = String.valueOf(MIN_CODE + random.nextInt(CODE_RANGE));

            // FIX: Try to claim ownership of the code slot atomically.
            // If another thread claimed it between selection and entry, this yields false.
            securedSlot = repository.reservePlaceholder(code);

        } while (!securedSlot);

        return code;
    }
}