package com.urlshortener.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.controllers.Request.ShortenRequest;
import com.urlshortener.exception.GlobalExceptionHandler;
import com.urlshortener.services.UrlShortenerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller slice tests using JUnit 5 + @WebMvcTest + @MockBean.
 *
 * @WebMvcTest loads only the web layer — no repository or service beans are started.
 * @MockBean replaces UrlShortenerService with a Mockito mock so tests are
 * fast, isolated, and focused purely on HTTP request/response behaviour.
 */
@WebMvcTest(UrlShortenerController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("UrlShortenerController")
class UrlShortenerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UrlShortenerService urlShortenerService;

    // =========================================================================
    // POST /shorten
    // =========================================================================

    @Nested
    @DisplayName("POST /shorten")
    class ShortenEndpointTests {

        @Test
        @DisplayName("returns 201 with originalUrl and shortUrl in body")
        void returns201WithShortenedUrl() throws Exception {
            String originalUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
            String shortUrl    = "http://localhost:8080/aB3cD4e";
            when(urlShortenerService.shorten(originalUrl)).thenReturn(shortUrl);

            mockMvc.perform(post("/shorten")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ShortenRequest(originalUrl))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.originalUrl").value(originalUrl))
                    .andExpect(jsonPath("$.shortUrl").value(shortUrl));

            verify(urlShortenerService, times(1)).shorten(originalUrl);
        }

        @Test
        @DisplayName("returns same shortUrl for duplicate request (idempotent)")
        void returnsSameShortUrlForDuplicateRequest() throws Exception {
            String originalUrl = "https://stackoverflow.com/questions/12345";
            String shortUrl    = "http://localhost:8080/xYz1234";
            when(urlShortenerService.shorten(originalUrl)).thenReturn(shortUrl);

            mockMvc.perform(post("/shorten")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ShortenRequest(originalUrl))))
                    .andExpect(jsonPath("$.shortUrl").value(shortUrl));

            mockMvc.perform(post("/shorten")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ShortenRequest(originalUrl))))
                    .andExpect(jsonPath("$.shortUrl").value(shortUrl));

            verify(urlShortenerService, times(2)).shorten(originalUrl);
        }

        @Test
        @DisplayName("response Content-Type is application/json")
        void returnsJsonContentType() throws Exception {
            when(urlShortenerService.shorten(anyString()))
                    .thenReturn("http://localhost:8080/aB3cD4e");

            mockMvc.perform(post("/shorten")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"url\": \"https://udemy.com/course/java\"}"))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("returns 400 when URL is blank")
        void returns400WhenUrlIsBlank() throws Exception {
            when(urlShortenerService.shorten("  "))
                    .thenThrow(new IllegalArgumentException("URL must not be blank"));

            mockMvc.perform(post("/shorten")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"url\": \"  \"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value("URL must not be blank"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("returns 400 when URL has no scheme")
        void returns400WhenUrlHasNoScheme() throws Exception {
            String badUrl = "youtube.com/watch?v=abc";
            when(urlShortenerService.shorten(badUrl))
                    .thenThrow(new IllegalArgumentException("URL must include a scheme and host: " + badUrl));

            mockMvc.perform(post("/shorten")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ShortenRequest(badUrl))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value("URL must include a scheme and host: " + badUrl));
        }

        @Test
        @DisplayName("returns 400 when request body is missing")
        void returns400WhenRequestBodyIsMissing() throws Exception {
            mockMvc.perform(post("/shorten")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.timestamp").exists());

            verifyNoInteractions(urlShortenerService);
        }

        @Test
        @DisplayName("returns 400 when request body is not valid JSON")
        void returns400WhenRequestBodyIsInvalidJson() throws Exception {
            mockMvc.perform(post("/shorten")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("this is not json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.timestamp").exists());

            verifyNoInteractions(urlShortenerService);
        }


        @Test
        @DisplayName("returns 500 when service throws unexpected exception")
        void returns500OnUnexpectedException() throws Exception {
            when(urlShortenerService.shorten(anyString()))
                    .thenThrow(new RuntimeException("Unexpected internal failure"));

            mockMvc.perform(post("/shorten")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"url\": \"https://youtube.com/watch?v=abc\"}"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.error").value("Internal Server Error"))
                    .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please try again later."))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    // =========================================================================
    // GET /{shortCode}
    // =========================================================================

    @Nested
    @DisplayName("GET /{shortCode}")
    class RedirectEndpointTests {

        @Test
        @DisplayName("returns 302 with Location header pointing to original URL")
        void returns302WithLocationHeader() throws Exception {
            String originalUrl = "https://en.wikipedia.org/wiki/Spring_Framework";
            when(urlShortenerService.resolve("aB3cD4e")).thenReturn(originalUrl);

            mockMvc.perform(get("/aB3cD4e"))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", originalUrl));

            verify(urlShortenerService, times(1)).resolve("aB3cD4e");
        }

        @Test
        @DisplayName("calls service with the exact short code from the path")
        void callsServiceWithExactShortCode() throws Exception {
            when(urlShortenerService.resolve("xYz9876"))
                    .thenReturn("https://udemy.com/course/java");

            mockMvc.perform(get("/xYz9876"))
                    .andExpect(status().isFound());

            verify(urlShortenerService, times(1)).resolve("xYz9876");
            verifyNoMoreInteractions(urlShortenerService);
        }

        @Test
        @DisplayName("returns 404 with error body when short code is not found")
        void returns404WhenShortCodeNotFound() throws Exception {
            when(urlShortenerService.resolve("unknown1"))
                    .thenThrow(new NoSuchElementException("Short code not found: unknown1"));

            mockMvc.perform(get("/unknown1"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value("Short code not found: unknown1"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("returns 500 when service throws unexpected exception")
        void returns500OnUnexpectedException() throws Exception {
            when(urlShortenerService.resolve(anyString()))
                    .thenThrow(new RuntimeException("Unexpected failure"));

            mockMvc.perform(get("/aB3cD4e"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.error").value("Internal Server Error"))
                    .andExpect(jsonPath("$.message")
                            .value("An unexpected error occurred. Please try again later."));
        }
    }

    // =========================================================================
    // GET /metrics/top-domains
    // =========================================================================

    @Nested
    @DisplayName("GET /metrics/top-domains")
    class MetricsEndpointTests {

        @Test
        @DisplayName("returns 200 with domain count map ordered by count descending")
        void returns200WithOrderedDomainMap() throws Exception {
            LinkedHashMap<String, Long> topDomains = new LinkedHashMap<>();
            topDomains.put("udemy.com",     6L);
            topDomains.put("youtube.com",   4L);
            topDomains.put("wikipedia.org", 2L);
            when(urlShortenerService.getTopDomains(3)).thenReturn(topDomains);

            mockMvc.perform(get("/metrics/top-domains"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.['udemy.com']").value(6))
                    .andExpect(jsonPath("$.['youtube.com']").value(4))
                    .andExpect(jsonPath("$.['wikipedia.org']").value(2));

            verify(urlShortenerService, times(1)).getTopDomains(3);
        }

        @Test
        @DisplayName("returns 200 with empty map when nothing has been shortened")
        void returns200WithEmptyMapWhenNothingShortened() throws Exception {
            when(urlShortenerService.getTopDomains(3)).thenReturn(new LinkedHashMap<>());

            mockMvc.perform(get("/metrics/top-domains"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("always passes 3 as the top-N limit to service")
        void alwaysPassesThreeAsTopNLimit() throws Exception {
            when(urlShortenerService.getTopDomains(3)).thenReturn(new LinkedHashMap<>());

            mockMvc.perform(get("/metrics/top-domains"));

            verify(urlShortenerService, times(1)).getTopDomains(3);
            verifyNoMoreInteractions(urlShortenerService);
        }

        @Test
        @DisplayName("returns 500 when service throws unexpected exception")
        void returns500OnUnexpectedException() throws Exception {
            when(urlShortenerService.getTopDomains(anyInt()))
                    .thenThrow(new RuntimeException("Unexpected failure"));

            mockMvc.perform(get("/metrics/top-domains"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.error").value("Internal Server Error"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }
}
