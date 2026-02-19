package com.urlshortener.controllers;


import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.urlshortener.controllers.Request.ShortenRequest;
import com.urlshortener.controllers.Response.ShortenResponse;
import com.urlshortener.services.UrlShortenerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST API for shortening URLs, redirecting, and viewing metrics.
 *
 * Endpoints:
 *   POST /shorten               — Accepts a URL and returns a shortened URL
 *   GET  /{shortCode}           — Redirects to the original URL
 *   GET  /metrics/top-domains   — Returns top 3 most-shortened domains
 */
@RestController
@Tag(name = "URL Shortener", description = "Shorten URLs, resolve short codes, and view domain metrics")
public class UrlShortenerController {
     private final UrlShortenerService urlShortenerService;

       public UrlShortenerController(UrlShortenerService urlShortenerService) {
        this.urlShortenerService = urlShortenerService;
    }

        @Operation(
            summary = "Shorten a URL",
            description = """
                    Accepts a full URL and returns a shortened version.
                    
                    This operation is **idempotent**: submitting the same URL multiple times
                    always returns the same short URL rather than generating a new one.
                    
                    The short code is a randomly generated 7-character string using [a-zA-Z0-9].
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "URL shortened successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ShortenResponse.class),
                            examples = @ExampleObject(
                                    name = "Success",
                                    value = """
                                            {
                                              "originalUrl": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                                              "shortUrl": "http://localhost:8080/aB3cD4e"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "The provided URL is missing, blank, or malformed",
                    content = @Content(
                            mediaType = MediaType.TEXT_PLAIN_VALUE,
                            examples = @ExampleObject(value = "URL must include a scheme and host: youtube-dot-com")
                    )
            )
    })
    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shortenUrl(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "The URL to shorten",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ShortenRequest.class),
                            examples = @ExampleObject(
                                    name = "YouTube URL",
                                    value = """
                                            { "url": "https://www.youtube.com/watch?v=dQw4w9WgXcQ" }
                                            """
                            )
                    )
            )
            @RequestBody ShortenRequest request) {

        String shortUrl = urlShortenerService.shorten(request.getUrl());
        ShortenResponse response = new ShortenResponse(request.getUrl(), shortUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


}
