package com.urlshortener.repositories;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.urlshortener.models.ShortenedUrl;

/**
 * In-memory store for shortened URLs.
 * Uses two maps for O(1) lookups in both directions.
 */
@Repository
public class InMemoryUrlRepository {

    // shortCode -> ShortenedUrl
    private final Map<String, ShortenedUrl> byShortCode = new ConcurrentHashMap<>();

    // originalUrl -> ShortenedUrl (for idempotent shortening)
    private final Map<String, ShortenedUrl> byOriginalUrl = new ConcurrentHashMap<>();

    public void save(ShortenedUrl shortenedUrl) {
        byShortCode.put(shortenedUrl.getShortCode(), shortenedUrl);
        byOriginalUrl.put(shortenedUrl.getOriginalUrl(), shortenedUrl);
    }

    public Optional<ShortenedUrl> findByOriginalUrl(String originalUrl) {
        return Optional.ofNullable(byOriginalUrl.get(originalUrl));
    }

    public boolean existsByShortCode(String shortCode) {
        return byShortCode.containsKey(shortCode);
    }


    public Optional<ShortenedUrl> findByShortCode(String shortCode) {
        return Optional.ofNullable(byShortCode.get(shortCode));
    }

    public Collection<ShortenedUrl> findAll() {
        return byShortCode.values();
    }

}
