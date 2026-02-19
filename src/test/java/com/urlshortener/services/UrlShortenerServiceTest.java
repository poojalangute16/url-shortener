package com.urlshortener.services;

import com.urlshortener.models.ShortenedUrl;
import com.urlshortener.repositories.InMemoryUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceTest {

    @Mock
    private InMemoryUrlRepository urlRepository;

    @InjectMocks
    private UrlShortenerService urlShortenerService;

    private final String BASE_URL = "http://localhost:8080";

    @BeforeEach
    void setup() {
        urlShortenerService = new UrlShortenerService(urlRepository, BASE_URL);
    }

    // ----------------------------------------------------
    // 1️⃣ Shorten - New URL
    // ----------------------------------------------------
    @Test
    void shouldShortenNewUrl() {

        when(urlRepository.findByOriginalUrl(anyString()))
                .thenReturn(Optional.empty());

        when(urlRepository.existsByShortCode(anyString()))
                .thenReturn(false);

        String result = urlShortenerService
                .shorten("https://www.youtube.com/watch?v=test");

        assertTrue(result.startsWith(BASE_URL + "/"));
        verify(urlRepository, times(1)).save(any(ShortenedUrl.class));
    }

    // ----------------------------------------------------
    // 2️⃣ Shorten - Idempotent (Already Exists)
    // ----------------------------------------------------
    @Test
    void shouldReturnExistingShortUrlIfAlreadyShortened() {

        ShortenedUrl existing =
                new ShortenedUrl("abc1234",
                        "https://youtube.com",
                        "youtube.com");

        when(urlRepository.findByOriginalUrl("https://youtube.com"))
                .thenReturn(Optional.of(existing));

        String result = urlShortenerService
                .shorten("https://youtube.com");

        assertEquals(BASE_URL + "/abc1234", result);
        verify(urlRepository, never()).save(any());
    }

    // ----------------------------------------------------
    // 3️⃣ Shorten - Blank URL
    // ----------------------------------------------------
    @Test
    void shouldThrowExceptionWhenUrlBlank() {

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class,
                        () -> urlShortenerService.shorten(""));

        assertEquals("URL must not be blank", ex.getMessage());
    }

    // ----------------------------------------------------
    // 4️⃣ Shorten - Malformed URL
    // ----------------------------------------------------
    @Test
    void shouldThrowExceptionWhenUrlMalformed() {

        assertThrows(IllegalArgumentException.class,
                () -> urlShortenerService.shorten("invalid-url"));
    }

    // ----------------------------------------------------
    // 5️⃣ Resolve - Success
    // ----------------------------------------------------
    @Test
    void shouldResolveShortCode() {

        ShortenedUrl url =
                new ShortenedUrl("abc1234",
                        "https://youtube.com",
                        "youtube.com");

        when(urlRepository.findByShortCode("abc1234"))
                .thenReturn(Optional.of(url));

        String result = urlShortenerService.resolve("abc1234");

        assertEquals("https://youtube.com", result);
    }

    // ----------------------------------------------------
    // 6️⃣ Resolve - Not Found
    // ----------------------------------------------------
    @Test
    void shouldThrowExceptionWhenShortCodeNotFound() {

        when(urlRepository.findByShortCode("invalid"))
                .thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> urlShortenerService.resolve("invalid"));
    }

    // ----------------------------------------------------
    // 7️⃣ Get Top Domains
    // ----------------------------------------------------
    @Test
    void shouldReturnTopDomains() {

        List<ShortenedUrl> urls = List.of(
                new ShortenedUrl("a1", "https://youtube.com/a", "youtube.com"),
                new ShortenedUrl("a2", "https://youtube.com/b", "youtube.com"),
                new ShortenedUrl("a3", "https://udemy.com/a", "udemy.com")
        );

        when(urlRepository.findAll()).thenReturn(urls);

        LinkedHashMap<String, Long> result =
                urlShortenerService.getTopDomains(2);

        assertEquals(2, result.get("youtube.com"));
        assertEquals(1, result.get("udemy.com"));
    }

    // ----------------------------------------------------
    // 8️⃣ Get Top Domains - Limit
    // ----------------------------------------------------
    @Test
    void shouldLimitTopDomains() {

        List<ShortenedUrl> urls = List.of(
                new ShortenedUrl("a1", "https://youtube.com/a", "youtube.com"),
                new ShortenedUrl("a2", "https://udemy.com/a", "udemy.com"),
                new ShortenedUrl("a3", "https://wikipedia.org/a", "wikipedia.org")
        );

        when(urlRepository.findAll()).thenReturn(urls);

        LinkedHashMap<String, Long> result =
                urlShortenerService.getTopDomains(2);

        assertEquals(2, result.size());
    }
}
