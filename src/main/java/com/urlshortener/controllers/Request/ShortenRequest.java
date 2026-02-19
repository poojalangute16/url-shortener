package com.urlshortener.controllers.Request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body for the URL shortening endpoint.
 */
@Schema(description = "Request body containing the URL to be shortened")
public class ShortenRequest {

    @Schema(
            description = "The full URL to shorten. Must include a scheme (http/https) and a valid host.",
            example = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String url;

    public ShortenRequest() {}

    public ShortenRequest(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
