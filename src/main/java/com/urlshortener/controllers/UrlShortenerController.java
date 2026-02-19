package com.urlshortener.controllers;


import java.net.URI;
import java.util.LinkedHashMap;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.urlshortener.controllers.Request.ShortenRequest;
import com.urlshortener.controllers.Response.ShortenResponse;
import com.urlshortener.services.UrlShortenerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
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
     private static final int TOP_DOMAINS_COUNT = 3;

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

    // -----------------------------------------------------------------------
    // GET /{shortCode}
    // -----------------------------------------------------------------------

    @Operation(
            summary = "Redirect to original URL",
            description = """
                    Resolves a short code and redirects the caller to the original URL via HTTP 302.
                    
                    Browsers follow this redirect automatically. When using curl, pass `-L` to follow it.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "302",
                    description = "Redirect to the original URL",
                    headers = @Header(
                            name = "Location",
                            description = "The original URL to redirect to",
                            schema = @Schema(type = "string", format = "uri",
                                    example = "https://www.youtube.com/watch?v=dQw4w9WgXcQ")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Short code not found",
                    content = @Content(
                            mediaType = MediaType.TEXT_PLAIN_VALUE,
                            examples = @ExampleObject(value = "Short code not found: abc1234")
                    )
            )
    })
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(
            @Parameter(
                    description = "The 7-character short code generated when the URL was shortened",
                    example = "aB3cD4e",
                    required = true
            )
            @PathVariable String shortCode) {

        String originalUrl = urlShortenerService.resolve(shortCode);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(originalUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

     // -----------------------------------------------------------------------
    // GET /metrics/top-domains
    // -----------------------------------------------------------------------

    @Operation(
            summary = "Top 3 most-shortened domains",
            description = """
                    Returns the top 3 domains by total number of URLs shortened, ordered by count descending.
                    
                    `www.` is stripped when grouping domains, so `www.youtube.com` and `youtube.com`
                    are counted together under `youtube.com`.
                    
                    If fewer than 3 distinct domains have been shortened, only those are returned.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Top domains returned successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    type = "object",
                                    description = "Map of domain name to shortened URL count, ordered highest first",
                                    example = """
                                            {
                                              "udemy.com": 6,
                                              "youtube.com": 4,
                                              "wikipedia.org": 2
                                            }
                                            """
                            ),
                            examples = @ExampleObject(
                                    name = "Top 3 domains",
                                    value = """
                                            {
                                              "udemy.com": 6,
                                              "youtube.com": 4,
                                              "wikipedia.org": 2
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping("/metrics/top-domains")
    public ResponseEntity<LinkedHashMap<String, Long>> getTopDomains() {
        LinkedHashMap<String, Long> topDomains = urlShortenerService.getTopDomains(TOP_DOMAINS_COUNT);
        return ResponseEntity.ok(topDomains);
    }

}
