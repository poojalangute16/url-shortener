package com.urlshortener.services;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.urlshortener.models.ShortenedUrl;
import com.urlshortener.repositories.InMemoryUrlRepository;

/**
 * Core business logic for shortening URLs, resolving short codes,
 * and computing domain-level metrics.
 */
@Service
public class UrlShortenerService {

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int SHORT_CODE_LENGTH = 7;

    private final InMemoryUrlRepository urlRepository;
    private final String baseUrl;
    private final Random random;

    public UrlShortenerService(
            InMemoryUrlRepository urlRepository,
            @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.urlRepository = urlRepository;
        this.baseUrl = baseUrl;
        this.random = new Random();
    }

    /**
     * Shortens the given URL. If the URL has already been shortened before,
     * returns the same short URL (idempotent).
     *
     * @param originalUrl the full URL to shorten
     * @return the complete shortened URL (e.g., http://localhost:8080/abc1234)
     */
    public String shorten(String originalUrl) {
        validateUrl(originalUrl);

        return urlRepository.findByOriginalUrl(originalUrl)
                .map(existing -> buildShortUrl(existing.getShortCode()))
                .orElseGet(() -> createAndSaveShortUrl(originalUrl));
    }

 
    private String createAndSaveShortUrl(String originalUrl) {
        String domain = extractDomain(originalUrl);
        String shortCode = generateUniqueShortCode();
        ShortenedUrl shortenedUrl = new ShortenedUrl(shortCode, originalUrl, domain);
        urlRepository.save(shortenedUrl);
        return buildShortUrl(shortCode);
    }

    private String generateUniqueShortCode() {
        String code;
        do {
            code = generateRandomCode();
        } while (urlRepository.existsByShortCode(code));
        return code;
    }

    private String generateRandomCode() {
        StringBuilder sb = new StringBuilder(SHORT_CODE_LENGTH);
        for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    private String buildShortUrl(String shortCode) {
        return baseUrl + "/" + shortCode;
    }

    private String extractDomain(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) {
                throw new IllegalArgumentException("Cannot extract domain from URL: " + url);
            }
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL: " + url, e);
        }
    }

    private void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL must not be blank");
        }
        try {
            URI uri = new URI(url);
            if (uri.getScheme() == null || uri.getHost() == null) {
                throw new IllegalArgumentException("URL must include a scheme and host: " + url);
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Malformed URL: " + url, e);
        }
    }
}
