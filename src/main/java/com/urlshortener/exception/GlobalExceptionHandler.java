package com.urlshortener.exception;

import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.urlshortener.models.ErrorResponse;

/**
 * Centralized exception handler for the entire application.
 *
 * Catches all exceptions thrown by any controller and returns a consistent
 * JSON error response structure instead of Spring's default error format.
 *
 * Every error response follows the ErrorResponse structure:
 * {
 *   "status":    400,
 *   "error":     "Bad Request",
 *   "message":   "URL must include a scheme and host",
 *   "timestamp": "2024-01-15T10:30:00Z"
 * }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // -----------------------------------------------------------------------
    // 400 Bad Request
    // -----------------------------------------------------------------------

    /**
     * Handles invalid URL input — missing scheme, blank URL, malformed syntax, etc.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Handles cases where the request body is missing or cannot be parsed as JSON.
     * Example: sending plain text instead of {"url": "..."}.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST,
                "Request body is missing or malformed. Expected JSON: {\"url\": \"https://example.com\"}");
    }

    /**
     * Handles path variable type mismatches.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Invalid value '%s' for parameter '%s'", ex.getValue(), ex.getName());
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    // -----------------------------------------------------------------------
    // 404 Not Found
    // -----------------------------------------------------------------------

    /**
     * Handles lookups for short codes that do not exist in the store.
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Handles requests to routes that do not exist.
     * Requires spring.mvc.throw-exception-if-no-handler-found=true in application.properties.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(NoHandlerFoundException ex) {
        String message = String.format("Route '%s %s' not found", ex.getHttpMethod(), ex.getRequestURL());
        return buildResponse(HttpStatus.NOT_FOUND, message);
    }

    // -----------------------------------------------------------------------
    // 405 Method Not Allowed
    // -----------------------------------------------------------------------

    /**
     * Handles requests using an unsupported HTTP method on a valid route.
     * Example: sending GET /shorten instead of POST /shorten.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        String message = String.format(
                "HTTP method '%s' is not supported for this endpoint. Supported methods: %s",
                ex.getMethod(), ex.getSupportedHttpMethods());
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, message);
    }

    // -----------------------------------------------------------------------
    // 500 Internal Server Error
    // -----------------------------------------------------------------------

    /**
     * Catch-all handler for any unexpected exception not handled above.
     * Prevents stack traces from leaking into API responses.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.");
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message) {
        ErrorResponse body = new ErrorResponse(status.value(), status.getReasonPhrase(), message);
        return ResponseEntity.status(status).body(body);
    }

   
}
