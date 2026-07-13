package com.ranjan.sharenow.model;

public enum ShareType {
    STORED,      // Standard behavior: file is saved to disk and can be downloaded anytime before expiration
    LIVE_STREAM  // Tunnel behavior: file is piped directly through an active network connection
}