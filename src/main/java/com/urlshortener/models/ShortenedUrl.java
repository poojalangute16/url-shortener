package com.urlshortener.models;

import java.time.Instant;

/**
 * Represents a mapping between a shortened code and the original URL.
 */
public class ShortenedUrl {

    private final String shortCode;
    private final String originalUrl;
    private final String domain;
    private final Instant createdAt;

    public ShortenedUrl(String shortCode, String originalUrl, String domain) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.domain = domain;
        this.createdAt = Instant.now();
    }

    public String getShortCode() {
        return shortCode;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public String getDomain() {
        return domain;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
