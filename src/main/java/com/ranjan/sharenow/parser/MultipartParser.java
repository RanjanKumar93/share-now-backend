package com.ranjan.sharenow.parser;

import com.ranjan.sharenow.dto.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MultipartParser {

    public MultipartFile parse(InputStream inputStream, String contentType) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(inputStream);
        String boundaryStr = "--" + extractBoundary(contentType);

        // Read up to initial starting boundary line marker
        String line = readLine(bis);
        while (line != null && !line.trim().equals(boundaryStr)) {
            line = readLine(bis);
        }

        if (line == null) {
            throw new IllegalArgumentException("Multipart initial boundary line not found.");
        }

        // Parse header components cleanly up to blank string breaker sequence
        StringBuilder headerBlock = new StringBuilder();
        while ((line = readLine(bis)) != null) {
            if (line.trim().isEmpty()) {
                break;
            }
            headerBlock.append(line);
        }

        String filename = parseFilename(headerBlock.toString());
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename key element missing inside payload headers.");
        }

        // Standard multipart payload sections isolate content data using a leading \r\n element
        byte[] boundaryBytes = ("\r\n" + boundaryStr).getBytes(StandardCharsets.US_ASCII);

        // Wrap with our structural decorator pattern stream
        InputStream cleanFileStream = new BoundaryStrippingInputStream(bis, boundaryBytes);

        return new MultipartFile(filename, cleanFileStream);
    }

    private String parseFilename(String headerText) {
        String[] lines = headerText.split("\r\n");
        for (String hLine : lines) {
            if (hLine.startsWith("Content-Disposition")) {
                String[] parts = hLine.split(";");
                for (String part : parts) {
                    part = part.trim();
                    if (part.startsWith("filename=")) {
                        return unquote(part.substring(9));
                    }
                }
            }
        }
        return null;
    }

    private String readLine(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        int b;
        while ((b = is.read()) != -1) {
            sb.append((char) b);
            if (b == '\n') {
                break;
            }
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private String unquote(String value) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private String extractBoundary(String contentType) {
        if (contentType == null) {
            throw new IllegalArgumentException("Missing Content-Type header descriptor.");
        }
        String[] parts = contentType.split(";");
        for (String part : parts) {
            part = part.trim();
            if (part.startsWith("boundary=")) {
                String boundary = part.substring("boundary=".length());
                // FIX: If the boundary value is surrounded by double quotes, strip them cleanly
                return unquote(boundary);
            }
        }
        throw new IllegalArgumentException("Multipart boundary token not present inside Content-Type.");
    }
}