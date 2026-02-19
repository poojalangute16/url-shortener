package com.urlshortener.controllers.Response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response body returned after successfully shortening a URL.
 */
@Schema(description = "Response body containing both the original and the generated short URL")
public class ShortenResponse {

    @Schema(
            description = "The original URL that was submitted for shortening",
            example = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    )
    private final String originalUrl;

    @Schema(
            description = "The shortened URL. Visiting this URL will redirect to the original.",
            example = "http://localhost:8080/aB3cD4e"
    )
    private final String shortUrl;

    public ShortenResponse(String originalUrl, String shortUrl) {
        this.originalUrl = originalUrl;
        this.shortUrl = shortUrl;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public String getShortUrl() {
        return shortUrl;
    }
}
