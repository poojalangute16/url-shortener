package com.urlshortener.controllers;

import org.springframework.web.bind.annotation.RestController;

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

}
