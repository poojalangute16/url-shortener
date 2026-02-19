package com.urlshortener.models;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

// -----------------------------------------------------------------------
// Error response model
// -----------------------------------------------------------------------

/**
 * Uniform error response body returned for all failed requests.
 */
@Schema(description = "Standard error response returned for all failed requests")
public  class ErrorResponse {

    @Schema(description = "HTTP status code", example = "404")
    private final int status;

    @Schema(description = "HTTP status reason phrase", example = "Not Found")
    private final String error;

    @Schema(description = "Human-readable explanation of what went wrong",
            example = "Short code not found: aB3cD4e")
    private final String message;

    @Schema(description = "UTC timestamp of when the error occurred",
            example = "2024-01-15T10:30:00Z")
    private final Instant timestamp;

    public ErrorResponse(int status, String error, String message) {
        this.status    = status;
        this.error     = error;
        this.message   = message;
        this.timestamp = Instant.now();
    }

    public int     getStatus()    { return status; }
    public String  getError()     { return error; }
    public String  getMessage()   { return message; }
    public Instant getTimestamp() { return timestamp; }
}