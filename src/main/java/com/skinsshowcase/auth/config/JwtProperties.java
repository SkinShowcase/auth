package com.skinsshowcase.auth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Настройки JWT: секрет (из env, не хардкодить), срок жизни.
 * Секрет для HS256 — минимум 32 байта (256 бит), в Base64.
 */
@ConfigurationProperties(prefix = "auth.jwt")
@Validated
public class JwtProperties {

    @NotBlank(message = "auth.jwt.secret must be set (e.g. AUTH_JWT_SECRET env)")
    private String secret;

    @Positive
    private long expirationMs = 86400_000L; // 24 часа

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public void setExpirationMs(long expirationMs) {
        this.expirationMs = expirationMs;
    }
}
