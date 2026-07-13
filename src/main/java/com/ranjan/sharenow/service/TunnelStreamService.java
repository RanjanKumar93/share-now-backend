package com.ranjan.sharenow.service;

import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class TunnelStreamService {

    private final Map<String, TunnelSession> activeTunnels = new ConcurrentHashMap<>();

    public static class TunnelSession {
        public final PipedInputStream pin;
        public final PipedOutputStream pout;
        public final CountDownLatch downloaderConnected = new CountDownLatch(1);
        public final String filename;

        public TunnelSession(String filename) throws Exception {
            this.filename = filename;
            // 64KB internal rolling cache buffer window
            this.pin = new PipedInputStream(1024 * 64);
            this.pout = new PipedOutputStream(this.pin);
        }
    }

    public TunnelSession createTunnel(String inviteCode, String filename) throws Exception {
        TunnelSession session = new TunnelSession(filename);
        activeTunnels.put(inviteCode, session);
        return session;
    }

    public TunnelSession getTunnel(String inviteCode) {
        return activeTunnels.get(inviteCode);
    }

    public void closeTunnel(String inviteCode) {
        TunnelSession session = activeTunnels.remove(inviteCode);
        if (session != null) {
            try {
                session.pout.close();
            } catch (Exception ignored) {
            }
            try {
                session.pin.close();
            } catch (Exception ignored) {
            }
        }
    }
}