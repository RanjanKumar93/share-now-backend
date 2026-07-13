package com.ranjan.sharenow.parser;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Decorator Pattern: Wraps an existing InputStream to add boundary-detection
 * capabilities, intercepting data chunks and terminating cleanly at the boundary.
 */
public final class BoundaryStrippingInputStream extends InputStream {

    private final BufferedInputStream delegate;
    private final byte[] boundaryBytes;
    private boolean eofReached = false;

    public BoundaryStrippingInputStream(InputStream sourceStream, byte[] boundaryBytes) {
        // Ensure we have buffering capabilities for marking/resetting stream positions
        this.delegate = sourceStream instanceof BufferedInputStream bis ? bis : new BufferedInputStream(sourceStream);
        this.boundaryBytes = boundaryBytes.clone();
    }

    @Override
    public int read() throws IOException {
        if (eofReached) {
            return -1;
        }

        delegate.mark(boundaryBytes.length);
        int currentByte = delegate.read();

        if (currentByte == -1) {
            eofReached = true;
            return -1;
        }

        // Potential boundary encounter: match sequence aggressively
        if (currentByte == (boundaryBytes[0] & 0xFF)) {
            byte[] buffer = new byte[boundaryBytes.length];
            buffer[0] = (byte) currentByte;

            int bytesRead = 1;
            while (bytesRead < boundaryBytes.length) {
                int next = delegate.read();
                if (next == -1) {
                    break;
                }
                buffer[bytesRead++] = (byte) next;
            }

            if (bytesRead == boundaryBytes.length && matchesBoundary(buffer)) {
                eofReached = true;
                return -1; // Artificial EOF injected perfectly
            }

            // Fallback: Mismatch discovered, rollback internal head index pointer
            delegate.reset();
            currentByte = delegate.read();
        }

        return currentByte;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (eofReached) {
            return -1;
        }
        if (len == 0) {
            return 0;
        }

        int firstByte = read();
        if (firstByte == -1) {
            return -1;
        }

        b[off] = (byte) firstByte;
        int count = 1;

        while (count < len) {
            int nextByte = read();
            if (nextByte == -1) {
                break;
            }
            b[off + count] = (byte) nextByte;
            count++;
        }
        return count;
    }

    private boolean matchesBoundary(byte[] buffer) {
        for (int i = 0; i < boundaryBytes.length; i++) {
            if (buffer[i] != boundaryBytes[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}