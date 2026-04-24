package com.skinsshowcase.auth.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Публичный базовый URL API (обычно gateway), для абсолютных ссылок на пресетные аватарки в JSON.
 */
@Component
@Getter
public class AuthPublicApiProperties {

    /**
     * Без завершающего слэша, например {@code https://api.example.com} или {@code http://localhost:8080}.
     */
    private final String baseUrl;

    public AuthPublicApiProperties(@Value("${auth.public-api.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
