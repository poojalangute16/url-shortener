package com.urlshortener.repositories;

import com.urlshortener.models.ShortenedUrl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryUrlRepositoryTest {

    private InMemoryUrlRepository repository;

    @BeforeEach
    void setup() {
        repository = new InMemoryUrlRepository();
    }

    // ----------------------------------------------------
    // 1️⃣ Save and Find By Short Code
    // ----------------------------------------------------
    @Test
    void shouldSaveAndFindByShortCode() {

        ShortenedUrl url =
                new ShortenedUrl("abc1234",
                        "https://youtube.com",
                        "youtube.com");

        repository.save(url);

        Optional<ShortenedUrl> result =
                repository.findByShortCode("abc1234");

        assertTrue(result.isPresent());
        assertEquals("https://youtube.com",
                result.get().getOriginalUrl());
    }

    // ----------------------------------------------------
    // 2️⃣ Save and Find By Original URL
    // ----------------------------------------------------
    @Test
    void shouldFindByOriginalUrl() {

        ShortenedUrl url =
                new ShortenedUrl("abc1234",
                        "https://youtube.com",
                        "youtube.com");

        repository.save(url);

        Optional<ShortenedUrl> result =
                repository.findByOriginalUrl("https://youtube.com");

        assertTrue(result.isPresent());
        assertEquals("abc1234",
                result.get().getShortCode());
    }

    // ----------------------------------------------------
    // 3️⃣ Exists By Short Code
    // ----------------------------------------------------
    @Test
    void shouldReturnTrueIfShortCodeExists() {

        ShortenedUrl url =
                new ShortenedUrl("abc1234",
                        "https://youtube.com",
                        "youtube.com");

        repository.save(url);

        assertTrue(repository.existsByShortCode("abc1234"));
        assertFalse(repository.existsByShortCode("invalid"));
    }

    // ----------------------------------------------------
    // 4️⃣ Find All
    // ----------------------------------------------------
    @Test
    void shouldReturnAllSavedUrls() {

        ShortenedUrl url1 =
                new ShortenedUrl("a1",
                        "https://youtube.com",
                        "youtube.com");

        ShortenedUrl url2 =
                new ShortenedUrl("a2",
                        "https://udemy.com",
                        "udemy.com");

        repository.save(url1);
        repository.save(url2);

        Collection<ShortenedUrl> all = repository.findAll();

        assertEquals(2, all.size());
    }

    // ----------------------------------------------------
    // 5️⃣ Overwrite Same Short Code
    // ----------------------------------------------------
    @Test
    void shouldOverwriteIfSameShortCodeSavedAgain() {

        ShortenedUrl url1 =
                new ShortenedUrl("abc1234",
                        "https://youtube.com",
                        "youtube.com");

        ShortenedUrl url2 =
                new ShortenedUrl("abc1234",
                        "https://udemy.com",
                        "udemy.com");

        repository.save(url1);
        repository.save(url2);

        Optional<ShortenedUrl> result =
                repository.findByShortCode("abc1234");

        assertTrue(result.isPresent());
        assertEquals("https://udemy.com",
                result.get().getOriginalUrl());
    }

}
